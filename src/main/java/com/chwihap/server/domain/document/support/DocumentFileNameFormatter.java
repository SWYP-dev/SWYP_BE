package com.chwihap.server.domain.document.support;

import org.springframework.util.StringUtils;

public final class DocumentFileNameFormatter {

    private DocumentFileNameFormatter() {
    }

    public static String format(String originalName, int version) {
        if (version <= 0) {
            return originalName;
        }

        String extension = StringUtils.getFilenameExtension(originalName);
        String baseName = StringUtils.stripFilenameExtension(originalName);

        if (!StringUtils.hasText(extension)) {
            return "%s_v%d".formatted(baseName, version);
        }
        return "%s_v%d.%s".formatted(baseName, version, extension);
    }
}
