package com.nyangmeong.global.log;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 작업 로그 목록 응답]
 */
@Data
public class AdminTaskLogSummary {
    /** 관리자작업로그일련번호 */
    private Long mngrTaskLogSn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 작업유형공통코드 */
    private String taskTypeCd;
    /** 대상테이블명 */
    private String trgtTblNm;
    /** 대상일련번호 */
    private Long trgtSn;
    /** 작업요약내용 */
    private String taskSmryCn;
}
