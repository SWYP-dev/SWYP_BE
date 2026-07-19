package com.chwihap.server.domain.user.service;

import com.chwihap.server.domain.auth.repository.RefreshTokenRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 탈퇴하면_deletedAt이_기록되고_providerId와_email이_익명화되며_리프레시_토큰이_삭제된다() {
        User user = User.create("kakao_1@users.chwihap.com", "닉네임", null, AuthProvider.KAKAO, "1");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        assertThat(user.isWithdrawn()).isTrue();
        assertThat(user.getProviderId()).isNull();
        assertThat(user.getEmail()).isEqualTo("withdrawn_1@deleted.chwihap.com");
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    void 존재하지_않는_회원을_탈퇴하려_하면_예외가_발생한다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(refreshTokenRepository, never()).deleteByUserId(1L);
    }

    @Test
    void 이미_탈퇴한_회원을_다시_탈퇴하려_하면_예외가_발생한다() {
        User user = User.create("kakao_1@users.chwihap.com", "닉네임", null, AuthProvider.KAKAO, "1");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_WITHDRAWN);

        verify(refreshTokenRepository, never()).deleteByUserId(1L);
    }
}
