package com.nyangmeong.health.domain;

/**
 * [검사 항목 코드]
 * - HLTG12 (INSP_NVL.INSP_ARTCL_CD) : 직접입력은 항목명을 따로 받음
 */
public enum InspectionArticle {
    WBC("HLTC0024"),
    RBC("HLTC0025"),
    HGB("HLTC0026"),
    HCT("HLTC0027"),
    PLT("HLTC0028"),
    BUN("HLTC0029"),
    CREA("HLTC0030"),
    ALT("HLTC0031"),
    AST("HLTC0032"),
    ALP("HLTC0033"),
    GLU("HLTC0034"),
    SDMA("HLTC0078"),
    DIRECT("HLTC0035");

    private final String code;

    InspectionArticle(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
