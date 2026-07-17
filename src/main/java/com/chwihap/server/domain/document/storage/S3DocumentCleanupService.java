package com.chwihap.server.domain.document.storage;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
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

    @Transactional
    public void deleteSoftDeletedFiles() {
        List<Document> documents = documentRepository
                .findByDocTypeAndDeletedAtIsNotNullAndStorageDeletedAtIsNull(DocumentType.FILE);

        for (Document document : documents) {
            try {
                documentStorage.delete(document.getFileUrl());
                document.markStorageDeleted();  // 문서 삭제 시간 마킹
            } catch (RuntimeException e) {
                log.error("Soft-deleted S3 파일정리 실패. documentId={}", document.getId(), e);
            }
        }
    }

}
