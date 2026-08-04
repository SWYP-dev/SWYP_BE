package com.chwihap.server.domain.document.controller;

import com.chwihap.server.domain.document.storage.S3DocumentCleanupScheduler;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 환경에서 매일 01시까지 기다리지 않고 S3 문서 정리 스케줄러를 실행한다.
 * {@code local} 프로필에서만 등록되므로 운영 환경에는 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/dev/s3-document-cleanup")
@RequiredArgsConstructor
@Profile("local")
public class S3DocumentCleanupDevController {

    private final S3DocumentCleanupScheduler s3DocumentCleanupScheduler;

    @PostMapping
    public ApiResponse<Void> triggerCleanup() {
        s3DocumentCleanupScheduler.deleteSoftDeletedFiles();
        return ApiResponse.success();
    }
}
