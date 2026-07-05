package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.kanban.dto.KanbanRequest;
import com.chwihap.server.domain.kanban.dto.KanbanResponse;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.KanbanRepository;
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
public class KanbanService {

    private static final int MAX_STAGE_COUNT = 10;

    private final KanbanRepository kanbanRepository;
    private final UserRepository userRepository;

    /**
     * 칸반에서 스테이지를 추가하는 기능
     *
     * @return 칸반응답 객체 반환
     *
     * @author Say_0
     */
    @Transactional
    public KanbanResponse addToStage(Long userId, KanbanRequest kanbanRequest) {
        long stageCount = kanbanRepository.countByUserId(userId);

        // [검증 1] 칸반 개수 제한(10개) 카운트
        if (stageCount >= MAX_STAGE_COUNT) {
            throw new BusinessException(ErrorCode.STAGE_LIMIT_EXCEEDED);
        }

        // [검증 2] 삽입 위치가 1보다 작거나 저장된 스테이지 개수보다 많을 때 에러
        int position = kanbanRequest.position();
        if (position < 1 || position > stageCount + 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 삽입 위치 이후의 기존 스테이지들을 한 칸씩 뒤로 밀기
        kanbanRepository.shiftPositionsFrom(userId, position);

        User user = userRepository.getReferenceById(userId);
        KanbanStage stage = KanbanStage.createCustom(user, kanbanRequest.name(), position);
        kanbanRepository.save(stage);

        return KanbanResponse.from(stage);
    }

}
