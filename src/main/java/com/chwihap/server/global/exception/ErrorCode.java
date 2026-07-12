package com.chwihap.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하지 않는 공고입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "F003", "스크랩한 공고를 찾을 수 없습니다."),
  
    // Kanban
    STAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "K001", "커스텀 스테이지는 최대 10개까지 생성할 수 있습니다."),
    POSITION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST,"K002","position 값이 유효 범위를 벗어났습니다."),
    DUPLICATE_KANBAN_CARD(HttpStatus.CONFLICT, "K003", "이미 등록된 공고입니다."),
    DEFAULT_STAGE_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"K004","기본 스테이지는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
