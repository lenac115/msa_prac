package com.example.apigateway.jwt;

import com.example.apigateway.redis.RedisUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtFilter implements GatewayFilter {

    private final JwtTokenProvider tokenProvider;
    private final WebClient.Builder webClientBuilder;
    private final RedisUtils redisUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Authorization 헤더 확인
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return handleError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 토큰 검증
        if (!tokenProvider.validateToken(token)) {
            if (tokenProvider.isTokenExpired(token)) {
                return reissueAccessToken(exchange, chain, token);
            } else {
                return handleError(exchange, HttpStatus.UNAUTHORIZED);
            }
        }

        // JWT에서 ROLE 정보 가져오기
        Claims claims = tokenProvider.parseClaims(token);
        String auth = claims.get("auth", String.class); // 예: "SELLER" 또는 "BUYER"
        String path = request.getURI().getPath();

        // 권한 검사
        if (!isAuthorized(path, auth)) {
            return handleError(exchange, HttpStatus.FORBIDDEN);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> reissueAccessToken(ServerWebExchange exchange, GatewayFilterChain chain, String expiredToken) {
        Authentication authentication = tokenProvider.getAuthentication(expiredToken);
        String refreshToken = (String) redisUtils.get("RT:" + authentication.getName());
        if (refreshToken == null) {
            return handleError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // `auth-service`에 `reissue` 요청 보내기
        return webClientBuilder.build()
                .post()
                .uri("http://auth-service/auth/common/reissue")
                .header("Authorization", "Bearer " + expiredToken)
                .header("Refresh-Token", refreshToken)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(response -> {
                    // 새로운 Access Token을 응답 헤더에 추가
                    exchange.getResponse().getHeaders().add("Authorization", "Bearer " + response.getAccessToken());
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> handleError(exchange, HttpStatus.UNAUTHORIZED));
    }

    // API 경로별 권한 검사
    private boolean isAuthorized(String path, String auth) {
        Map<String, List<String>> roleMappings = new HashMap<>();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        // Auth Service (인증 관련)
        roleMappings.put("/auth/public/**", List.of("PUBLIC"));  // 인증 없이 접근 가능
        roleMappings.put("/auth/buyer/**", List.of("BUYER"));    // BUYER 전용
        roleMappings.put("/auth/seller/**", List.of("SELLER"));  // SELLER 전용
        roleMappings.put("/auth/common/**", List.of("BUYER", "SELLER")); // BUYER & SELLER 둘 다 가능

        // Order Service (주문 관련)
        roleMappings.put("/order/public/**", List.of("PUBLIC"));  // 인증 없이 접근 가능
        roleMappings.put("/order/buyer/**", List.of("BUYER"));    // BUYER 전용
        roleMappings.put("/order/seller/**", List.of("SELLER"));  // SELLER 전용
        roleMappings.put("/order/common/**", List.of("BUYER", "SELLER")); // BUYER & SELLER 둘 다 가능
        // Payment Service (결제 관련)
        roleMappings.put("/payment/**", List.of("BUYER")); // BUYER만 결제 가능

        // Product Service (판매자 전용)
        roleMappings.put("/product/**", List.of("SELLER")); // SELLER만 접근 가능

        for (Map.Entry<String, List<String>> entry : roleMappings.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {  // URL 패턴 매칭
                List<String> allowedRoles = entry.getValue();
                if (allowedRoles.contains("PUBLIC")) {
                    return true; // 인증 없이 접근 가능
                }
                return allowedRoles.contains(auth); // 권한이 맞는지 체크
            }
        }
        return false; // 기본적으로 차단
    }

    // 오류 응답
    private Mono<Void> handleError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
