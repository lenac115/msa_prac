package com.example.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class ApiGatewayIntegrationTest {
/*
    @Autowired
    private WebTestClient webTestClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    static final String API_URL = "http://localhost:8080";

    @BeforeEach
    void resetAuthServiceDb() {
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(API_URL + "/auth/public/reset")
                .exchange()
                .expectStatus().isOk();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(API_URL + "/order/public/reset")
                .exchange()
                .expectStatus().isOk();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(API_URL + "/payment/public/reset")
                .exchange()
                .expectStatus().isOk();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri(API_URL + "/product/public/reset")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void totalFlowTest() throws IOException {
        // 1. 회원가입
        String registerPayload = """
                {
                    "email": "seller@test.com",
                    "password": "1234",
                    "name": "Test User",
                    "address": "Test Address",
                    "phone": "000-0000-0000",
                    "auth": "SELLER"
                }
                """;

        webTestClient.post().uri(API_URL + "/auth/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerPayload)
                .exchange()
                .expectStatus().isCreated();

        // 2. 로그인
        String loginPayload = """
                {
                    "email": "seller@test.com",
                    "password": "1234"
                }
                """;

        byte[] responseBody = webTestClient.post().uri(API_URL + "/auth/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginPayload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        JsonNode json = objectMapper.readTree(responseBody);
        String accessToken = json.get("accessToken").asText();
        System.out.println(accessToken);

        // 3. 상품 생성
        String productCreatePayload = """
                {
                    "productName": "product name",
                    "price": 10000,
                    "stock": 10000
                }
                """;
        byte[] productResponseBody = webTestClient.post().uri(API_URL + "/product/new")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productCreatePayload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        JsonNode productJson = objectMapper.readTree(productResponseBody);
        Long productId = productJson.get("id").asLong();
        System.out.println("productId = " + productId);

        // 4. 로그아웃
        webTestClient.post().uri(API_URL + "/auth/common/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
        /* 구매자 테스트 플로우, 그러나 결제 생성의 주요 주체를 프론트로 옮겨서 백엔드에서 결제를 생성할 수는 있으나 생성한 결제는 토스에 먼저 결제를 생성하고
            그 결제를 검증하기 위한 것이기 때문에 테스트 환경에서 테스트가 불가함. 테스트 메소드 연구 당시에는 주문 요청까지는 완전히 실행되었으나
            결제 생성 파트에서 실제 결제를 생성하지 못함.
        // 1. 회원가입
        String registerBuyerPayload = """
                {
                    "email": "buyer@test.com",
                    "password": "1234",
                    "name": "Test User2",
                    "address": "Test Address2",
                    "phone": "000-0000-00002",
                    "auth": "BUYER"
                }
                """;

        webTestClient.post().uri(API_URL + "/auth/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerBuyerPayload)
                .exchange()
                .expectStatus().isCreated();

        // 2. 로그인
        String loginBuyerPayload = """
                {
                    "email": "buyer@test.com",
                    "password": "1234"
                }
                """;

        byte[] buyerResponseBody = webTestClient.post().uri(API_URL + "/auth/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginBuyerPayload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        JsonNode buyerJson = objectMapper.readTree(buyerResponseBody);
        String buyerAccessToken = buyerJson.get("accessToken").asText();

        // 3. 주문 요청
        String orderPayload = """
                [
                    {
                        "productId": %d,
                        "quantity": 2,
                        "amount": 20000
                    }
                ]
                """.formatted(productId);

        byte[] orderResponseBody = webTestClient.post().uri(API_URL + "/order/buyer/create")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderPayload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        JsonNode orderJson = objectMapper.readTree(orderResponseBody);
        Long orderId = orderJson.get("id").asLong();

        // 4. 결제 확인
        String paymentPayload = String.format("""
                {
                    "orderId": %d,
                    "paymentKey": "%s",
                    "amount": 20000
                }
                """.formatted(orderId, "test-Key"));


        webTestClient.post().uri(API_URL + "/payment/confirm")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(paymentPayload)
                .exchange()
                .expectStatus().isOk();

        // 5. 주문 상태 확인
        webTestClient.get().uri(API_URL + "/order/1")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PAID");
    }*/
}