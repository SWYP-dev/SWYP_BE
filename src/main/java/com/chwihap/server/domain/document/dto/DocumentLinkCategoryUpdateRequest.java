package com.chwihap.server.domain.document.dto;

import com.chwihap.server.domain.document.enums.DocumentLinkCategory;
import jakarta.validation.constraints.NotNull;

public record DocumentLinkCategoryUpdateRequest(
        @NotNull
        DocumentLinkCategory category
) {
}
