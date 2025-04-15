package com.example.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWebTestClient
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    static GenericContainer<?> authService = new GenericContainer<>("auth-service:latest")
                    .withExposedPorts(8081);

    @Container
    static GenericContainer<?> orderService = new GenericContainer<>("order-service:latest")
            .withExposedPorts(8082);

    @Container
    static GenericContainer<?> paymentService = new GenericContainer<>("payment-service:latest")
            .withExposedPorts(8083);

    @Container
    static GenericContainer<?> productService = new GenericContainer<>("product-service:latest")
            .withExposedPorts(8084);


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("auth-service.url", () -> "http://" + authService.getHost() + ":" + authService.getMappedPort(8081));
        registry.add("order-service.url", () -> "http://" + orderService.getHost() + ":" + orderService.getMappedPort(8082));
        registry.add("payment-service.url", () -> "http://" + paymentService.getHost() + ":" + paymentService.getMappedPort(8083));
        registry.add("product-service.url", () -> "http://" + productService.getHost() + ":" + productService.getMappedPort(8084));
    }
}