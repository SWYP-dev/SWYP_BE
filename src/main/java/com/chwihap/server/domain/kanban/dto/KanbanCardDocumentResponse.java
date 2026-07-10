package com.chwihap.server.domain.kanban.dto;

import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KanbanCardDocumentResponse(
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
    public static KanbanCardDocumentResponse from(Document document) {
        DocumentType type = document.getDocType();
        LocalDateTime createdAt = document.getCreatedAt();

        return new KanbanCardDocumentResponse(
                document.getId(),
                type,
                resolveName(document),
                type == DocumentType.FILE ? document.getVersion() : null,
                type == DocumentType.FILE ? document.getFileSize() : null,
                type == DocumentType.LINK ? document.getLinkUrl() : null,
                type == DocumentType.MEMO ? document.getMemo() : null,
                type == DocumentType.FILE ? createdAt : null,
                type == DocumentType.FILE ? null : createdAt
        );
    }

    private static String resolveName(Document document) {
        if (document.getOriginalName() != null) {
            return document.getOriginalName();
        }

        return document.getFileName();
    }
}
