package com.samsamotot.otboo.recommendation.prompt;

import com.samsamotot.otboo.clothes.dto.OotdDto;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public static final String SYSTEM_PROMPT = """
        너는 한국어로만 응답하는 전문 스타일리스트다.
        규칙:
        - 입력으로 주어진 후보 아이템/속성 외의 내용을 새로 만들어내지 말 것.
        - 브랜드명/상품명/매장명/가격(숫자+통화) 언급 금지, 고유명사 지양.
        - 출력은 오직 한 문장(자연스러운 한국어)이어야 하며, 다른 텍스트를 덧붙이지 말 것.
        - 이모지는 0~2개만 사용하고 문장 앞/뒤에만 배치(연속 사용 금지).
        톤:
        - 밝고 친근한 '해요체', 권유형(~해보세요/좋아요), 과장/감탄사(!) 금지.
        - 딱딱한 표현(적합한/조합/제공/권장)은 피하고 일상어(매치/어울려요/가볍게/산뜻하게) 사용.
        안전:
        - 민감한 내용/주관적 판단을 피하고, 사실에 근거해 간결하게 답할 것.
        """;

    private final int maxLen;

    public PromptBuilder(@Value("${llm.reason.max-length:60}") int maxLen) {
        this.maxLen = Math.max(20, maxLen);
    }

    /**
     * 사용자 컨텍스트를 user 메시지로 구성합니다.
     *
     * @param temperature 실제 기온(예: 18.2)
     * @param isRainingOrSnowing 비/눈 여부
     * @param currentMonth 현재 월(예: 3월)
     * @param feelsLike 보정된 체감온도(예: 17.0)
     * @param sensitivity 사용자 온도 민감도(0~5)
     * @param recommendedItems 추천된 아이템 목록
     * @return user 메시지
     */
    public String buildOneLiner(
        Double temperature,
        boolean isRainingOrSnowing,
        Month currentMonth,
        Double feelsLike,
        Double sensitivity,
        List<OotdDto> recommendedItems
    ) {
        String t = temperature == null ? "알 수 없음" : String.format("%.1f℃", temperature);
        String w = isRainingOrSnowing ? "비/눈" : "맑음";
        String m = currentMonth == null ? "알 수 없음" : currentMonth.toString();
        String f = feelsLike == null ? "알 수 없음" : String.format("%.1f℃", feelsLike);
        String s = sensitivity == null ? "알 수 없음" : String.format("%.1f (0~5)", sensitivity);
        String items = (recommendedItems == null || recommendedItems.isEmpty())
            ? "없음"
            : recommendedItems.stream()
                .map(OotdDto::toSummaryStringWithDefs)
                .collect(Collectors.joining(", "));

        return String.join("\n",
            "[CONTEXT]",
            "- 현재 월: " + m,
            "- 날씨: " + w,
            "- 기온: " + t,
            "- 체감온도: " + f,
            "- 사용자 온도 민감도: " + s,
            "- 추천 아이템: " + items,
            "",
            "[RULES]",
            "- 결과는 한국어 정확히 1문장.",
            "- 길이: " + maxLen + "자 이내.",
            "- 브랜드/상품/매장/가격 언급 금지, 고유명사 지양.",
            "- 줄바꿈/리스트/따옴표/마크다운/코드블록 금지.",
            "- 어투: 밝고 친근한 해요체, 권유형(~해보세요/좋아요).",
            "- 이모지 0~2개 허용(날씨/의상/무드 관련), 문장 앞/뒤에만 배치(연속 사용 금지).",
            "- 끝맺음: 마지막 문자는 마침표(.) 또는 이모지 하나 중 하나로만 종료.",
            "",
            "[TASK]",
            "아이템 속성(예: 방수·보온·통기성·핏·색감)에 근거해, 일상적인 어휘로 친근하게 권유하는 한 문장을 작성하세요."
        );
    }

    /** 후처리: 개행/따옴표 정리, 길이 보정, 마침표 하나로 종결 */
    private String truncateByCodePoints(String s, int max) {
        int cps = s.codePointCount(0, s.length());
        if (cps <= max) return s;
        int end = s.offsetByCodePoints(0, max);
        return s.substring(0, end);
    }

    public String postProcessOneLiner(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String s = raw.replace("\n"," ").replace("\"","").replace("“","").replace("”","")
            .replaceAll("\\s{2,}", " ").trim();
        // 여러 문장 방지: 첫 문장만
        s = s.replaceAll("\\s*([.!?。！？])\\s*", "$1");
        String first = s.split("[.!?。！？]")[0].trim();
        s = first;
        // 코드포인트 기준 길이 제한
        s = truncateByCodePoints(s, maxLen).trim();
        // 끝문자 처리: 이모지면 유지, 아니면 마침표 보장
        if (!endsWithEmoji(s)) {
            s = s.replaceAll("[.!?。！？]+$", "").trim() + ".";
        }
        return s;
    }

    private static boolean endsWithEmoji(String s) {
        if (s == null || s.isEmpty()) return false;
        int cp = s.codePointBefore(s.length());
        return isEmoji(cp);
    }
    private static boolean isEmoji(int cp) {
        // 주요 이모지 범위(대략): Symbols & Pictographs, Dingbats 등
        return (cp >= 0x1F300 && cp <= 0x1FAFF)   // Misc Symbols & Pictographs ~ Supplemental Symbols
            || (cp >= 0x2600 && cp <= 0x26FF)     // Misc symbols (☀️☔️ 등)
            || (cp >= 0x2700 && cp <= 0x27BF);    // Dingbats
    }

    public String fallbackOneLiner() {
        return "오늘 날씨에 맞는 옷을 추천해드릴게요.";
    }
}
