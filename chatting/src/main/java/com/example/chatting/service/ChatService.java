package com.example.chatting.service;

import com.example.chatting.domain.ChatMessage;
import com.example.chatting.repository.ChatRepository;
import com.example.commonevents.chatting.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatMessageRepository;

    public ChatMessage saveMessage(ChatMessageDto dto) {
        ChatMessage entity = ChatMessage.builder()
                .orderId(dto.getOrderId())
                .sentAt(LocalDateTime.now())
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .content(dto.getContent())
                .build();
        return chatMessageRepository.save(entity);
    }
    public List<ChatMessage> getMessagesByOrder(Long orderId) {
        return chatMessageRepository.findByOrderId(orderId);
    }
}