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
    INVALID_SEND_RANGE(HttpStatus.BAD_REQUEST, "개별발송 범위 값이 올바르지 않습니다."),
    INVALID_SPREADSHEET_URL(HttpStatus.BAD_REQUEST, "스프레드시트 URL 형식이 올바르지 않습니다."),
    INVALID_SPREADSHEET_COLUMN_MAPPING(HttpStatus.BAD_REQUEST, "스프레드시트 열 매핑 값이 올바르지 않습니다."),
    INVALID_SELECTED_RECIPIENTS(HttpStatus.BAD_REQUEST, "발송할 수신자를 한 명 이상 선택해 주세요."),
    INVALID_SPREADSHEET_SNAPSHOT(HttpStatus.BAD_REQUEST, "스프레드시트 미리보기 정보가 없거나 만료되었습니다. 다시 미리보기를 불러와 주세요."),
    SPREADSHEET_SHEET_NOT_FOUND(HttpStatus.BAD_REQUEST, "대상 시트를 찾을 수 없습니다."),
    SPREADSHEET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "스프레드시트 접근 권한이 없거나 추가 동의가 필요합니다."),
    SPREADSHEET_READ_FAILED(HttpStatus.BAD_GATEWAY, "스프레드시트 데이터를 읽는 데 실패했습니다."),

    // 기존 예외
    NOT_ALLOWED_STRING(HttpStatus.BAD_REQUEST, "허용되지 않은 문자열이 포함되어 있습니다."),

    // 인증 예외
    NOT_ALLOWED_ACCOUNT(HttpStatus.FORBIDDEN, "허용되지 않은 계정입니다.")
    ;

    private final HttpStatus status;
    private final String message;
}
