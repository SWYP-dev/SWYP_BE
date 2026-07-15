package com.chwihap.server.domain.document.storage;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.feed.enums.JobPlatform;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
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
    private final JobPostingRepository jobPostingRepository;
    private final KanbanCardRepository kanbanCardRepository;

    @Transactional
    public void deleteSoftDeletedFiles() {
        List<Document> documents = documentRepository
                .findByDocTypeAndDeletedAtIsNotNull(DocumentType.FILE);

        for (Document document : documents) {
            try {
                Long jobPostingId = document.getJobPosting().getId();
                boolean directPosting = document.getJobPosting().getPlatform() == JobPlatform.DIRECT;

                documentStorage.delete(document.getFileUrl());
                documentRepository.delete(document);
                documentRepository.flush();
                deleteOrphanDirectPosting(jobPostingId, directPosting);
            } catch (RuntimeException e) {
                log.error("Soft-deleted S3 파일정리 실패. documentId={}", document.getId(), e);
            }
        }
    }

    private void deleteOrphanDirectPosting(Long jobPostingId, boolean directPosting) {
        if (!directPosting) {
            return;
        }

        boolean cardExists = kanbanCardRepository.existsByJobPosting_Id(jobPostingId);
        boolean pendingFileExists = documentRepository
                .existsByJobPosting_IdAndDocTypeAndDeletedAtIsNotNull(jobPostingId, DocumentType.FILE);

        if (cardExists || pendingFileExists) {
            return;
        }

        // LINK/MEMO는 카드 삭제 시점에 이미 hard delete 되었으므로 JobPosting만 정리하면 된다.
        jobPostingRepository.deleteById(jobPostingId);
    }

}
