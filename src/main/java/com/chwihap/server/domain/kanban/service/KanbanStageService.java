package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.kanban.dto.KanbanStageCreateRequest;
import com.chwihap.server.domain.kanban.dto.KanbanStageRequest;
import com.chwihap.server.domain.kanban.dto.KanbanStageCreateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanStageUpdateResponse;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanStageService {

    private static final int MAX_STAGE_COUNT = 10;
    private static final List<String> DEFAULT_STAGE_NAMES = List.of("지원 전", "면접", "최종 결과");

    private final KanbanStageRepository kanbanStageRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final UserRepository userRepository;

    /**
     *  한글/영문/숫자가 최소 1자 이상 포함되어야 함(특수문자+공백 조합만으로는 통과 불가)
     */
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[가-힣a-zA-Z0-9]");

    /**
     * 3.8 칸반에서 스테이지를 추가하는 기능
     * @param userId 스테이지를 추가하려는 유저 ID
     * @param kanbanStageCreateRequest 추가하려는 스테이지 정보 데이터
     * @return 칸반 DTO 응답 객체 반환
     * @author say_0
     */
    @Transactional
    public KanbanStageCreateResponse addStage(Long userId, KanbanStageCreateRequest kanbanStageCreateRequest) {
        // [예외처리] 이름 예외처리
        validateStageName(kanbanStageCreateRequest.name());

        // 같은 사용자의 스테이지 변경 작업을 직렬화
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        ensureDefaultStages(userId);

        // [예외처리] 스테이지 이름 중복 예외처리
        if (kanbanStageRepository.existsByUser_IdAndStageName(userId, kanbanStageCreateRequest.name())) {
            throw new BusinessException(ErrorCode.STAGE_NAME_DUPLICATE);
        }

        // [검증 1] 칸반 개수 제한(10개) 카운트
        long customStageCount = kanbanStageRepository.countByUserIdAndIsDefaultFalse(userId);
        if (customStageCount >= MAX_STAGE_COUNT) {
            throw new BusinessException(ErrorCode.STAGE_LIMIT_EXCEEDED);
        }

        int position = kanbanStageRepository.findMaxPositionByUserId(userId) + 1;

        KanbanStage stage = KanbanStage.createCustom(user, kanbanStageCreateRequest.name(), position);
        kanbanStageRepository.save(stage);

        return KanbanStageCreateResponse.from(stage);
    }

    @Transactional
    public List<KanbanStage> ensureDefaultStages(Long userId) {
        List<KanbanStage> stages = kanbanStageRepository.findByUser_IdOrderByPositionAsc(userId);
        if (!stages.isEmpty()) {
            return stages;
        }

        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        stages = kanbanStageRepository.findByUser_IdOrderByPositionAsc(userId);
        if (!stages.isEmpty()) {
            return stages;
        }

        int position = 1;
        for (String defaultName : DEFAULT_STAGE_NAMES) {
            kanbanStageRepository.save(KanbanStage.kanbanDefault(user, defaultName, position++));
        }

        return kanbanStageRepository.findByUser_IdOrderByPositionAsc(userId);
    }

    /**
     * 3.9 칸반에서 스테이지를 수정하는 기능
     * @param userId 스테이지를 수정하려는 유저 ID
     * @param stageId 수정하려는 스테이지 ID
     * @param kanbanStageRequest 수정할 스테이지 정보 데이터
     * @return 수정된 스테이지 반환
     * @author say_0
     */
    @Transactional
    public KanbanStageUpdateResponse updateStage(Long userId, Long stageId, KanbanStageRequest kanbanStageRequest) {
        // 동시 업데이트 시 충돌 방지 락
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanStage stage = kanbanStageRepository.findByUserIdAndId(userId, stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [예외처리] 예외처리 메소드
        validateStageName(kanbanStageRequest.name());
        if (stage.isDefault() && !stage.getStageName().equals(kanbanStageRequest.name())) {
            throw new BusinessException(ErrorCode.DEFAULT_STAGE_NAME_CHANGE_NOT_ALLOWED);
        }
        // [예외처리] 스테이지 이름 중복 예외처리
        if (kanbanStageRepository.existsByUser_IdAndStageNameAndIdNot(userId, kanbanStageRequest.name(), stageId)) {
            throw new BusinessException(ErrorCode.STAGE_NAME_DUPLICATE);
        }

        int oldPosition = stage.getPosition();
        int newPosition = kanbanStageRequest.position();
        int maxPosition = kanbanStageRepository.findMaxPositionByUserId(userId);
        if (newPosition < 1 || newPosition > maxPosition) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 이름만 변경되면 이름만 수정
        if (oldPosition == newPosition) {
            stage.updateStage(kanbanStageRequest.name(), newPosition);
            return KanbanStageUpdateResponse.from(stage);
        }

        int temporaryPosition = -stageId.intValue();    // position 유니크제약 충돌 방지용 임시 위치
        kanbanStageRepository.updatePosition(userId, stageId, temporaryPosition);
        // position이 변경되면 이동 방향에 따라서 스테이지 순서 재정렬
        if (newPosition < oldPosition) {
            kanbanStageRepository.shiftPositionsForMoveUp(userId, oldPosition, newPosition);
        } else {
            kanbanStageRepository.shiftPositionsForMoveDown(userId, oldPosition, newPosition);
        }
        kanbanStageRepository.updateStageNameAndPosition(userId, stageId, kanbanStageRequest.name(), newPosition);

        KanbanStage updatedStage = kanbanStageRepository.findByUserIdAndId(userId, stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        return KanbanStageUpdateResponse.from(updatedStage);
    }

    /**
     * 3.10 칸반 보드 스테이지를 삭제하는 기능
     * @param userId 스테이지를 삭제하려는 유저 ID
     * @param stageId 삭제하려는 스테이지 ID
     * @param moveToStageId 삭제전 카드를 이동시킬 스테이지 ID
     * @author say_0
     */
    @Transactional
    public void deleteStage(Long userId, Long stageId, Long moveToStageId) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanStage deleteStage = kanbanStageRepository.findByUserIdAndId(userId, stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [검증 1] 기본 스테이지 삭제 에러
        if (deleteStage.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_STAGE_DELETE_NOT_ALLOWED);
        }

        // 카드가 있으면 이동 대상 없이 단계를 삭제할 수 없음
        long cardCount = kanbanCardRepository.countByStage(deleteStage);
        if (cardCount > 0) {
            if (moveToStageId == null || stageId.equals(moveToStageId)) {
                throw new BusinessException(ErrorCode.STAGE_HAS_CARDS);
            }

            KanbanStage moveToStage = kanbanStageRepository.findByUserIdAndId(userId, moveToStageId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
            int positionOffset = kanbanCardRepository.findMaxPositionByStage(moveToStage);
            kanbanCardRepository.moveCardsToStage(deleteStage, moveToStage, positionOffset);
        }

        int deletedPosition = deleteStage.getPosition();
        kanbanStageRepository.delete(deleteStage);
        kanbanStageRepository.flush();
        kanbanStageRepository.shiftPositionsAfterDelete(userId, deletedPosition);
    }

    /**
     * 스테이지 이름 입력 예외처리 메소드
     * @param stageName 검증하려는 스테이지의 문자열 이름
     */
    private void validateStageName(String stageName) {
        // [예외처리] 전형 이름 공백 예외처리
        if (stageName == null || stageName.isBlank()) {
            throw new BusinessException(ErrorCode.STAGE_NAME_REQUIRED);
        }

        // [예외처리] 이름 길이가 2보다 작으면 예외처리
        if (stageName.length() < 2) {
            throw new BusinessException(ErrorCode.STAGE_NAME_TOO_SHORT);
        }

        if (stageName.length() > 20) {
            throw new BusinessException(ErrorCode.STAGE_NAME_TOO_LONG);
        }

        // [예외처리] 전형 이름에 특수 문자 입력시 예외처리
        if (!SPECIAL_CHAR.matcher(stageName).find()) {
            throw new BusinessException(ErrorCode.STAGE_NAME_SPECIAL_CHAR);
        }
    }

}
