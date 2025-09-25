package com.example.mailsender.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    // 데이터 검증 관련 예외
    INVALID_TICKET_NUMBER(HttpStatus.BAD_REQUEST, "예약번호 형식이 올바르지 않습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),

    // 기존 예외
    NOT_ALLOWED_STRING(HttpStatus.BAD_REQUEST, "허용되지 않은 문자열이 포함되어 있습니다."),

    // 인증 예외
    NOT_ALLOWED_ACCOUNT(HttpStatus.FORBIDDEN, "허용되지 않은 계정입니다.")
    ;

    private final HttpStatus status;
    private final String message;
}