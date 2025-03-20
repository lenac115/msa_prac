package com.example.auth.kafka;

import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserInfo(UserDto event) {

        kafkaTemplate.send("user-info-topic", event);
        log.info("send user info to kafka topic: {}" , event);
    }

    public void sendUserList(List<UserDto> users) {
        kafkaTemplate.send("user-list-topic", users);
        log.info("send user list to kafka topic: {}" , users);
    }
}
