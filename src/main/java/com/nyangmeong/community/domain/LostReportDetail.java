package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * [실종제보 상세 조회 결과]
 * - LOST_TPOF 에 게시글·작성자·품종·지역을 조인한 서비스 내부용, 응답으로 그대로 반환하지 않는다
 */
@Getter
@Setter
public class LostReportDetail extends LostReport {
    private Long mbrSn;
    private String mbrNcnm;
    private String pstSttsCd;
    private String pstTtl;
    private String pstCn;
    private Integer inqCnt;
    private Integer rcmdtnCnt;
    private LocalDateTime pstRegDt;
    private String vrtyNm;
    private String rgnNm;
}
