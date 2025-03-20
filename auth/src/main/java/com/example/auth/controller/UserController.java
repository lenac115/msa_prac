package com.example.auth.controller;

import com.example.auth.dto.ChangePasswordReq;
import com.example.auth.dto.ResetPasswordReq;
import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserDto;
import com.example.auth.kafka.UserEventProducer;
import com.example.auth.service.EmailService;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserEventProducer userEventProducer;

    @PostMapping("/public/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.login(userDto.getEmail(), userDto.getPassword()));
    }

    @PostMapping("/common/logout")
    public ResponseEntity<Object> logout(@RequestHeader("Authorization") String authorizationHeader, @AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(authorizationHeader, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body("User logged out");
    }

    @GetMapping("/common/get/me")
    public ResponseEntity<Object> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        userEventProducer.sendUserInfo(userService.getUser(userDetails.getUsername()));
        return ResponseEntity.status(HttpStatus.OK).body("User-info transport");
    }

    @GetMapping("/seller/get/user/list")
    public ResponseEntity<Object> getUserList() {
        userEventProducer.sendUserList(userService.getUsers());
        return ResponseEntity.status(HttpStatus.OK).body("User-list transport");
    }

    @PostMapping("/public/register")
    public ResponseEntity<Object> register(@RequestBody UserDto userDto) {
        userService.register(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }

    @PostMapping("/common/update")
    public ResponseEntity<Object> update(@RequestBody UserDto userDto, @AuthenticationPrincipal UserDetails userDetails) {
        userService.update(userDto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body("User updated");
    }

    @PostMapping("/common/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenResponse tokenResponse) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.reissue(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken()));
    }

    @PostMapping("/common/change-password")
    public ResponseEntity<Object> changePassword(@RequestBody ChangePasswordReq changePasswordReq, @AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestHeader("Authorization") String authorizationHeader) {
        userService.changePassword(changePasswordReq, userDetails.getUsername(), authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body("Password changed");
    }

    @PostMapping("/public/send/reset-password-email")
    public ResponseEntity<Object> resetEmailSend(@RequestBody String username) {
        userService.sendResetPasswordEmail(username);
        return ResponseEntity.status(HttpStatus.OK).body("Reset mail Send");
    }

    @GetMapping("/public/reset-password-page")
    public String resetPasswordPage(@RequestParam String token) {
        return token;
    }

    @PostMapping("/public/reset-password-page")
    public ResponseEntity<Object> resetPassword(@RequestBody ResetPasswordReq resetPasswordReq) {
        userService.resetPassword(resetPasswordReq.getToken(), resetPasswordReq.getNewPassword());

        return ResponseEntity.status(HttpStatus.OK).body("Password changed");
    }

    @PostMapping("/common/delete")
    public ResponseEntity<Object> delete(@RequestHeader("Authorization") String authorizationHeader, @AuthenticationPrincipal UserDetails userDetails) {
        userService.delete(authorizationHeader, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body("User deleted");
    }

    @GetMapping("/common/get/buyer-id")
    public ResponseEntity<Long> getBuyerId(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserId(userDetails.getUsername()));
    }

    @GetMapping("/common/get/buyer-email")
    public ResponseEntity<String> getBuyerEmail(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserEmail(userDetails.getUsername()));
    }
}
