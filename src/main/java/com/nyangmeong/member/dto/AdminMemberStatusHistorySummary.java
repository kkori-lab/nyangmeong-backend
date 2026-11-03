package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 회원 상세 상태 이력 한 줄]
 * - SCR-ADM-011, 배치 처리는 prcsMbrSn·prcsMbrNcnm 이 null
 */
@Data
public class AdminMemberStatusHistorySummary {
    /** 회원상태이력일련번호 */
    private Long mbrSttsHstrySn;
    /** 변경전상태공통코드 */
    private String bfrSttsCd;
    /** 변경후상태공통코드 */
    private String aftrSttsCd;
    /** 변경사유 */
    private String chgRsn;
    /** 처리회원일련번호 */
    private Long prcsMbrSn;
    /** 처리회원닉네임 */
    private String prcsMbrNcnm;
    /** 등록일시 */
    private LocalDateTime regDt;
}
