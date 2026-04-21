package com.jobconnect.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    @Value("${google.client.id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String idTokenString = body.get("idToken");
        String role = body.getOrDefault("role", "CANDIDATE").toUpperCase();

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google token."));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            String googleId = payload.getSubject();

            // Find or create user
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                // Create new user from Google account
                String username = email.split("@")[0] + "_" + googleId.substring(0, 6);
                // Make username unique
                if (userRepository.existsByUsername(username)) {
                    username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
                }
                user = new User(username, passwordEncoder.encode(UUID.randomUUID().toString()),
                        firstName != null ? firstName : email.split("@")[0],
                        lastName != null ? lastName : "",
                        email, "");
                user.setRole(role);
                userRepository.save(user);
            }

            String token = jwtUtil.generateToken(user.getUsername());
            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                              (user.getLastName() != null ? user.getLastName() : "")).trim();

            return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "fullName", fullName.isEmpty() ? user.getUsername() : fullName,
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole() != null ? user.getRole() : "CANDIDATE"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }
}
