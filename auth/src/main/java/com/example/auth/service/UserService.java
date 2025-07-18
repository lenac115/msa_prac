package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.kafka.UserEventProducer;
import com.example.commonevents.auth.SendMailEvent;
import com.example.exception.CustomException;
import com.example.exception.errorcode.CommonErrorCode;
import com.example.exception.errorcode.UserErrorCode;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.redis.RedisUtils;
import com.example.auth.repository.UserRepository;
import com.example.auth.uuid.BasicUUIDGenerator;
import com.example.commonevents.auth.ChangePasswordReq;
import com.example.commonevents.auth.TokenResponse;
import com.example.commonevents.auth.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final RedisUtils redisUtils;
    private final PasswordEncoder passwordEncoder;
    private final BasicUUIDGenerator uuidGenerator;
    private final AuthenticationManager authenticationManager;
    private final UserEventProducer userEventProducer;

    @Transactional
    public TokenResponse reissue(String requestAccessToken, String requestRefreshToken, String deviceId) {
        if (!tokenProvider.validateToken(requestRefreshToken).getValid()) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        Authentication authentication = tokenProvider.getAuthEvenIfExpired(requestAccessToken);

        String redisRefreshToken = Optional.ofNullable(redisUtils.get("RT:" + authentication.getName() + ":" + deviceId))
                .filter(value -> value instanceof String)
                .map(value -> (String) value)
                .orElseThrow(() -> new CustomException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND_IN_REDIS));


        if (!tokenProvider.validateToken(redisRefreshToken).getValid()) {
            redisUtils.delete("RT:" + authentication.getName() + ":" + deviceId);
            throw new CustomException(UserErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!requestRefreshToken.equals(redisRefreshToken)) {
            throw new CustomException(UserErrorCode.TOKEN_MISMATCH_BETWEEN_CLIENT_AND_SERVER);
        }

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);
        redisUtils.delete("RT:" + authentication.getName() + ":" + deviceId);
        redisUtils.set("RT:" + authentication.getName() + ":" + deviceId, tokenResponse.getRefreshToken(), 1440);

        return tokenResponse;
    }

    @Transactional
    public TokenResponse login(String email, String password, String deviceId) {

        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));


        if (!passwordEncoder.matches(password, findUser.getPassword())) {
            throw new CustomException(UserErrorCode.NOT_EQUAL_PASSWORD);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);
        redisUtils.set("RT:" + email + ":" + deviceId, tokenResponse.getRefreshToken(), 1440);

        return tokenResponse;
    }

    @Transactional
    public void logout(String accessToken, String email, String deviceId) {

        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        if (!tokenProvider.validateToken(accessToken).getValid()) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        if (!authentication.getName().equals(findUser.getEmail())) {
            throw new CustomException(UserErrorCode.NOT_ACCOUNT_AUTH);
        }

        SecurityContextHolder.getContext().setAuthentication(null);
        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.delete("RT:" + email + ":" + deviceId);
    }

    public UserDto getUser(String username) {

        return convertUser(userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL)));
    }

    public List<UserDto> getUsers() {
        return Optional.ofNullable(userRepository.findAll())
                .orElseGet(Collections::emptyList).stream().map(this::convertUser).collect(Collectors.toList());
    }

    @Transactional
    public void register(UserDto userDto) {

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new CustomException(UserErrorCode.ALREADY_EXIST_EMAIL);
        }
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        User registerUser = convertUserDto(userDto);
        userRepository.save(registerUser);
    }


    @Transactional
    public void delete(String accessToken, String username) {

        User findUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        if (!tokenProvider.validateToken(accessToken).getValid()) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        if (!authentication.getName().equals(findUser.getEmail())) {
            throw new CustomException(UserErrorCode.NOT_ACCOUNT_AUTH);
        }

        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.deleteAllRefreshTokens(username);
        SecurityContextHolder.getContext().setAuthentication(null);
        userRepository.delete(findUser);
    }

    @Transactional
    public void update(UserDto userDto, String username) {
        User findUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        if (!findUser.getEmail().equals(username)) {
            throw new CustomException(CommonErrorCode.INVALID_PARAMETER);
        }

        findUser.updateUser(userDto);
    }

    @Transactional
    public void changePassword(ChangePasswordReq changePasswordReq, String username, String accessToken) {
        User findUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        if (!passwordEncoder.matches(changePasswordReq.getNewPassword(), findUser.getPassword())) {
            throw new CustomException(UserErrorCode.NOT_EQUAL_PASSWORD);
        }

        if (!tokenProvider.validateToken(accessToken).getValid()) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.delete("RT:" + username);
        SecurityContextHolder.getContext().setAuthentication(null);
        findUser.updatePassword(passwordEncoder.encode(changePasswordReq.getNewPassword()));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (!redisUtils.hasKey(token)) {
            throw new CustomException(CommonErrorCode.INVALID_PARAMETER);
        }

        String username = (String) redisUtils.get(token);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        user.updatePassword(passwordEncoder.encode(newPassword)); // 비밀번호 암호화 저장

        // 사용한 토큰 삭제
        redisUtils.delete(token);
    }

    public void sendResetPasswordEmail(String username) {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        String token = uuidGenerator.generateStringUUID(); // 랜덤 토큰 생성
        redisUtils.set(token, user.getEmail(), 10);

        // 이메일로 비밀번호 재설정 링크 전송
        userEventProducer.sendResetMail(SendMailEvent.builder()
                        .email(user.getEmail())
                        .token(token)
                .build());
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    public Long getUserId(String username) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(username)) {
            throw new CustomException(UserErrorCode.NOT_ACCOUNT_AUTH);
        }

        User user =
                userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        return user.getId();
    }

    public String getUserEmail(String username) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(username)) {
            throw new CustomException(UserErrorCode.NOT_ACCOUNT_AUTH);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        return user.getEmail();
    }

    private User convertUserDto(UserDto userDto) {
        return User.builder()
                .phone(userDto.getPhone())
                .money(userDto.getMoney())
                .email(userDto.getEmail())
                .password(userDto.getPassword())
                .name(userDto.getName())
                .id(userDto.getId())
                .auth(userDto.getAuth())
                .address(userDto.getAddress())
                .birthday(userDto.getBirthDay())
                .build();
    }

    private UserDto convertUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .money(user.getMoney())
                .email(user.getEmail())
                .name(user.getName())
                .auth(user.getAuth())
                .address(user.getAddress())
                .birthDay(user.getBirthday())
                .build();
    }
}
