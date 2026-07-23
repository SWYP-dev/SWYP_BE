package com.chwihap.server.domain.notification.service;

import com.chwihap.server.domain.notification.dto.*;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.entity.NotificationSetting;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.notification.repository.NotificationSettingRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int INBOX_DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final Set<Integer> ALLOWED_REMIND_DAYS = Set.of(7, 3, 1, 0);

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 5.1 알림 설정 조회<br>
     * 사용자의 알림 설정이 존재하지 않는 경우 기본값으로 생성한 후 조회한다.
     * @param userId 알림 설정을 조회하려는 유저 ID
     * @return 이메일 주소를 포함한 사용자의 알림 설정 정보 반환
     * @author say_0
     */
    @Transactional
    public NotificationSettingResponse getSettings(Long userId) {
        User user = getUser(userId);
        NotificationSetting setting = notificationSettingRepository.findByUser_Id(userId)
                .orElseGet(() -> notificationSettingRepository.save(NotificationSetting.createDefault(user)));

        return new NotificationSettingResponse(
                setting.isEmailEnabled(),
                setting.isInAppEnabled(),
                user.getEmail(),
                List.copyOf(setting.getRemindDays())
        );
    }

    /**
     * 5.2 알림 설정 수정<br>
     * 사용자의 이메일/인앱 알림 수신 여부와 리마인드 기준일을 수정한다.
     * @param userId 알림 설정을 수정하려는 유저 ID
     * @param request 변경하려는 알림 설정 정보
     * @return 서버에서 정렬 및 중복 제거한 알림 설정 정보 반환
     * @author say_0
     */
    @Transactional
    public NotificationSettingUpdateResponse updateSettings(
            Long userId,
            NotificationSettingUpdateRequest request
    ) {
        List<Integer> remindDays = normalizeRemindDays(request.remindDays());
        User user = getUser(userId);
        NotificationSetting setting = notificationSettingRepository.findByUser_Id(userId)
                .orElseGet(() -> notificationSettingRepository.save(NotificationSetting.createDefault(user)));

        setting.update(request.emailEnabled(), request.inAppEnabled(), remindDays);
        return new NotificationSettingUpdateResponse(
                setting.isEmailEnabled(),
                setting.isInAppEnabled(),
                List.copyOf(setting.getRemindDays())
        );
    }

    /**
     * 5.3 알림 발송 이력 조회<br>
     * 사용자에게 발송된 이메일 알림 이력을 커서 기반으로 최신순 조회한다.
     * @param userId 이메일 알림 이력을 조회하려는 유저 ID
     * @param cursor 다음 이메일 알림 이력을 조회하기 위한 커서
     * @param size 조회할 이메일 알림 이력 개수
     * @return 다음 커서와 이메일 알림 발송 이력 목록 반환
     * @author say_0
     */
    public NotificationHistoryResponse getHistory(Long userId, String cursor, Integer size) {
        Long cursorId = parseCursor(cursor);
        int pageSize = resolveSize(size, DEFAULT_SIZE);
        List<Notification> result = notificationRepository.findHistory(
                userId,
                NotificationType.EMAIL,
                cursorId,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = result.size() > pageSize;
        List<Notification> page = hasNext ? result.subList(0, pageSize) : result;

        List<NotificationHistoryItemResponse> items = page.stream()
                .map(notification -> new NotificationHistoryItemResponse(
                        notification.getId(),
                        notification.getType(),
                        notification.getKanbanCard().getId(),
                        notification.getKanbanCard().getJobPosting().getCompanyName(),
                        notification.getMessage(),
                        notification.getSentAt(),
                        notification.getStatus()
                ))
                .toList();
        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;
        return new NotificationHistoryResponse(items, nextCursor, hasNext);
    }

    /**
     * 5.4 인앱 알림 목록 조회<br>
     * 사용자의 최근 인앱 알림을 커서 기반으로 최신순 조회하고, 전체 미읽음 알림 개수를 함께 반환한다.
     * @param userId 인앱 알림을 조회하려는 유저 ID
     * @param cursor 다음 인앱 알림(더보기)을 조회하기 위한 커서
     * @param size 조회할 인앱 알림 개수
     * @return 다음 커서와 미읽음 개수, 인앱 알림 목록 반환
     * @author say_0
     */
    public InAppNotificationListResponse getInbox(Long userId, String cursor, Integer size) {
        Long cursorId = parseCursor(cursor);
        int pageSize = resolveSize(size, INBOX_DEFAULT_SIZE);
        List<Notification> result = notificationRepository.findInbox(
                userId,
                NotificationType.IN_APP,
                cursorId,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = result.size() > pageSize;
        List<Notification> page = hasNext ? result.subList(0, pageSize) : result;

        List<InAppNotificationItemResponse> items = page.stream()
                .map(notification -> new InAppNotificationItemResponse(
                        notification.getId(),
                        notification.getKanbanCard().getId(),
                        notification.getKanbanCard().getJobPosting().getCompanyName(),
                        notification.getMessage(),
                        notification.isRead(),
                        notification.getCreatedAt()
                ))
                .toList();

        String nextCursor = hasNext ? String.valueOf(page.get(page.size() - 1).getId()) : null;
        long unreadCount = notificationRepository.countByUser_IdAndTypeAndIsReadFalse(
                userId, NotificationType.IN_APP);
        return new InAppNotificationListResponse(unreadCount, items, nextCursor, hasNext);
    }

    /**
     * 5.5 인앱 알림 읽음 처리<br>
     * 요청한 ID 중 로그인 사용자가 소유한 미읽음 인앱 알림만 읽음 처리한다.
     * @param userId 인앱 알림을 읽음 처리하려는 유저 ID
     * @param request 읽음 처리할 인앱 알림 ID 목록
     * @return 실제 읽음 처리된 인앱 알림 개수 반환
     * @author say_0
     */
    @Transactional
    public InAppNotificationReadResponse readInbox(Long userId, InAppNotificationReadRequest request) {
        List<Long> ids = request.ids().stream().distinct().toList();
        int updatedCount = notificationRepository.markAsRead(userId, NotificationType.IN_APP, ids);
        return new InAppNotificationReadResponse(updatedCount);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<Integer> normalizeRemindDays(List<Integer> remindDays) {
        if (remindDays == null
                || remindDays.stream().anyMatch(day -> day == null || !ALLOWED_REMIND_DAYS.contains(day))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new LinkedHashSet<>(remindDays).stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(cursor);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private int resolveSize(Integer size, int defaultSize) {
        if (size == null || size <= 0) {
            return defaultSize;
        }
        return Math.min(size, MAX_SIZE);
    }
}
