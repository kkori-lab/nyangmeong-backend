package com.nyangmeong.place.domain;

import java.util.Arrays;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [장소 카테고리]
 * - PLCG01 (PLC.PLC_CLSF_CD), 카카오 검색에 붙이는 단어
 * - 동반을 뺀 카테고리는 카카오 카테고리 이름에 "반려동물"이 든 업체만 남김
 */
public enum PlaceCategory {
    HOSPITAL("PLCC0001", "동물병원", true),
    GROOMING("PLCC0002", "애견미용", true),
    SUPPLIES("PLCC0003", "애견용품", true),
    COMPANION("PLCC0004", "애견동반", false),
    HOTEL("PLCC0005", "애견호텔", true);

    private static final String PET_CATEGORY_WORD = "반려동물";

    private final String code;
    private final String searchWord;
    private final boolean petCategoryOnly;

    PlaceCategory(String code, String searchWord, boolean petCategoryOnly) {
        this.code = code;
        this.searchWord = searchWord;
        this.petCategoryOnly = petCategoryOnly;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 카테고리 찾기 : 없는 값이면 400
     */
    public static PlaceCategory from(String code) {
        return Arrays.stream(values())
                .filter(category -> category.is(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }

    /**
     * 카카오 검색어 : 검색어가 없으면 카테고리 단어, 이미 들어 있으면 그대로
     */
    public String toQuery(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return searchWord;
        }
        String trimmed = keyword.trim();
        return trimmed.contains(searchWord) ? trimmed : trimmed + " " + searchWord;
    }

    /**
     * 카카오 카테고리 이름이 이 카테고리에 맞는지
     */
    public boolean matches(String categoryName) {
        return !petCategoryOnly || (categoryName != null && categoryName.contains(PET_CATEGORY_WORD));
    }
}
