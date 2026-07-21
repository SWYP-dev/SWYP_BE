package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public int deleteExpiredNotifications(LocalDateTime now, int retentionDays) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("알림 보존 기간은 1일 이상이어야 합니다.");
        }

        LocalDateTime threshold = now.minusDays(retentionDays);
        return notificationRepository.deleteByCreatedAtBefore(threshold);
    }
}
