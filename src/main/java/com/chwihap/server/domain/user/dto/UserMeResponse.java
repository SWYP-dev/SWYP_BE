package com.chwihap.server.domain.user.dto;

import com.chwihap.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답")
public record UserMeResponse(
        @Schema(description = "계정 이메일. 카카오 이메일 미동의 등으로 실제 이메일이 없으면 null", example = "user@example.com", nullable = true)
        String email
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(user.getContactableEmail());
    }
}
