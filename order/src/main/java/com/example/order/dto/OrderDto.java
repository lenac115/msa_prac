package com.example.order.dto;

import com.example.order.domain.Order;
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
    public Long buyerId;
    public Order.Status status = Order.Status.PENDING;
}
