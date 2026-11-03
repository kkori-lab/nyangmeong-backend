package com.nyangmeong.place.domain;

import java.util.Arrays;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [장소 검색 정렬]
 * - RECOMMEND : 추천순(기본, 카카오 정확도순), DISTANCE : 거리순, NAME : 이름순 (FN-HOS-001-06)
 */
public enum PlaceSort {
    RECOMMEND("RECOMMEND"),
    DISTANCE("DISTANCE"),
    NAME("NAME");

    private final String code;

    PlaceSort(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 요청값으로 정렬 찾기 : 비우면 추천순, 없는 값이면 400
     */
    public static PlaceSort from(String code) {
        if (code == null || code.isBlank()) {
            return RECOMMEND;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.is(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }
}
