package com.example.commonevents.payment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockRestoreEvent {

    private String eventId;
    private String eventType;
    private Long orderId;
}
