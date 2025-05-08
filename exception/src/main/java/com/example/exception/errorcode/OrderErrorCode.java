package com.example.exception.errorcode;

import com.example.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    NOT_EXISTS_ORDER(HttpStatus.NOT_FOUND, "Not Exists Order"),;

    private final HttpStatus httpStatus;
    private final String message;
}
