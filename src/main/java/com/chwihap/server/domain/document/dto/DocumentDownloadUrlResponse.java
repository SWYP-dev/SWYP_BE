package com.chwihap.server.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record DocumentDownloadUrlResponse(
        String downloadUrl,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiresAt
) {
}
