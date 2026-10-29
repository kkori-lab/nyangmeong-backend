package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [서비스 기본 케어 항목]
 * - 일상관리 5개, 정기관리 6개 (FN-HLT-003-12) : 선언 순서가 생성 순서
 * - mainSortSeq : 메인 표시 기본값(양치·빗질·발톱 관리), 0이면 기본 표시 아님
 */
public enum BasicCareItem {
    TOILET_CLEANING("화장실 청소", CareZone.DAILY, null, "cleaning_services", 0),
    TOOTH_BRUSHING("양치", CareZone.DAILY, null, "dentistry", 1),
    BRUSHING("빗질", CareZone.DAILY, null, "brush", 2),
    PAW_CARE("발 관리", CareZone.DAILY, null, "task_alt", 0),
    PLAY("놀이", CareZone.DAILY, null, "task_alt", 0),
    EAR_CLEANING("귀 청소", CareZone.REGULAR, CareCycle.WEEK_1, "hearing", 0),
    NAIL_CARE("발톱 관리", CareZone.REGULAR, CareCycle.WEEK_2, "content_cut", 3),
    BATH("목욕", CareZone.REGULAR, CareCycle.MONTH_1, "bathtub", 0),
    EYE_CARE("눈 주변 관리", CareZone.REGULAR, CareCycle.WEEK_2, "task_alt", 0),
    PAW_HAIR_CARE("발바닥 털 관리", CareZone.REGULAR, CareCycle.WEEK_2, "content_cut", 0),
    MOUTH_CHECK("구강 상태 확인", CareZone.REGULAR, CareCycle.WEEK_2, "sentiment_satisfied", 0);

    private final String careArtclNm;
    private final CareZone zone;
    private final CareCycle cycle;
    private final String rprsIconId;
    private final int mainSortSeq;

    BasicCareItem(String careArtclNm, CareZone zone, CareCycle cycle, String rprsIconId, int mainSortSeq) {
        this.careArtclNm = careArtclNm;
        this.zone = zone;
        this.cycle = cycle;
        this.rprsIconId = rprsIconId;
        this.mainSortSeq = mainSortSeq;
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

    public int getMainSortSeq() {
        return mainSortSeq;
    }

    /**
     * 항목명으로 기본 항목 조회
     */
    public static Optional<BasicCareItem> of(String careArtclNm) {
        return Arrays.stream(values()).filter(item -> item.careArtclNm.equals(careArtclNm)).findFirst();
    }
}
