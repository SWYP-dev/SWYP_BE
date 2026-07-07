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
            @RequestParam(required = false) String cursor,
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
                principal.id(), cursor, size, sort, platform, jobCategory, career, region, deadlineSoon, keyword);
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
     * 2.3 즐겨찾기 추가
     */
    @PostMapping("/{postingId}/favorite")
    public ApiResponse<FavoriteAddResponse> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postingId
    ) {
        FavoriteAddResponse response = feedService.addFavorite(principal.id(), postingId);
        return ApiResponse.success(response);
    }

    /**
     * 2.4 즐겨찾기 해제
     */
    @DeleteMapping("/favorites/{jobPostingId}")
    public ApiResponse<FavoriteRemoveResponse> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long jobPostingId
    ) {
        FavoriteRemoveResponse response = feedService.removeFavorite(principal.id(), jobPostingId);
        return ApiResponse.success(response);
    }

    /**
     * 2.5 즐겨찾기 목록 조회
     */
    @GetMapping("/favorites")
    public ApiResponse<FavoriteListResponse> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        FavoriteListResponse response = feedService.getFavorites(principal.id(), cursor, size);
        return ApiResponse.success(response);
    }
}
