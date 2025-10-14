package com.samsamotot.otboo.oauth2.util;

import java.text.Normalizer;
import java.util.Objects;
import java.util.function.Predicate;

public final class KakaoEmailFactory {

    private static final String DOMAIN = "@kakao.com";
    private static final int MAX_LOCAL = 64;

    private KakaoEmailFactory() {}

    /**
     * 규칙: {nickname}_{kakaoId}@kakao.com
     * - 닉네임은 유니코드 그대로 보존(한글 OK)
     * - 공백류 → '_' 치환, 제어문자/개행 제거, '@' 제거
     * - 선두/후미의 '.' '_' 제거(미관/호환성)
     * - local-part ≤ 64 보장(닉네임만 코드포인트 단위로 절단, kakaoId는 보존)
     * - existsByEmail로 충돌 검사 → "-1", "-2" ... 접미사
     */
    public static String generate(String nickname, String kakaoId, Predicate<String> existsByEmail) {
        Objects.requireNonNull(kakaoId, "kakaoId must not be null");
        Objects.requireNonNull(existsByEmail, "existsByEmail must not be null");

        String nn = normalizeNickname(nickname);
        if (nn.isBlank()) nn = "user";

        // 1) 기본 로컬파트: {닉네임}_{id}
        String local = nn + "_" + kakaoId;

        // 2) 길이 제한(local <= 64): 닉네임만 절단, id 는 보존
        if (local.length() > MAX_LOCAL) {
            int keepForId = ("_" + kakaoId).length();
            int maxNickCodePoints = Math.max(1, MAX_LOCAL - keepForId);
            nn = safeTruncateByCodePoint(nn, maxNickCodePoints);
            local = nn + "_" + kakaoId;
        }

        // 3) 최종 후보
        String candidate = local + DOMAIN;

        // 4) 충돌 회피: 절단으로 동일해질 가능성 대비
        int seq = 1;
        while (existsByEmail.test(candidate)) {
            String withSeq = local;
            String suffix = "-" + seq;
            int maxLocalWithSeq = MAX_LOCAL - suffix.length();
            if (codePointLength(withSeq) > maxLocalWithSeq) {
                withSeq = safeTruncateByCodePoint(withSeq, maxLocalWithSeq);
            }
            candidate = withSeq + suffix + DOMAIN;
            seq++;
        }
        return candidate;
    }

    /** 닉네임 정리: 유니코드 보존, 공백→'_', 제어/개행 제거, '@' 제거, 선두/후미 '.' '_' 제거 */
    private static String normalizeNickname(String nickname) {
        if (nickname == null) return "";
        // 시각적으로 일관되게 (권장: NFKC)
        String s = Normalizer.normalize(nickname, Normalizer.Form.NFKC);
        // 제어문자/개행 제거
        s = s.replaceAll("\\p{Cntrl}+", "");
        // 공백류 → '_'
        s = s.replaceAll("\\s+", "_");
        // '@' 제거(로컬파트 구문 충돌 방지)
        s = s.replace("@", "");
        // 연속 '_' 축약
        s = s.replaceAll("_+", "_");
        // 앞뒤의 '.' '_' 제거
        s = s.replaceAll("^[._]+", "").replaceAll("[._]+$", "");
        // 빈 문자열 방지
        return s.isBlank() ? "user" : s;
    }

    /** 코드포인트 기준 길이 반환 */
    private static int codePointLength(String s) {
        return s.codePointCount(0, s.length());
    }

    /** 코드포인트 기준 안전 절단(서로게이트 분할 방지) */
    private static String safeTruncateByCodePoint(String s, int maxCodePoints) {
        int cps = codePointLength(s);
        if (cps <= maxCodePoints) return s;
        int endIndex = s.offsetByCodePoints(0, maxCodePoints);
        return s.substring(0, endIndex);
    }
}
