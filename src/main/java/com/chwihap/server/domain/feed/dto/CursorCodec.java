package com.chwihap.server.domain.feed.dto;

import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CursorCodec() {
    }

    public static String encode(Object payload) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("커서 인코딩 실패", e);
        }
    }

    public static <T> T decode(String cursor, Class<T> type) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8));
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}
