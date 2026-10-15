package com.chwihap.server.global.util;

import org.springframework.web.util.HtmlUtils;

/**
 * 외부 API에서 수집한 텍스트에 섞여 들어온 HTML 엔티티(&amp;apos; 등)를 원래 문자로 되돌린다.
 */
public final class HtmlEntityUtils {

    /**
     * 원천 데이터가 이중으로 escape된 경우(&amp;amp;apos;)까지 풀기 위한 최대 디코딩 횟수.
     */
    private static final int MAX_DECODE_COUNT = 2;

    /**
     * HtmlUtils는 HTML 4.01 엔티티만 지원해 XML/HTML5 엔티티인 &amp;apos;는 풀지 않으므로 별도로 치환한다.
     */
    private static final String APOS_ENTITY = "&apos;";

    private HtmlEntityUtils() {
    }

    /**
     * HTML 엔티티를 디코딩합니다. 값이 더 이상 바뀌지 않거나 최대 횟수에 도달하면 멈춥니다.
     * 디코딩할 수 없는 엔티티가 있으면 그 단계 직전까지 디코딩된 값을 반환합니다.
     *
     * @param value 디코딩할 문자열
     * @return 디코딩된 문자열, 입력이 null이면 null
     */
    public static String decode(String value) {
        if (value == null) {
            return null;
        }
        String decoded = value;
        for (int i = 0; i < MAX_DECODE_COUNT; i++) {
            String next;
            try {
                next = HtmlUtils.htmlUnescape(decoded.replace(APOS_ENTITY, "'"));
            } catch (IllegalArgumentException e) {
                // &#-1; 처럼 유효하지 않은 코드 포인트는 HtmlUtils가 예외를 던진다. 수집 배치 전체가 중단되지 않도록 여기서 멈춘다.
                break;
            }
            if (next.equals(decoded)) {
                break;
            }
            decoded = next;
        }
        return decoded;
    }
}
