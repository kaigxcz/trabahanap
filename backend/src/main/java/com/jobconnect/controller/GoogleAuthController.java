package com.jobconnect.controller;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.security.JwtUtil;

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
                // New Google user — return their info so the frontend can collect missing fields
                return ResponseEntity.ok(Map.of(
                    "needsRegistration", true,
                    "email",     email != null ? email : "",
                    "firstName", firstName != null ? firstName : "",
                    "lastName",  lastName  != null ? lastName  : "",
                    "googleId",  googleId
                ));
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

    @PostMapping("/google/complete")
    public ResponseEntity<?> googleComplete(@RequestBody Map<String, String> body) {
        String email      = body.get("email");
        String firstName  = body.get("firstName");
        String middleName = body.get("middleName");
        String lastName   = body.get("lastName");
        String suffix     = body.get("suffix");
        String username   = body.get("username");
        String location   = body.get("location");
        String phone      = body.get("phone");
        String role       = body.getOrDefault("role", "CANDIDATE").toUpperCase();

        if (email == null || username == null || location == null || location.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields."));
        if (userRepository.existsByUsername(username))
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
        if (userRepository.findByEmail(email).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error", "An account with this email already exists."));

        User user = new User(username, passwordEncoder.encode(UUID.randomUUID().toString()),
                firstName != null ? firstName : email.split("@")[0],
                lastName  != null ? lastName  : "",
                email, location);
        user.setRole(role);
        if (middleName != null && !middleName.isBlank()) user.setMiddleName(middleName);
        if (suffix     != null && !suffix.isBlank())     user.setSuffix(suffix);
        if (phone      != null && !phone.isBlank())      user.setPhone(phone);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName()  != null ? user.getLastName()  : "")).trim();
        return ResponseEntity.ok(Map.of(
            "token",    token,
            "username", user.getUsername(),
            "fullName", fullName.isEmpty() ? user.getUsername() : fullName,
            "email",    user.getEmail(),
            "role",     user.getRole()
        ));
    }
}
