package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [건강기록 대시보드 기록 있는 항목 요약]
 * - 반려동물 1마리의 항목별 기록 여부. 알레르기·기저질환은 PET 컬럼이라 이 쿼리 밖에서 판정
 */
@Data
public class HealthRecordedItemSummary {

    /** 반려동물일련번호 */
    private Long petSn;
    /** 체중 기록 있음 */
    private boolean weight;
    /** 식사량 기록 있음 */
    private boolean meal;
    /** 음수량 기록 있음 */
    private boolean water;
    /** 산책 기록 있음 */
    private boolean walk;
    /** 배변 상태 기록 있음 */
    private boolean poop;
    /** 검진기록 있음 */
    private boolean ckup;
    /** 검사 수치 있음 */
    private boolean insp;
    /** 처방 있음 */
    private boolean presc;
    /** 첨부 자료 있음 */
    private boolean atch;
    /** 지속 관찰 중인 항목 있음 */
    private boolean obsv;
    /** 예방접종·정기검진 기록 있음 */
    private boolean vacc;
    /** 일상관리 케어 항목 있음 */
    private boolean care;
}
