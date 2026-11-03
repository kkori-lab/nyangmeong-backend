package com.nyangmeong.community.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [펫돌봄 모집 검색 조건]
 * - 모두 선택, 주면 AND (FN-CMU-004-02)
 * - rcrtSttsCd : 없으면 전체, 모집중이면 PCAC0001
 * - rgnSn 은 그 지역과 하위 지역 전체
 * - prtcBgngDe ~ prtcEndDe : 돌봄 일정이 이 기간과 겹치는 글, 일정이 없는 글은 빠진다
 * - petSizeCd : SMALL·MEDIUM·LARGE, 강아지가 포함된 글의 크기 필터. 고양이만 고르면 무시
 * - mineYn : true 면 내 글만
 */
@Data
public class CareRecruitmentSearchCond {
    private String rcrtSttsCd;
    private boolean mineYn;
    private Long rgnSn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate prtcBgngDe;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate prtcEndDe;
    private String petSeCd;
    private String petSizeCd;
}
