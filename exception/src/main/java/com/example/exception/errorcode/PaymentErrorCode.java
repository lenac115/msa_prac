package com.example.exception.errorcode;

import com.example.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    NOT_EXIST_PAYMENT(HttpStatus.NOT_FOUND, "Not Exist Payment"),
    FAILED_VALIDATION(HttpStatus.BAD_REQUEST, "Failed Validation"),;


    private final HttpStatus httpStatus;
    private final String message;
}
