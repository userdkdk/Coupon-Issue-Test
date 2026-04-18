package com.practice.coupon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C-001","server error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C-002", "Invalid input error"),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "C-003", "Unauthorized user access"),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U-001", "user not found"),

    // EVENT,
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E-001", "event not found"),
    EVENT_COUPON_EXHAUSTED(HttpStatus.BAD_REQUEST, "E-002", "event coupon exhausted"),
    EVENT_CREATE_ERROR(HttpStatus.BAD_REQUEST, "E-003", "event create error"),
    // COUPON
    COUPON_DUPLICATED(HttpStatus.CONFLICT, "CO-001", "Coupon already ordered");




    private final HttpStatus status;
    private final String code;
    private final String message;
}
