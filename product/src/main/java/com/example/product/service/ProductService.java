package com.example.product.service;

import com.example.product.domain.Product;
import com.example.product.dto.OrderCreatedEvent;
import com.example.product.dto.OrderFailedEvent;
import com.example.product.dto.PaymentCreatedEvent;
import com.example.product.dto.ProductDto;
import com.example.product.kafka.ProductProducer;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductProducer productProducer;

    public ProductDto create(ProductDto productDto) {

        Product newProduct = convertToProduct(productDto);
        return convertToProductDto(productRepository.save(newProduct));
    }

    public ProductDto updateProduct(ProductDto productDto) {

        Product updatedProduct = productRepository.findById(productDto.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToProductDto(updatedProduct.update(productDto));
    }

    public void checkStock(OrderCreatedEvent event) {

        event.getOrderedProducts().forEach(product -> {
            Product findProduct = productRepository.findById(product.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
            if (!findProduct.checkStock(product.getQuantity())) {
                productProducer.sendOrderFailedEvent(OrderFailedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("ORDER_FAILED")
                        .orderId(event.getOrderId())
                        .timestamp(Instant.now())
                        .build());
            }
        });

        reduceStock(event);
    }

    public void reduceStock(OrderCreatedEvent event) {

        event.getOrderedProducts().forEach(product -> {
            Product findProduct = productRepository.findById(product.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
            findProduct.buy(product.getQuantity());
        });


        productProducer.sendPaymentCreated(PaymentCreatedEvent.builder()
                .orderId(event.getOrderId())
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .eventType("PAYMENT_CREATED")
                .buyerId(event.getBuyerId())
                .amount(event.getOrderedProducts().stream().mapToInt(product ->
                        product.getPrice() * product.getQuantity()).sum())
                .build());
    }


    private Product convertToProduct(ProductDto productDto) {
        return Product.builder()
                .id(productDto.getId())
                .stock(productDto.getStock())
                .productName(productDto.getProductName())
                .price(productDto.getPrice())
                .build();
    }

    private ProductDto convertToProductDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .stock(product.getStock())
                .productName(product.getProductName())
                .price(product.getPrice())
                .build();
    }
}
