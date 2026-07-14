package com.chwihap.server.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentLinkCreateRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 500)
        String url
) {
}
