package com.nyangmeong.global.log;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 작업 로그 VO]
 * - MNGR_TASK_LOG 1:1
 */
@Data
public class AdminTaskLog {
    /** 관리자작업로그일련번호 */
    private Long mngrTaskLogSn;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 작업유형공통코드 */
    private String taskTypeCd;
    /** 대상테이블명 */
    private String trgtTblNm;
    /** 대상일련번호 */
    private Long trgtSn;
    /** 작업요약내용 */
    private String taskSmryCn;
    /** 변경전내용 */
    private String chgBfrCn;
    /** 변경후내용 */
    private String chgAftrCn;
    /** 접속IP주소 */
    private String cntnIpAddr;
    /** 등록일시 */
    private LocalDateTime regDt;
}
