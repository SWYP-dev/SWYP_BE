package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.kanban.dto.KanbanStageRequest;
import com.chwihap.server.domain.kanban.dto.KanbanStageUpdateResponse;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KanbanStageServiceTest {

    @Mock
    private KanbanStageRepository kanbanStageRepository;

    @Mock
    private KanbanCardRepository kanbanCardRepository;

    @Mock
    private UserRepository userRepository;

    private KanbanStageService kanbanStageService;

    private User user;

    @BeforeEach
    void setUp() {
        kanbanStageService = new KanbanStageService(
                kanbanStageRepository,
                kanbanCardRepository,
                userRepository
        );
        user = User.create("user@example.com", "사용자", null, AuthProvider.KAKAO, "kakao-id");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void 이름만_요청하면_기존_위치를_유지하고_이름을_변경한다() {
        KanbanStage stage = customStage("코딩 테스트", 4);
        stubStage(stage);
        when(kanbanStageRepository.existsByUser_IdAndStageNameAndIdNot(1L, "인적성 검사", 23L))
                .thenReturn(false);

        KanbanStageUpdateResponse response =
                kanbanStageService.updateStage(1L, 23L, new KanbanStageRequest("인적성 검사", null));

        assertThat(response.name()).isEqualTo("인적성 검사");
        assertThat(response.position()).isEqualTo(4);
        verify(kanbanStageRepository, never()).findMaxPositionByUserId(1L);
        verify(kanbanStageRepository, never()).updatePosition(1L, 23L, -23);
    }

    @Test
    void 위치만_요청하면_기존_이름을_유지하고_위치를_변경한다() {
        KanbanStage stage = customStage("코딩 테스트", 4);
        KanbanStage updatedStage = customStage("코딩 테스트", 2);
        when(userRepository.lockById(1L)).thenReturn(Optional.of(user));
        when(kanbanStageRepository.findByUserIdAndId(1L, 23L))
                .thenReturn(Optional.of(stage), Optional.of(updatedStage));
        when(kanbanStageRepository.findMaxPositionByUserId(1L)).thenReturn(5);

        KanbanStageUpdateResponse response =
                kanbanStageService.updateStage(1L, 23L, new KanbanStageRequest(null, 2));

        assertThat(response.name()).isEqualTo("코딩 테스트");
        assertThat(response.position()).isEqualTo(2);
        verify(kanbanStageRepository, never())
                .existsByUser_IdAndStageNameAndIdNot(1L, "코딩 테스트", 23L);
        verify(kanbanStageRepository)
                .updateStageNameAndPosition(1L, 23L, "코딩 테스트", 2);
    }

    @Test
    void 이름과_위치를_모두_생략하면_C001을_반환한다() {
        assertErrorCode(
                new KanbanStageRequest(null, null),
                ErrorCode.INVALID_INPUT_VALUE
        );

        verify(userRepository, never()).lockById(1L);
    }

    @Test
    void 공백_이름은_K005를_반환한다() {
        assertErrorCode(new KanbanStageRequest("   ", null), ErrorCode.STAGE_NAME_REQUIRED);
    }

    @Test
    void 한_글자_이름은_K008을_반환한다() {
        assertErrorCode(new KanbanStageRequest("면", null), ErrorCode.STAGE_NAME_TOO_SHORT);
    }

    @Test
    void 스무_글자를_초과한_이름은_K009를_반환한다() {
        assertErrorCode(new KanbanStageRequest("가".repeat(21), null), ErrorCode.STAGE_NAME_TOO_LONG);
    }

    @Test
    void 특수문자만_있는_이름은_K007을_반환한다() {
        assertErrorCode(new KanbanStageRequest("!!!", null), ErrorCode.STAGE_NAME_SPECIAL_CHAR);
    }

    @Test
    void 이모지만_있는_이름은_K007을_반환한다() {
        assertErrorCode(new KanbanStageRequest("😀😀", null), ErrorCode.STAGE_NAME_SPECIAL_CHAR);
    }

    @Test
    void 중복된_이름은_K006을_반환한다() {
        KanbanStage stage = customStage("코딩 테스트", 4);
        stubStage(stage);
        when(kanbanStageRepository.existsByUser_IdAndStageNameAndIdNot(1L, "인적성 검사", 23L))
                .thenReturn(true);

        assertErrorCode(new KanbanStageRequest("인적성 검사", null), ErrorCode.STAGE_NAME_DUPLICATE);
    }

    @Test
    void 이름의_앞뒤_공백과_줄바꿈을_제거한_뒤_저장한다() {
        KanbanStage stage = customStage("코딩 테스트", 4);
        stubStage(stage);
        when(kanbanStageRepository.existsByUser_IdAndStageNameAndIdNot(1L, "1차면접", 23L))
                .thenReturn(false);

        KanbanStageUpdateResponse response =
                kanbanStageService.updateStage(1L, 23L, new KanbanStageRequest("  1차\n면접\r  ", null));

        assertThat(response.name()).isEqualTo("1차면접");
        assertThat(response.position()).isEqualTo(4);
    }

    @Test
    void 범위를_벗어난_위치는_K002를_반환한다() {
        KanbanStage stage = customStage("코딩 테스트", 4);
        stubStage(stage);
        when(kanbanStageRepository.findMaxPositionByUserId(1L)).thenReturn(5);

        assertErrorCode(new KanbanStageRequest(null, 6), ErrorCode.POSITION_OUT_OF_RANGE);
    }

    @Test
    void 기본_스테이지의_이름을_변경하면_K023을_반환한다() {
        KanbanStage stage = KanbanStage.kanbanDefault(user, "지원 전", 1);
        ReflectionTestUtils.setField(stage, "id", 23L);
        stubStage(stage);

        assertErrorCode(
                new KanbanStageRequest("서류 접수", null),
                ErrorCode.DEFAULT_STAGE_NAME_CHANGE_NOT_ALLOWED
        );
    }

    private void assertErrorCode(KanbanStageRequest request, ErrorCode expected) {
        assertThatThrownBy(() -> kanbanStageService.updateStage(1L, 23L, request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected)
                );
    }

    private void stubStage(KanbanStage stage) {
        when(userRepository.lockById(1L)).thenReturn(Optional.of(user));
        when(kanbanStageRepository.findByUserIdAndId(1L, 23L)).thenReturn(Optional.of(stage));
    }

    private KanbanStage customStage(String name, int position) {
        KanbanStage stage = KanbanStage.createCustom(user, name, position);
        ReflectionTestUtils.setField(stage, "id", 23L);
        return stage;
    }
}
