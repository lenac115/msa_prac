package com.example.product.domain;

import com.example.commonevents.product.ProductDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private String description;

    private Integer price;

    private Integer stock;

    public Product update(ProductDto productDto) {
        this.price = productDto.getPrice();
        this.stock = productDto.getStock();
        this.productName = productDto.getProductName();

        return this;
    }

    public void buy(Integer quantity) {
        if(quantity < this.stock) {
            this.stock -= quantity;
        } else {
            throw new IllegalArgumentException("quantity must be less than stock");
        }
    }

    public void restore(Integer quantity) {
        this.stock += quantity;
    }

    public boolean checkStock(Integer quantity) {
        return quantity <= this.stock;
    }
}
