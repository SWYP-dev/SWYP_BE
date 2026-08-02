package com.chwihap.server.domain.kanban.service;

import com.chwihap.server.domain.feed.entity.JobPosting;
import com.chwihap.server.domain.feed.repository.JobPostingRepository;
import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.kanban.dto.KanbanBoardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardDetailResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardCreateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardSaveRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardMemoRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardMemoUpdateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageMoveRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageMoveResponse;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageDeadlineUpdateRequest;
import com.chwihap.server.domain.kanban.dto.KanbanCardStageDeadlineUpdateResponse;
import com.chwihap.server.domain.kanban.dto.KanbanDeadlineCardResponse;
import com.chwihap.server.domain.kanban.dto.KanbanDeadlineListResponse;
import com.chwihap.server.domain.kanban.dto.KanbanStageResponse;
import com.chwihap.server.domain.kanban.entity.ApplicationPosting;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.entity.KanbanStage;
import com.chwihap.server.domain.kanban.repository.ApplicationPostingRepository;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.kanban.repository.KanbanStageRepository;
import com.chwihap.server.domain.notification.repository.NotificationRepository;
import com.chwihap.server.domain.user.entity.User;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanbanCardService {

    private final KanbanCardRepository kanbanCardRepository;
    private final KanbanStageRepository kanbanStageRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationPostingRepository applicationPostingRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final KanbanStageService kanbanStageService;

    // 한글/영문/숫자가 최소 1자 이상 포함되어야 함(특수문자+공백 조합만으로는 통과 불가)
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[가-힣a-zA-Z0-9]");

    private static final String INITIAL_STAGE_NAME = "지원 전";

    /**
     * 3.1 칸반 보드 전체 조회
     * @param userId 사용자 ID
     * @return 칸반보드 전체 조회목록 반환
     * @author say_0
     */
    @Transactional
    public KanbanBoardResponse getKanbanBoard(Long userId) {
        // 1. 서비스에서 해당 사용자의 카드 리스트 조회
        List<KanbanCard> cards = kanbanCardRepository.findByUser_IdOrderByPositionAsc(userId);

        // 2. [분기 1. 기본 스테이지 생성] - [지원 현황] 버튼을 통해서 진입
        List<KanbanStage> stages = kanbanStageService.ensureDefaultStages(userId);

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
     * 3.12 지원 마감일 페이지 목록 조회</br>
     * 오늘 마감인 공고를 포함하고, 마감일이 없거나 지난 공고는 제외한다.
     *
     * @param userId 사용자 ID
     * @return 마감일 오름차순으로 정렬된 칸반 카드 목록
     * @author say_0
     */
    public KanbanDeadlineListResponse getDeadlineCards(Long userId) {
        return getDeadlineCards(userId, LocalDate.now());
    }
    // 카드를 마감일 순서대로 가져와서 리스트 형태로 반환
    KanbanDeadlineListResponse getDeadlineCards(Long userId, LocalDate today) {
        List<KanbanDeadlineCardResponse> cards = kanbanCardRepository
                .findUpcomingDeadlineCards(userId, today)
                .stream()
                .map(KanbanDeadlineCardResponse::from)
                .toList();

        return new KanbanDeadlineListResponse(cards);
    }

    /**
     * 3.2 칸반 보드 자동 등록(내 지원 현황에 추가)
     * @param request 카드 생성을 위한 요청 데이터
     * @return 생성한 카드 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardCreateResponse createCard(KanbanCardRequest request, Long userId) {
        JobPosting sourceJobPosting = jobPostingRepository.findByIdAndUser_Id(request.postingId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        return createCardForPosting(userId, sourceJobPosting);
    }

    /**
     * 이미 확보한 원본 JobPosting의 스냅샷을 생성하고 지원 전 스테이지에 카드를 생성한다.<br>
     * 통합 공고 피드에서 스크랩 없이 바로 등록하는 경우처럼 호출자가 원본 공고를 확보한 뒤
     * 지원 건 생성을 위임할 때 사용한다.
     * @param userId 카드를 만드는 유저 ID
     * @param sourceJobPosting 스냅샷의 원본 공고
     * @return 생성한 카드 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardCreateResponse createCardForPosting(Long userId, JobPosting sourceJobPosting) {
        // 동시 생성 시 중복 체크-저장 사이 race condition 방지: 유저 행에 락을 건 뒤 중복 검사
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [방어 로직] 사용자와 원본 공고 소유자가 같은지 확인(다른 사용자의 공고를 카드로 만드는 것을 방지)
        if (!user.getId().equals(sourceJobPosting.getUser().getId())) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }

        // 1. 중복 생성 방지
        if (kanbanCardRepository.existsByUser_IdAndApplicationPosting_SourceJobPosting_Id(userId, sourceJobPosting.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_KANBAN_CARD);
        }

        // 2. 원본 공고의 스냅샷 생성
        ApplicationPosting applicationPosting = ApplicationPosting.copyFromJobPosting(sourceJobPosting);
        applicationPostingRepository.save(applicationPosting);

        // 3. 카드 생성 - 최초 생성은 지원 전 스테이지에 생성(생성 순서대로 정렬)
        KanbanCard kanbanCard = createCardInInitialStage(applicationPosting, userId);

        return KanbanCardCreateResponse.from(kanbanCard);
    }

    /**
     * 3.3 칸반 보드 수동 등록(사용자가 공고를 직접 입력해서 추가)
     * @param request 칸반 보드 수동 등록을 위한 데이터(카드 수정과 DTO 공유)
     * @param userId 카드를 만드는 유저 ID
     * @return 생성한 카드 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardCreateResponse createDirectCard(KanbanCardSaveRequest request, Long userId) {
        // [예외처리] 유저가 카드 생성시 예외처리
        validateCompanyName(request.companyName());
        validateJobPostingName(request.title());
        validateJobPostingLink(request.originalUrl());

        // 카드 생성시 유저 행에 락을 건 뒤 중복 검사
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // [예외처리] 이미 등록된 링크 입력시 예외처리
        if (kanbanCardRepository.existsByUser_IdAndApplicationPosting_OriginalUrl(
                userId,
                request.originalUrl()
        )) {
            throw new BusinessException(ErrorCode.DUPLICATE_KANBAN_CARD);
        }

        ApplicationPosting applicationPosting = ApplicationPosting.createDirect(
                user,
                request.companyName(),
                request.title(),
                request.deadline(),
                request.originalUrl()
        );
        applicationPostingRepository.save(applicationPosting);

        KanbanCard kanbanCard = createCardInInitialStage(applicationPosting, userId);

        return KanbanCardCreateResponse.from(kanbanCard);
    }

    /**
     * 3.4 칸반 보드 카드 수정(지원 현황 공고 정보 수정)</br>
     * 원본 JobPosting의 등록 방식과 관계없이 ApplicationPosting 스냅샷만 수정한다.
     * @param userId 카드를 수정하는 유저 ID
     * @param cardId 수정하려는 카드 ID
     * @param request 칸반 보드 카드 수정을 위한 데이터(카드 수정과 DTO 공유)
     * @return 수정된 카드 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardCreateResponse updateCard(Long userId, Long cardId, KanbanCardSaveRequest request) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        ApplicationPosting applicationPosting = card.getApplicationPosting();

        // [예외처리] 유저가 카드 수정시 예외처리
        validateCompanyName(request.companyName());
        validateJobPostingName(request.title());
        validateJobPostingLink(request.originalUrl());

        // [방어 로직] DB에 저장된 URL과 문자열이 똑같은지 검사하는 로직(URL 문자열이 동일할 경우 저장 불가)
        if (kanbanCardRepository.existsByUser_IdAndApplicationPosting_OriginalUrlAndIdNot(
                userId,
                request.originalUrl(),
                cardId
        )) {
            throw new BusinessException(ErrorCode.DUPLICATE_KANBAN_CARD);
        }

        applicationPosting.updateDetails(
                request.companyName(),
                request.title(),
                request.deadline(),
                request.originalUrl()
        );

        return KanbanCardCreateResponse.from(card);
    }

    /**
     * 지원 전 스테이지에 카드를 생성(생성 순서대로 정렬)</br>
     * 동시 생성 시 position 충돌 방지: 스테이지 행에 락을 건 뒤 MAX 계산
     * @param applicationPosting 카드에 연결할 지원 공고 스냅샷
     * @param userId 로그인한 유저 Id
     * @return 생성된 카드 반환
     * @author say_0
     */
    private KanbanCard createCardInInitialStage(ApplicationPosting applicationPosting, Long userId) {
        kanbanStageService.ensureDefaultStages(userId);

        KanbanStage initialStage = kanbanStageRepository
                .findByUser_IdAndStageName(userId, INITIAL_STAGE_NAME)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        KanbanStage lockedStage = kanbanStageRepository.lockById(initialStage.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        int position = kanbanCardRepository.findMaxPositionByStage(lockedStage) + 1;
        KanbanCard kanbanCard = KanbanCard.createCard(lockedStage, applicationPosting, position);
        kanbanCardRepository.save(kanbanCard);

        return kanbanCard;
    }

    /**
     * 3.5 칸반 보드 카드 상세 조회
     * @param userId 카드를 조회하려는 유저 ID
     * @param cardId 조회하려는 카드 ID
     * @return 카드 상세 정보 반환
     * @author say_0
     */
    public KanbanCardDetailResponse getCardDetail(Long userId, Long cardId) {
        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Long applicationPostingId = card.getApplicationPosting().getId();
        List<Document> documents = documentRepository.findActiveByUserIdAndApplicationPostingId(
                userId,
                applicationPostingId
        );

        return KanbanCardDetailResponse.from(card, documents);
    }

    /**
     * 칸반 보드 카드 스테이지 이동 메소드
     * @param userId 카드를 이동시키려는 유저 ID
     * @param cardId 이동시키는 카드 ID
     * @param request 카드 변환 정보
     * @return 이동된 카드 위치 정보 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardStageMoveResponse moveCardStage(Long userId, Long cardId, KanbanCardStageMoveRequest request) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

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
     * 3.13 지원 마감일 카드 수정
     * 카드 생성 방식과 관계없이 지원 마감일과 전형 단계를 부분 수정한다.
     * 실제로 스테이지가 변경되는 경우에만 대상 스테이지의 첫 번째 위치로 이동한다.
     *
     * @param userId 카드를 수정하는 사용자 ID
     * @param cardId 수정할 카드 ID
     * @param request 변경할 전형 단계와 지원 마감일
     * @return 수정된 카드의 전형 단계와 지원 마감일
     * @author say_0
     */
    @Transactional
    public KanbanCardStageDeadlineUpdateResponse updateStageAndDeadline(
            Long userId,
            Long cardId,
            KanbanCardStageDeadlineUpdateRequest request
    ) {
        boolean stageRequested = request.stageId() != null;
        boolean deadlineRequested = request.deadline() != null;
        // [검증] DTO 상태를 검증하는 로직
        if (!stageRequested && !deadlineRequested) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // [락] 비관적 락 적용
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        ApplicationPosting applicationPosting = card.getApplicationPosting();
        if (deadlineRequested) {
            applicationPosting.updateDeadline(request.deadline());
        }

        KanbanStage oldStage = card.getStage();
        int oldPosition = card.getPosition();
        if (!stageRequested) {
            return KanbanCardStageDeadlineUpdateResponse.of(
                    cardId,
                    oldStage,
                    oldPosition,
                    applicationPosting.getDeadline()
            );
        }

        KanbanStage targetStage = kanbanStageRepository.findByUserIdAndId(userId, request.stageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        boolean sameStage = oldStage.getId().equals(targetStage.getId());
        int newPosition = 1;
        if (sameStage) {
            return KanbanCardStageDeadlineUpdateResponse.of(
                    cardId,
                    targetStage,
                    oldPosition,
                    applicationPosting.getDeadline()
            );
        }

        int temporaryPosition = -cardId.intValue();
        kanbanCardRepository.updatePosition(cardId, temporaryPosition);

        kanbanCardRepository.shiftPositionsAfterDelete(oldStage.getId(), oldPosition);
        kanbanCardRepository.shiftPositionsFrom(targetStage.getId(), newPosition);
        kanbanCardRepository.updateStageAndPosition(cardId, targetStage.getId(), newPosition);

        return KanbanCardStageDeadlineUpdateResponse.of(
                cardId,
                targetStage,
                newPosition,
                applicationPosting.getDeadline()
        );
    }

    /**
     * 스테이지 변화가 없는 경우 메소드
     * @param stageId 스테이지 ID
     * @param oldPosition 과거 스테이지 위치(position은 x축 기준)
     * @param newPosition 새로운 스테이지 위치(position은 x축 기준)
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
     * @param oldStageId 과거 스테이지 ID
     * @param targetStageId 목표 스테이지 ID
     * @param oldPosition 과거 스테이지 위치(position은 x 축 기준)
     * @param newPosition 새로운 스테이지 위치(position은 x 축 기준)
     * @author say_0
     */
    private void moveToDifferentStage(Long oldStageId, Long targetStageId, int oldPosition, int newPosition) {
        kanbanCardRepository.shiftPositionsAfterDelete(oldStageId, oldPosition);
        kanbanCardRepository.shiftPositionsFrom(targetStageId, newPosition);
    }

    /**
     * 3.6 칸반 보드 카드 메모 수정
     * @param userId 메모를 수정하려는 유저 ID
     * @param cardId 수정하려는 카드 ID
     * @param request 수정하려는 정보 데이터
     * @return 수정된 카드 메모 반환
     * @author say_0
     */
    @Transactional
    public KanbanCardMemoUpdateResponse updateCardMemo(Long userId, Long cardId, KanbanCardMemoRequest request) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        card.updateMemo(request.memo());
        return KanbanCardMemoUpdateResponse.from(card);
    }

    /**
     * 3.7 칸반 보드 카드 삭제</br>
     * 카드와 연관된 LINK/MEMO 문서는 즉시 삭제하고, FILE 문서는 S3 정리를 위해 soft delete한다.</br>
     * FILE 문서가 없으면 ApplicationPosting을 즉시 삭제하고, FILE 문서가 있으면 원본 연결을
     * 해제한 뒤 S3 정리가 완료될 때까지 ApplicationPosting을 유지한다.
     * @param userId 카드를 삭제하려는 유저 ID
     * @param cardId 삭제하려는 카드 ID
     * @author say_0
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        KanbanCard card = kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        ApplicationPosting applicationPosting = card.getApplicationPosting();
        Long applicationPostingId = applicationPosting.getId();
        List<Document> documents = documentRepository.findByUser_IdAndApplicationPosting_Id(
                userId,
                applicationPostingId
        );
        List<Document> fileDocuments = documents.stream()
                .filter(document -> document.getDocType() == DocumentType.FILE)
                .toList();
        List<Document> nonFileDocuments = documents.stream()
                .filter(document -> document.getDocType() != DocumentType.FILE)
                .toList();
        Long stageId = card.getStage().getId();
        int position = card.getPosition();

        // Notification은 KanbanCard를 필수 FK로 참조하므로 카드를 지우기 전에 먼저 삭제한다.
        notificationRepository.deleteAllByKanbanCardId(cardId);
        kanbanCardRepository.delete(card);
        kanbanCardRepository.flush();
        kanbanCardRepository.shiftPositionsAfterDelete(stageId, position);

        // FILE은 S3 정리가 필요해 soft delete 후 배치가 처리, LINK/MEMO는 S3 의존이 없어 즉시 hard delete.
        fileDocuments.forEach(Document::softDelete);
        if (!nonFileDocuments.isEmpty()) {
            documentRepository.deleteAll(nonFileDocuments);
            documentRepository.flush();
        }

        if (fileDocuments.isEmpty()) {
            applicationPostingRepository.delete(applicationPosting);
            applicationPostingRepository.flush();
            return;
        }

        // 동일 원본 공고의 재등록이 가능하도록 유니크 제약을 해제한다.
        // ApplicationPosting은 마지막 FILE의 S3 정리가 완료된 뒤 정리 배치에서 삭제한다.
        applicationPosting.detachSourceJobPosting();
    }

    /**
     * 회사 이름 검증 메소드(에러코드 반환)
     * @param companyName 검증하려는 DTO의 문자열 타입의 회사 이름
     * @author say_0
     */
    private void validateCompanyName(String companyName) {
        // [예외처리] 회사 이름 공백 예외처리
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_NAME_REQUIRED);
        }

        // [예외처리] 이름 길이가 2보다 작으면 예외처리
        if (companyName.length() < 2) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_NAME_TOO_SHORT);
        }

        // [예외처리] 이름 길이가 50보다 크면 예외처리
        if (companyName.length() > 50) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_NAME_TOO_LONG);
        }

        // [예외처리] 회사 이름에 특수 문자만 입력시 예외처리
        if (!SPECIAL_CHAR.matcher(companyName).find()) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_NAME_SPECIAL_CHAR);
        }
    }

    /**
     * 모집 공고 이름 검증 메소드(에러코드 반환)
     * @param jobPostingName 검증하려는 모집 공고글의 문자열 타입의 공고 이름
     * @author say_0
     */
    private void validateJobPostingName(String jobPostingName) {
        // [예외처리] 공고 이름 공백 예외처리
        if (jobPostingName == null || jobPostingName.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_NAME_REQUIRED);
        }
        // [예외처리] 이름 길이가 2보다 작으면 예외처리
        if (jobPostingName.length() < 2) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_NAME_TOO_SHORT);
        }

        // [예외처리] 이름 길이가 100보다 크면 예외처리
        if (jobPostingName.length() > 100) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_NAME_TOO_LONG);
        }

        // [예외처리] 공고 이름에 특수 문자만 입력시 예외처리
        if (!SPECIAL_CHAR.matcher(jobPostingName).find()) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_NAME_SPECIAL_CHAR);
        }
    }

    /**
     * 모집 공고 URL 검증 메소드(에러코드 반환)
     * @param jobPostingLink 검증하려는 URL의 문자열 타입의 주소
     * @author say_0
     */
    private void validateJobPostingLink(String jobPostingLink) {
        // [예외처리] URL 필수 입력 예외처리(null 또는 공백)
        if (jobPostingLink == null || jobPostingLink.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_URL_REQUIRED);
        }
        if (jobPostingLink.length() > 2048) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_URL_TOO_LONG);
        }
        // [예외처리] URL 링크 형식이 아니면 예외처리
        try {
            URI uri = URI.create(jobPostingLink);

            boolean invalidScheme = uri.getScheme() == null ||
                    !(uri.getScheme().equalsIgnoreCase("http") ||
                            uri.getScheme().equalsIgnoreCase("https"));

            if (invalidScheme || uri.getHost() == null) {
                throw new BusinessException(ErrorCode.CARD_JOB_POSTING_URL_INVALID);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.CARD_JOB_POSTING_URL_INVALID);
        }
    }

}
