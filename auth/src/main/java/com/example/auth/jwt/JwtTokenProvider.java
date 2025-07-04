package com.example.auth.jwt;

import com.example.auth.redis.RedisUtils;
import com.example.commonevents.auth.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider implements InitializingBean {

    private final String secret;
    private Key key;
    private final RedisUtils redisUtils;


    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, RedisUtils redisUtils) {
        this.secret = secretKey;
        this.redisUtils = redisUtils;
    }

    // 유저 정보를 가지고 AccessToken, RefreshToken 을 생성하는 메서드
    public TokenResponse generateToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.joining(","));

        System.out.println("authorities: " + authorities);
        long now = (new Date()).getTime();
        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + (1000 * 60));
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(new Date(now + 259200000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthEvenIfExpired(String accessToken) {

        Claims claims;
        try {
            claims = parseClaims(accessToken);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        }
        // 토큰 복호화

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 정보를 검증하는 메서드
    public TokenValidationResult validateToken(String realToken) {
        try {
            String token = realToken.replace("Bearer ", "");
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            if(redisUtils.hasKeyBlackList(token)) {
                log.info("Token In Blacklist");
                return TokenValidationResult.builder()
                        .valid(false)
                        .tokenErrorReason(TokenValidationResult.TokenErrorReason.IN_BLACKLIST)
                        .build();
            }
            return TokenValidationResult.builder()
                    .valid(true)
                    .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                    .build();

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            return TokenValidationResult.builder()
                    .valid(false)
                    .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                    .build();
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            return TokenValidationResult.builder()
                    .valid(false)
                    .tokenErrorReason(TokenValidationResult.TokenErrorReason.EXPIRED)
                    .build();
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            return TokenValidationResult.builder()
                    .valid(false)
                    .tokenErrorReason(TokenValidationResult.TokenErrorReason.UNSUPPORTED)
                    .build();
        } catch (IllegalArgumentException | NullPointerException e) {
            log.info("JWT claims string is empty.", e);
            return TokenValidationResult.builder()
                    .valid(false)
                    .tokenErrorReason(TokenValidationResult.TokenErrorReason.JWT_EMPTY)
                    .build();
        }
    }

    private Claims parseClaims(String accessToken) {
        String realToken = accessToken.replace("Bearer ", "");
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(realToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            return true;
        }
        return false;
    }
}
