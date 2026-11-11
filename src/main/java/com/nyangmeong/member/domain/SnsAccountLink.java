package com.nyangmeong.member.domain;

import lombok.Data;

/**
 * [SNS 계정 연동 VO]
 * - SNS_ACNT_LINK 1:1 중 연동 판단에 쓰는 컬럼만
 * - 조회는 mapper/member/MemberMapper.xml
 */
@Data
public class SnsAccountLink {
    private Long snsAcntLinkSn;
    private Long mbrSn;
    private String snsOffrCd;
    private String useYn;
}
