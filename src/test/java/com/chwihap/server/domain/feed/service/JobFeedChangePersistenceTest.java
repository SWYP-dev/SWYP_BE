package com.chwihap.server.domain.feed.service;

import com.chwihap.server.domain.feed.entity.JobFeed;
import com.chwihap.server.domain.feed.entity.JobFeedChange;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobFeedChangeRepository;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.notification.entity.Notification;
import com.chwihap.server.domain.notification.enums.NotificationType;
import com.chwihap.server.domain.notification.enums.NotificationStatus;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway와 Hibernate validate를 실제 MySQL의 전용 테스트 스키마에서 검증한다.
 * CHWIHAP_MYSQL_TEST_URL=jdbc:mysql://localhost:3306/chwihap_notification_test_...
 * CHWIHAP_MYSQL_TEST_USERNAME, CHWIHAP_MYSQL_TEST_PASSWORD도 지정해야 한다.
 * 일반 test 프로필의 create-drop을 사용하지 않는다.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JobFeedUpdateService.class)
@EnabledIfEnvironmentVariable(named = "CHWIHAP_MYSQL_TEST_URL",
        matches = "jdbc:mysql://[^/]+/chwihap_notification_test_[A-Za-z0-9_]+(?:\\?.*)?")
class JobFeedChangePersistenceTest {
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private KanbanCardRepository cards;
    @Autowired private JobFeedChangeRepository changes;
    @Autowired private JobFeedUpdateService updateService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("CHWIHAP_MYSQL_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("CHWIHAP_MYSQL_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("CHWIHAP_MYSQL_TEST_PASSWORD"));
    }

    @Test
    void 원본이_연결된_기존_카드만_변경_알림_대상으로_조회한다() {
        LocalDateTime changedAt = LocalDateTime.now().plusMinutes(1);
        JobFeed feed = feed();
        User user = user();
        entityManager.persist(user);
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 완료", 1);
        entityManager.persist(stage);
        KanbanCard before = card(feed, user, stage, 1);
        KanbanCard after = card(feed, user, stage, 2);
        ApplicationPosting direct = ApplicationPosting.createDirect(user, "회사", "직접 등록", null, null);
        entityManager.persist(direct);
        entityManager.persist(KanbanCard.createCard(stage, direct, 3));
        KanbanCard detached = card(feed, user, stage, 4);
        detached.getApplicationPosting().detachSourceJobPosting();
        card(JobFeed.create(feed.getExternalId(), "회사", "제목", null,
                null, null, JobPlatform.ROCKETPUNCH, Set.of(), null, null, null), user, stage, 5);
        entityManager.flush();
        jdbc.update("UPDATE kanban_cards SET created_at = ? WHERE id = ?",
                changedAt.plusMinutes(1), after.getId());
        entityManager.clear();

        assertThat(cards.findJobFeedChangeTargets(JobPlatform.PUBLIC, feed.getExternalId(), changedAt))
                .extracting(KanbanCard::getId).containsExactly(before.getId());
    }

    @Test
    void 동일_이력과_카드와_채널의_중복을_DB에서_차단한다() {
        JobFeed feed = feed();
        User user = user();
        entityManager.persist(user);
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 전", 1);
        entityManager.persist(stage);
        KanbanCard card = card(feed, user, stage, 1);
        JobFeedChange change = changes.save(JobFeedChange.capture(feed, "새 회사", null, null,
                LocalDateTime.now()));
        Notification first = Notification.jobFeedChanged(user, card, change, NotificationType.IN_APP, "변경");
        entityManager.persist(first);
        Notification email = Notification.jobFeedChanged(user, card, change, NotificationType.EMAIL, "메일");
        entityManager.persist(email);
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class,
                email.getId())).isEqualTo("PENDING");
        assertThat(email.getDaysBeforeDeadline()).isNull();
        email.completeEmail(NotificationStatus.SUCCESS, LocalDateTime.now());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class,
                email.getId())).isEqualTo("SUCCESS");

        // Repository의 예외 변환을 통해 DB 유일 제약 위반을 확인한다.
        Notification duplicate = Notification.jobFeedChanged(user, card, change, NotificationType.IN_APP, "중복");
        assertThatThrownBy(() -> notificationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 처리_완료한_변경_이력은_다시_조회되지_않는다() {
        JobFeed feed = feed();
        entityManager.persist(feed);
        updateService.update(feed, "새 회사", "제목", null, null, null,
                Set.of(), null, null, null);
        entityManager.flush();
        // 변경 시각은 알림 시간대(Asia/Seoul)이므로 충분히 뒤인 기준 시각을 사용한다.
        LocalDateTime cutoff = LocalDateTime.now().plusDays(1);
        JobFeedChange change = changes
                .findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(cutoff)
                .stream().filter(item -> item.getExternalId().equals(feed.getExternalId()))
                .findFirst().orElseThrow();
        change.markProcessed(cutoff);
        entityManager.flush();
        entityManager.clear();

        assertThat(changes.findByProcessedAtIsNullAndChangedAtLessThanEqualOrderByChangedAtAscIdAsc(cutoff))
                .extracting(JobFeedChange::getId).doesNotContain(change.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 갱신이_롤백되면_변경_이력도_함께_롤백된다() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        JobFeed feed = feed();
        transaction.executeWithoutResult(status -> entityManager.persist(feed));
        try {
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                JobFeed managed = entityManager.find(JobFeed.class, feed.getId());
                updateService.update(managed, "새 회사", "제목", null, null, null,
                        Set.of(), null, null, null);
                entityManager.flush();
                throw new IllegalStateException("롤백");
            })).isInstanceOf(IllegalStateException.class);

            transaction.executeWithoutResult(status -> {
                assertThat(entityManager.find(JobFeed.class, feed.getId()).getCompanyName()).isEqualTo("회사");
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_feed_changes WHERE external_id = ?",
                        Long.class, feed.getExternalId())).isZero();
            });
        } finally {
            transaction.executeWithoutResult(status -> entityManager.remove(
                    entityManager.find(JobFeed.class, feed.getId())));
        }
    }

    private KanbanCard card(JobFeed feed, User user, KanbanStage stage, int position) {
        JobPosting source = JobPosting.copyFromFeed(feed, user);
        entityManager.persist(source);
        ApplicationPosting posting = ApplicationPosting.copyFromJobPosting(source);
        entityManager.persist(posting);
        KanbanCard card = KanbanCard.createCard(stage, posting, position);
        entityManager.persist(card);
        return card;
    }

    private JobFeed feed() {
        return JobFeed.create(UUID.randomUUID().toString(), "회사", "제목", null, null, null,
                JobPlatform.PUBLIC, Set.of(), null, null, null);
    }

    private User user() {
        String id = UUID.randomUUID().toString();
        return User.create(id + "@example.com", "사용자", null, AuthProvider.KAKAO, id);
    }
}
