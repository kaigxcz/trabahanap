package com.jobconnect.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobconnect.model.User;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.security.JwtUtil;

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
        String suffix = body.getOrDefault("suffix", "");
        String email = body.get("email");
        String location = body.get("location");
        String role = body.getOrDefault("role", "CANDIDATE").toUpperCase();

        if (!role.equals("CANDIDATE") && !role.equals("EMPLOYER")) role = "CANDIDATE";

        if (username == null || password == null || firstName == null || lastName == null || location == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All required fields must be filled."));
        }

        // Name validation
        String namePattern = "^[^0-9]{2,20}$";
        if (!firstName.matches(namePattern) || firstName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "First name must be 2-20 characters with no numbers."));
        }
        if (!lastName.matches(namePattern) || lastName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Last name must be 2-20 characters with no numbers."));
        }
        long firstSpaces = firstName.chars().filter(c -> c == ' ').count();
        long lastSpaces = lastName.chars().filter(c -> c == ' ').count();
        if (firstSpaces > 2 || lastSpaces > 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name cannot contain more than 2 spaces."));
        }

        // Email validation: @gmail.com only
        if (email == null || !email.endsWith("@gmail.com") || email.equals("@gmail.com")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only @gmail.com email addresses are allowed."));
        }

        // Phone validation: 11 digits, no 4+ consecutive same digit
        String phone = body.getOrDefault("phone", "");
        if (!phone.isEmpty()) {
            if (!phone.matches("\\d{10}")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number must be exactly 10 digits."));
            }
            if (phone.matches(".*(\\d)\\1{3,}.*")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number cannot have 4 or more consecutive identical digits."));
            }
        }
        if (username.length() < 3 || username.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be between 3 and 20 characters."));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken."));
        }

        User user = new User(username, passwordEncoder.encode(password), firstName, lastName, email, location);
        user.setRole(role);
        if (!middleName.isBlank()) user.setMiddleName(middleName);
        if (!suffix.isBlank()) user.setSuffix(suffix);
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        if (body.containsKey("middleName") && !body.get("middleName").isEmpty()) user.setMiddleName(body.get("middleName"));
        if (body.containsKey("birthday") && !body.get("birthday").isEmpty()) {
            user.setBirthday(body.get("birthday"));
            // Compute and validate age
            try {
                java.time.LocalDate birth = java.time.LocalDate.parse(body.get("birthday"));
                int age = java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
                if (age < 18) return ResponseEntity.badRequest().body(Map.of("error", "You must be at least 18 years old."));
                if (age > 50) return ResponseEntity.badRequest().body(Map.of("error", "Age must not exceed 50 years."));
                user.setAge(age);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid birthday format."));
            }
        }
        userRepository.save(user);

        String fullName = firstName
            + (middleName != null && !middleName.isBlank() ? " " + middleName : "")
            + " " + lastName
            + (suffix != null && !suffix.isBlank() ? " " + suffix : "");

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
        String sfx = user.getSuffix() != null && !user.getSuffix().isBlank() ? " " + user.getSuffix() : "";
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "")
                          + mn + " "
                          + (user.getLastName() != null ? user.getLastName() : "")
                          + sfx).trim();
        if (fullName.isEmpty()) fullName = username;
        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "username", username,
            "fullName", fullName,
            "location", user.getLocation() != null ? user.getLocation() : "",
            "email", user.getEmail() != null ? user.getEmail() : "",
            "jobTitle", user.getJobTitle() != null ? user.getJobTitle() : "",
            "skills", user.getSkills() != null ? user.getSkills() : "",
            "role", user.getRole() != null ? user.getRole() : "CANDIDATE"
        ));
    }
}
