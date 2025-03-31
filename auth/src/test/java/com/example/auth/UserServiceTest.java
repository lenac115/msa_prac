package com.example.auth;

import com.example.auth.domain.User;
import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserDto;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.redis.RedisUtils;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.UserService;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {

    @Value("${jwt.secret}")
    private String secretKey;

    @InjectMocks
    private UserService userService;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        Key key = Keys.hmacShaKeyFor(secretBytes);

        ReflectionTestUtils.setField(tokenProvider, "key", key);
        ReflectionTestUtils.setField(tokenProvider, "redisUtils", redisUtils);
    }



    @Test
    public void 회원가입_성공() {
        // given
        UserDto request = UserDto.builder()
                .phone("01012345678")
                .email("testUser@naver.com")
                .name("테스트")
                .auth(User.Auth.BUYER)
                .password("123456")
                .address("테스트 주소")
                .build();

        User savedUser = User.builder()
                .address(request.getAddress())
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .auth(User.Auth.BUYER)
                .password(request.getPassword())
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(savedUser));
            return savedUser;
        }).when(userRepository).save(any(User.class));

        // when

        userService.register(request);

        // then
        Optional<User> user = userRepository.findByEmail("testUser@naver.com");
        assertTrue(user.isPresent());  // 회원가입 후 유저가 DB에 존재하는지 확인
        assertEquals("BUYER", user.get().getAuth().name());
    }

    @Test
    public void 회원가입_실패_중복_아이디() {
        // given
        UserDto request = UserDto.builder()
                .phone("01012345678")
                .email("testUser@naver.com")
                .name("테스트")
                .auth(User.Auth.BUYER)
                .password("123456")
                .address("테스트 주소")
                .build();

        User savedUser = User.builder()
                .address(request.getAddress())
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .auth(User.Auth.BUYER)
                .password(request.getPassword())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(savedUser));
        // when & then
        assertThrows(RuntimeException.class, () -> userService.register(request));
    }

    @Test
    public void 회원정보_수정() {
        // given
        String email = "testUser@naver.com";
        User savedUser = User.builder()
                .email(email)
                .name("테스트")
                .phone("01012345678")
                .address("테스트 주소")
                .auth(User.Auth.BUYER)
                .password("123456")
                .build();

        UserDto updateRequest = UserDto.builder()
                .email(email)
                .name("테스트 수정")
                .phone("01098765432")
                .address("수정된 주소")
                .auth(User.Auth.BUYER)
                .password("123456")
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(savedUser));

        // when
        userService.update(updateRequest, email);

        // then
        assertEquals("테스트 수정", savedUser.getName());
        assertEquals("01098765432", savedUser.getPhone());
        assertEquals("수정된 주소", savedUser.getAddress());
    }

    @Test
    public void 로그인() {
        // given
        String email = "testUser@naver.com";
        String password = "123456";
        User savedUser = User.builder()
                .email(email)
                .name("테스트")
                .phone("01012345678")
                .address("테스트 주소")
                .auth(User.Auth.BUYER)
                .password(password)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(savedUser));

        Authentication authentication = new TestingAuthenticationToken(email, password, "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenCallRealMethod();
        when(tokenProvider.validateToken(any(String.class))).thenCallRealMethod();

        // when & then
        when(redisUtils.hasKeyBlackList(anyString())).thenReturn(Boolean.FALSE);
        TokenResponse tokenResponse = userService.login(email, password);
        assertNotNull(tokenResponse);
        assertTrue(tokenProvider.validateToken(tokenResponse.getAccessToken()));

        when(redisUtils.hasKeyBlackList(anyString())).thenReturn(Boolean.TRUE);
        tokenResponse = userService.login(email, password);
        assertNotNull(tokenResponse);
        assertFalse(tokenProvider.validateToken(tokenResponse.getAccessToken()));
    }
}