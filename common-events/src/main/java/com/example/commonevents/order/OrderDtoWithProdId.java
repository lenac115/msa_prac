package com.example.commonevents.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDtoWithProdId {
    public Long id;
    public String orderEventId;
    public Long buyerId;
    public List<Long> productId;
    public Status status = Status.PENDING;
}
