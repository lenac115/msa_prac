package com.example.auth.config;

import com.example.auth.jwt.JwtAuthenticationEntryPoint;
import com.example.auth.jwt.JwtSecurityConfig;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.redis.RedisUtils;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RedisUtils redisUtils;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(HttpMethod.POST, "/auth/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/public/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll() //정적파일
                        .anyRequest().authenticated() // 인증 되지 않는 사용자일 경우 모든 요청을 Spring Security 에서 가로챔(설정한 url을 제외한 url은 이 설정을 적용할 예정)
                )
                .formLogin(
                        AbstractHttpConfigurer::disable
                )
                .logout(
                        AbstractHttpConfigurer::disable
                )
                .exceptionHandling((exceptionHandling) -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedPage("/api/users/accessDenied")) // 권한에 따른 접근 불가 페이지 설정
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers("/auth/**"))
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .with(new JwtSecurityConfig(tokenProvider, userService, redisUtils), Customizer.withDefaults());
                /*.cors((cors) -> cors
                        .configurationSource(corsConfigurationSource()));*/

        return httpSecurity.build();
    }
}
