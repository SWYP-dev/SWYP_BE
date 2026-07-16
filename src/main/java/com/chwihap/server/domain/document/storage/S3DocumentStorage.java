package com.chwihap.server.domain.document.storage;

import com.chwihap.server.domain.document.config.S3Properties;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DocumentStorage implements DocumentStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public void upload(String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, contentLength)
            );
        } catch (RuntimeException e) {
            log.error("S3 업로드 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_ERROR);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (RuntimeException e) {
            log.error("S3 삭제 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_ERROR);
        }
    }

    @Override
    public String createDownloadUrl(String key, String downloadFileName, Duration duration) {
        try {
            String encodedName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .responseContentDisposition("attachment; filename*=UTF-8''" + encodedName)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(objectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
        } catch (RuntimeException e) {
            log.error("S3 URL 생성 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_ERROR);
        }
    }
}
