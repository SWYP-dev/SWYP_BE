package com.chwihap.server.domain.document.storage;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3DocumentCleanupServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private ApplicationPostingRepository applicationPostingRepository;
    @Mock
    private KanbanCardRepository kanbanCardRepository;

    private S3DocumentCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new S3DocumentCleanupService(
                documentRepository,
                documentStorage,
                applicationPostingRepository,
                kanbanCardRepository
        );
    }

    @Test
    void 마지막_파일이_저장소에서_삭제되면_고아가_된_직접_등록_공고도_함께_삭제한다() {
        Long applicationPostingId = 10L;
        Document document = mock(Document.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);

        when(documentRepository.findByDocTypeAndDeletedAtIsNotNull(DocumentType.FILE))
                .thenReturn(List.of(document));
        when(document.getFileUrl()).thenReturn("documents/resume.pdf");
        when(document.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(applicationPostingId);
        when(kanbanCardRepository.existsByApplicationPosting_Id(applicationPostingId)).thenReturn(false);
        when(documentRepository.existsByApplicationPosting_IdAndDocTypeAndDeletedAtIsNotNull(
                applicationPostingId, DocumentType.FILE)).thenReturn(false);

        cleanupService.deleteSoftDeletedFiles();

        verify(documentStorage).delete("documents/resume.pdf");
        verify(documentRepository).delete(document);
        verify(applicationPostingRepository).deleteById(applicationPostingId);
    }

    @Test
    void 다른_파일이_정리_대기_중이면_직접_등록_공고를_유지한다() {
        Long applicationPostingId = 10L;
        Document document = mock(Document.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);

        when(documentRepository.findByDocTypeAndDeletedAtIsNotNull(DocumentType.FILE))
                .thenReturn(List.of(document));
        when(document.getFileUrl()).thenReturn("documents/resume.pdf");
        when(document.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(applicationPostingId);
        when(kanbanCardRepository.existsByApplicationPosting_Id(applicationPostingId)).thenReturn(false);
        when(documentRepository.existsByApplicationPosting_IdAndDocTypeAndDeletedAtIsNotNull(
                applicationPostingId, DocumentType.FILE)).thenReturn(true);

        cleanupService.deleteSoftDeletedFiles();

        verify(documentRepository).delete(document);
        verify(applicationPostingRepository, never()).deleteById(applicationPostingId);
    }

    @Test
    void 저장소_삭제에_실패하면_문서는_삭제하지_않고_유지한다() {
        Document document = mock(Document.class);
        ApplicationPosting applicationPosting = mock(ApplicationPosting.class);

        when(documentRepository.findByDocTypeAndDeletedAtIsNotNull(DocumentType.FILE))
                .thenReturn(List.of(document));
        when(document.getApplicationPosting()).thenReturn(applicationPosting);
        when(applicationPosting.getId()).thenReturn(10L);
        when(document.getFileUrl()).thenReturn("documents/resume.pdf");
        doThrow(new RuntimeException("S3 unavailable"))
                .when(documentStorage).delete("documents/resume.pdf");

        cleanupService.deleteSoftDeletedFiles();

        verify(documentRepository, never()).delete(document);
        verify(applicationPostingRepository, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
    }
}
