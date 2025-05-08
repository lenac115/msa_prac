package com.example.commonevents.chatting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long orderId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime timestamp;
}
