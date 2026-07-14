package com.chwihap.server.domain.document.controller;

import com.chwihap.server.domain.document.dto.DocumentDownloadUrlResponse;
import com.chwihap.server.domain.document.dto.DocumentLinkCreateRequest;
import com.chwihap.server.domain.document.dto.DocumentListResponse;
import com.chwihap.server.domain.document.dto.DocumentMemoCreateRequest;
import com.chwihap.server.domain.document.dto.DocumentResponse;
import com.chwihap.server.domain.document.service.DocumentService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/kanban/cards/{cardId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> uploadFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(documentService.uploadFile(principal.id(), cardId, file));
    }

    @PostMapping("/link")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> registerLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId,
            @Valid @RequestBody DocumentLinkCreateRequest request
    ) {
        return ApiResponse.success(documentService.registerLink(principal.id(), cardId, request));
    }

    @PostMapping("/memo")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> registerMemo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId,
            @Valid @RequestBody DocumentMemoCreateRequest request
    ) {
        return ApiResponse.success(documentService.registerMemo(principal.id(), cardId, request));
    }

    @GetMapping
    public ApiResponse<DocumentListResponse> getDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId
    ) {
        return ApiResponse.success(documentService.getDocuments(principal.id(), cardId));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId,
            @PathVariable Long documentId
    ) {
        documentService.deleteDocument(principal.id(), cardId, documentId);
        return ApiResponse.success();
    }

    @GetMapping("/{documentId}/download")
    public ApiResponse<DocumentDownloadUrlResponse> createDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId,
            @PathVariable Long documentId
    ) {
        return ApiResponse.success(documentService.createDownloadUrl(principal.id(), cardId, documentId));
    }
}
