package com.teamwork.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gửi email qua SMTP (mặc định Gmail). Thông tin đăng nhập đọc từ {@code mail.properties}
 * trên classpath (file này nằm trong .gitignore; xem mail.properties.example).
 */
public class MailUtil {

    private static final Logger LOGGER = Logger.getLogger(MailUtil.class.getName());

    private MailUtil() {
    }

    /**
     * Gửi mã OTP đặt lại mật khẩu.
     *
     * @return true nếu gửi thành công, false nếu thiếu cấu hình hoặc SMTP lỗi
     */
    public static boolean sendOtp(String toEmail, String fullName, String otp) {
        Properties cfg = loadConfig();
        if (cfg == null) {
            return false;
        }
        final String user = cfg.getProperty("mail.user", "").trim();
        final String pass = cfg.getProperty("mail.pass", "").replace(" ", "");
        if (user.isEmpty() || pass.isEmpty()) {
            LOGGER.severe("mail.properties thiếu mail.user hoặc mail.pass — không thể gửi OTP.");
            return false;
        }

        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", cfg.getProperty("mail.host", "smtp.gmail.com"));
        smtp.put("mail.smtp.port", cfg.getProperty("mail.port", "587"));
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.connectiontimeout", "10000");
        smtp.put("mail.smtp.timeout", "10000");
        smtp.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(smtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(user, "TeamWork Hub", "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            msg.setSubject("Mã OTP đặt lại mật khẩu TeamWork Hub", "UTF-8");
            String name = (fullName == null || fullName.isBlank()) ? "bạn" : fullName;
            msg.setText("Xin chào " + name + ",\n\n"
                    + "Mã OTP đặt lại mật khẩu của bạn là: " + otp + "\n\n"
                    + "Mã có hiệu lực trong 3 phút. Bạn được nhập sai tối đa 3 lần.\n"
                    + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n"
                    + "TeamWork Hub", "UTF-8");
            Transport.send(msg);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gửi OTP thất bại tới " + toEmail, e);
            return false;
        }
    }

    private static Properties loadConfig() {
        try (InputStream in = MailUtil.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in == null) {
                LOGGER.severe("Không tìm thấy mail.properties trên classpath (sao chép từ mail.properties.example).");
                return null;
            }
            Properties p = new Properties();
            p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            return p;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Không đọc được mail.properties", e);
            return null;
        }
    }
}
