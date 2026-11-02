package com.nyangmeong.community.dto;

import java.time.LocalDate;

import com.nyangmeong.community.domain.ReportTargetType;

import lombok.Data;

/**
 * [관리자 신고 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminReportSearchCond {
    /** 신고처리상태공통코드 */
    private String status;
    /** 신고사유공통코드 */
    private String dclrRsnCd;
    /** 대상 종류 */
    private ReportTargetType targetType;
    /** 자동 블라인드·자동 삭제 여부 */
    private Boolean autoBlindYn;
    /** 접수 시작일자 */
    private LocalDate bgngYmd;
    /** 접수 종료일자 */
    private LocalDate endYmd;
}
