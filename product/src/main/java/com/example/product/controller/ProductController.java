package com.example.product.controller;

import com.example.commonevents.product.ProductDto;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/buyer/new")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(productDto));
    }

    @PostMapping("/buyer/update")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto) {

        return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(productDto));
    }

    @GetMapping("/common/get/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.OK).body(productService.getProduct(id));
    }

    @GetMapping("/common/get/list")
    public ResponseEntity<List<ProductDto>> getProductList() {

        return ResponseEntity.status(HttpStatus.OK).body(productService.getList());
    }
}
