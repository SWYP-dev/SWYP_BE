package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.kanban.dto.KanbanBoardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardCreateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanStageResponse;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanCardService {

    private final KanbanCardRepository kanbanCardRepository;
    private final KanbanStageRepository kanbanStageRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    /**
     * 기본(지원 전, 면접, 최종 결과) 스테이지 생성
     */
    private static final List<String> DEFAULT_STAGE_NAMES = List.of("지원 전", "면접", "최종 결과");

    /**
     * 칸반보드 조회목록 반환(지원 현황)
     * @param userId
     * @return 칸반보드 전체 조회목록 반환
     * @author say_0
     */
    @Transactional
    public KanbanBoardResponse getKanbanBoard(Long userId) {
        // 1. 서비스에서 해당 사용자의 카드 리스트 조회
        List<KanbanCard> cards = kanbanCardRepository.findByUser_IdOrderByPositionAsc(userId);

        // 2. [분기 1. 기본 스테이지 생성] - [지원 현황] 버튼을 통해서 진입
        List<KanbanStage> stages = ensureDefaultStages(userId);

        // 3. 서비스에서 해당 사용자의 스테이지 목록 조회 + 카드들을 stageId 기준으로 그룹핑
        Map<Long, List<KanbanCardResponse>> cardsByStageId = cards.stream()
                .collect(Collectors.groupingBy(
                        card -> card.getStage().getId(),
                        Collectors.mapping(KanbanCardResponse::from,
                                Collectors.toList())
                ));

        // 4. 각 스테이지에 속한 카드 목록 조회
        List<KanbanStageResponse> stageResponses = stages.stream()
                .map(stage -> KanbanStageResponse.from(
                        stage,
                        cardsByStageId.getOrDefault(stage.getId(),
                                List.of())
                )).toList();

        return new KanbanBoardResponse(stageResponses);
    }

    /**
     * 칸반보드 카드 만들기(내 지원 현황에 추가)
     * @param request
     * @return 생성한 카드 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardCreateResponse createCard(KanbanCardRequest request, Long userId) {
        JobPosting jobPosting = jobPostingRepository.findById(request.postingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // 1. 중복 생성 방지
        if (kanbanCardRepository.existsByUser_IdAndJobPosting_Id(userId, request.postingId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_KANBAN_CARD);
        }

        // 2. [분기 2. 기본 스테이지 생성] - [내 지원 현황에 추가하기] 버튼을 통해 진입
        List<KanbanStage> stages = ensureDefaultStages(userId);

        // 3. 카드 생성 - 최초 생성은 지원 전 스테이지에 생성(생성 순서대로 정렬)
        KanbanStage firstStage = stages.get(0);
        int position = kanbanCardRepository.findMaxPositionByStage(firstStage) + 1;
        KanbanCard kanbanCard = KanbanCard.createCard(user, firstStage, jobPosting, position);
        kanbanCardRepository.save(kanbanCard);

        return KanbanCardCreateResponse.from(kanbanCard);
    }

    /**
     * 초기 스테이지 생성(지원 전, 면접, 면접 결과)</br>
     * 해당 메소드에서 스테이지가 비어있는지 검증 후 기본 스테이지 추가 해서 결과 반환
     * @param userId
     * @return 기본 스테이지 생성 후 반환
     * @author say_0
     */
    private List<KanbanStage> ensureDefaultStages(Long userId) {
        List<KanbanStage> stages = kanbanStageRepository.findByUser_IdOrderByPositionAsc(userId);

        // [검증 1] 스테이지가 비어있지 않으면
        if (!stages.isEmpty()) {
            return stages;
        }

        User user = userRepository.getReferenceById(userId);

        // [검증 2] 스테이지가 비어있으면 -> 스테이지 생성
        List<KanbanStage> createdStage = new ArrayList<>();
        int position = 1;
        for (String name : DEFAULT_STAGE_NAMES) {
            createdStage.add(kanbanStageRepository.save(KanbanStage.kanbanDefault(user, name, position)));
            position++;
        }

        return createdStage;
    }

}
