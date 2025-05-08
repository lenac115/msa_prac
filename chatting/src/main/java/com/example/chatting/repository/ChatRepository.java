package com.example.chatting.repository;

import com.example.chatting.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select c from ChatMessage c where c.orderId = :orderId")
    List<ChatMessage> findByOrderId(Long orderId);
}
