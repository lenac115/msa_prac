package com.example.auth;

import com.example.auth.domain.User;
import com.example.exception.CustomException;
import com.example.exception.errorcode.CommonErrorCode;
import com.example.exception.errorcode.UserErrorCode;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.jwt.TokenValidationResult;
import com.example.auth.redis.RedisUtils;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.EmailService;
import com.example.auth.service.UserService;
import com.example.auth.uuid.BasicUUIDGenerator;
import com.example.commonevents.auth.Auth;
import com.example.commonevents.auth.ChangePasswordReq;
import com.example.commonevents.auth.TokenResponse;
import com.example.commonevents.auth.UserDto;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
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
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private BasicUUIDGenerator uuidGenerator;

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
                .auth(Auth.BUYER)
                .password("123456")
                .address("테스트 주소")
                .build();

        User savedUser = User.builder()
                .address(request.getAddress())
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .auth(Auth.BUYER)
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
                .auth(Auth.BUYER)
                .password("123456")
                .address("테스트 주소")
                .build();

        User savedUser = User.builder()
                .address(request.getAddress())
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .auth(Auth.BUYER)
                .password(request.getPassword())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(savedUser));

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.register(request));
        assertEquals(UserErrorCode.ALREADY_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 회원정보_수정_존재하지_않는_유저일_경우_예외발생() {
        // given
        String email = "testUser@naver.com";

        UserDto updateRequest = UserDto.builder()
                .email(email)
                .name("테스트 수정")
                .phone("01098765432")
                .address("수정된 주소")
                .auth(Auth.BUYER)
                .password("123456")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.update(updateRequest, email));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 회원정보_수정_다른_회원일_경우_예외발생() {
        // given
        String email = "testUser@naver.com";

        UserDto updateRequest = UserDto.builder()
                .email(email)
                .name("테스트 수정")
                .phone("01098765432")
                .address("수정된 주소")
                .auth(Auth.BUYER)
                .password("123456")
                .build();

        User savedUser = User.builder()
                .email(email + "1")
                .name("테스트")
                .phone("01012345678")
                .address("테스트 주소")
                .auth(Auth.BUYER)
                .password("123456")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(savedUser));


        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.update(updateRequest, email));
        assertEquals(CommonErrorCode.INVALID_PARAMETER, exception.getErrorCode());
    }

    @Test
    public void 회원정보_수정_성공() {
        // given
        String email = "testUser@naver.com";
        User savedUser = User.builder()
                .email(email)
                .name("테스트")
                .phone("01012345678")
                .address("테스트 주소")
                .auth(Auth.BUYER)
                .password("123456")
                .build();

        UserDto updateRequest = UserDto.builder()
                .email(email)
                .name("테스트 수정")
                .phone("01098765432")
                .address("수정된 주소")
                .auth(Auth.BUYER)
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
    public void 로그인_성공() {
        // given
        String email = "testUser@naver.com";
        String password = "123456";
        String encodedPassword = "encodedPassword";
        String deviceId = "test-device";
        User mockUser = User.builder()
                .email(email)
                .password(encodedPassword)
                .build();

        TokenResponse mockTokenResponse = new TokenResponse("mockAccessToken", "Bearer", "mockRefreshToken");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockTokenResponse);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(new UsernamePasswordAuthenticationToken(email, password));
        when(passwordEncoder.matches(password, mockUser.getPassword())).thenReturn(true);

        // when
        TokenResponse tokenResponse = userService.login(email, password, deviceId);

        // then
        assertNotNull(tokenResponse);
        assertEquals("mockAccessToken", tokenResponse.getAccessToken());
        assertEquals("mockRefreshToken", tokenResponse.getRefreshToken());

        verify(redisUtils, times(1)).set("RT:" + email + ":" + deviceId, "mockRefreshToken", 1440);
    }

    @Test
    public void 로그인_존재하지_않는_유저일_경우_예외발생() {
        // given
        String email = "testUser@naver.com";
        String password = "123456";
        String deviceId = "test-device";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.login(email, password, deviceId));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 로그인_비밀번호가_일치하지_않는_경우_예외발생() {
        // given
        String email = "testUser@naver.com";
        String password = "123456";
        String deviceId = "test-device";
        User savedUser = User.builder()
                .email(email)
                .name("테스트")
                .phone("01012345678")
                .address("테스트 주소")
                .auth(Auth.BUYER)
                .password(password + "1")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(savedUser));

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.login(email, password, deviceId));
        assertEquals(UserErrorCode.NOT_EQUAL_PASSWORD, exception.getErrorCode());
    }

    @Test
    public void 로그아웃_존재하지_않는_유저일_경우_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        String deviceId = "test-deviceId";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.logout(accessToken, email, deviceId));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 로그아웃_유효하지_않은_토큰일_경우_예외발생() {
        // given
        String accessToken = "invalidAccessToken";
        String email = "testUser@naver.com";
        String deviceId = "test-deviceId";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(false)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                .build());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.logout(accessToken, email, deviceId));
        assertEquals(UserErrorCode.INVALID_USER_TOKEN, exception.getErrorCode());
    }

    @Test
    public void 로그아웃_토큰_소유자와_유저가_다를_경우_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        String deviceId = "test-deviceId";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        Authentication authentication = new TestingAuthenticationToken("wrongUser@naver.com", "password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);

        // when & then
        CustomException exception
                = assertThrows(CustomException.class, () -> userService.logout(accessToken, email, deviceId));
        assertEquals(UserErrorCode.NOT_ACCOUNT_AUTH, exception.getErrorCode());
    }

    @Test
    public void 로그아웃_성공() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        String deviceId = "test-deviceId";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        Authentication authentication = new TestingAuthenticationToken(email, "password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);

        // when
        userService.logout(accessToken, email, deviceId);

        // then
        verify(redisUtils, times(1)).setBlackList(accessToken, "accessToken", 1440);
        verify(redisUtils, times(1)).delete("RT:" + email + ":" + deviceId);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void 계정삭제_존재하지_않는_유저일_경우_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.delete(accessToken, email));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 계정삭제_유효하지_않은_토큰일_경우_예외발생() {
        // given
        String accessToken = "invalidAccessToken";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(false)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                .build());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.delete(accessToken, email));
        assertEquals(UserErrorCode.INVALID_USER_TOKEN, exception.getErrorCode());
    }

    @Test
    public void 계정삭제_토큰_소유자와_유저가_다를_경우_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        Authentication authentication = new TestingAuthenticationToken("wrongUser@naver.com", "password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.delete(accessToken, email));
        assertEquals(UserErrorCode.NOT_ACCOUNT_AUTH, exception.getErrorCode());
    }

    @Test
    public void 계정삭제_성공() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        String deviceId = "test-deviceId";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        Authentication authentication = new TestingAuthenticationToken(email, "password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);

        // when
        userService.delete(accessToken, email);

        // then
        verify(redisUtils, times(1)).setBlackList(accessToken, "accessToken", 1440);
        verify(redisUtils, times(1)).deleteAllRefreshTokens(email);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    public void 비밀번호_변경_존재하지_않는_유저_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        ChangePasswordReq changePasswordReq = ChangePasswordReq.builder()
                .newPassword("newPassword")
                .oldPassword("oldPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.changePassword(changePasswordReq, email, accessToken));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 비밀번호_변경_기존_비밀번호_불일치() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("notPassword")
                .build();
        ChangePasswordReq changePasswordReq = ChangePasswordReq.builder()
                .newPassword("newPassword")
                .oldPassword("oldPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.changePassword(changePasswordReq, email, accessToken));
        assertEquals(UserErrorCode.NOT_EQUAL_PASSWORD, exception.getErrorCode());
    }

    @Test
    public void 비밀번호_변경_유효하지_않은_토큰_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("oldPassword")
                .build();
        ChangePasswordReq changePasswordReq = ChangePasswordReq.builder()
                .newPassword("newPassword")
                .oldPassword("oldPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(false)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                .build());
        when(passwordEncoder.matches(changePasswordReq.getNewPassword(), user.getPassword())).thenReturn(true);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.changePassword(changePasswordReq, email, accessToken));
        assertEquals(UserErrorCode.INVALID_USER_TOKEN, exception.getErrorCode());
    }

    @Test
    public void 비밀번호_변경_성공() {
        // given
        String accessToken = "validAccessToken";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("oldPassword")
                .build();
        ChangePasswordReq changePasswordReq = ChangePasswordReq.builder()
                .newPassword("newPassword")
                .oldPassword("oldPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenProvider.validateToken(accessToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(passwordEncoder.matches(changePasswordReq.getNewPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(changePasswordReq.getNewPassword())).thenReturn("newPassword");

        // when

        userService.changePassword(changePasswordReq, email, accessToken);

        //then
        verify(redisUtils, times(1)).setBlackList(accessToken, "accessToken", 1440);
        verify(redisUtils, times(1)).delete("RT:" + email);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user.getPassword(), changePasswordReq.getNewPassword());
    }

    @Test
    public void 비밀번호_초기화_키값_없을시_예외발생() {
        // given
        String accessToken = "emptyAccessToken";
        String password = "newPassword";

        when(redisUtils.hasKey(accessToken)).thenReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.resetPassword(accessToken, password));
        assertEquals(CommonErrorCode.INVALID_PARAMETER, exception.getErrorCode());
    }

    @Test
    public void 비밀번호_초기화_유저_없을시_예외발생() {
        // given
        String accessToken = "validAccessToken";
        String password = "newPassword";
        String email = "testUser@naver.com";

        when(redisUtils.hasKey(accessToken)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.resetPassword(accessToken, password));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 비밀번호_초기화_성공() {
        // given
        String accessToken = "validAccessToken";
        String password = "newPassword";
        String encodedPassword = "encodedPassword";
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("oldPassword")
                .build();

        when(redisUtils.hasKey(accessToken)).thenReturn(true);
        when(redisUtils.get(accessToken)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        // when
        userService.resetPassword(accessToken, password);

        // then
        verify(redisUtils, times(1)).delete(accessToken);
        assertEquals(encodedPassword, user.getPassword());
    }

    @Test
    public void 초기화_메일_전송_존재하지_않는_유저일시_예외발생() {
        // given
        String email = "testUser@naver.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.sendResetPasswordEmail(email));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    public void 초기화_메일_전송_성공() {
        // given
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();
        String token = "token";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(uuidGenerator.generateStringUUID()).thenReturn(token);

        // when
        userService.sendResetPasswordEmail(email);

        // then
        verify(redisUtils, times(1)).set(token, email, 10);
        verify(emailService, times(1)).sendResetPasswordEmail(email, token);
    }

    @Test
    @WithMockUser(value = "testUser1@naver.com", roles = {"BUYER"})
    public void 유저_ID_조회_현재_접속중인_계정과_이름이_다를_경우_예외발생() {
        String email = "testUser@naver.com";

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserId(email));
        assertEquals(UserErrorCode.NOT_ACCOUNT_AUTH, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 유저_ID_조회_유저가_존재하지_않는_경우_예외발생() {
        // given
        String email = "testUser@naver.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserId(email));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 유저_ID_조회_성공() {
        // given
        String email = "testUser@naver.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .password("password")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when
        Long mockId = userService.getUserId(email);

        // then
        assertEquals(user.getId(), mockId);
    }

    @Test
    @WithMockUser(value = "testUser1@naver.com", roles = {"BUYER"})
    public void 유저_이메일_조회_현재_접속중인_계정과_이름이_다를_경우_예외발생() {
        String email = "testUser@naver.com";

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserEmail(email));
        assertEquals(UserErrorCode.NOT_ACCOUNT_AUTH, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 유저_이메일_조회_유저가_존재하지_않는_경우_예외발생() {
        // given
        String email = "testUser@naver.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserEmail(email));
        assertEquals(UserErrorCode.NOT_EXIST_EMAIL, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 유저_이메일_조회_성공() {
        // given
        String email = "testUser@naver.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // when
        String mockEmail = userService.getUserEmail(email);

        // then
        assertEquals(user.getEmail(), mockEmail);
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 토큰_재발급_리프레시_토큰_검증_실패시_예외발생() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "RT:testUser@naver.com";
        String deviceId = "test-deviceId";

        when(tokenProvider.validateToken(refreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(false)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                .build());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.reissue(accessToken, refreshToken, deviceId));
        assertEquals(UserErrorCode.INVALID_USER_TOKEN, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 토큰_재발급_Redis에서_토큰을_못찾는_경우_예외발생() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "RT:testUser@naver.com";
        String redisRefreshToken = "RT:testUser@naver.com";
        String deviceId = "test-deviceId";

        Authentication authentication = new TestingAuthenticationToken("testUser@naver.com", "password");

        when(tokenProvider.validateToken(refreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);
        when(redisUtils.get(refreshToken)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.reissue(accessToken, refreshToken, deviceId));
        assertEquals(UserErrorCode.REFRESH_TOKEN_NOT_FOUND_IN_REDIS, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 토큰_재발급_Redis에서_가져온_리프레시_토큰_검증_실패시_예외발생() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "RT:testUser@naver.com";
        String redisRefreshToken = "RT:testUser@naver.com";
        String deviceId = "test-deviceId";

        Authentication authentication = new TestingAuthenticationToken("testUser@naver.com", "password");

        when(tokenProvider.validateToken(refreshToken))
                .thenReturn(TokenValidationResult.builder()
                        .valid(true)
                        .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                        .build())
                .thenReturn(TokenValidationResult.builder()
                        .valid(false)
                        .tokenErrorReason(TokenValidationResult.TokenErrorReason.INVALID)
                        .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);
        when(redisUtils.get(refreshToken + ":" + deviceId)).thenReturn(redisRefreshToken);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.reissue(accessToken, refreshToken, deviceId));
        assertEquals(UserErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 토큰_재발급_가져온_리프레시_토큰_불일치시_예외발생() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "RT:testUser@naver.com";
        String redisRefreshToken = "RT:testUser1@naver.com";
        String deviceId = "test-deviceId";

        Authentication authentication = new TestingAuthenticationToken("testUser@naver.com", "password");

        when(tokenProvider.validateToken(refreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.validateToken(redisRefreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);
        when(redisUtils.get(refreshToken + ":" + deviceId)).thenReturn(redisRefreshToken);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> userService.reissue(accessToken, refreshToken, deviceId));
        assertEquals(UserErrorCode.TOKEN_MISMATCH_BETWEEN_CLIENT_AND_SERVER, exception.getErrorCode());
    }

    @Test
    @WithMockUser(value = "testUser@naver.com", roles = {"BUYER"})
    public void 토큰_재발급_성공() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "testUser@naver.com";
        String redisRefreshToken = "testUser@naver.com";
        String deviceId = "test-deviceId";

        Authentication authentication = new TestingAuthenticationToken("testUser@naver.com", "password");
        TokenResponse mockTokenResponse = new TokenResponse(accessToken, "Bearer", refreshToken);

        when(tokenProvider.validateToken(refreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.validateToken(redisRefreshToken)).thenReturn(TokenValidationResult.builder()
                .valid(true)
                .tokenErrorReason(TokenValidationResult.TokenErrorReason.VALID)
                .build());
        when(tokenProvider.getAuthentication(accessToken)).thenReturn(authentication);
        when(redisUtils.get("RT:" + refreshToken + ":" + deviceId)).thenReturn(redisRefreshToken);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn(mockTokenResponse);

        // when
        TokenResponse tokenResponse = userService.reissue(accessToken, refreshToken, deviceId);

        // then
        assertNotNull(tokenResponse);
        assertEquals(accessToken, tokenResponse.getAccessToken());
        assertEquals(refreshToken, tokenResponse.getRefreshToken());
        verify(redisUtils, times(1)).delete(tokenResponse.getRefreshToken());
        verify(redisUtils, times(1)).set("RT:" + authentication.getName() + ":" + deviceId, refreshToken, 1440);
    }
}