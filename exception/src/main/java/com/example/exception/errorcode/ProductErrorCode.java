package com.example.exception.errorcode;

import com.example.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    NOT_EXIST_PRODUCT(HttpStatus.NOT_FOUND, "Not Exist Product"),;


    private final HttpStatus httpStatus;
    private final String message;
}
