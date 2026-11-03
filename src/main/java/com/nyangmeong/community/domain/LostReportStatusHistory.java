package com.nyangmeong.community.domain;

import lombok.Data;

/**
 * [실종제보 상태 이력 VO]
 * - LOST_TPOF_STTS_HSTRY 1:1
 */
@Data
public class LostReportStatusHistory {
    private Long lostTpofSttsHstrySn;
    private Long lostTpofSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
}
