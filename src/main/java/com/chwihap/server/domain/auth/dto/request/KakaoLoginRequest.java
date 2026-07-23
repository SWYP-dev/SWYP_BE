package com.chwihap.server.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {
}
