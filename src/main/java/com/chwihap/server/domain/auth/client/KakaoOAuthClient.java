package com.chwihap.server.domain.auth.client;

import com.chwihap.server.domain.auth.client.dto.KakaoTokenResponse;
import com.chwihap.server.domain.auth.client.dto.KakaoUserInfoResponse;
import com.chwihap.server.global.exception.BusinessException;
import com.chwihap.server.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final Set<String> allowedRedirectUris;

    public KakaoOAuthClient(
            @Value("${app.kakao.client-id}") String clientId,
            @Value("${app.kakao.client-secret}") String clientSecret,
            @Value("${app.kakao.allowed-redirect-uris}") String allowedRedirectUris
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.allowedRedirectUris = Arrays.stream(allowedRedirectUris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /**
     * 인가 코드로 카카오 액세스 토큰을 발급받고, 이를 이용해 사용자 정보를 조회한다.
     * redirectUri는 인가 코드 발급 시 프론트가 실제로 사용한 값과 일치해야 하며,
     * 화이트리스트(app.kakao.allowed-redirect-uris)에 등록된 값만 허용한다.
     */
    public KakaoUserInfoResponse getUserInfo(String authorizationCode, String redirectUri) {
        if (!allowedRedirectUris.contains(redirectUri)) {
            throw new BusinessException(ErrorCode.INVALID_KAKAO_REDIRECT_URI);
        }
        String kakaoAccessToken = requestAccessToken(authorizationCode, redirectUri);
        return requestUserInfo(kakaoAccessToken);
    }

    private String requestAccessToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);
        if (StringUtils.hasText(clientSecret)) {
            body.add("client_secret", clientSecret);
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
            }
            return response.accessToken();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("Invalid kakao authorization code: {}", e.getResponseBodyAsString());
                throw new BusinessException(ErrorCode.INVALID_KAKAO_CODE);
            }
            log.error("Kakao token request failed: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
        } catch (RestClientException e) {
            log.error("Kakao token request failed", e);
            throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
        }
    }

    private KakaoUserInfoResponse requestUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
            }
            return response;
        } catch (RestClientException e) {
            log.error("Kakao user info request failed", e);
            throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
        }
    }
}
