package com.example.chatting.controller;

import com.example.chatting.domain.ChatMessage;
import com.example.chatting.service.ChatService;
import com.example.commonevents.chatting.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send.{orderId}")
    @SendTo("/topic/chat/{orderId}")
    public ChatMessage sendMessage(@DestinationVariable Long orderId, ChatMessageDto message) {
        message.setTimestamp(LocalDateTime.now());
        return chatService.saveMessage(message); // DB 저장
    }

    @GetMapping("/messages/{orderId}")
    public List<ChatMessage> getMessagesByOrder(@PathVariable Long orderId) {
        return chatService.getMessagesByOrder(orderId);
    }
}