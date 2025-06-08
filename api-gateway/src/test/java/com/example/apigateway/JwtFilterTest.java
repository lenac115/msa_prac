package com.example.apigateway;

import com.example.apigateway.jwt.JwtFilter;
import com.example.apigateway.jwt.JwtTokenProvider;
import com.example.apigateway.jwt.TokenResponse;
import com.example.apigateway.jwt.TokenValidationResult;
import com.example.apigateway.redis.RedisUtils;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
public class JwtFilterTest {

  /*  @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Value("${jwt.secret}")
    private String secretKey;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);

        ReflectionTestUtils.setField(tokenProvider, "key", key);
        ReflectionTestUtils.setField(tokenProvider, "redisUtils", redisUtils);
    }

    @Test
    void 유효하지_않은_토큰이면_UNAUTHORIZED_반환() {
        ServerHttpRequest request = MockServerHttpRequest
                .get("/auth/common/get/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from((MockServerHttpRequest) request);
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);

        when(tokenProvider.validateToken(anyString())).thenReturn(TokenValidationResult.builder()
                        .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                        .valid(false)
                .build());
        when(tokenProvider.isTokenExpired(anyString())).thenReturn(false);

        StepVerifier.create(jwtFilter.filter(exchange, mockChain))
                .expectComplete()
                .verify();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void 만료된_토큰이면_자동으로_재발급_받고_체인_필터_진행() {
        // given
        String expiredToken = "expired.token.here";
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new.access.token";

        ServerHttpRequest request = MockServerHttpRequest
                .get("/auth/common/get/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from((MockServerHttpRequest) request);

        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());

        // mock tokenProvider 동작 설정
        when(tokenProvider.validateToken(expiredToken)).thenReturn(TokenValidationResult.builder()
                        .valid(false)
                        .tokenErrorReason(TokenValidationResult.TokenErrorReason.EXPIRED)
                .build());
        when(tokenProvider.isTokenExpired(expiredToken)).thenReturn(true);
        when(tokenProvider.getAuthentication(expiredToken))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@naver.com", "1234"));

        // redis에서 refresh token 존재하는 경우
        when(redisUtils.get("RT:test@naver.com")).thenReturn(refreshToken);

        WebClient mockWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        // WebClient 빌드 -> post() -> uri() -> header() -> retrieve() -> bodyToMono()
        when(webClientBuilder.build()).thenReturn(mockWebClient);
        when(mockWebClient.post()).thenReturn(uriSpec);

        // uri() → bodySpec으로 반환 (!!! 여기가 핵심)
        when(uriSpec.uri(eq("/auth/common/reissue"))).thenReturn(bodySpec);

        // header() → bodySpec 계속 반환 (체인 유지)
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);


        // retrieve() → responseSpec
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        // 최종 응답 모킹
        when(responseSpec.bodyToMono(TokenResponse.class))
                .thenReturn(Mono.just(new TokenResponse(newAccessToken, "Bearer", refreshToken)));

        // when
        StepVerifier.create(jwtFilter.filter(exchange, mockChain))
                .expectComplete()
                .verify();

        // then
        // 응답 헤더에 새 access token 들어갔는지 확인
        assertEquals(newAccessToken, exchange.getResponse().getHeaders().getFirst("New-Access-Token"));
    }

    @ParameterizedTest
    @CsvSource({
            "/auth/public/info,PUBLIC,true",
            "/auth/common/info,BUYER,true",
            "/auth/common/info,SELLER,true",
            "/auth/seller/profile,SELLER,true",
            "/auth/seller/profile,BUYER,false",
            "/order/buyer/create,BUYER,true",
            "/order/buyer/create,SELLER,false",
            "/payment/checkout,BUYER,true",
            "/payment/checkout,SELLER,false",
            "/product/manage,SELLER,true",
            "/product/manage,BUYER,false",
            "/unknown/path,BUYER,false"
    })
    void 경로와_권한에_따른_접근_허용_여부_검사(String path, String role, boolean expected) {
        boolean result = jwtFilter.isAuthorized(path, role);
        assertEquals(expected, result);
    }*/
}
