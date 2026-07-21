package com.chwihap.server.domain.notification.repository;

import com.chwihap.server.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUser_Id(Long userId);

    List<NotificationSetting> findByUser_IdIn(Collection<Long> userIds);
}
