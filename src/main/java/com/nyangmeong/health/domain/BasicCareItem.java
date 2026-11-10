package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [서비스 기본 케어 항목]
 * - 일상관리 5개, 정기관리 6개 (FN-HLT-003-12) : 선언 순서가 생성 순서
 */
public enum BasicCareItem {
    TOILET_CLEANING("화장실 청소", CareZone.DAILY, null, "cleaning_services"),
    TOOTH_BRUSHING("양치", CareZone.DAILY, null, "dentistry"),
    BRUSHING("빗질", CareZone.DAILY, null, "brush"),
    PAW_CARE("발 관리", CareZone.DAILY, null, "task_alt"),
    PLAY("놀이", CareZone.DAILY, null, "task_alt"),
    EAR_CLEANING("귀 청소", CareZone.REGULAR, CareCycle.WEEK_1, "hearing"),
    NAIL_CARE("발톱 관리", CareZone.REGULAR, CareCycle.WEEK_2, "content_cut"),
    BATH("목욕", CareZone.REGULAR, CareCycle.MONTH_1, "bathtub"),
    EYE_CARE("눈 주변 관리", CareZone.REGULAR, CareCycle.WEEK_2, "task_alt"),
    PAW_HAIR_CARE("발바닥 털 관리", CareZone.REGULAR, CareCycle.WEEK_2, "content_cut"),
    MOUTH_CHECK("구강 상태 확인", CareZone.REGULAR, CareCycle.WEEK_2, "sentiment_satisfied");

    private final String careArtclNm;
    private final CareZone zone;
    private final CareCycle cycle;
    private final String rprsIconId;

    BasicCareItem(String careArtclNm, CareZone zone, CareCycle cycle, String rprsIconId) {
        this.careArtclNm = careArtclNm;
        this.zone = zone;
        this.cycle = cycle;
        this.rprsIconId = rprsIconId;
    }

    public String getCareArtclNm() {
        return careArtclNm;
    }

    public CareZone getZone() {
        return zone;
    }

    public CareCycle getCycle() {
        return cycle;
    }

    public String getRprsIconId() {
        return rprsIconId;
    }

    /**
     * 항목명으로 기본 항목 조회
     */
    public static Optional<BasicCareItem> of(String careArtclNm) {
        return Arrays.stream(values()).filter(item -> item.careArtclNm.equals(careArtclNm)).findFirst();
    }
}
