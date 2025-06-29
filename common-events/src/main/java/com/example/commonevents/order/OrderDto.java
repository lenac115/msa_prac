package com.example.commonevents.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    public Long id;
    public String orderEventId;
    public Long buyerId;
    public Status status = Status.PENDING;
}
