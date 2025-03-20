package com.example.product.domain;

import com.example.product.dto.ProductDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

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

    public boolean checkStock(Integer quantity) {
        return quantity <= this.stock;
    }
}
