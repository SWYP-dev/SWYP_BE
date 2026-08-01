package com.chwihap.server.domain.document.service;

import com.chwihap.server.domain.document.config.S3Properties;
import com.chwihap.server.domain.document.dto.DocumentLinkCategoryUpdateRequest;
import com.chwihap.server.domain.document.dto.DocumentLinkCreateRequest;
import com.chwihap.server.domain.document.dto.DocumentLinkUpdateRequest;
import com.chwihap.server.domain.document.dto.DocumentResponse;
import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentLinkCategory;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.document.storage.DocumentStorage;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private KanbanCardRepository kanbanCardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentStorage documentStorage;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setMaxFileSize(DataSize.ofMegabytes(10));
        properties.setAccountStorageLimit(DataSize.ofMegabytes(100));
        properties.setPresignedUrlDuration(Duration.ofMinutes(10));
        documentService = new DocumentService(
                documentRepository,
                kanbanCardRepository,
                userRepository,
                documentStorage,
                properties
        );
    }

    @Test
    void 같은_버전_그룹에_업로드하면_버전이_증가한다() {
        // Given
        Long userId = 1L;
        Long cardId = 10L;
        User user = User.create("user@example.com", "사용자", null, null, null);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanCard card = mock(KanbanCard.class);
        Document previousDocument = mock(Document.class);
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "content".getBytes());

        // When
        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.sumActiveFileSizeByUserId(userId, DocumentType.FILE)).thenReturn(0L);
        when(documentRepository.findTopByUser_IdAndApplicationPosting_IdAndVersionGroupOrderByVersionDesc(
                any(), any(), any())).thenReturn(Optional.of(previousDocument));
        when(previousDocument.getVersion()).thenReturn(2);
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.uploadFile(userId, cardId, file);

        // Then
        assertThat(response.version()).isEqualTo(3);
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
        verify(documentStorage).upload(any(), any(), any(Long.class), any());
    }

    @Test
    void 링크를_등록할_때_선택한_카테고리를_저장한다() {
        Long userId = 1L;
        Long cardId = 10L;
        User user = User.create("user@example.com", "사용자", null, null, null);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanCard card = mock(KanbanCard.class);

        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.registerLink(
                userId,
                cardId,
                new DocumentLinkCreateRequest(
                        "https://example.com/portfolio",
                        DocumentLinkCategory.PORTFOLIO
                )
        );

        assertThat(response.category()).isEqualTo(DocumentLinkCategory.PORTFOLIO);
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getLinkCategory()).isEqualTo(DocumentLinkCategory.PORTFOLIO);
    }

    @Test
    void 저장한_링크의_카테고리를_수정할_수_있다() {
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        User user = User.create("user@example.com", "사용자", null, null, null);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanCard card = mock(KanbanCard.class);
        Document document = Document.link(
                applicationPosting,
                "https://example.com/blog",
                DocumentLinkCategory.PERSONAL_CHANNEL
        );

        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(documentId, userId, 20L))
                .thenReturn(Optional.of(document));

        DocumentResponse response = documentService.updateLinkCategory(
                userId,
                cardId,
                documentId,
                new DocumentLinkCategoryUpdateRequest(DocumentLinkCategory.OTHER)
        );

        assertThat(document.getLinkCategory()).isEqualTo(DocumentLinkCategory.OTHER);
        assertThat(response.category()).isEqualTo(DocumentLinkCategory.OTHER);
    }

    @Test
    void 링크가_아닌_문서의_카테고리는_수정할_수_없다() {
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        Document document = mock(Document.class);

        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(documentId, userId, 20L))
                .thenReturn(Optional.of(document));
        when(document.getDocType()).thenReturn(DocumentType.FILE);

        assertThatThrownBy(() -> documentService.updateLinkCategory(
                userId,
                cardId,
                documentId,
                new DocumentLinkCategoryUpdateRequest(DocumentLinkCategory.RESUME)
        ))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_LINK_DOCUMENT_TYPE)
                );
    }

    @Test
    void 저장한_링크의_URL을_수정할_수_있다() {
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        User user = User.create("user@example.com", "사용자", null, null, null);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        KanbanCard card = mock(KanbanCard.class);
        Document document = Document.link(
                applicationPosting,
                "https://example.com/old",
                DocumentLinkCategory.PORTFOLIO
        );

        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(documentId, userId, 20L))
                .thenReturn(Optional.of(document));

        DocumentResponse response = documentService.updateLink(
                userId,
                cardId,
                documentId,
                new DocumentLinkUpdateRequest("https://example.com/new")
        );

        assertThat(document.getFileName()).isNull();
        assertThat(document.getLinkUrl()).isEqualTo("https://example.com/new");
        assertThat(response.name()).isNull();
        assertThat(response.url()).isEqualTo("https://example.com/new");
    }

    @Test
    void 링크가_아닌_문서의_URL은_수정할_수_없다() {
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        Document document = mock(Document.class);

        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(documentId, userId, 20L))
                .thenReturn(Optional.of(document));
        when(document.getDocType()).thenReturn(DocumentType.FILE);

        assertThatThrownBy(() -> documentService.updateLink(
                userId,
                cardId,
                documentId,
                new DocumentLinkUpdateRequest("https://example.com")
        ))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_LINK_DOCUMENT_TYPE)
                );
    }

    @Test
    void 파일_문서를_삭제하면_소프트_삭제한다() {
        // Given
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        Document document = mock(Document.class);

        // When
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(eq(documentId), eq(userId), eq(20L)))
                .thenReturn(Optional.of(document));
        when(document.getDocType()).thenReturn(DocumentType.FILE);

        documentService.deleteDocument(userId, cardId, documentId);

        // Then
        verify(document).softDelete();
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void 파일이_아닌_문서를_삭제하면_하드_삭제한다() {
        // Given
        Long userId = 1L;
        Long cardId = 10L;
        Long documentId = 100L;
        KanbanCard card = mock(KanbanCard.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);
        Document document = mock(Document.class);

        // When
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(card.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(20L);
        when(documentRepository.findActiveByIdAndOwner(eq(documentId), eq(userId), eq(20L)))
                .thenReturn(Optional.of(document));
        when(document.getDocType()).thenReturn(DocumentType.LINK);

        documentService.deleteDocument(userId, cardId, documentId);

        // Then
        verify(documentRepository).delete(document);
        verify(document, never()).softDelete();
    }

    @Test
    void 계정_저장_용량_한도를_초과하면_업로드를_거부한다() {
        // Given
        Long userId = 1L;
        Long cardId = 10L;
        User user = User.create("user@example.com", "사용자", null, null, null);
        KanbanCard card = mock(KanbanCard.class);
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[10]);

        // When
        when(userRepository.lockById(userId)).thenReturn(Optional.of(user));
        when(kanbanCardRepository.findByIdAndUser_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(documentRepository.sumActiveFileSizeByUserId(userId, DocumentType.FILE))
                .thenReturn(DataSize.ofMegabytes(100).toBytes() - 5);

        // Then
        assertThatThrownBy(() -> documentService.uploadFile(userId, cardId, file))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.STORAGE_LIMIT_EXCEEDED));
        verify(documentStorage, never()).upload(any(), any(), any(Long.class), any());
    }
}
