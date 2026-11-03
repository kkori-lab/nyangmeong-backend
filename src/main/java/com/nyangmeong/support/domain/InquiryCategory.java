package com.nyangmeong.support.domain;

import java.util.Arrays;
import java.util.List;

/**
 * [문의 내부 분류]
 * - 관리자 1:1 문의 목록·필터의 분쟁·오류·기타 (FN-ADM-042-01·02)
 * - 공통코드 컬럼이 없어 문의 유형별로 고정 : CSCG02 설명의 "내부 분류"와 같게 유지
 */
public enum InquiryCategory {
    DISPUTE(List.of(InquiryType.COMMUNITY, InquiryType.POINT_PAYMENT_LEDGER)),
    ERROR(List.of(InquiryType.HEALTH_RECORD, InquiryType.PIXEL_CHARACTER)),
    ETC(List.of(InquiryType.PLACE, InquiryType.ACCOUNT_LOGIN, InquiryType.ETC));

    private final List<InquiryType> types;

    InquiryCategory(List<InquiryType> types) {
        this.types = types;
    }

    /**
     * 분류에 속한 문의 유형 코드 목록
     */
    public List<String> typeCodes() {
        return types.stream().map(InquiryType::getCode).toList();
    }

    /**
     * 문의 유형 코드의 분류 (모르는 코드는 기타)
     */
    public static InquiryCategory of(String inqryTypeCd) {
        return Arrays.stream(values())
                .filter(category -> category.types.stream().anyMatch(type -> type.is(inqryTypeCd)))
                .findFirst()
                .orElse(ETC);
    }

    /**
     * 이름으로 분류 찾기 (없으면 null)
     */
    public static InquiryCategory from(String name) {
        return Arrays.stream(values()).filter(category -> category.name().equals(name)).findFirst().orElse(null);
    }
}
