package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "http://localhost:8081")
public interface AuthServiceClient {

    @GetMapping("/auth/common/get/buyer-id")
    Long getBuyerId(@RequestHeader("Authorization") String authorization);

    @GetMapping("/auth/common/get/buyer-email")
    String getBuyerEmail(@RequestHeader("Authorization") String authorization);
}
