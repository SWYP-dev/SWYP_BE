package com.chwihap.server.domain.document.repository;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            SELECT d FROM Document d
            WHERE d.user.id = :userId
              AND d.applicationPosting.id = :applicationPostingId
              AND d.deletedAt IS NULL
            ORDER BY d.createdAt ASC
            """)
    List<Document> findActiveByUserIdAndApplicationPostingId(
            @Param("userId") Long userId,
            @Param("applicationPostingId") Long applicationPostingId
    );

    List<Document> findByUser_IdAndApplicationPosting_Id(
            Long userId,
            Long applicationPostingId
    );

    @Query("""
            SELECT d FROM Document d
            WHERE d.id = :id
              AND d.user.id = :userId
              AND d.applicationPosting.id = :applicationPostingId
              AND d.deletedAt IS NULL
            """)
    Optional<Document> findActiveByIdAndOwner(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("applicationPostingId") Long applicationPostingId
    );

    Optional<Document> findTopByUser_IdAndApplicationPosting_IdAndVersionGroupOrderByVersionDesc(
            Long userId,
            Long applicationPostingId,
            String versionGroup
    );

    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d
            WHERE d.user.id = :userId
              AND d.docType = :docType
              AND d.deletedAt IS NULL
            """)
    long sumActiveFileSizeByUserId(
            @Param("userId") Long userId,
            @Param("docType") DocumentType docType
    );

    List<Document> findByDocTypeAndDeletedAtIsNotNull(DocumentType docType);

    boolean existsByApplicationPosting_IdAndDocTypeAndDeletedAtIsNotNull(
            Long applicationPostingId,
            DocumentType docType
    );
}
