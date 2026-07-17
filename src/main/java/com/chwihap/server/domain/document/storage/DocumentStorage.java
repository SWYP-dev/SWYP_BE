package com.chwihap.server.domain.document.storage;

import java.io.InputStream;
import java.time.Duration;

public interface DocumentStorage {

    void upload(String key, InputStream inputStream, long contentLength, String contentType);

    void delete(String key);

    String createDownloadUrl(String key, String downloadFileName, Duration duration);
}
