package com.jobconnect.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobconnect.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService resetService;

    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    /** POST /api/auth/forgot-password  { email } */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        try {
            resetService.sendOtp(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of("message", "If that email is registered, an OTP has been sent."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to send OTP. Please try again."));
        }
    }

    /** POST /api/auth/verify-otp  { email, otp } */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");
        if (email == null || otp == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email and OTP are required."));
        boolean valid = resetService.verifyOtp(email.trim().toLowerCase(), otp.trim());
        if (!valid)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP."));
        return ResponseEntity.ok(Map.of("message", "OTP verified."));
    }

    /** POST /api/auth/reset-password  { email, otp, newPassword } */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String otp         = body.get("otp");
        String newPassword = body.get("newPassword");
        if (email == null || otp == null || newPassword == null)
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));
        if (newPassword.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters."));
        boolean ok = resetService.resetPassword(email.trim().toLowerCase(), otp.trim(), newPassword);
        if (!ok)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP."));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
