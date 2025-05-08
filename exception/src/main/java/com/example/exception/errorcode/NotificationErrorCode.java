package com.example.exception.errorcode;

import com.example.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    SEND_MAIL_FAILURE(HttpStatus.BAD_REQUEST, "Send Mail Failure"),;

    private final HttpStatus httpStatus;
    private final String message;
}
