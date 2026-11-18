package com.chwihap.server.global.exception;

import com.chwihap.server.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 의견_제출_경로에서_업로드_한도를_넘으면_의견_이미지_용량_에러로_응답한다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(10L), request("/api/v1/feedback"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("FB007");
        assertThat(response.getBody().getMessage()).isEqualTo("10MB 이하의 이미지만 첨부할 수 있어요.");
    }

    @Test
    void 서류_업로드_경로에서_업로드_한도를_넘으면_기존_파일_용량_에러로_응답한다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(10L), request("/api/v1/kanban/cards/1/documents/file"));

        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getCode());
    }

    @Test
    void multipart_필수_파트가_없으면_400으로_응답한다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingServletRequestPartException(
                new MissingServletRequestPartException("request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        return request;
    }
}
