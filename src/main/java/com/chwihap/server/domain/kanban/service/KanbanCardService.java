package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.kanban.dto.KanbanBoardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardDetailResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardCreateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardMemoRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardMemoUpdateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageMoveRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageMoveResponse;
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
    private final DocumentRepository documentRepository;

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
        // 동시 생성 시 position 충돌 방지: 스테이지 행에 락을 건 뒤 MAX 계산
        KanbanStage firstStage = kanbanStageRepository.lockById(stages.get(0).getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        int position = kanbanCardRepository.findMaxPositionByStage(firstStage) + 1;
        KanbanCard kanbanCard = KanbanCard.createCard(user, firstStage, jobPosting, position);
        kanbanCardRepository.save(kanbanCard);

        return KanbanCardCreateResponse.from(kanbanCard);
    }

    /**
     * 칸반 보드 카드 상세 조회
     * @param userId
     * @param cardId
     * @return 카드 상세 정보 반환
     * @author say_0
     */
    public KanbanCardDetailResponse getCardDetail(Long userId, Long cardId) {
        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Long jobPostingId = card.getJobPosting().getId();
        List<Document> documents = documentRepository.findByUser_IdAndJobPosting_IdOrderByCreatedAtAsc(
                userId,
                jobPostingId
        );

        return KanbanCardDetailResponse.from(card, documents);
    }

    /**
     * 칸반 보드 카드 스테이지 이동
     * @param userId
     * @param cardId
     * @param request
     * @return 이동된 카드 위치 정보 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardStageMoveResponse moveCardStage(Long userId, Long cardId, KanbanCardStageMoveRequest request) {
        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanStage targetStage = kanbanStageRepository.findByUserIdAndId(userId, request.stageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanStage oldStage = card.getStage();
        int oldPosition = card.getPosition();
        int newPosition = request.position();
        boolean sameStage = oldStage.getId().equals(targetStage.getId());

        // [검사 1] 새로운 포지션이 포지션 사이의 값인지 검사(갭이 있어도 정확하도록 MAX 기반으로 계산)
        int targetMaxPosition = kanbanCardRepository.findMaxPositionByStage(targetStage);
        int maxPosition = sameStage ? targetMaxPosition : targetMaxPosition + 1;
        if (newPosition < 1 || newPosition > maxPosition) {
            throw new BusinessException(ErrorCode.POSITION_OUT_OF_RANGE);
        }

        // [검사 2] 기존 스테이지와 새로운 스테이지가 같다면 그대로 반환
        if (sameStage && oldPosition == newPosition) {
            return KanbanCardStageMoveResponse.of(cardId, targetStage, newPosition);
        }

        int temporaryPosition = -cardId.intValue(); // 음수값 포지션(중복 방지)
        kanbanCardRepository.updatePosition(cardId, temporaryPosition);

        // [검사 3] 스테이지 변화 감지 분기점
        if (sameStage) {
            moveWithinSameStage(oldStage.getId(), oldPosition, newPosition);
        } else {
            moveToDifferentStage(oldStage.getId(), targetStage.getId(), oldPosition, newPosition);
        }

        kanbanCardRepository.updateStageAndPosition(cardId, targetStage.getId(), newPosition);
        return KanbanCardStageMoveResponse.of(cardId, targetStage, newPosition);
    }

    /**
     * 스테이지 변화가 없는 경우 메소드
     * @param stageId
     * @param oldPosition
     * @param newPosition
     * @author say_0
     */
    private void moveWithinSameStage(Long stageId, int oldPosition, int newPosition) {
        if (newPosition < oldPosition) {
            kanbanCardRepository.shiftPositionsForMoveUp(stageId, oldPosition, newPosition);
            return;
        }

        kanbanCardRepository.shiftPositionsForMoveDown(stageId, oldPosition, newPosition);
    }

    /**
     * 스테이지 변화가 있는 경우 메소드
     * @param oldStageId
     * @param targetStageId
     * @param oldPosition
     * @param newPosition
     * @author say_0
     */
    private void moveToDifferentStage(Long oldStageId, Long targetStageId, int oldPosition, int newPosition) {
        kanbanCardRepository.shiftPositionsAfterDelete(oldStageId, oldPosition);
        kanbanCardRepository.shiftPositionsFrom(targetStageId, newPosition);
    }

    /**
     * 초기 스테이지 생성(지원 전, 면접, 면접 결과)</br>
     * 해당 메소드에서 스테이지가 비어있는지 검증 후 기본 스테이지 추가 해서 결과 반환
     * @param userId
     * @return 기본 스테이지 생성 후 반환
     * @author say_0
     */
    @Transactional
    public List<KanbanStage> ensureDefaultStages(Long userId) {
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

    /**
     * 칸반 보드 카드 메모 수정
     * @param userId
     * @param cardId
     * @param request
     * @return 수정된 카드 메모 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardMemoUpdateResponse updateCardMemo(Long userId, Long cardId, KanbanCardMemoRequest request) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [검증 1] 카드 주인이 유저인지 검증
        if (!card.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        card.updateMemo(request.memo());
        return KanbanCardMemoUpdateResponse.from(card);
    }

    /**
     * 칸반 보드 카드 삭제
     * @param userId
     * @param cardId
     * @author say_0
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        KanbanCard card = kanbanCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [검증 1] 카드 주인이 유저인지 검증
        if (!card.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Long stageId = card.getStage().getId();
        int position = card.getPosition();

        kanbanCardRepository.delete(card);
        kanbanCardRepository.flush();
        kanbanCardRepository.shiftPositionsAfterDelete(stageId, position);
    }

}
