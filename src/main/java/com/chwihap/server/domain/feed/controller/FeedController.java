package com.chwihap.server.domain.feed.controller;

import com.chwihap.server.domain.feed.dto.*;
import com.chwihap.server.domain.feed.enums.FeedSort;
import com.chwihap.server.domain.feed.service.FeedService;
import com.chwihap.server.global.auth.UserPrincipal;
import com.chwihap.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 2.1 공고 피드 조회
     */
    @GetMapping
    public ApiResponse<FeedListResponse> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) FeedSort sort,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String jobCategory,
            @RequestParam(required = false) String career,
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "false") boolean deadlineSoon,
            @RequestParam(required = false) String keyword
    ) {
        FeedListResponse response = feedService.getFeed(
                principal.id(), page, size, sort, platform, jobCategory, career, region, deadlineSoon, keyword);
        return ApiResponse.success(response);
    }

    /**
     * 2.2 공고 상세 조회
     */
    @GetMapping("/{postingId}")
    public ApiResponse<FeedDetailResponse> getFeedDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postingId
    ) {
        FeedDetailResponse response = feedService.getFeedDetail(principal.id(), postingId);
        return ApiResponse.success(response);
    }

    /**
     * 2.3 스크랩 추가
     */
    @PostMapping("/{postingId}/scrap")
    public ApiResponse<ScrapAddResponse> addScrap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postingId
    ) {
        ScrapAddResponse response = feedService.addScrap(principal.id(), postingId);
        return ApiResponse.success(response);
    }

    /**
     * 2.4 스크랩 해제
     */
    @DeleteMapping("/scraps/{jobPostingId}")
    public ApiResponse<ScrapRemoveResponse> removeScrap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long jobPostingId
    ) {
        ScrapRemoveResponse response = feedService.removeScrap(principal.id(), jobPostingId);
        return ApiResponse.success(response);
    }

    /**
     * 2.5 스크랩 목록 조회
     */
    @GetMapping("/scraps")
    public ApiResponse<ScrapListResponse> getScraps(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        ScrapListResponse response = feedService.getScraps(principal.id(), page, size);
        return ApiResponse.success(response);
    }
}
