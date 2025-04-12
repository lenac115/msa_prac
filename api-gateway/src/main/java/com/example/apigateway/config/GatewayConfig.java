package com.example.apigateway.config;

import com.example.apigateway.jwt.JwtFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, JwtFilter jwtFilter) {
        return builder.routes()
                /*.route("auth-service",
                        r -> r.path("/auth/login", "/auth/register",
                                        "/auth/reissue", "/auth/send/reset-password-email",
                                        "/auth/reset-password-page", "")
                        .uri("http://localhost:8081"))*/
                .route("auth-service", r -> r.path("/auth/**")
                        .filters(f -> f.filter(jwtFilter))
                        .uri("http://localhost:8081"))
                .route("order-service", r -> r.path("/order/**")
                        .filters(f -> f.filter(jwtFilter))
                        .uri("http://localhost:8082"))
                .route("payment-service", r -> r.path("/payment/**")
                        .filters(f -> f.filter(jwtFilter))
                        .uri("http://localhost:8083"))
                .route("product-service", r -> r.path("/product/**")
                        .filters(f -> f.filter(jwtFilter))
                        .uri("http://localhost:8084"))
                .build();
    }
}

