package com.jobconnect.controller;

import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String firstName = body.get("firstName");
        String middleName = body.getOrDefault("middleName", "");
        String lastName = body.get("lastName");
        String email = body.get("email");
        String location = body.get("location");
        String role = body.getOrDefault("role", "CANDIDATE").toUpperCase();

        if (!role.equals("CANDIDATE") && !role.equals("EMPLOYER")) role = "CANDIDATE";

        if (username == null || password == null || firstName == null || lastName == null || location == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All required fields must be filled."));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
        }

        User user = new User(username, passwordEncoder.encode(password), firstName, lastName, email, location);
        user.setRole(role);
        if (!middleName.isBlank()) user.setMiddleName(middleName);
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        userRepository.save(user);

        String fullName = firstName
            + (middleName != null && !middleName.isBlank() ? " " + middleName : "")
            + " " + lastName;

        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", username,
            "fullName", fullName.trim(),
            "location", location,
            "email", email != null ? email : "",
            "role", role
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
        }

        User user = userOpt.get();
        String mn = user.getMiddleName() != null && !user.getMiddleName().isBlank() ? " " + user.getMiddleName() : "";
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "")
                          + mn + " "
                          + (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (fullName.isEmpty()) fullName = username;
        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", username,
            "fullName", fullName,
            "location", user.getLocation() != null ? user.getLocation() : "",
            "email", user.getEmail() != null ? user.getEmail() : "",
            "jobTitle", user.getJobTitle() != null ? user.getJobTitle() : "",
            "role", user.getRole() != null ? user.getRole() : "CANDIDATE"
        ));
    }
}
