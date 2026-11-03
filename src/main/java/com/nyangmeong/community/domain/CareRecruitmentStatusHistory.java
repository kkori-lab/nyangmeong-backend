package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [펫돌봄 모집 상태 이력 VO]
 * - PET_PRTC_RCRT_STTS_HSTRY 1:1
 */
@Data
public class CareRecruitmentStatusHistory {
    private Long petPrtcRcrtSttsHstrySn;
    private Long petPrtcRcrtSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
