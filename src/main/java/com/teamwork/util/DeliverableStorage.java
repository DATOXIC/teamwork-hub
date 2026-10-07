package com.teamwork.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lưu và đọc tệp bàn giao (deliverable) của công việc.
 *
 * <ul>
 *   <li>Lưu NGOÀI thư mục webapp (không ai mở trực tiếp bằng URL được): mặc định {@code ~/teamwork-hub-uploads},
 *       đổi bằng biến môi trường {@code TEAMWORK_UPLOAD_DIR} hoặc {@code -Dteamwork.upload.dir=...}.
 *       Mỗi task một thư mục con {@code task-<id>}.</li>
 *   <li>Tên trên đĩa = {@code <32 ký tự hex ngẫu nhiên>__<tên gốc đã làm sạch>}: không đoán được, không trùng,
 *       không chứa "/" hay "..", và vẫn giữ tên gốc để hiển thị / tải về.</li>
 *   <li>Chỉ nhận các đuôi tệp trong {@link #ALLOWED}; tối đa {@link #MAX_BYTES}.</li>
 *   <li>Tải về luôn ở dạng tệp đính kèm (Content-Disposition: attachment) qua {@code TaskFileServlet},
 *       servlet đó kiểm tra quyền thành viên dự án.</li>
 * </ul>
 */
public final class DeliverableStorage {

    private DeliverableStorage() {}

    /** 20 MB — khớp với @MultipartConfig của TaskServlet. */
    public static final long MAX_BYTES = 20L * 1024 * 1024;

    /** Đuôi tệp → kiểu MIME khi tải về. */
    public static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "zip", "application/zip",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "txt", "text/plain");

    /** Danh sách đuôi cho thuộc tính accept của &lt;input type=file&gt; và thông báo lỗi. */
    public static final String ACCEPT = ".pdf,.zip,.docx,.pptx,.xlsx,.png,.jpg,.jpeg,.txt";

    private static final Pattern STORED = Pattern.compile("^([0-9a-f]{32})__([\\p{L}\\p{M}\\p{N} _.()\\-]{1,150})$");

    /** Lỗi người dùng cần biết (đuôi không hợp lệ, quá lớn, rỗng...). */
    public static class InvalidFileException extends Exception {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    /** Thư mục gốc lưu tệp bàn giao. */
    public static Path baseDir() {
        String dir = System.getProperty("teamwork.upload.dir");
        if (dir == null || dir.isBlank()) dir = System.getenv("TEAMWORK_UPLOAD_DIR");
        if (dir == null || dir.isBlank()) dir = Paths.get(System.getProperty("user.home"), "teamwork-hub-uploads").toString();
        return Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Giá trị cột deliverable_file là tệp đã tải lên (không phải link / tên gõ tay kiểu cũ). */
    public static boolean isStoredName(String value) {
        return value != null && STORED.matcher(value).matches();
    }

    /** Tên hiển thị (tên gốc) của tệp đã tải lên; giá trị kiểu cũ thì trả nguyên văn. */
    public static String displayName(String value) {
        if (value == null) return "";
        Matcher m = STORED.matcher(value);
        return m.matches() ? m.group(2) : value;
    }

    /** Đuôi tệp viết thường, không có dấu chấm; "" nếu không có. */
    static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot < 0 || dot == name.length() - 1) ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Làm sạch tên gốc: bỏ đường dẫn (C:\...\, ../), bỏ ký tự lạ, giữ chữ có dấu tiếng Việt, cắt tối đa 150 ký tự
     * nhưng giữ đuôi tệp.
     */
    static String sanitize(String original) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = Normalizer.normalize(name, Normalizer.Form.NFC);
        name = name.replaceAll("[^\\p{L}\\p{M}\\p{N} _.()\\-]", "_").replaceAll("\\.{2,}", ".").trim();
        name = name.replaceAll("^[ .]+", "");
        if (name.length() > 150) {
            String ext = extensionOf(name);
            String stem = name.substring(0, 150 - ext.length() - 1);
            name = stem + "." + ext;
        }
        return name;
    }

    /**
     * Kiểm tra rồi lưu tệp người dùng tải lên.
     *
     * @return tên lưu trữ (ghi vào cột deliverable_file)
     */
    public static String store(int taskId, String originalName, long size, InputStream content)
            throws InvalidFileException, IOException {
        if (taskId <= 0) throw new InvalidFileException("Công việc không hợp lệ.");
        if (size <= 0) throw new InvalidFileException("Tệp rỗng, vui lòng chọn lại.");
        if (size > MAX_BYTES) throw new InvalidFileException("Tệp vượt quá giới hạn 20 MB.");
        String clean = sanitize(originalName);
        String ext = extensionOf(clean);
        if (clean.isEmpty() || !ALLOWED.containsKey(ext)) {
            throw new InvalidFileException("Chỉ nhận tệp " + ACCEPT.replace(",", ", ") + ".");
        }
        String stored = UUID.randomUUID().toString().replace("-", "") + "__" + clean;
        Path dir = taskDir(taskId);
        Files.createDirectories(dir);
        Path target = dir.resolve(stored);
        Path tmp = dir.resolve(stored + ".part");
        try {
            long copied = Files.copy(content, tmp, StandardCopyOption.REPLACE_EXISTING);
            if (copied > MAX_BYTES) throw new InvalidFileException("Tệp vượt quá giới hạn 20 MB.");
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return stored;
    }

    /**
     * Đường dẫn tới tệp đã lưu của task, hoặc null nếu tên không hợp lệ / tệp không tồn tại.
     * Luôn kiểm tra đường dẫn cuối cùng vẫn nằm trong thư mục của task (chống ../).
     */
    public static Path resolve(int taskId, String storedName) {
        if (taskId <= 0 || !isStoredName(storedName)) return null;
        Path dir = taskDir(taskId);
        Path file = dir.resolve(storedName).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) return null;
        return file;
    }

    /** Xóa tệp cũ khi task được nộp lại kèm tệp mới (lỗi xóa chỉ để lại tệp mồ côi, không ảnh hưởng người dùng). */
    public static void deleteQuietly(int taskId, String storedName) {
        Path file = resolve(taskId, storedName);
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // bỏ qua
        }
    }

    /** Kiểu MIME khi tải về theo đuôi tệp. */
    public static String contentType(String storedName) {
        return ALLOWED.getOrDefault(extensionOf(storedName), "application/octet-stream");
    }

    private static Path taskDir(int taskId) {
        return baseDir().resolve("task-" + taskId);
    }
}
