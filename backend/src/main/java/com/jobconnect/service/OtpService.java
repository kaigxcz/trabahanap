package com.jobconnect.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    // email -> { otp, expiresAt }
    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Generate a 6-digit OTP, store it, and send to the given email. */
    public void sendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        long expiresAt = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
        otpStore.put(email, new long[]{ Long.parseLong(otp), expiresAt });

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("trabahanap2026@gmail.com");
        msg.setTo(email);
        msg.setSubject("TrabaHanap — Your Verification Code");
        msg.setText(
            "Hello!\n\n" +
            "Your TrabaHanap verification code is:\n\n" +
            "  " + otp + "\n\n" +
            "This code expires in 5 minutes. Do not share it with anyone.\n\n" +
            "— TrabaHanap Team"
        );
        mailSender.send(msg);
    }

    /** Returns true if the OTP matches and has not expired. Removes it after use. */
    public boolean verifyOtp(String email, String otp) {
        long[] stored = otpStore.get(email);
        if (stored == null) return false;
        boolean valid = String.valueOf((long) stored[0]).equals(otp.trim())
                     && System.currentTimeMillis() < stored[1];
        if (valid) otpStore.remove(email);
        return valid;
    }
}
