package com.teamwork.util;

import com.teamwork.business.User;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tìm những người được nhắc tên (@mention) trong một tin nhắn / bình luận.
 *
 * <p>Ô chat chèn {@code @} + HỌ TÊN ĐẦY ĐỦ (có khoảng trắng, vd {@code @Bob Tran}); người dùng cũng có thể gõ
 * {@code @username}. Vì tên có khoảng trắng nên không tách theo từ được: tại mỗi dấu {@code @}, chọn tên DÀI NHẤT
 * trong danh sách thành viên khớp ngay sau đó (để {@code @Bob Tran} không bị hiểu là {@code @Bob}),
 * và ký tự tiếp theo phải không phải chữ/số (để {@code @Anh} không khớp {@code @Anhthu}).</p>
 *
 * <p>So khớp không phân biệt hoa thường và dạng Unicode của dấu tiếng Việt (NFC).</p>
 */
public final class MentionParser {

    private MentionParser() {}

    private static String norm(String s) {
        return s == null ? "" : Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    /** ID những người trong {@code candidates} được nhắc trong {@code text} (giữ thứ tự xuất hiện, không trùng). */
    public static Set<Integer> findMentionedUserIds(String text, Collection<User> candidates) {
        Set<Integer> result = new LinkedHashSet<>();
        if (text == null || text.indexOf('@') < 0 || candidates == null || candidates.isEmpty()) return result;

        // (tên đã chuẩn hóa, userId) — gồm cả họ tên và username; tên dài trước để ưu tiên khớp dài nhất
        List<String[]> names = new ArrayList<>();
        for (User u : candidates) {
            if (u == null) continue;
            for (String n : new String[] {u.getFullName(), u.getUsername()}) {
                String k = norm(n).trim();
                if (!k.isEmpty()) names.add(new String[] {k, String.valueOf(u.getId())});
            }
        }
        names.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));

        String t = norm(text);
        for (int at = t.indexOf('@'); at >= 0; at = t.indexOf('@', at + 1)) {
            // "ten@domain.com" là email, không phải nhắc tên
            if (at > 0 && Character.isLetterOrDigit(t.charAt(at - 1))) continue;
            int start = at + 1;
            int matchedLength = -1;
            for (String[] n : names) {
                String name = n[0];
                if (matchedLength >= 0 && name.length() < matchedLength) break;   // đã có tên dài hơn khớp
                if (!t.startsWith(name, start)) continue;
                int end = start + name.length();
                if (end < t.length() && Character.isLetterOrDigit(t.charAt(end))) continue;
                // Hai người cùng ứng với một tên (vd họ tên "Anh" và username "anh") → báo cả hai, không đoán
                matchedLength = name.length();
                result.add(Integer.parseInt(n[1]));
            }
        }
        return result;
    }
}
