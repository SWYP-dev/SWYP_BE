package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.BookmarkRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.dto.KanbanDeadlineListResponse;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KanbanCardServiceTest {

    @Mock
    private KanbanCardRepository kanbanCardRepository;
    @Mock
    private KanbanStageRepository kanbanStageRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private KanbanStageService kanbanStageService;

    private KanbanCardService kanbanCardService;

    @BeforeEach
    void setUp() {
        kanbanCardService = new KanbanCardService(
                // 서비스 인스턴스를 생성
                kanbanCardRepository,
                kanbanStageRepository,
                jobPostingRepository,
                bookmarkRepository,
                userRepository,
                documentRepository,
                notificationRepository,
                kanbanStageService
        );
    }

    @Test
    void 지원_마감일_목록을_마감일과_전형_단계_정보로_변환한다() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 7, 30);
        LocalDate deadline = LocalDate.of(2026, 8, 1);
        KanbanCard card = mock(KanbanCard.class);
        JobPosting jobPosting = mock(JobPosting.class);
        KanbanStage stage = mock(KanbanStage.class);

        when(kanbanCardRepository.findUpcomingDeadlineCards(userId, today)).thenReturn(List.of(card));
        when(card.getId()).thenReturn(10L);
        when(card.getJobPosting()).thenReturn(jobPosting);
        when(card.getStage()).thenReturn(stage);
        when(jobPosting.getCompanyName()).thenReturn("취합");
        when(jobPosting.getTitle()).thenReturn("백엔드 개발자");
        when(jobPosting.getDeadline()).thenReturn(deadline);
        when(stage.getId()).thenReturn(20L);
        when(stage.getStageName()).thenReturn("서류 지원");

        KanbanDeadlineListResponse response = kanbanCardService.getDeadlineCards(userId, today);

        assertThat(response.cards()).singleElement().satisfies(item -> {
            assertThat(item.cardId()).isEqualTo(10L);
            assertThat(item.companyName()).isEqualTo("취합");
            assertThat(item.jobTitle()).isEqualTo("백엔드 개발자");
            assertThat(item.deadline()).isEqualTo(deadline);
            assertThat(item.stageId()).isEqualTo(20L);
            assertThat(item.stageName()).isEqualTo("서류 지원");
        });
        verify(kanbanCardRepository).findUpcomingDeadlineCards(userId, today);
    }

    @Test
    void 파일_문서와_북마크가_없으면_카드_삭제_시_문서와_JobPosting을_즉시_하드_삭제한다() {
        // Given(준비)
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        Document link = mock(Document.class);
        Document memo = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, jobPostingId, List.of(link, memo));
        when(bookmarkRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(false);

        // When(언제)
        when(link.getDocType()).thenReturn(DocumentType.LINK);
        when(memo.getDocType()).thenReturn(DocumentType.MEMO);

        // 카드삭제 진행
        kanbanCardService.deleteCard(userId, cardId);

        // Then(검증)
        verify(documentRepository).deleteAll(List.of(link, memo));
        verify(link, never()).softDelete(); // link를 Hard delete 하는지 검증(문서만 soft delete)
        verify(memo, never()).softDelete(); // memo를 Hard delete 하는지 검증(문서만 soft delete)
        verify(jobPostingRepository).deleteById(jobPostingId);
        verify(kanbanCardRepository).delete(card);
    }

    @Test
    void FILE_문서가_있으면_카드_삭제_시_파일은_soft_delete하고_JobPosting은_유지한다() {
        // Given
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        Document file = mock(Document.class);
        stubCardDeletion(userId, cardId, jobPostingId, List.of(file));
        // When
        when(file.getDocType()).thenReturn(DocumentType.FILE);

        kanbanCardService.deleteCard(userId, cardId);

        // Then
        verify(file).softDelete();
        verify(documentRepository, never()).deleteAll(anyList());
        verify(jobPostingRepository, never()).deleteById(jobPostingId);
    }

    @Test
    void 활성_여부와_관계없이_북마크가_남아있으면_카드_삭제_시_JobPosting을_유지한다() {
        // Given: 비활성 Bookmark도 JobPosting을 참조하므로 Bookmark 행이 남아 있으면
        // 카드가 삭제돼도 JobPosting을 지우면 안 된다.
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        Document link = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, jobPostingId, List.of(link));
        when(bookmarkRepository.existsByJobPosting_Id(jobPostingId)).thenReturn(true);

        // When
        when(link.getDocType()).thenReturn(DocumentType.LINK);

        kanbanCardService.deleteCard(userId, cardId);

        // Then
        verify(documentRepository).deleteAll(List.of(link));
        verify(jobPostingRepository, never()).deleteById(jobPostingId);
        verify(kanbanCardRepository).delete(card);
    }

    @Test
    void 알림이_연결된_카드는_알림을_먼저_삭제한_후_카드를_삭제한다() {
        // Given
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        KanbanCard card = stubCardDeletion(userId, cardId, jobPostingId, List.of());

        // When
        kanbanCardService.deleteCard(userId, cardId);

        // Then
        InOrder inOrder = inOrder(notificationRepository, kanbanCardRepository);
        inOrder.verify(notificationRepository).deleteAllByKanbanCardId(cardId);
        inOrder.verify(kanbanCardRepository).delete(card);
        inOrder.verify(kanbanCardRepository).flush();
    }

    // Given
    private KanbanCard stubCardDeletion(
            Long userId,
            Long cardId,
            Long jobPostingId,
            List<Document> documents
    ) {
        User user = mock(User.class);
        KanbanCard card = mock(KanbanCard.class);
        JobPosting jobPosting = mock(JobPosting.class);
        KanbanStage stage = mock(KanbanStage.class);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getJobPosting()).thenReturn(jobPosting);
        when(card.getStage()).thenReturn(stage);
        when(card.getPosition()).thenReturn(1);
        when(stage.getId()).thenReturn(4L);
        when(jobPosting.getId()).thenReturn(jobPostingId);
        when(documentRepository.findByUser_IdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(documents);
        return card;
    }
}
