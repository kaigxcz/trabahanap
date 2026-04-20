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
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        userRepository.save(user);

        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", username,
            "fullName", firstName + " " + lastName,
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
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName() != null ? user.getLastName() : "")).trim();
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
