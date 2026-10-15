package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.enums.CareerType;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobFeedUpdateService {
    private final JobFeedChangeRepository changeRepository;

    @Value("${app.notification.scheduler-zone:Asia/Seoul}")
    private String schedulerZone = "Asia/Seoul";

    /** 수집 페이지의 트랜잭션 안에서 원본 갱신과 변경 이력을 함께 저장한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(JobFeed feed, String companyName, String title, LocalDate deadline,
                       String thumbnailUrl, String originalUrl, Set<CareerType> careerTypes,
                       String category, String region, String regionRaw) {
        JobFeedChange change = JobFeedChange.capture(feed, companyName, deadline, originalUrl,
                LocalDateTime.now(ZoneId.of(schedulerZone)));
        feed.update(companyName, title, deadline, thumbnailUrl, originalUrl, careerTypes,
                category, region, regionRaw);
        if (change != null) {
            changeRepository.save(change);
        }
    }
}
