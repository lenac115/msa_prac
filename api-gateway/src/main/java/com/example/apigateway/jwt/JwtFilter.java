package com.example.apigateway.jwt;

import com.example.apigateway.redis.RedisUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
            String path = exchange.getRequest().getPath().toString();
            // 권한 체크는 해야 함
            if (!isAuthorized(path, null)) {
                return handleError(exchange, HttpStatus.FORBIDDEN);
            }
            return chain.filter(exchange); // 인증 없이 허용
        }

        // Device Id 헤더 확인
        if (request.getHeaders().get("X-Device-Id") == null) {
            return handleError(exchange, HttpStatus.BAD_REQUEST);
        }

        String token = authHeader.substring(7);

        // 토큰 검증
        if (!tokenProvider.validateToken(token).getValid()) {
            String path = exchange.getRequest().getPath().toString();

            if (!path.contains("public")) {
                if (tokenProvider.isTokenExpired(token)) {
                    return reissueAccessToken(exchange, chain, token);
                } else {
                    return handleError(exchange, HttpStatus.UNAUTHORIZED);
                }
            }
        }


        // JWT에서 ROLE 정보 가져오기
        Claims claims = tokenProvider.parseClaims(token);
        String auth = claims.get("auth", String.class); // "SELLER" 또는 "BUYER"
        String path = request.getURI().getPath();


        // 권한 검사
        if (!isAuthorized(path, auth)) {
            System.out.println("권한 검사 실패");
            System.out.println(auth);
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
                .uri("/auth/common/reissue")
                .header("Authorization", "Bearer " + expiredToken)
                .header("Refresh-Token", refreshToken)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(response -> {
                    // 새로운 Access Token을 Authorization 헤더에 다시 셋팅
                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("Authorization", "Bearer " + response.getAccessToken())
                            .build();
                    exchange.getResponse().getHeaders().add("New-Access-Token", response.getAccessToken());
                    // request 교체 후 체인 필터 진행
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    // API 경로별 권한 검사
    public boolean isAuthorized(String path, String role) {

        Map<String, List<String>> roleMappings = new LinkedHashMap<>();
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
        roleMappings.put("/payment/public/reset", List.of("PUBLIC"));
        roleMappings.put("/payment/**", List.of("BUYER")); // BUYER만 결제 가능

        // Product Service (판매자 전용)
        roleMappings.put("/product/public/**", List.of("PUBLIC"));
        roleMappings.put("/product/buyer/**", List.of("BUYER"));
        roleMappings.put("/product/common/**", List.of("BUYER", "SELLER"));
        roleMappings.put("/product/seller/**", List.of("SELLER")); // SELLER만 접근 가능

        for (Map.Entry<String, List<String>> entry : roleMappings.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                List<String> allowedRoles = entry.getValue();
                System.out.println("요청 경로: " + path);
                System.out.println("요청 역할: " + role);
                System.out.println("매칭된 패턴: " + entry.getKey());
                System.out.println("허용된 역할: " + allowedRoles);

                // 토큰이 아예 없는 사용자도 접근 허용 (PUBLIC 경로만)
                if ((role == null || role.contains("BUYER") || role.contains("SELLER")) && path.contains("public")) {
                    return true;
                }

                if (role != null && allowedRoles.stream()
                        .anyMatch(allowed -> allowed.equalsIgnoreCase(role))) {
                    return true;
                }
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
