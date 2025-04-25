package com.example.auth;

import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.jwt.TokenValidationResult;
import com.example.auth.redis.RedisUtils;
import com.example.commonevents.auth.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class JwtTest {

    @Value("${jwt.secret}")
    private String secretKey;

    @InjectMocks
    private JwtTokenProvider tokenProvider;

    @Mock
    private RedisUtils redisUtils;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);

        ReflectionTestUtils.setField(tokenProvider, "key", key);
        ReflectionTestUtils.setField(tokenProvider, "redisUtils", redisUtils);
    }

    @Test
    public void 토큰_정상_생성_및_claim_확인() {
        // given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_BUYER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@naver.com", "password", authorities);

        // when
        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        // then
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getRefreshToken());
        assertEquals("Bearer", tokenResponse.getTokenType());

        // accessToken 내부 claims 확인
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(tokenResponse.getAccessToken())
                .getBody();

        assertEquals("test@naver.com", claims.getSubject());
        assertEquals("BUYER", claims.get("auth")); // 권한이 들어갔는지
    }

    @Test
    public void 권한_흭득_권한_없을시_예외발생() {
        // given
        String refreshToken = "refreshToken";
        String accessToken = "accessToken";
        TokenResponse tokenResponse = TokenResponse.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();

        // when & then
        assertThrows(RuntimeException.class, () -> tokenProvider.getAuthentication(tokenResponse.getAccessToken()));
    }

    @Test
    public void 권한_흭득_성공() {
        // given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_BUYER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@naver.com", "password", authorities);

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        // when
        Authentication gotAuthentication = tokenProvider.getAuthentication(tokenResponse.getAccessToken());

        // then
        assertEquals("test@naver.com", gotAuthentication.getName());
        assertEquals("BUYER", gotAuthentication.getAuthorities().iterator().next().getAuthority());
        // assertEquals("password", gotAuthentication.getCredentials()); 패스워드는 민감사항이므로 JWT 생성시 기입되지않음.
    }

    @Test
    public void 토큰_검증_블랙리스트_예외발생() {
        // given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_BUYER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@naver.com", "password", authorities);

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        when(redisUtils.hasKeyBlackList(tokenResponse.getAccessToken()))
                .thenReturn(true);

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(tokenResponse.getAccessToken());

        // then
        assertFalse(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.IN_BLACKLIST, validationResult.getTokenErrorReason());
    }

    @Test
    public void 토큰_검증_유효하지_않는_토큰_예외발생() {
        // given
        String malformedToken = "this.is.not.jwt";

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(malformedToken);

        // then
        assertFalse(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.INVALID, validationResult.getTokenErrorReason());
    }

    @Test
    public void 토큰_검증_만료시_예외발생() {
        // given
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        String expiredToken = Jwts.builder()
                .setSubject("test@naver.com")
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전 -> 만료된 토큰
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(expiredToken);

        // then
        assertFalse(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.EXPIRED, validationResult.getTokenErrorReason());
    }


    @Test
    public void 토큰_검증_지원하지_않는_토큰_사용시_예외발생() {
        // given
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"test\"}".getBytes());
        String token = header + "." + payload + "."; // 서명이 없음

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(token);

        // then
        assertFalse(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.UNSUPPORTED, validationResult.getTokenErrorReason());
    }

    @Test
    public void 토큰_검증_빈_토큰일시_예외발생() {
        // given
        String token = null;

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(token);

        // then
        assertFalse(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.JWT_EMPTY, validationResult.getTokenErrorReason());
    }

    @Test
    public void 토큰_검증_성공() {
        // given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_BUYER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@naver.com", "password", authorities);

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        // when

        TokenValidationResult validationResult = tokenProvider.validateToken(tokenResponse.getAccessToken());

        // then
        assertTrue(validationResult.getValid());
        assertEquals(TokenValidationResult.TokenErrorReason.VALID, validationResult.getTokenErrorReason());
    }

    @Test
    public void 토큰_만료검사_성공() {
        // given
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        String expiredToken = Jwts.builder()
                .setSubject("test@naver.com")
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전 -> 만료된 토큰
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // when & then
        assertTrue(() -> tokenProvider.isTokenExpired(expiredToken));
    }

    @Test
    public void 토큰_만료검사_실패() {
        // given
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);
        String expiredToken = Jwts.builder()
                .setSubject("test@naver.com")
                .setExpiration(new Date(System.currentTimeMillis() + 1000)) // 1초 후 -> 만료 안된 토큰
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // when & then
        assertFalse(() -> tokenProvider.isTokenExpired(expiredToken));
    }
}
