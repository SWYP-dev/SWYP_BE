package com.chwihap.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증 실패 또는 토큰이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않거나 만료된 토큰입니다."),
    INVALID_KAKAO_CODE(HttpStatus.BAD_REQUEST, "A003", "유효하지 않은 카카오 인가 코드입니다."),
    KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "A004", "카카오 서버 오류가 발생했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "만료되거나 유효하지 않은 Refresh Token입니다."),

    POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하지 않는 공고입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "F003", "스크랩한 공고를 찾을 수 없습니다."),

    // Kanban
    STAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "K001", "커스텀 스테이지는 최대 10개까지 생성할 수 있습니다."),
    POSITION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST,"K002","position 값이 유효 범위를 벗어났습니다."),
    DUPLICATE_KANBAN_CARD(HttpStatus.CONFLICT, "K003", "이미 등록된 공고입니다."),
    DEFAULT_STAGE_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"K004","기본 스테이지는 삭제할 수 없습니다."),

    STAGE_NAME_REQUIRED(HttpStatus.BAD_REQUEST,"K005", "전형 이름을 입력해주세요."),
    STAGE_NAME_DUPLICATE(HttpStatus.BAD_REQUEST,"K006", "이미 존재하는 전형 이름입니다."),
    STAGE_NAME_SPECIAL_CHAR(HttpStatus.BAD_REQUEST, "K007", "올바른 전형 이름을 입력해주세요."),
    STAGE_NAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "K008", "스테이지명 최소 2자 입력 제한"),
    STAGE_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "K009", "스테이지명 최대 20자 입력 제한"),

    CARD_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "K010", "직접 등록한 공고만 수정할 수 있습니다."),

    CARD_COMPANY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "K011", "회사 이름을 입력해주세요."),
    CARD_COMPANY_NAME_SPECIAL_CHAR(HttpStatus.BAD_REQUEST, "K012", "올바른 회사명을 입력해주세요."),
    CARD_COMPANY_NAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "K013", "회사명 최소 2자 입력 제한"),
    CARD_COMPANY_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "K014", "회사명 최대 50자 입력 제한"),

    CARD_JOB_POSTING_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "K015", "공고명을 입력해주세요."),
    CARD_JOB_POSTING_NAME_SPECIAL_CHAR(HttpStatus.BAD_REQUEST, "K016", "올바른 공고명을 입력해주세요."),
    CARD_JOB_POSTING_NAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "K017", "공고명 최소 2자 입력 제한"),
    CARD_JOB_POSTING_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "K018", "공고명 최대 100자 입력 제한"),

    CARD_JOB_POSTING_URL_REQUIRED(HttpStatus.BAD_REQUEST, "K019", "공고 링크를 입력해주세요."),
    CARD_JOB_POSTING_URL_INVALID(HttpStatus.BAD_REQUEST, "K020", "올바른 공고 링크를 입력해주세요."),
    CARD_JOB_POSTING_URL_TOO_LONG(HttpStatus.BAD_REQUEST, "K021", "공고 링크 최대 2048자 입력 제한"),
    STAGE_HAS_CARDS(HttpStatus.CONFLICT, "K022", "지원 내역이 있는 단계입니다. 카드를 이동할 단계를 선택해주세요."),
    DEFAULT_STAGE_NAME_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "K023", "기본 스테이지의 이름은 변경할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
