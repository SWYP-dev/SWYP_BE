package com.chwihap.server.domain.user.service;

import com.chwihap.server.domain.auth.repository.RefreshTokenRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 회원 탈퇴를 처리한다. providerId/email을 익명화해 소셜 계정 연동을 끊고,
     * 발급된 Refresh Token을 모두 무효화한다.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        user.withdraw();
        refreshTokenRepository.deleteByUserId(userId);
    }
}
