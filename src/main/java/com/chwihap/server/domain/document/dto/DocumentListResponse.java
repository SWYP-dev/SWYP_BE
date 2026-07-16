package com.chwihap.server.domain.document.dto;

import java.util.List;

public record DocumentListResponse(
        List<DocumentResponse> documents
) {
}
