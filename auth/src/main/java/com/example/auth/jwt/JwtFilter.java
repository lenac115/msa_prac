package com.example.auth.jwt;

import com.example.auth.dto.TokenResponse;
import com.example.auth.redis.RedisUtils;
import com.example.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        try {
            if (token != null) {
                if (tokenProvider.validateToken(token)) {
                    // 유효한 토큰 처리
                    Authentication authentication = tokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("Authentication set: " + authentication.getName());
                } else if (tokenProvider.isTokenExpired(token)) {
                    // Access Token 만료 시 처리
                    handleExpiredToken(request, response, token);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
                    return;
                }
            }

            // 필터 체인을 계속 진행
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 예외 발생 시 처리
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token processing failed");
        }
    }

    private void handleExpiredToken(HttpServletRequest request, HttpServletResponse response, String expiredToken)
            throws IOException {
        Authentication authentication = tokenProvider.getAuthentication(expiredToken);
        System.out.println("try new token");

        if (authentication == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }

        String email = authentication.getName();
        String refreshToken = (String) redisUtils.get("RT:" + email);

        if (refreshToken != null) {
            refreshToken = refreshToken.replace("\"", "");
            if(!tokenProvider.validateToken(refreshToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }

        try {
            // 새로운 Access Token 발급
            TokenResponse newTokens = userService.reissue(expiredToken, refreshToken);

            response.setHeader("Authorization", "Bearer " + newTokens.getAccessToken());
            redisUtils.set("RT:" + email, newTokens.getRefreshToken(), 4320); // Redis에 Refresh Token 갱신

            // SecurityContextHolder에 새로운 인증 설정
            Authentication newAuthentication = tokenProvider.getAuthentication(newTokens.getAccessToken());
            SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token generation failed");
        }
    }

    // Request Header 에서 토큰 정보 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
