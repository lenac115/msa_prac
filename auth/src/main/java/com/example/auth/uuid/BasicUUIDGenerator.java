package com.example.auth.uuid;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BasicUUIDGenerator implements UUIDGenerator {
    @Override
    public UUID generateUUID() {
        return UUID.randomUUID();
    }

    @Override
    public String generateStringUUID() {
        return UUID.randomUUID().toString();
    }

    @Override
    public UUID fromStringUUID(String stringUUID) {
        return UUID.fromString(stringUUID);
    }
}
