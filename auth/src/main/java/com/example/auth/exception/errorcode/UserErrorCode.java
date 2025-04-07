package com.example.auth.exception.errorcode;

import com.example.auth.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    NOT_EXIST_EMAIL(HttpStatus.BAD_REQUEST, "Not Exist Email"),
    NOT_ACTIVATED_ACCOUNT(HttpStatus.FORBIDDEN, "Not Activated Account"),
    EXIST_BANNED_LIST(HttpStatus.FORBIDDEN,"Exist Banned List"),
    INVALID_USER_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid User Token"),
    NOT_ACCOUNT_AUTH(HttpStatus.UNAUTHORIZED, "Not Account Auth"),
    NOT_EQUAL_PASSWORD(HttpStatus.UNAUTHORIZED, "Not Equal Password"),
    ALREADY_EXIST_EMAIL(HttpStatus.BAD_REQUEST, "Already Exist Email" ),
    REFRESH_TOKEN_NOT_FOUND_IN_REDIS(HttpStatus.BAD_REQUEST, "Refresh Token Not Found In Redis"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Refresh Token Expired"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Access Token Expired"),
    TOKEN_MISMATCH_BETWEEN_CLIENT_AND_SERVER(HttpStatus.BAD_REQUEST, "Token Mismatch Between Client And Server"),;

    private final HttpStatus httpStatus;
    private final String message;
}
