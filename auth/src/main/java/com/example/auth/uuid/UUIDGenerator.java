package com.example.auth.uuid;

import java.util.UUID;

public interface UUIDGenerator {

    UUID generateUUID();

    String generateStringUUID();

    UUID fromStringUUID(String stringUUID);
}
