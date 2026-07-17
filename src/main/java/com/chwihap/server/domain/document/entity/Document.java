package com.chwihap.server.domain.document.entity;

import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(
                        name = "idx_documents_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_documents_job_posting_id",
                        columnList = "job_posting_id"
                ),
                @Index(
                        name = "idx_documents_version_group",
                        columnList = "job_posting_id, version_group"
                )
        }
)
@Check(constraints = "(doc_type = 'FILE' AND file_url IS NOT NULL) " +
        "OR (doc_type = 'LINK' AND link_url IS NOT NULL) " +
        "OR (doc_type = 'MEMO' AND memo IS NOT NULL)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType docType;

    @Column(nullable = true)
    private String fileName;

    @Column(nullable = true)
    private String originalName;

    @Column(nullable = true, length = 500)
    private String fileUrl;

    @Column(nullable = true)
    private Long fileSize;

    @Column(nullable = true, length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = true, length = 100)
    private String versionGroup;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true, columnDefinition = "text")
    private String memo;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @Column(nullable = true)
    private LocalDateTime storageDeletedAt;

    public static Document file(
            User user,
            JobPosting jobPosting,
            String fileName,
            String originalName,
            String storageKey,
            long fileSize,
            int version,
            String versionGroup
    ) {
        Document document = new Document();
        document.user = user;
        document.jobPosting = jobPosting;
        document.docType = DocumentType.FILE;
        document.fileName = fileName;
        document.originalName = originalName;
        document.fileUrl = storageKey;
        document.fileSize = fileSize;
        document.version = version;
        document.versionGroup = versionGroup;
        return document;
    }

    public static Document link(User user, JobPosting jobPosting, String name, String linkUrl) {
        Document document = new Document();
        document.user = user;
        document.jobPosting = jobPosting;
        document.docType = DocumentType.LINK;
        document.fileName = name;
        document.linkUrl = linkUrl;
        return document;
    }

    public static Document memo(User user, JobPosting jobPosting, String name, String memo) {
        Document document = new Document();
        document.user = user;
        document.jobPosting = jobPosting;
        document.docType = DocumentType.MEMO;
        document.fileName = name;
        document.memo = memo;
        return document;
    }

    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public void markStorageDeleted() {
        storageDeletedAt = LocalDateTime.now();
    }

}
