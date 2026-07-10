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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanStageService {

    private static final int MAX_STAGE_COUNT = 10;

    private final KanbanStageRepository kanbanStageRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final UserRepository userRepository;

    /**
     * 칸반에서 스테이지를 추가하는 기능
     * @param userId
     * @param kanbanStageCreateRequest
     * @return 칸반 DTO 응답 객체 반환
     * @author say_0
     */
    @Transactional
    public KanbanStageCreateResponse addToStage(Long userId, KanbanStageCreateRequest kanbanStageCreateRequest) {
        long customStageCount = kanbanStageRepository.countByUserIdAndIsDefaultFalse(userId);

        // [검증 1] 칸반 개수 제한(10개) 카운트
        if (customStageCount >= MAX_STAGE_COUNT) {
            throw new BusinessException(ErrorCode.STAGE_LIMIT_EXCEEDED);
        }

        // 동시 생성 시 position 충돌 방지: 유저 행에 락을 건 뒤 MAX 계산(갭이 있어도 충돌 없도록 MAX 기반)
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        int position = kanbanStageRepository.findMaxPositionByUserId(userId) + 1;

        KanbanStage stage = KanbanStage.createCustom(user, kanbanStageCreateRequest.name(), position);
        kanbanStageRepository.save(stage);

        return KanbanStageCreateResponse.from(stage);
    }

    /**
     * 칸반에서 스테이지를 수정하는 기능
     * @param userId
     * @param stageId
     * @param kanbanStageRequest
     * @return 수정된 스테이지 반환
     * @author say_0
     */
    @Transactional
    public KanbanStageUpdateResponse updateToStage(Long userId, Long stageId, KanbanStageRequest kanbanStageRequest) {
        KanbanStage stage = kanbanStageRepository.findByUserIdAndId(userId, stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

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
     * 칸반 보드 스테이지를 삭제하는 기능
     * @param userId
     * @param stageId
     * @param moveToStageId
     * @author say_0
     */
    @Transactional
    public void deleteToStage(Long userId, Long stageId, Long moveToStageId) {
        KanbanStage deleteStage = kanbanStageRepository.findByUserIdAndId(userId, stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [검증 1] 기본 스테이지 삭제 에러
        if (deleteStage.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_STAGE_DELETE_NOT_ALLOWED);
        }

        // [검증 2] 기본 스테이지 카운트(0보다 크게)
        long cardCount = kanbanCardRepository.countByStage(deleteStage);
        if (cardCount > 0) {
            if (moveToStageId == null || stageId.equals(moveToStageId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
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

}
