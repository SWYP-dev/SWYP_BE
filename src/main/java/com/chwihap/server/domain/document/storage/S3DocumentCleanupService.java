package com.chwihap.server.domain.document.storage;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3DocumentCleanupService {

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final ApplicationPostingRepository applicationPostingRepository;
    private final KanbanCardRepository kanbanCardRepository;

    @Transactional
    public void deleteSoftDeletedFiles() {
        List<Document> documents = documentRepository
                .findByDocTypeAndDeletedAtIsNotNull(DocumentType.FILE);

        for (Document document : documents) {
            try {
                Long applicationPostingId = document.getApplicationPosting().getId();

                documentStorage.delete(document.getFileUrl());
                documentRepository.delete(document);
                documentRepository.flush();
                deleteOrphanApplicationPosting(applicationPostingId);
            } catch (RuntimeException e) {
                log.error("Soft-deleted S3 파일정리 실패. documentId={}", document.getId(), e);
            }
        }
    }

    private void deleteOrphanApplicationPosting(Long applicationPostingId) {
        boolean cardExists = kanbanCardRepository.existsByApplicationPosting_Id(applicationPostingId);
        boolean pendingFileExists = documentRepository
                .existsByApplicationPosting_IdAndDocTypeAndDeletedAtIsNotNull(
                        applicationPostingId,
                        DocumentType.FILE
                );

        if (cardExists || pendingFileExists) {
            return;
        }

        // 카드 삭제 시 LINK/MEMO는 이미 hard delete 되었으므로 삭제 대기 지원 건만 정리한다.
        applicationPostingRepository.deleteById(applicationPostingId);
    }

}
