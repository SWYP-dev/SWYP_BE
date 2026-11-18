package com.chwihap.server.domain.feedback.controller;

import com.chwihap.server.domain.feedback.dto.FeedbackCreateRequest;
import com.chwihap.server.domain.feedback.dto.FeedbackCreateResponse;
import com.chwihap.server.domain.feedback.enums.FeedbackType;
import com.chwihap.server.domain.feedback.service.FeedbackService;
import com.chwihap.server.global.security.JwtTokenProvider;
import com.chwihap.server.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@Import(SecurityConfig.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void 비로그인_사용자도_필드별_form_data로_의견을_제출할_수_있다() throws Exception {
        given(feedbackService.create(isNull(), any(), any(FeedbackCreateRequest.class), isNull()))
                .willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(bugReport())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedbackId").value(1));

        verify(feedbackService).create(isNull(), eq("127.0.0.1"),
                eq(new FeedbackCreateRequest(FeedbackType.BUG_REPORT, "오류가 있어요", null)), isNull());
    }

    @Test
    void 서비스_이용_문의는_이메일_필드를_함께_전달한다() throws Exception {
        given(feedbackService.create(any(), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(multipart("/api/v1/feedback")
                        .param("type", "SERVICE_INQUIRY")
                        .param("content", "문의합니다")
                        .param("email", "user@example.com"))
                .andExpect(status().isCreated());

        verify(feedbackService).create(isNull(), any(),
                eq(new FeedbackCreateRequest(FeedbackType.SERVICE_INQUIRY, "문의합니다", "user@example.com")), isNull());
    }

    @Test
    void 로그인_사용자는_사용자_ID와_이미지를_함께_제출한다() throws Exception {
        given(jwtTokenProvider.validateToken("valid", JwtTokenProvider.TokenType.ACCESS)).willReturn(true);
        given(jwtTokenProvider.getUserId("valid")).willReturn(7L);
        given(feedbackService.create(eq(7L), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(bugReport().file(imagePart()).header("Authorization", "Bearer valid"))
                .andExpect(status().isCreated());

        verify(feedbackService).create(eq(7L), any(), any(), any(MockMultipartFile.class));
    }

    @Test
    void 만료된_토큰으로_제출하면_비로그인_건으로_접수한다() throws Exception {
        given(jwtTokenProvider.validateToken("expired", JwtTokenProvider.TokenType.ACCESS)).willReturn(false);
        given(feedbackService.create(isNull(), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(bugReport().header("Authorization", "Bearer expired"))
                .andExpect(status().isCreated());

        verify(feedbackService).create(isNull(), any(), any(), any());
    }

    @Test
    void 클라이언트가_보낸_X_Forwarded_For가_아니라_nginx가_설정한_X_Real_IP로_제한한다() throws Exception {
        given(feedbackService.create(any(), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(bugReport()
                        .header("X-Forwarded-For", "1.1.1.1, 203.0.113.7")
                        .header("X-Real-IP", "203.0.113.7"))
                .andExpect(status().isCreated());

        verify(feedbackService).create(isNull(), eq("203.0.113.7"), any(), any());
    }

    @Test
    void X_Real_IP가_없으면_요청_주소로_제한한다() throws Exception {
        given(feedbackService.create(any(), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(bugReport()
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.9");
                            return request;
                        }))
                .andExpect(status().isCreated());

        verify(feedbackService).create(isNull(), eq("198.51.100.9"), any(), any());
    }

    @Test
    void 유형_필드가_없으면_서비스_검증으로_넘긴다() throws Exception {
        given(feedbackService.create(any(), any(), any(), any())).willReturn(new FeedbackCreateResponse(1L));

        mockMvc.perform(multipart("/api/v1/feedback").param("content", "내용"))
                .andExpect(status().isCreated());

        verify(feedbackService).create(isNull(), any(), eq(new FeedbackCreateRequest(null, "내용", null)), isNull());
    }

    @Test
    void 존재하지_않는_유형이면_400으로_응답한다() throws Exception {
        mockMvc.perform(multipart("/api/v1/feedback")
                        .param("type", "UNKNOWN")
                        .param("content", "내용"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));

        verifyNoInteractions(feedbackService);
    }

    private MockMultipartHttpServletRequestBuilder bugReport() {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/v1/feedback")
                .param("type", "BUG_REPORT")
                .param("content", "오류가 있어요");
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile("image", "error.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
    }
}
