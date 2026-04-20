package com.jobconnect.controller;

import com.jobconnect.model.Notification;
import com.jobconnect.model.User;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationRepository.findByUserOrderByCreatedAtDesc(getCurrentUser());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", notificationRepository.countByUserAndReadFalse(getCurrentUser()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead() {
        User user = getCurrentUser();
        List<Notification> notifs = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
        return ResponseEntity.ok(Map.of("message", "All marked as read."));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(n -> {
            n.setRead(true);
            notificationRepository.save(n);
            return ResponseEntity.ok(Map.of("message", "Marked as read."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
