package com.chwihap.server.global.exception;

import com.chwihap.server.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
		log.warn("MaxUploadSizeExceededException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.FILE_SIZE_EXCEEDED.getStatus())
			.body(ApiResponse.fail(ErrorCode.FILE_SIZE_EXCEEDED));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		log.warn("BusinessException: {}", e.getMessage());
		return ResponseEntity
			.status(e.getErrorCode().getStatus())
			.body(ApiResponse.fail(e.getErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.debug("MethodArgumentNotValidException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>>
	handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
		log.debug("MethodArgumentTypeMismatchException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.debug("HttpMessageNotReadableException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
		log.debug("NoResourceFoundException: {}", e.getMessage());
		return ResponseEntity
			.status(ErrorCode.ENTITY_NOT_FOUND.getStatus())
			.body(ApiResponse.fail(ErrorCode.ENTITY_NOT_FOUND));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("Unhandled exception occurred", e);
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
