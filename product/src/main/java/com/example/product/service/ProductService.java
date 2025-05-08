package com.example.product.service;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.commonevents.order.OrderFailedEvent;
import com.example.commonevents.order.OrderedProductDto;
import com.example.commonevents.product.ProductDto;
import com.example.exception.CustomException;
import com.example.exception.errorcode.ProductErrorCode;
import com.example.product.domain.Product;
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

        Product newProduct = productRepository.save(convertToProduct(productDto));
        System.out.println("new :" + newProduct.getId());
        return convertToProductDto(productRepository.save(newProduct));
    }

    public ProductDto updateProduct(ProductDto productDto) {

        Product updatedProduct = productRepository.findById(productDto.getId())
                .orElseThrow(() -> new CustomException(ProductErrorCode.NOT_EXIST_PRODUCT));
        return convertToProductDto(updatedProduct.update(productDto));
    }

    public void checkStock(OrderCreatedEvent event) {

        event.getOrderedProducts().forEach(product -> {
            System.out.println("ordered :" + product.getProductId());
            Product findProduct = productRepository.findById(product.getProductId())
                    .orElseThrow(() -> new CustomException(ProductErrorCode.NOT_EXIST_PRODUCT));
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
            Product findProduct = productRepository.findById(product.getProductId())
                    .orElseThrow(() -> new CustomException(ProductErrorCode.NOT_EXIST_PRODUCT));
            findProduct.buy(product.getQuantity());
        });
    }

    public void restoreStock(OrderedProductDto productDto) {

        Product product = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new CustomException(ProductErrorCode.NOT_EXIST_PRODUCT));
        product.restore(productDto.getQuantity());
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
