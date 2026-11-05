package com.nyangmeong.ai.domain;

import java.util.Arrays;
import java.util.Set;

/**
 * [관리자가 편집하는 키워드 분류]
 * - 사전용도(AIMG10) + 키워드분류명 한 묶음 = 관리자 화면 탭 하나 (SCR-ADM-081)
 * - codeGroup이 있으면 결과가 코드인 분류 : 연결 코드(MPNG_CD)가 그 그룹의 코드여야 한다
 * - AIMC0037의 '판정 규칙'·'시간' 행은 서버 규칙이라 여기에 없다 (2026-10-30 결정)
 */
public enum AiKeywordClass {

    REGISTER_VERB(AiKeywordClass.SCHEDULE, "등록 동사", null),
    DATE(AiKeywordClass.SCHEDULE, "날짜", null),
    CYCLE(AiKeywordClass.SCHEDULE, "주기", null),
    SCHEDULE_TYPE(AiKeywordClass.SCHEDULE, "일정 종류", "HLTG19"),
    CHECKUP_TYPE(AiKeywordClass.SCHEDULE, "검진유형", "HLTG01"),
    EXPENSE_CATEGORY(AiKeywordClass.EXPENSE, "가계부 카테고리", "MYPG05");

    /** 일정 등록 판정 */
    public static final String SCHEDULE = "AIMC0037";
    /** 가계부 분류 */
    public static final String EXPENSE = "AIMC0038";

    private static final Set<String> USAGES = Set.of(SCHEDULE, EXPENSE);

    private final String dictUsgCd;
    private final String kwrdClsfNm;
    private final String codeGroup;

    AiKeywordClass(String dictUsgCd, String kwrdClsfNm, String codeGroup) {
        this.dictUsgCd = dictUsgCd;
        this.kwrdClsfNm = kwrdClsfNm;
        this.codeGroup = codeGroup;
    }

    public String getDictUsgCd() {
        return dictUsgCd;
    }

    public String getKwrdClsfNm() {
        return kwrdClsfNm;
    }

    public String getCodeGroup() {
        return codeGroup;
    }

    /**
     * 관리자 화면이 다루는 사전용도인지 (뉴스 용도는 아니다)
     */
    public static boolean isUsage(String dictUsgCd) {
        return dictUsgCd != null && USAGES.contains(dictUsgCd);
    }

    /**
     * 용도·분류명에 맞는 분류, 없으면 null
     */
    public static AiKeywordClass of(String dictUsgCd, String kwrdClsfNm) {
        return Arrays.stream(values())
                .filter(c -> c.dictUsgCd.equals(dictUsgCd) && c.kwrdClsfNm.equals(kwrdClsfNm))
                .findFirst()
                .orElse(null);
    }
}
