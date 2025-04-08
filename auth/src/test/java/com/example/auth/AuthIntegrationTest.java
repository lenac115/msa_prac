package com.example.auth;

import com.example.auth.dto.TokenResponse;
import com.example.auth.redis.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 로그인_후_토큰_발급_및_재발급_성공() throws Exception {
        // given: 유저 등록 (DB에 유저가 있어야 로그인 가능)
        회원가입요청();

        // when: 로그인 요청
        MvcResult loginResult = mockMvc.perform(post("/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "test@naver.com",
                            "password": "1234"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class
        );

        // then: 발급된 RefreshToken이 Redis에 저장되었는지 확인
        String redisKey = "RT:test@naver.com";
        String savedRefreshToken = (String) redisUtils.get(redisKey);
        assertEquals(savedRefreshToken, tokenResponse.getRefreshToken());

        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        String expiredAccessToken = Jwts.builder()
                .setSubject("test@naver.com")
                .claim("auth", "BUYER")
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 이미 만료됨
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // when: 재발급 요청
        MvcResult reissueResult = mockMvc.perform(post("/auth/public/reissue")
                        .header("Authorization", "Bearer " + expiredAccessToken)
                        .header("Refresh-Token", tokenResponse.getRefreshToken()))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse reissuedToken = objectMapper.readValue(
                reissueResult.getResponse().getContentAsString(), TokenResponse.class
        );

        // then: 새로 발급된 AccessToken, RefreshToken 확인
        assertNotNull(reissuedToken.getAccessToken());
        assertNotNull(reissuedToken.getRefreshToken());
    }


    private void 회원가입요청() throws Exception {
        mockMvc.perform(post("/auth/public/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "email": "test@naver.com",
                    "password": "1234",
                    "name": "testName",
                    "address": "testAddress",
                    "phone": "testPhone",
                    "auth": "BUYER"
                }
                """)).andExpect(status().isCreated());
    }
}