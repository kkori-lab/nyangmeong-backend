package com.nyangmeong.pet.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [관리자 캐릭터 생성 현황 검색 조건]
 * - SCR-ADM-082, 조건은 모두 선택. 기간은 요청일(REG_DT) 양 끝 날짜 포함
 * - 상태 건수(sttsCnts)는 기간만, 목록은 상태까지 건다
 */
@Data
public class AdminCharacterJobSearchCond {
    /** 시작일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromYmd;
    /** 종료일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toYmd;
    /** 생성작업상태공통코드 (CHRG01) */
    private String crtJobSttsCd;
}
