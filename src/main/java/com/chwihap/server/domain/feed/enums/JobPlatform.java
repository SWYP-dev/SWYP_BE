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
    ROCKETPUNCH,
    JOBABA;

    private static final Set<JobPlatform> PUBLIC_SECTOR = EnumSet.of(WORKNET, PUBLIC, PUBLIC_PERSONNEL, JOBABA);

    /**
     * 워크넷·공공데이터포털·인사혁신처·경기도일자리재단(잡아바)처럼 정부/공공기관이 출처인 플랫폼인지 여부.
     * 피드 노출 우선순위(사설 우선) 등 정책 판단에 사용한다.
     */
    public boolean isPublicSector() {
        return PUBLIC_SECTOR.contains(this);
    }
}
