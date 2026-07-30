package com.chwihap.server.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentLinkUpdateRequest(
        @NotBlank
        @Size(max = 500)
        String url
) {
}
