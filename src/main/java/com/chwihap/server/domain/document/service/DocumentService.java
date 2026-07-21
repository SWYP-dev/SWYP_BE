package com.chwihap.server.domain.document.service;

import com.chwihap.server.domain.document.config.S3Properties;
import com.chwihap.server.domain.document.dto.DocumentDownloadUrlResponse;
import com.chwihap.server.domain.document.dto.DocumentLinkCreateRequest;
import com.chwihap.server.domain.document.dto.DocumentListResponse;
import com.chwihap.server.domain.document.dto.DocumentMemoCreateRequest;
import com.chwihap.server.domain.document.dto.DocumentResponse;
import com.chwihap.server.domain.document.entity.Document;
import com.chwihap.server.domain.document.enums.DocumentType;
import com.chwihap.server.domain.document.repository.DocumentRepository;
import com.chwihap.server.domain.document.storage.DocumentStorage;
import com.chwihap.server.domain.kanban.entity.KanbanCard;
import com.chwihap.server.domain.kanban.repository.KanbanCardRepository;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "pptx");
    private static final String DOCUMENT_PATTERN = "users/%d/cards/%d/documents/%s%s";

    private final DocumentRepository documentRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final S3Properties s3Properties;

    /**
     * 4.1 서류 업로드(파일)<\br>
     * 사용자의 서류를 업로드하는 메소드(개당 파일 크기제한 10MB, 전체 파일 크기 제한 100MB)
     * @param userId 로그인한 유저 ID
     * @param cardId 카드 ID
     * @param file 파일 객체
     * @return 업로드된 파일 정보 반환
     */
    @Transactional
    public DocumentResponse uploadFile(Long userId, Long cardId, MultipartFile file) {
        // [예외처리] 파일 이름 및 파일 크기 검증후 원본이름 문자열에 저장
        String originalName = validateAndGetOriginalFileName(file);

        userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        KanbanCard card = getOwnedCard(userId, cardId);

        // [예외처리] 유저의 최대 파일 업로드 양 검증
        validateStorageLimit(userId, file.getSize());

        String versionGroup = UUID.nameUUIDFromBytes(
                originalName.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        ).toString();
        // 같은 이름의 문서 버전 증가
        int version = documentRepository
                .findTopByUser_IdAndJobPosting_IdAndVersionGroupOrderByVersionDesc(
                        userId, card.getJobPosting().getId(), versionGroup)
                .map(document -> document.getVersion() + 1)
                .orElse(1);
        String storageKey = createStorageKey(userId, cardId, originalName);
        String contentType = file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();

        // 업로드 파일 스트림 처리(S3 저장)
        try (InputStream inputStream = file.getInputStream()) {
            documentStorage.upload(
                    storageKey,
                    inputStream,
                    file.getSize(),
                    contentType
            );
        } catch (IOException e) {
            log.error("업로드 파일 스트림 처리 실패. key={}", storageKey, e);
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_ERROR);
        }

        // 객체 생성후 DB에 저장 -> S3에만 저장되고 DB에는 저장되지 않는 고아 문서 방지 로직
        try {
            Document document = Document.file(
                    card.getUser(),
                    card.getJobPosting(),
                    storageKey.substring(storageKey.lastIndexOf('/') + 1),
                    originalName,
                    storageKey,
                    file.getSize(),
                    version,
                    versionGroup
            );
            return DocumentResponse.from(documentRepository.saveAndFlush(document));
        } catch (RuntimeException originalException) {
            log.error("문서 처리 실패로 S3 보상 삭제를 시도합니다. key={}", storageKey, originalException);
            try {
                documentStorage.delete(storageKey);
            } catch (RuntimeException cleanupException) {
                originalException.addSuppressed(cleanupException);
                log.error("S3 보상 삭제에도 실패했습니다. key={}",storageKey, cleanupException);
            }
            throw originalException;
        }
    }

    /**
     * 4.2 서류 등록(외부 링크)<\br>
     * Notion, Google Docs등 외부 URL을 서류로 등록한다.
     * @param userId 유저 ID
     * @param cardId 서류를 첨부할 카드 ID
     * @param request 서류 이름과 URL을 받는다.
     * @return 저장한 문서 정보를 반환
     */
    @Transactional
    public DocumentResponse registerLink(Long userId, Long cardId, DocumentLinkCreateRequest request) {
        KanbanCard card = getOwnedCard(userId, cardId);
        validateHttpUrl(request.url());
        Document document = Document.link(
                card.getUser(),
                card.getJobPosting(),
                request.name().trim(),
                request.url().trim()
        );
        return DocumentResponse.from(documentRepository.saveAndFlush(document));
    }

    /**
     * 4.3 메모 등록<\br>
     * 서류에 관련된 메모를 등록한다.(카드 메모와는 다른 기능)
     * @param userId 유저 ID
     * @param cardId 카드 ID
     * @param request 이름, 내용을 받는다.
     * @return 메모와 관련된 정보를 반환한다.
     */
    @Transactional
    public DocumentResponse registerMemo(Long userId, Long cardId, DocumentMemoCreateRequest request) {
        KanbanCard card = getOwnedCard(userId, cardId);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_MEMO_REQUIRED);
        }

        Document document = Document.memo(
                card.getUser(), card.getJobPosting(), request.name().trim(), content);
        return DocumentResponse.from(documentRepository.saveAndFlush(document));
    }

    /**
     * 4.4 서류 목록 조회<\br>
     * 카드에 첨부된 FILE / LINK / MEMO 전체를 리스트 형태로 반환
     * @param userId 유저 ID
     * @param cardId 조회하려는 카드 ID
     * @return 문서 정보를 담은 리스트 반환
     */
    public DocumentListResponse getDocuments(Long userId, Long cardId) {
        KanbanCard card = getOwnedCard(userId, cardId);
        List<DocumentResponse> documents = documentRepository
                .findActiveByUserIdAndJobPostingId(userId, card.getJobPosting().getId())
                .stream()
                .map(DocumentResponse::from)
                .toList();
        return new DocumentListResponse(documents);
    }

    /**
     * 4.5 서류 삭제<\br>
     * 첨부된 서류를 삭제한다. FILE은 S3 정리가 필요해 soft delete, LINK/MEMO는 즉시 hard delete.
     * @param userId 유저 ID
     * @param cardId 삭제하려는 문서의 카드 ID
     * @param documentId 삭제하려는 문서의 ID
     */
    @Transactional
    public void deleteDocument(Long userId, Long cardId, Long documentId) {
        KanbanCard card = getOwnedCard(userId, cardId);
        Document document = getOwnedDocument(userId, card, documentId);
        if (document.getDocType() == DocumentType.FILE) {
            document.softDelete();
        } else {
            documentRepository.delete(document);
        }
    }

    /**
     * 4.6 파일 다운로드 URL 발급<\br>
     * S3에 저장된 파일을 다운로드할 수 있는 Presigned URL을 발급한다.
     * @param userId 유저 ID
     * @param cardId 다운로드할 파일 카드 ID
     * @param documentId 다운로드할 문서 ID
     * @return 다운로드할 문서 URL 객체를 반환
     */
    public DocumentDownloadUrlResponse createDownloadUrl(Long userId, Long cardId, Long documentId) {
        KanbanCard card = getOwnedCard(userId, cardId);
        Document document = getOwnedDocument(userId, card, documentId);
        if (document.getDocType() != DocumentType.FILE) {
            throw new BusinessException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }

        String downloadFileName = createVersionedDownloadFileName(
                document.getOriginalName(), document.getVersion());
        String downloadUrl = documentStorage.createDownloadUrl(
                document.getFileUrl(), downloadFileName, s3Properties.getPresignedUrlDuration());
        return new DocumentDownloadUrlResponse(
                downloadUrl,
                LocalDateTime.now().plus(s3Properties.getPresignedUrlDuration())
        );
    }

    private KanbanCard getOwnedCard(Long userId, Long cardId) {
        return kanbanCardRepository.findByIdAndUser_Id(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private Document getOwnedDocument(Long userId, KanbanCard card, Long documentId) {
        return documentRepository.findActiveByIdAndOwner(
                        documentId, userId, card.getJobPosting().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    /**
     * 파일을 검증하고 안전하게 정리된 원본 파일명을 반환하는 메소드
     * @param file 파일
     * @return 경로 정보가 제거된 원본 파일명
     */
    private String validateAndGetOriginalFileName(MultipartFile file) {
        // [예외처리] 파일이 존재하는지 검증
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_FILE_REQUIRED);
        }
        // [예외처리] 파일이 10MB 이하인지 검증
        if (file.getSize() > s3Properties.getMaxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        // [예외처리] 원본 파일 이름 및 경로를 검증
        String originalName = cleanFileName(file.getOriginalFilename());
        // [예외처리] 확장자명이 맞는지 검사
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        return originalName;
    }

    private void validateStorageLimit(Long userId, long uploadSize) {
        long used = documentRepository.sumActiveFileSizeByUserId(userId, DocumentType.FILE);
        long limit = s3Properties.getAccountStorageLimit().toBytes();
        // 유저의 최대사용량(100MB) > 각 개체의 최대 업로드 양(10MB) - 현재 업로드 파일 크기(MB)
        if (used > limit - uploadSize) {
            throw new BusinessException(ErrorCode.STORAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 파일 이름만 추출하는 메소드
     * @param originalFilename 경로를 포함한 파일이름
     * @return 경로를 제외한 파일 이름 반환
     */
    private String cleanFileName(String originalFilename) {
        String fileName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        if (!StringUtils.hasText(fileName) || fileName.contains("..") || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.DOCUMENT_NAME_REQUIRED);
        }
        return fileName;
    }

    /**
     * 원본 파일명은 유지하면서 다운로드 파일명에 문서 버전을 추가한다.
     * @param originalName 원본 파일명
     * @param version 문서 버전
     * @return 확장자 앞에 버전이 추가된 다운로드 파일명
     */
    private String createVersionedDownloadFileName(String originalName, int version) {
        String extension = StringUtils.getFilenameExtension(originalName);
        String baseName = StringUtils.stripFilenameExtension(originalName);
        if (!StringUtils.hasText(extension)) {
            return "%s_v%d".formatted(baseName, version);
        }
        return "%s_v%d.%s".formatted(baseName, version, extension);
    }

    /**
     * 들어오는 경로가 올바른 경로인지 체크하는 메소드
     * @param value url 경로
     */
    private void validateHttpUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID);
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_URL_INVALID);
        }
    }

    /**
     * 저장소 키를 만드는 메소드
     * @param userId 유저 ID
     * @param cardId 카드 ID
     * @param fileName 파일이름
     * @return 파일이 저장되는 경로 열쇠를 반환
     */
    private String createStorageKey(Long userId, Long cardId, String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        String suffix = StringUtils.hasText(extension)
                ? "." + extension.toLowerCase(Locale.ROOT)
                : "";
        return DOCUMENT_PATTERN.formatted(
                userId,
                cardId,
                UUID.randomUUID(),
                suffix
        );
    }
}
