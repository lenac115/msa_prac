package com.example.commonevents.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderedProductDto {

    private Long id;
    private Long productId;
    private Long orderId;
    private Integer quantity;
    private Integer price;
}
