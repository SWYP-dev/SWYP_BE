package com.chwihap.server.domain.feedback.controller;

import com.chwihap.server.domain.feedback.dto.FeedbackCreateRequest;
import com.chwihap.server.domain.feedback.dto.FeedbackCreateResponse;
import com.chwihap.server.domain.feedback.service.FeedbackService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "의견 보내기(Q&A 창구) API")
public class FeedbackController {

    private static final String REAL_IP_HEADER = "X-Real-IP";

    private final FeedbackService feedbackService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "의견 제출",
            description = """
                    오류 제보 / 기능 개선 제안 / 서비스 이용 문의 / 기타 의견을 접수합니다. 로그인하지 않아도 제출할 수 있으며, 로그인 상태면 작성자가 연결됩니다.

                    **요청 형식 (`multipart/form-data`, 필드별 전송)**
                    - `type` (필수): BUG_REPORT / FEATURE_SUGGESTION / SERVICE_INQUIRY / ETC
                    - `content` (필수)
                    - `email` (SERVICE_INQUIRY일 때 필수)
                    - `image` (선택): PNG·JPG(JPEG) 1장, 10MB 이하
                    - 예: `formData.append('type', 'BUG_REPORT'); formData.append('content', '...'); formData.append('image', file);`

                    **검증 규칙**
                    - `type` 필수
                    - `content` 필수, 앞뒤 공백 제외 최대 1,000자
                    - `email`은 `SERVICE_INQUIRY`일 때만 필수이며 형식을 검증합니다. 다른 유형이면 보내더라도 저장하지 않습니다.
                    - 사용자 ID(비로그인은 IP)별로 10분에 5회까지 제출할 수 있습니다.

                    **실패 응답 처리 (FE)**
                    - `FB006`, `FB007`, `FB008`: 응답 `message`(이미지 관련 문구)를 그대로 표시
                    - 그 외 실패: "의견 제출에 실패했어요. 다시 시도해 주세요."
                    """
    )
    public ApiResponse<FeedbackCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            // JSON 파트 대신 필드별 form-data로 받아 클라이언트가 파트 Content-Type을 지정하지 않아도 되게 한다.
            @ModelAttribute FeedbackCreateRequest request,
            @Parameter(description = "첨부 이미지 (PNG·JPG, 10MB 이하, 선택)")
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest httpRequest
    ) {
        // 비로그인 제출을 허용하므로 principal은 null일 수 있다.
        Long userId = principal == null ? null : principal.id();
        return ApiResponse.success(feedbackService.create(userId, resolveClientIp(httpRequest), request, image));
    }

    /**
     * Rate limit에 사용할 클라이언트 IP를 구한다.
     * X-Forwarded-For는 클라이언트가 보낸 값 뒤에 nginx가 실제 IP를 덧붙이는 구조라 첫 번째 값을 위조할 수 있다.
     * nginx가 실제 접속 IP로 덮어쓰는 X-Real-IP를 우선 사용한다. (앱 포트는 외부에 열려 있지 않음)
     */
    private String resolveClientIp(HttpServletRequest request) {
        String realIp = request.getHeader(REAL_IP_HEADER);
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
