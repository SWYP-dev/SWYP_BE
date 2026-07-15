package com.chwihap.server.domain.document.dto;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
        Long id,
        DocumentType type,
        String name,
        Integer version,
        Long size,
        String url,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime uploadedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registeredAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getDocType(),
                document.getOriginalName() != null ? document.getOriginalName() : document.getFileName(),
                document.getDocType() == DocumentType.FILE ? document.getVersion() : null,
                document.getDocType() == DocumentType.FILE ? document.getFileSize() : null,
                document.getDocType() == DocumentType.LINK ? document.getLinkUrl() : null,
                document.getDocType() == DocumentType.MEMO ? document.getMemo() : null,
                document.getDocType() == DocumentType.FILE ? document.getCreatedAt() : null,
                document.getDocType() == DocumentType.FILE ? null : document.getCreatedAt()
        );
    }
}
