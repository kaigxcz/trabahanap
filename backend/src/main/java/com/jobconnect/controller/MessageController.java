package com.jobconnect.controller;

import com.jobconnect.model.Message;
import com.jobconnect.model.User;
import com.jobconnect.repository.MessageRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, String> body) {
        User sender = getCurrentUser();
        User receiver = userRepository.findByUsername(body.get("receiverUsername")).orElseThrow();
        Message msg = new Message(sender, receiver, body.getOrDefault("subject", ""), body.get("content"));
        messageRepository.save(msg);
        return ResponseEntity.ok(Map.of("message", "Message sent!"));
    }

    @GetMapping("/inbox")
    public List<Map<String, Object>> getInbox() {
        User user = getCurrentUser();
        return messageRepository.findConversations(user).stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("senderUsername", m.getSender().getUsername());
            map.put("senderName", m.getSender().getFirstName() + " " + m.getSender().getLastName());
            map.put("receiverUsername", m.getReceiver().getUsername());
            map.put("subject", m.getSubject());
            map.put("content", m.getContent());
            map.put("sentAt", m.getSentAt());
            map.put("isRead", m.isRead());
            map.put("isMine", m.getSender().getId().equals(user.getId()));
            return map;
        }).toList();
    }

    @GetMapping("/conversation/{username}")
    public List<Map<String, Object>> getConversation(@PathVariable String username) {
        User user = getCurrentUser();
        User other = userRepository.findByUsername(username).orElseThrow();
        return messageRepository.findConversationBetween(user, other).stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("senderUsername", m.getSender().getUsername());
            map.put("senderName", m.getSender().getFirstName() + " " + m.getSender().getLastName());
            map.put("content", m.getContent());
            map.put("sentAt", m.getSentAt());
            map.put("isMine", m.getSender().getId().equals(user.getId()));
            if (!m.isRead() && m.getReceiver().getId().equals(user.getId())) {
                m.setRead(true);
                messageRepository.save(m);
            }
            return map;
        }).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", messageRepository.countByReceiverAndIsReadFalse(getCurrentUser()));
    }
}
