package com.example.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    static final String AUTH_URL = "http://localhost:8081";
    static final String ORDER_URL = "http://localhost:8082";
    static final String PAYMENT_URL = "http://localhost:8083";
    static final String PRODUCT_URL = "http://localhost:8084";
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

        webTestClient.post().uri(AUTH_URL + "/auth/public/register")
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

        byte[] responseBody = webTestClient.post().uri(AUTH_URL + "/auth/public/login")
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
        webTestClient.post().uri(PRODUCT_URL + "/product/new")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productCreatePayload)
                .exchange()
                .expectStatus().isCreated();

        // 4. 로그아웃
        webTestClient.post().uri(AUTH_URL + "/auth/common/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        // 1. 회원가입
        String registerBuyerPayload = """
                {
                    "email": "buyer@test.com",
                    "password": "1234",
                    "name": "Test User",
                    "address": "Test Address",
                    "phone": "000-0000-0000",
                    "auth": "BUYER"
                }
                """;

        webTestClient.post().uri(AUTH_URL + "/auth/public/register")
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

        String buyerAccessToken = webTestClient.post().uri(AUTH_URL + "/auth/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginBuyerPayload)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").value(Matchers.notNullValue())
                .returnResult()
                .getResponseBodyContent().toString();

        // 3. 주문 요청
        String orderPayload = """
                {
                    "productId": 1,
                    "quantity": 2,
                    "amount": 1
                }
                """;

        webTestClient.post().uri(ORDER_URL + "/order/buyer/create")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderPayload)
                .exchange()
                .expectStatus().isCreated();

        // 4. 결제
        AtomicReference<String> paymentKeyRef = new AtomicReference<>();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    String body = webTestClient.get().uri(PAYMENT_URL + "/payments/order/1")
                            .header("Authorization", "Bearer " + buyerAccessToken)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .jsonPath("$.paymentKey").value(Matchers.notNullValue())
                            .returnResult()
                            .getResponseBodyContent().toString();

                    // JSON 파싱해서 paymentKey 추출
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode root = objectMapper.readTree(body);
                    String paymentKey = root.get("paymentKey").asText();
                    paymentKeyRef.set(paymentKey);
                });

        String paymentPayload = String.format("""
                {
                    "orderId": 1,
                    "paymentKey": "%s",
                    "amount": 1
                }
                """, paymentKeyRef.get());


        webTestClient.post().uri(PAYMENT_URL + "/payments")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(paymentPayload)
                .exchange()
                .expectStatus().isOk();

        // 5. 주문 상태 확인
        webTestClient.get().uri(ORDER_URL + "/orders/1")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PAID");
    }
}