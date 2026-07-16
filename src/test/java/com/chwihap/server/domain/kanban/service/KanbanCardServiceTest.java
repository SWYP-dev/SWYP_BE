package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    private UserRepository userRepository;
    @Mock
    private DocumentRepository documentRepository;
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
                userRepository,
                documentRepository,
                kanbanStageService
        );
    }

    @Test
    void s3_정리_대상_파일이_없으면_문서와_직접등록_공고를_즉시_하드_삭제한다() {
        // Given(준비)
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        Document link = mock(Document.class);
        Document memo = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, jobPostingId, List.of(link, memo));

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
    void 카드_삭제_시_파일_삭제_일시를_기록하고_저장소가_정리될_때까지_직접_등록한_공고는_유지한다() {
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
    void 공고가_직접_등록이_아니어도_파일이_아닌_문서는_하드_삭제한다() {
        // Given
        Long userId = 1L;
        Long cardId = 2L;
        Long jobPostingId = 3L;
        Document link = mock(Document.class);
        KanbanCard card = stubCardDeletion(userId, cardId, jobPostingId, List.of(link), JobPlatform.SARAMIN);

        // When
        when(link.getDocType()).thenReturn(DocumentType.LINK);

        kanbanCardService.deleteCard(userId, cardId);

        // Then
        verify(documentRepository).deleteAll(List.of(link));
        verify(link, never()).softDelete();
        verify(jobPostingRepository, never()).deleteById(jobPostingId);
        verify(kanbanCardRepository).delete(card);
    }

    // Given
    private KanbanCard stubCardDeletion(
            Long userId,
            Long cardId,
            Long jobPostingId,
            List<Document> documents
    ) {
        return stubCardDeletion(userId, cardId, jobPostingId, documents, JobPlatform.DIRECT);
    }

    // Given
    private KanbanCard stubCardDeletion(
            Long userId,
            Long cardId,
            Long jobPostingId,
            List<Document> documents,
            JobPlatform platform
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
        when(jobPosting.getPlatform()).thenReturn(platform);
        when(documentRepository.findByUser_IdAndJobPosting_Id(userId, jobPostingId))
                .thenReturn(documents);
        return card;
    }
}
