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

        // 4. 결제 생성
        String newPaymentPayload = """
                
                    {
                        "orderId": %d,
                        "buyerId": 1,
                        "amount": 20000,
                        "paymentKey": "test-paymentKey"
                    }
                
                """.formatted(orderId);

        byte[] paymentResponseBody = webTestClient.post().uri(API_URL + "/payment/new")
                .header("Authorization", "Bearer " + buyerAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newPaymentPayload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        JsonNode newPaymentJson = objectMapper.readTree(paymentResponseBody);
        String paymentKey = newPaymentJson.get("id").asText();

        // 5. 결제 확인
        AtomicReference<String> paymentKeyRef = new AtomicReference<>();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    byte[] body = webTestClient.get().uri(API_URL + "/payment/order/" + orderId)
                            .header("Authorization", "Bearer " + buyerAccessToken)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .jsonPath("$.paymentKey").value(Matchers.notNullValue())
                            .returnResult()
                            .getResponseBodyContent();

                    // JSON 파싱해서 paymentKey 추출
                    JsonNode root = objectMapper.readTree(body);
                    String findPaymentKey = root.get("paymentKey").asText();
                    paymentKeyRef.set(findPaymentKey);
                });

        String paymentPayload = String.format("""
                {
                    "orderId": %d,
                    "paymentKey": "%s",
                    "amount": 1
                }
                """.formatted(orderId, paymentKeyRef.get()));


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
    }
}