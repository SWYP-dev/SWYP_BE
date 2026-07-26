package com.chwihap.server.domain.document.dto;

import com.chwihap.server.domain.document.enums.DocumentLinkCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentLinkCreateRequest(
        @NotBlank
        @Size(max = 500)
        String url,

        @NotNull
        DocumentLinkCategory category
) {
}
