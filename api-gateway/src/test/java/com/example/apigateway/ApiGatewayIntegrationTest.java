package com.example.apigateway;

import com.example.apigateway.jwt.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    /*@Test
    void 만료된_엑세스토큰으로_요청시_재발급_요청됨() {
        String expiredAccessToken = "만료된_JWT";
        String refreshToken = "유효한_RefreshToken";

        webTestClient.post()
                .uri("/auth/common/get/me")  // Gateway 경유
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("New-Access-Token"); // JwtFilter가 헤더에 넣어주는지 확인
    }*/

    @Test
    void buyer가_로그인하고_주문을_생성하는_시나리오() {
        // 0. 회원가입
        Map<String, String> joinBody = new HashMap<>();
        joinBody.put("email", "test@gmail.com");
        joinBody.put("password", "123456");
        joinBody.put("address", "testAddress");
        joinBody.put("phone", "testPhone");
        joinBody.put("auth", "BUYER");

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/auth/public/register")
                .bodyValue(joinBody)
                .exchange()
                .expectStatus().isCreated();

        // 1. 로그인 요청 → access + refresh 토큰 발급
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", "test@gmail.com");
        loginBody.put("password", "123456");

        TokenResponse tokens = webTestClient.post()
                .uri("/auth/public/login")
                .bodyValue(loginBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        /*// 2. access token으로 주문 요청
        webTestClient.post()
                .uri("/order/buyer/create")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .bodyValue(new OrderRequest("상품A", 1))
                .exchange()
                .expectStatus().isOk();

        // 3. 주문 조회 (공통 권한)
        webTestClient.get()
                .uri("/order/common/my")
                .header("Authorization", "Bearer " + tokens.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orders.length()").isEqualTo(1);*/
    }
}