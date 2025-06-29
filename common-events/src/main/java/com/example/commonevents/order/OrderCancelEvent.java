package com.example.commonevents.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelEvent {

    private String eventId;
    private String eventType;
    private String orderEventId;
    private Instant timestamp;
}
