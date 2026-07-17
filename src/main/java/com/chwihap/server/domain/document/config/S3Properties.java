package com.chwihap.server.domain.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {
    // application 설정값을 안전하게 바인딩하여 관련 클래스에서 주입받아 사용
    private String bucket;
    private String region;
    private Duration presignedUrlDuration;
    private DataSize maxFileSize;
    private DataSize accountStorageLimit;
}
