package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.dto.ChangePasswordReq;
import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserDto;
import com.example.auth.exception.CustomException;
import com.example.auth.exception.errorcode.CommonErrorCode;
import com.example.auth.exception.errorcode.UserErrorCode;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.redis.RedisUtils;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Transactional
    public TokenResponse reissue(String requestAccessToken, String requestRefreshToken) {
        if (!tokenProvider.validateToken(requestRefreshToken)) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        Authentication authentication = tokenProvider.getAuthentication(requestAccessToken);

        String refreshToken = (String) redisUtils.get("RT:" + authentication.getName());

        if (refreshToken == null) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }
        if (!tokenProvider.validateToken(refreshToken)) {
            redisUtils.delete("RT:" + authentication.getName());
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);
        redisUtils.delete(tokenResponse.getRefreshToken());
        redisUtils.set("RT:" + authentication.getName(), tokenResponse.getRefreshToken(), 1440);

        return tokenResponse;
    }

    @Transactional
    public TokenResponse login(String email, String password) {

        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        if (!password.equals(findUser.getPassword())) {
            throw new CustomException(UserErrorCode.NOT_EQUAL_PASSWORD);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);
        redisUtils.set("RT:" + email, tokenResponse.getRefreshToken(), 1440);

        return tokenResponse;
    }

    @Transactional
    public void logout(String accessToken, String email) {

        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        if (!authentication.getName().equals(findUser.getEmail())) {
            throw new CustomException(UserErrorCode.INVALID_USER_TOKEN);
        }

        SecurityContextHolder.getContext().setAuthentication(null);
        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.delete("RT:" + email);
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

        if (userRepository.findByEmail(userDto.getEmail()).orElse(null) != null) {
            throw new CustomException(UserErrorCode.ALREADY_EXIST_EMAIL);
        }

        User registerUser = convertUserDto(userDto);
        userRepository.save(registerUser);
    }


    @Transactional
    public void delete(String accessToken, String username) {

        User findUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));

        if (!findUser.getEmail().equals(username)) {
            throw new CustomException(UserErrorCode.ALREADY_EXIST_EMAIL);
        }

        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.delete("RT:" + username);
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

        if(!findUser.getPassword().equals(changePasswordReq.getOldPassword())) {
            throw new CustomException(UserErrorCode.NOT_EQUAL_PASSWORD);
        }

        redisUtils.setBlackList(accessToken, "accessToken", 1440);
        redisUtils.delete("RT:" + username);
        SecurityContextHolder.getContext().setAuthentication(null);
        userRepository.delete(findUser);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if(!redisUtils.hasKey(token)) {
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

        String token = UUID.randomUUID().toString(); // 랜덤 토큰 생성
        redisUtils.set(token, user.getEmail(), 10);

        // 이메일로 비밀번호 재설정 링크 전송
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    public Long getUserId(String username) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!authentication.getName().equals(username)) {
            throw new CustomException(UserErrorCode.NOT_EXIST_EMAIL);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        return user.getId();
    }

    public String getUserEmail(String username) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!authentication.getName().equals(username)) {
            throw new CustomException(UserErrorCode.NOT_EXIST_EMAIL);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new CustomException(UserErrorCode.NOT_EXIST_EMAIL));
        return user.getEmail();
    }

    private User convertUserDto(UserDto userDto) {
        return User.builder()
                .phone(userDto.getPhone())
                .email(userDto.getEmail())
                .password(userDto.getPassword())
                .name(userDto.getName())
                .id(userDto.getId())
                .address(userDto.getAddress())
                .build();
    }

    private UserDto convertUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .name(user.getName())
                .address(user.getAddress())
                .build();
    }
}
