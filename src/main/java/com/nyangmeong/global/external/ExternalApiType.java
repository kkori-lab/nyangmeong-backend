package com.nyangmeong.global.external;

import java.util.Arrays;
import java.util.List;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [외부 API 구분]
 * - SYSG05 (EXTL_API_CALL_LOG.EXTL_API_SE_CD)
 * - 기능명세서 FN-ADM-112-01 의 7종. 장소 공공데이터(SYSC0019)는 화면 대상이 아니라 제외
 */
public enum ExternalApiType {
    AI_CHAT("SYSC0015", "AI 대화", true),
    AI_CLASSIFICATION("SYSC0024", "AI 분류", true),
    OCR("SYSC0016", "OCR", false),
    CHARACTER("SYSC0017", "캐릭터 생성", false),
    PDF_ANALYSIS("SYSC0025", "PDF 분석", false),
    KAKAO_LOCAL("SYSC0018", "카카오 로컬", false),
    MAIL("SYSC0020", "메일", false);

    private final String code;
    private final String label;
    private final boolean ai;

    ExternalApiType(String code, String label, boolean ai) {
        this.code = code;
        this.label = label;
        this.ai = ai;
    }

    /**
     * 코드로 외부 API 구분 조회
     */
    public static ExternalApiType from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }

    /**
     * 코드의 화면 이름, 7종 밖 코드는 코드 그대로
     */
    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .map(ExternalApiType::getLabel)
                .findFirst()
                .orElse(code);
    }

    /**
     * 토큰을 세는 AI 호출 코드 목록
     */
    public static List<String> aiCodes() {
        return Arrays.stream(values())
                .filter(type -> type.ai)
                .map(ExternalApiType::getCode)
                .toList();
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
