package com.chwihap.server.domain.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentMemoCreateRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 10000)
        String content
) {
}
