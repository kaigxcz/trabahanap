package com.jobconnect.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long OTP_TTL_MS = 5 * 60 * 1000L; // 5 minutes

    private record OtpEntry(String otp, Instant expiresAt) {}

    // In-memory store: email -> OtpEntry (sufficient for single-instance deployments)
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

    /** Generate OTP, store it, and send email. Returns false if email not found. */
    public boolean sendOtp(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("OTP requested for unknown email: {}", email);
            return false;
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp, Instant.now().plusMillis(OTP_TTL_MS)));
        log.info("Sending OTP to {}", email);
        try {
            sendOtpEmail(email, user.getFirstName() != null ? user.getFirstName() : "User", otp);
            log.info("OTP email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw e;
        }
        return true;
    }

    /** Returns true if OTP is valid and not expired. */
    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return entry.otp().equals(otp);
    }

    /** Resets the password. Call only after verifyOtp returns true. */
    public boolean resetPassword(String email, String otp, String newPassword) {
        if (!verifyOtp(email, otp)) return false;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        store.remove(email.toLowerCase());
        return true;
    }

    private void sendOtpEmail(String to, String firstName, String otp) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("TrabaHanap — Your Password Reset OTP");
        helper.setText(buildEmailHtml(firstName, otp), true);
        mailSender.send(msg);
    }

    private String buildEmailHtml(String firstName, String otp) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:480px;margin:0 auto;background:#f8fafc;padding:32px;border-radius:16px">
              <div style="text-align:center;margin-bottom:24px">
                <div style="display:inline-flex;align-items:center;gap:8px">
                  <div style="width:36px;height:36px;background:#4f46e5;border-radius:10px;display:inline-flex;align-items:center;justify-content:center">
                    <span style="color:white;font-weight:900;font-size:18px">T</span>
                  </div>
                  <span style="font-weight:800;font-size:20px;color:#0f172a">TrabaHanap</span>
                </div>
              </div>
              <div style="background:white;border-radius:12px;padding:32px;box-shadow:0 2px 12px rgba(0,0,0,0.06)">
                <h2 style="margin:0 0 8px;color:#0f172a;font-size:20px">Password Reset Request</h2>
                <p style="color:#64748b;margin:0 0 24px">Hi %s, use the OTP below to reset your password. It expires in 5 minutes.</p>
                <div style="background:#f5f3ff;border:2px dashed #4f46e5;border-radius:12px;padding:20px;text-align:center;margin-bottom:24px">
                  <span style="font-size:36px;font-weight:900;letter-spacing:12px;color:#4f46e5">%s</span>
                </div>
                <p style="color:#94a3b8;font-size:13px;margin:0">If you didn't request this, you can safely ignore this email.</p>
              </div>
            </div>
            """.formatted(firstName, otp);
    }
}
