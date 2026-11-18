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
import com.chwihap.server.domain.user.enums.AuthProvider;
import com.chwihap.server.domain.user.repository.UserRepository;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final String IP = "127.0.0.1";
    private static final String CONTENT = "칸반 카드가 이동하지 않아요.";

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private FeedbackImageValidator imageValidator;
    @Mock
    private FeedbackRateLimiter rateLimiter;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(
                feedbackRepository, userRepository, documentStorage, imageValidator, rateLimiter);
    }

    @Test
    void 비로그인이면_user_없이_저장한다() {
        givenSaveReturnsId(10L);

        FeedbackCreateResponse response = feedbackService.create(
                null, IP, request(FeedbackType.BUG_REPORT, CONTENT, null), null);

        assertThat(response.feedbackId()).isEqualTo(10L);
        assertThat(savedFeedback().getUser()).isNull();
        verify(rateLimiter).checkSubmit(null, IP);
    }

    @Test
    void 로그인이면_user를_연결한다() {
        User user = User.create("user@kakao.com", "닉네임", null, AuthProvider.KAKAO, "1");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        givenSaveReturnsId(10L);

        feedbackService.create(1L, IP, request(FeedbackType.BUG_REPORT, CONTENT, null), null);

        assertThat(savedFeedback().getUser()).isSameAs(user);
    }

    @Test
    void 토큰의_회원이_존재하지_않으면_비로그인_건으로_저장한다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());
        givenSaveReturnsId(10L);

        feedbackService.create(1L, IP, request(FeedbackType.BUG_REPORT, CONTENT, null), null);

        assertThat(savedFeedback().getUser()).isNull();
    }

    @Test
    void 요청_본문이_없으면_유형_필수_예외가_발생한다() {
        assertRejected(null, null, ErrorCode.FEEDBACK_TYPE_REQUIRED);
    }

    @Test
    void 유형이_없으면_예외가_발생한다() {
        assertRejected(request(null, CONTENT, null), null, ErrorCode.FEEDBACK_TYPE_REQUIRED);
    }

    @Test
    void 공백만_있는_내용은_거부한다() {
        assertRejected(request(FeedbackType.ETC, "   \n ", null), null, ErrorCode.FEEDBACK_CONTENT_REQUIRED);
    }

    @Test
    void 내용이_없으면_거부한다() {
        assertRejected(request(FeedbackType.ETC, null, null), null, ErrorCode.FEEDBACK_CONTENT_REQUIRED);
    }

    @Test
    void 내용_1000자는_허용한다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.ETC, "가".repeat(1000), null), null);

        assertThat(savedFeedback().getContent()).hasSize(1000);
    }

    @Test
    void 앞뒤_공백을_제외하고_1000자면_허용한다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.ETC, " " + "가".repeat(1000) + " ", null), null);

        assertThat(savedFeedback().getContent()).hasSize(1000);
    }

    @Test
    void 내용이_1001자면_거부한다() {
        assertRejected(request(FeedbackType.ETC, "가".repeat(1001), null), null, ErrorCode.FEEDBACK_CONTENT_TOO_LONG);
    }

    @Test
    void 서비스_이용_문의는_이메일이_없으면_거부한다() {
        assertRejected(request(FeedbackType.SERVICE_INQUIRY, CONTENT, "  "), null, ErrorCode.FEEDBACK_EMAIL_REQUIRED);
    }

    @Test
    void 서비스_이용_문의_이메일_형식이_틀리면_거부한다() {
        assertRejected(request(FeedbackType.SERVICE_INQUIRY, CONTENT, "a@b"), null, ErrorCode.FEEDBACK_EMAIL_INVALID);
    }

    @Test
    void 이메일이_254자를_넘으면_거부한다() {
        String email = "a".repeat(245) + "@test.com"; // 254 + 1자
        assertThat(email).hasSize(254);
        assertRejected(request(FeedbackType.SERVICE_INQUIRY, CONTENT, "a" + email), null,
                ErrorCode.FEEDBACK_EMAIL_INVALID);
    }

    @Test
    void 서비스_이용_문의는_trim한_이메일을_저장한다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP,
                request(FeedbackType.SERVICE_INQUIRY, "  " + CONTENT + "  ", " user@example.com "), null);

        Feedback saved = savedFeedback();
        assertThat(saved.getContent()).isEqualTo(CONTENT);
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getType()).isEqualTo(FeedbackType.SERVICE_INQUIRY);
    }

    @Test
    void 서비스_이용_문의가_아니면_이메일을_저장하지_않는다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.BUG_REPORT, CONTENT, "user@example.com"), null);

        assertThat(savedFeedback().getEmail()).isNull();
    }

    @Test
    void 서비스_이용_문의가_아니면_형식이_틀린_이메일도_무시한다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.ETC, CONTENT, "a@b"), null);

        assertThat(savedFeedback().getEmail()).isNull();
    }

    @Test
    void 이미지_없이_저장하면_업로드하지_않는다() {
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.ETC, CONTENT, null), null);

        assertThat(savedFeedback().getImageKey()).isNull();
        verifyNoInteractions(documentStorage, imageValidator);
    }

    @Test
    void 빈_이미지_파트는_첨부하지_않은_것으로_처리한다() {
        givenSaveReturnsId(10L);
        MockMultipartFile empty = new MockMultipartFile("image", "", "application/octet-stream", new byte[0]);

        feedbackService.create(null, IP, request(FeedbackType.ETC, CONTENT, null), empty);

        assertThat(savedFeedback().getImageKey()).isNull();
        verifyNoInteractions(documentStorage, imageValidator);
    }

    @Test
    void 이미지가_있으면_feedback_경로로_업로드하고_key를_저장한다() {
        MockMultipartFile image = image();
        given(imageValidator.validate(image)).willReturn(FeedbackImageType.PNG);
        givenSaveReturnsId(10L);

        feedbackService.create(null, IP, request(FeedbackType.BUG_REPORT, CONTENT, null), image);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentStorage).upload(keyCaptor.capture(), any(InputStream.class), eq(image.getSize()), eq("image/png"));
        assertThat(keyCaptor.getValue()).matches("^feedback/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png$");
        assertThat(savedFeedback().getImageKey()).isEqualTo(keyCaptor.getValue());
    }

    @Test
    void 텍스트_검증에_실패하면_이미지를_검증하거나_업로드하지_않는다() {
        assertRejected(request(FeedbackType.ETC, " ", null), image(), ErrorCode.FEEDBACK_CONTENT_REQUIRED);

        verifyNoInteractions(documentStorage, imageValidator);
    }

    @Test
    void 이미지_검증에_실패하면_업로드하지_않는다() {
        MockMultipartFile image = image();
        given(imageValidator.validate(image))
                .willThrow(new BusinessException(ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE));

        assertRejected(request(FeedbackType.ETC, CONTENT, null), image, ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);

        verifyNoInteractions(documentStorage);
    }

    @Test
    void S3_업로드에_실패하면_이미지_첨부_실패_예외가_발생한다() {
        MockMultipartFile image = image();
        given(imageValidator.validate(image)).willReturn(FeedbackImageType.PNG);
        willThrow(new BusinessException(ErrorCode.DOCUMENT_STORAGE_ERROR))
                .given(documentStorage).upload(anyString(), any(InputStream.class), anyLong(), anyString());

        assertRejected(request(FeedbackType.ETC, CONTENT, null), image, ErrorCode.FEEDBACK_IMAGE_UPLOAD_FAILED);

        verify(feedbackRepository, never()).saveAndFlush(any());
    }

    @Test
    void DB_저장에_실패하면_업로드한_이미지를_삭제하고_원래_예외를_던진다() {
        MockMultipartFile image = image();
        given(imageValidator.validate(image)).willReturn(FeedbackImageType.PNG);
        RuntimeException dbError = new RuntimeException("db error");
        given(feedbackRepository.saveAndFlush(any(Feedback.class))).willThrow(dbError);

        assertThatThrownBy(() -> feedbackService.create(null, IP, request(FeedbackType.ETC, CONTENT, null), image))
                .isSameAs(dbError);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentStorage).upload(keyCaptor.capture(), any(InputStream.class), anyLong(), anyString());
        verify(documentStorage).delete(keyCaptor.getValue());
    }

    @Test
    void 보상_삭제도_실패하면_원래_예외에_삭제_예외를_붙여_던진다() {
        MockMultipartFile image = image();
        given(imageValidator.validate(image)).willReturn(FeedbackImageType.PNG);
        RuntimeException dbError = new RuntimeException("db error");
        given(feedbackRepository.saveAndFlush(any(Feedback.class))).willThrow(dbError);
        RuntimeException deleteError = new RuntimeException("s3 delete error");
        willThrow(deleteError).given(documentStorage).delete(anyString());

        assertThatThrownBy(() -> feedbackService.create(null, IP, request(FeedbackType.ETC, CONTENT, null), image))
                .isSameAs(dbError)
                .hasSuppressedException(deleteError);
    }

    @Test
    void 제출_한도를_넘으면_검증과_저장을_하지_않는다() {
        willThrow(new BusinessException(ErrorCode.FEEDBACK_RATE_LIMITED)).given(rateLimiter).checkSubmit(null, IP);

        assertRejected(request(FeedbackType.ETC, CONTENT, null), image(), ErrorCode.FEEDBACK_RATE_LIMITED);

        verifyNoInteractions(documentStorage, imageValidator, feedbackRepository);
    }

    private FeedbackCreateRequest request(FeedbackType type, String content, String email) {
        return new FeedbackCreateRequest(type, content, email);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "error.png", "image/png", new byte[]{1, 2, 3});
    }

    private void givenSaveReturnsId(Long id) {
        given(feedbackRepository.saveAndFlush(any(Feedback.class))).willAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            ReflectionTestUtils.setField(feedback, "id", id);
            return feedback;
        });
    }

    private Feedback savedFeedback() {
        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void assertRejected(FeedbackCreateRequest request, MockMultipartFile image, ErrorCode errorCode) {
        assertThatThrownBy(() -> feedbackService.create(null, IP, request, image))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
        verify(feedbackRepository, never()).saveAndFlush(any());
    }
}
