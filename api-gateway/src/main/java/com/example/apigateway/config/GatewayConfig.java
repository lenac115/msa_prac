package com.example.apigateway.config;

import com.example.apigateway.jwt.JwtFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

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
                .route("auth-service-write", r -> r.path("/auth/**")
                        .and().method(HttpMethod.POST, HttpMethod.PUT)
                        .filters(f -> f
                                .modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.justOrEmpty(s))
                                .filter(jwtFilter))
                        .uri("lb://auth-service"))
                .route("auth-service-read", r -> r.path("/auth/**")
                        .and().method(HttpMethod.GET, HttpMethod.DELETE)
                        .filters(f -> f
                                .filter(jwtFilter))
                        .uri("lb://auth-service"))
                .route("order-service-write", r -> r.path("/order/**")
                        .and().method(HttpMethod.POST, HttpMethod.PUT)
                        .filters(f -> f
                                .modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.justOrEmpty(s))
                                .filter(jwtFilter))
                        .uri("lb://order-service"))
                .route("order-service-read", r -> r.path("/order/**")
                        .and().method(HttpMethod.GET, HttpMethod.DELETE)
                        .filters(f -> f
                                .filter(jwtFilter))
                        .uri("lb://order-service"))
                .route("payment-service-write", r -> r.path("/payment/**")
                        .and().method(HttpMethod.POST, HttpMethod.PUT)
                        .filters(f -> f
                                .modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.justOrEmpty(s))
                                .filter(jwtFilter)
                        )
                        .uri("lb://payment-service"))
                .route("payment-service-read", r -> r.path("/payment/**")
                        .and().method(HttpMethod.GET, HttpMethod.DELETE)
                        .filters(f -> f
                                .filter(jwtFilter)
                        )
                        .uri("lb://payment-service"))
                .route("product-service-write", r -> r.path("/product/**")
                        .and().method(HttpMethod.POST, HttpMethod.PUT)
                        .filters(f -> f
                                .modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.justOrEmpty(s))
                                .filter(jwtFilter))
                        .uri("lb://product-service"))
                .route("product-service-read", r -> r.path("/product/**")
                        .and().method(HttpMethod.GET, HttpMethod.DELETE)
                        .filters(f -> f
                                .filter(jwtFilter))
                        .uri("lb://product-service"))
                .build();
    }
}

