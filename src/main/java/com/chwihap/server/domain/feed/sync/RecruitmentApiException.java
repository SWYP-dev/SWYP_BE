package com.chwihap.server.domain.feed.sync;

/**
 * data.go.kr 채용정보 API 호출이 재시도 후에도 실패했을 때 발생한다.
 * 수집 배치는 이 예외를 페이지 단위로 잡아 해당 페이지를 건너뛰고 계속 진행한다.
 */
public class RecruitmentApiException extends RuntimeException {

    public RecruitmentApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
