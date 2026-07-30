package com.chwihap.server.domain.feed.enums;

import java.util.EnumSet;
import java.util.Set;

public enum JobPlatform {
    SARAMIN,
    WORKNET,
    PUBLIC,
    PUBLIC_PERSONNEL,
    DIRECT,
    EXTERNAL,
    ROCKETPUNCH;

    private static final Set<JobPlatform> PUBLIC_SECTOR = EnumSet.of(WORKNET, PUBLIC, PUBLIC_PERSONNEL);

    /**
     * 워크넷·공공데이터포털·인사혁신처처럼 정부/공공기관이 출처인 플랫폼인지 여부.
     * 피드 노출 우선순위(사설 우선) 등 정책 판단에 사용한다.
     */
    public boolean isPublicSector() {
        return PUBLIC_SECTOR.contains(this);
    }
}
