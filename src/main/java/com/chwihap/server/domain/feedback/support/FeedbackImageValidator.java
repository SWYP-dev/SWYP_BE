package com.chwihap.server.domain.feedback.support;

import com.chwihap.server.domain.feedback.enums.FeedbackImageType;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * 의견 첨부 이미지를 검증한다.
 * 클라이언트가 보낸 Content-Type은 조작할 수 있으므로 확장자와 파일 앞부분(매직바이트)이 모두 일치해야 허용한다.
 */
@Component
public class FeedbackImageValidator {

    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final Map<String, FeedbackImageType> EXTENSION_TYPES = Map.of(
            "png", FeedbackImageType.PNG,
            "jpg", FeedbackImageType.JPEG,
            "jpeg", FeedbackImageType.JPEG
    );

    /**
     * @param image 첨부 이미지 (비어 있지 않아야 함)
     * @return 파일 내용으로 판정한 이미지 형식
     */
    public FeedbackImageType validate(MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.FEEDBACK_IMAGE_SIZE_EXCEEDED);
        }

        String extension = StringUtils.getFilenameExtension(image.getOriginalFilename());
        FeedbackImageType type = extension == null
                ? null
                : EXTENSION_TYPES.get(extension.toLowerCase(Locale.ROOT));
        if (type == null || !matchesSignature(image, signatureOf(type))) {
            throw new BusinessException(ErrorCode.FEEDBACK_IMAGE_UNSUPPORTED_TYPE);
        }
        return type;
    }

    private byte[] signatureOf(FeedbackImageType type) {
        return type == FeedbackImageType.PNG ? PNG_SIGNATURE : JPEG_SIGNATURE;
    }

    private boolean matchesSignature(MultipartFile image, byte[] signature) {
        try (InputStream inputStream = image.getInputStream()) {
            byte[] header = inputStream.readNBytes(signature.length);
            return Arrays.equals(header, signature);
        } catch (IOException e) {
            return false;
        }
    }
}
