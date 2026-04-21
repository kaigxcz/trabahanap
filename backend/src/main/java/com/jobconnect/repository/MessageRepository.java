package com.jobconnect.repository;

import com.jobconnect.model.Message;
import com.jobconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.receiver = :user) ORDER BY m.sentAt DESC")
    List<Message> findConversations(@Param("user") User user);

    @Query("SELECT m FROM Message m WHERE (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1) ORDER BY m.sentAt ASC")
    List<Message> findConversationBetween(@Param("u1") User u1, @Param("u2") User u2);

    long countByReceiverAndIsReadFalse(User receiver);
}
