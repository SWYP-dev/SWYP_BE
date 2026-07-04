package com.chwihap.server.domain.auth.dto.response;

import com.chwihap.server.domain.user.entity.User;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        UserSummary user
) {

    public static AuthTokenResponse of(String accessToken, String refreshToken, boolean isNewUser, User user) {
        return new AuthTokenResponse(accessToken, refreshToken, isNewUser, UserSummary.from(user));
    }

    public record UserSummary(
            Long id,
            String nickname,
            String profileImage
    ) {
        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getNickname(), user.getProfileImage());
        }
    }
}
