package com.chwihap.server.domain.feed.sync;

import com.chwihap.server.domain.feed.enums.Region;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 로켓펀치 API는 채용공고에 지역 정보를 전혀 제공하지 않아({@code JobDetailResponse}/{@code JobCompanyResponse}
 * 어디에도 region/address 필드 없음), 알려진 회사에 한해 이 정적 매핑으로 지역을 보충한다.
 * <p>
 * 회사 위치를 알게 될 때마다 이 맵에 한 줄만 추가하면 된다. DB에 직접 UPDATE하는 방식과 달리
 * 배치 수집({@link JobFeedRocketPunchSyncService}) 때마다 이 매핑이 적용되므로 재수집해도 값이
 * 유지된다. 매핑에 없는 회사는 계속 region=null(지역 필터에서 제외)로 저장된다.
 * <p>
 * 회사명은 로켓펀치 응답의 {@code company.name} 원문과 정확히 일치해야 매칭된다.
 */
@Component
public class RocketPunchCompanyRegionMapper {

    private static final Map<String, Region> REGION_BY_COMPANY_NAME = Map.ofEntries(
            // 예시: Map.entry("페이타랩", Region.SEOUL),
            // 확인되는 대로 아래에 한 줄씩 추가한다.
    );

    /**
     * 회사명으로 알려진 지역을 찾는다. 매칭되지 않으면 null.
     */
    public String regionFor(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return null;
        }
        Region region = REGION_BY_COMPANY_NAME.get(companyName.trim());
        return region == null ? null : region.getLabel();
    }
}
