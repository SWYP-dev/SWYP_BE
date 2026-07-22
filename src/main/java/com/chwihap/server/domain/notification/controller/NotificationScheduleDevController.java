package com.chwihap.server.domain.notification.controller;

import com.chwihap.server.domain.notification.service.NotificationScheduler;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 개발 환경에서 cron(매일 09시) 대기 없이 마감일 알림 배치를 즉시 실행하기 위한 전용 엔드포인트.
 * 이메일(다이제스트)과 인앱 알림은 {@link NotificationScheduler#dispatch()} 하나에서 함께 처리되므로
 * 별도 엔드포인트 없이 단일 호출로 두 알림을 모두 트리거한다.
 * {@code local} 프로필에서만 등록되며, 운영 환경에는 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/dev/notification-schedule")
@RequiredArgsConstructor
@Profile("local")
public class NotificationScheduleDevController {

    private final NotificationScheduler notificationScheduler;

    @PostMapping
    public ApiResponse<Void> triggerDispatch() {
        notificationScheduler.dispatch();
        return ApiResponse.success();
    }
}
