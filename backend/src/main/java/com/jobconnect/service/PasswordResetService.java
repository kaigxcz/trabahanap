package com.jobconnect.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long OTP_TTL_MS = 5 * 60 * 1000L;

    private record OtpEntry(String otp, Instant expiresAt) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.mailSender      = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean sendOtp(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("OTP requested for unknown email: {}", email);
            return false;
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp, Instant.now().plusMillis(OTP_TTL_MS)));
        sendEmailAsync(email, user.getFirstName() != null ? user.getFirstName() : "User", otp);
        return true;
    }

    @Async
    public void sendEmailAsync(String to, String firstName, String otp) {
        try {
            log.info("Sending OTP email to {}", to);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("TrabaHanap — Your Password Reset OTP");
            helper.setText(buildEmailHtml(firstName, otp), true);
            mailSender.send(msg);
            log.info("OTP email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
        }
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return entry.otp().equals(otp);
    }

    public boolean resetPassword(String email, String otp, String newPassword) {
        if (!verifyOtp(email, otp)) return false;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        store.remove(email.toLowerCase());
        return true;
    }

    private String buildEmailHtml(String firstName, String otp) {
        return "<div style='font-family:Inter,sans-serif;max-width:480px;margin:0 auto;background:#f8fafc;padding:32px;border-radius:16px'>"
            + "<div style='background:white;border-radius:12px;padding:32px;box-shadow:0 2px 12px rgba(0,0,0,0.06)'>"
            + "<h2 style='margin:0 0 8px;color:#0f172a;font-size:20px'>Password Reset Request</h2>"
            + "<p style='color:#64748b;margin:0 0 24px'>Hi " + firstName + ", use the OTP below. It expires in 5 minutes.</p>"
            + "<div style='background:#f5f3ff;border:2px dashed #4f46e5;border-radius:12px;padding:20px;text-align:center;margin-bottom:24px'>"
            + "<span style='font-size:36px;font-weight:900;letter-spacing:12px;color:#4f46e5'>" + otp + "</span>"
            + "</div>"
            + "<p style='color:#94a3b8;font-size:13px;margin:0'>If you did not request this, ignore this email.</p>"
            + "</div></div>";
    }
}
