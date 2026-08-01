package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageDeadlineUpdateRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageDeadlineUpdateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanDeadlineListResponse;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ApplicationPostingRepository applicationPostingRepository;
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
                applicationPostingRepository,
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
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage stage = mock(KanbanStage.class);

        when(kanbanCardRepository.findUpcomingDeadlineCards(userId, today)).thenReturn(List.of(card));
        when(card.getId()).thenReturn(10L);
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(stage);
        when(applicationPosting.getCompanyName()).thenReturn("취합");
        when(applicationPosting.getTitle()).thenReturn("백엔드 개발자");
        when(applicationPosting.getDeadline()).thenReturn(deadline);
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
    void 지원_마감일과_전형_단계를_수정하고_대상_스테이지의_최상단으로_이동한다() {
        Long userId = 1L;
        Long cardId = 10L;
        LocalDate deadline = LocalDate.of(2026, 8, 20);
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage oldStage = mock(KanbanStage.class);
        KanbanStage targetStage = mock(KanbanStage.class);
        User user = mock(User.class);
        KanbanCardStageDeadlineUpdateRequest request =
                new KanbanCardStageDeadlineUpdateRequest(3L, deadline);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(kanbanStageRepository.findByUserIdAndId(userId, 3L)).thenReturn(Optional.of(targetStage));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(oldStage);
        when(card.getPosition()).thenReturn(2);
        when(oldStage.getId()).thenReturn(1L);
        when(targetStage.getId()).thenReturn(3L);
        when(targetStage.getStageName()).thenReturn("서류 지원");
        when(applicationPosting.getDeadline()).thenReturn(deadline);

        KanbanCardStageDeadlineUpdateResponse response =
                kanbanCardService.updateStageAndDeadline(userId, cardId, request);

        verify(applicationPosting).updateDeadline(deadline);
        verify(kanbanCardRepository).updatePosition(cardId, -10);
        verify(kanbanCardRepository).shiftPositionsAfterDelete(1L, 2);
        verify(kanbanCardRepository).shiftPositionsFrom(3L, 1);
        verify(kanbanCardRepository).updateStageAndPosition(cardId, 3L, 1);
        verify(applicationPosting, never()).getPlatform();
        assertThat(response).isEqualTo(
                new KanbanCardStageDeadlineUpdateResponse(
                        cardId, 3L, "서류 지원", 1, deadline));
    }

    @Test
    void 같은_전형_단계를_요청하면_기존_위치를_유지한다() {
        Long userId = 1L;
        Long cardId = 10L;
        LocalDate deadline = LocalDate.of(2026, 8, 20);
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage stage = mock(KanbanStage.class);
        User user = mock(User.class);
        KanbanCardStageDeadlineUpdateRequest request =
                new KanbanCardStageDeadlineUpdateRequest(3L, deadline);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(kanbanStageRepository.findByUserIdAndId(userId, 3L)).thenReturn(Optional.of(stage));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(stage);
        when(card.getPosition()).thenReturn(2);
        when(stage.getId()).thenReturn(3L);
        when(stage.getStageName()).thenReturn("서류 지원");
        when(applicationPosting.getDeadline()).thenReturn(deadline);

        KanbanCardStageDeadlineUpdateResponse response =
                kanbanCardService.updateStageAndDeadline(userId, cardId, request);

        verify(applicationPosting).updateDeadline(deadline);
        verify(kanbanCardRepository, never()).updatePosition(anyLong(), anyInt());
        verify(kanbanCardRepository, never()).shiftPositionsForMoveUp(anyLong(), anyInt(), anyInt());
        verify(kanbanCardRepository, never()).updateStageAndPosition(anyLong(), anyLong(), anyInt());
        verify(kanbanCardRepository, never()).shiftPositionsAfterDelete(anyLong(), anyInt());
        verify(kanbanCardRepository, never()).shiftPositionsFrom(anyLong(), anyInt());
        assertThat(response.position()).isEqualTo(2);
        assertThat(response.deadline()).isEqualTo(deadline);
    }

    @Test
    void 지원_마감일만_수정하면_전형_단계와_위치를_유지한다() {
        Long userId = 1L;
        Long cardId = 10L;
        LocalDate deadline = LocalDate.of(2026, 8, 20);
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage stage = mock(KanbanStage.class);
        User user = mock(User.class);
        KanbanCardStageDeadlineUpdateRequest request =
                new KanbanCardStageDeadlineUpdateRequest(null, deadline);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(stage);
        when(card.getPosition()).thenReturn(2);
        when(stage.getId()).thenReturn(3L);
        when(stage.getStageName()).thenReturn("서류 지원");
        when(applicationPosting.getDeadline()).thenReturn(deadline);

        KanbanCardStageDeadlineUpdateResponse response =
                kanbanCardService.updateStageAndDeadline(userId, cardId, request);

        verify(applicationPosting).updateDeadline(deadline);
        verifyNoInteractions(kanbanStageRepository);
        verify(kanbanCardRepository, never()).updatePosition(anyLong(), anyInt());
        verify(kanbanCardRepository, never()).updateStageAndPosition(anyLong(), anyLong(), anyInt());
        assertThat(response.stageId()).isEqualTo(3L);
        assertThat(response.position()).isEqualTo(2);
        assertThat(response.deadline()).isEqualTo(deadline);
    }

    @Test
    void 전형_단계만_수정하면_기존_마감일을_유지하고_최상단으로_이동한다() {
        Long userId = 1L;
        Long cardId = 10L;
        LocalDate existingDeadline = LocalDate.of(2026, 8, 10);
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage oldStage = mock(KanbanStage.class);
        KanbanStage targetStage = mock(KanbanStage.class);
        User user = mock(User.class);
        KanbanCardStageDeadlineUpdateRequest request =
                new KanbanCardStageDeadlineUpdateRequest(3L, null);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(kanbanStageRepository.findByUserIdAndId(userId, 3L)).thenReturn(Optional.of(targetStage));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(oldStage);
        when(card.getPosition()).thenReturn(2);
        when(oldStage.getId()).thenReturn(1L);
        when(targetStage.getId()).thenReturn(3L);
        when(targetStage.getStageName()).thenReturn("서류 지원");
        when(applicationPosting.getDeadline()).thenReturn(existingDeadline);

        KanbanCardStageDeadlineUpdateResponse response =
                kanbanCardService.updateStageAndDeadline(userId, cardId, request);

        verify(applicationPosting, never()).updateDeadline(any());
        verify(kanbanCardRepository).updatePosition(cardId, -10);
        verify(kanbanCardRepository).shiftPositionsAfterDelete(1L, 2);
        verify(kanbanCardRepository).shiftPositionsFrom(3L, 1);
        verify(kanbanCardRepository).updateStageAndPosition(cardId, 3L, 1);
        assertThat(response.deadline()).isEqualTo(existingDeadline);
        assertThat(response.position()).isEqualTo(1);
    }

    @Test
    void 수정할_항목이_없으면_DB를_조회하기_전에_요청을_거부한다() {
        KanbanCardStageDeadlineUpdateRequest request =
                new KanbanCardStageDeadlineUpdateRequest(null, null);

        assertThatThrownBy(() -> kanbanCardService.updateStageAndDeadline(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verifyNoInteractions(
                userRepository,
                kanbanCardRepository,
                kanbanStageRepository,
                jobPostingRepository
        );
    }

    @Test
    void 파일_문서가_없으면_카드_삭제_시_문서와_ApplicationPosting을_즉시_하드_삭제한다() {
        // Given(준비)
        Long userId = 1L;
        Long cardId = 2L;
        Long applicationPostingId = 3L;
        Document link = mock(Document.class);
        Document memo = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, applicationPostingId, List.of(link, memo));
        ApplicationPosting applicationPosting = card.getApplicationPosting();

        // When(언제)
        when(link.getDocType()).thenReturn(DocumentType.LINK);
        when(memo.getDocType()).thenReturn(DocumentType.MEMO);

        // 카드삭제 진행
        kanbanCardService.deleteCard(userId, cardId);

        // Then(검증)
        verify(documentRepository).deleteAll(List.of(link, memo));
        verify(link, never()).softDelete(); // link를 Hard delete 하는지 검증(문서만 soft delete)
        verify(memo, never()).softDelete(); // memo를 Hard delete 하는지 검증(문서만 soft delete)
        verify(applicationPostingRepository).delete(applicationPosting);
        verify(kanbanCardRepository).delete(card);
    }

    @Test
    void FILE_문서가_있으면_카드_삭제_시_파일은_soft_delete하고_ApplicationPosting은_유지한다() {
        // Given
        Long userId = 1L;
        Long cardId = 2L;
        Long applicationPostingId = 3L;
        Document file = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, applicationPostingId, List.of(file));
        ApplicationPosting applicationPosting = card.getApplicationPosting();
        // When
        when(file.getDocType()).thenReturn(DocumentType.FILE);

        kanbanCardService.deleteCard(userId, cardId);

        // Then
        verify(file).softDelete();
        verify(documentRepository, never()).deleteAll(anyList());
        verify(applicationPostingRepository, never()).delete(applicationPosting);
        verify(applicationPosting).detachSourceJobPosting();
    }

    @Test
    void 알림이_연결된_카드는_알림을_먼저_삭제한_후_카드를_삭제한다() {
        // Given
        Long userId = 1L;
        Long cardId = 2L;
        Long applicationPostingId = 3L;
        KanbanCard card = stubCardDeletion(userId, cardId, applicationPostingId, List.of());

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
            Long applicationPostingId,
            List<Document> documents
    ) {
        User user = mock(User.class);
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanStage stage = mock(KanbanStage.class);

        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(card.getStage()).thenReturn(stage);
        when(card.getPosition()).thenReturn(1);
        when(stage.getId()).thenReturn(4L);
        when(applicationPosting.getId()).thenReturn(applicationPostingId);
        when(documentRepository.findByUser_IdAndApplicationPosting_Id(userId, applicationPostingId))
                .thenReturn(documents);
        return card;
    }
}
