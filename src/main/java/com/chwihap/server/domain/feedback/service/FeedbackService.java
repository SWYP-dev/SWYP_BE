package com.chwihap.server.domain.feedback.service;

import com.chwihap.server.domain.document.storage.DocumentStorage;
import com.chwihap.server.domain.feedback.dto.FeedbackCreateRequest;
import com.chwihap.server.domain.feedback.dto.FeedbackCreateResponse;
import com.chwihap.server.domain.feedback.entity.Feedback;
import com.chwihap.server.domain.feedback.enums.FeedbackImageType;
import com.chwihap.server.domain.feedback.enums.FeedbackType;
import com.chwihap.server.domain.feedback.repository.FeedbackRepository;
import com.chwihap.server.domain.feedback.support.FeedbackImageValidator;
import com.chwihap.server.domain.user.entity.User;
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
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    public static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_EMAIL_LENGTH = 254;
    // FE와 같은 정규식. @Email은 "a@b"처럼 최상위 도메인이 없는 주소도 통과시키므로 사용하지 않는다.
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String IMAGE_KEY_PATTERN = "feedback/%d/%02d/%s.%s";

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final FeedbackImageValidator imageValidator;
    private final FeedbackRateLimiter rateLimiter;

    /**
     * 의견을 접수한다. 로그인하지 않은 사용자도 제출할 수 있다.
     * 이미지는 S3에 먼저 올린 뒤 DB에 저장하며, DB 저장에 실패하면 올린 이미지를 삭제한다.
     * @param userId 로그인한 유저 ID (비로그인이면 null)
     * @param clientIp 비로그인 사용자의 제출 횟수 제한에 사용할 IP
     * @param request 문의 유형, 내용, 이메일
     * @param image 첨부 이미지 (선택)
     * @return 접수된 의견 ID
     */
    @Transactional
    public FeedbackCreateResponse create(Long userId, String clientIp, FeedbackCreateRequest request, MultipartFile image) {
        rateLimiter.checkSubmit(userId, clientIp);

        // [예외처리] 잘못된 요청 때문에 S3에 업로드되지 않도록 텍스트 → 이미지 순으로 먼저 검증
        validateType(request);
        String content = normalizeContent(request.content());
        String email = request.type() == FeedbackType.SERVICE_INQUIRY ? normalizeEmail(request.email()) : null;
        boolean hasImage = image != null && !image.isEmpty();
        FeedbackImageType imageType = hasImage ? imageValidator.validate(image) : null;

        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        String imageKey = hasImage ? uploadImage(image, imageType) : null;

        // 객체 생성 후 DB에 저장 -> S3에만 저장되고 DB에는 저장되지 않는 고아 이미지 방지 로직
        try {
            Feedback feedback = feedbackRepository.saveAndFlush(
                    Feedback.create(user, request.type(), content, email, imageKey));
            return new FeedbackCreateResponse(feedback.getId());
        } catch (RuntimeException originalException) {
            if (imageKey != null) {
                deleteUploadedImage(imageKey, originalException);
            }
            throw originalException;
        }
    }

    private void validateType(FeedbackCreateRequest request) {
        if (request == null || request.type() == null) {
            throw new BusinessException(ErrorCode.FEEDBACK_TYPE_REQUIRED);
        }
    }

    private String normalizeContent(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.FEEDBACK_CONTENT_REQUIRED);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.FEEDBACK_CONTENT_TOO_LONG);
        }
        return content;
    }

    private String normalizeEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new BusinessException(ErrorCode.FEEDBACK_EMAIL_REQUIRED);
        }
        String email = rawEmail.trim();
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.FEEDBACK_EMAIL_INVALID);
        }
        return email;
    }

    private String uploadImage(MultipartFile image, FeedbackImageType imageType) {
        LocalDate today = LocalDate.now();
        String imageKey = IMAGE_KEY_PATTERN.formatted(
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), imageType.getExtension());

        try (InputStream inputStream = image.getInputStream()) {
            documentStorage.upload(imageKey, inputStream, image.getSize(), imageType.getContentType());
        } catch (IOException | BusinessException e) {
            log.error("의견 이미지 업로드 실패. key={}", imageKey, e);
            throw new BusinessException(ErrorCode.FEEDBACK_IMAGE_UPLOAD_FAILED);
        }
        return imageKey;
    }

    private void deleteUploadedImage(String imageKey, RuntimeException originalException) {
        log.error("의견 저장 실패로 S3 보상 삭제를 시도합니다. key={}", imageKey, originalException);
        try {
            documentStorage.delete(imageKey);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.error("S3 보상 삭제에도 실패했습니다. key={}", imageKey, cleanupException);
        }
    }
}
