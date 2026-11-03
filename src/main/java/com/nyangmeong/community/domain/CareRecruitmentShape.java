package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [모집 돌봄 형태 VO]
 * - RCRT_PRTC_SHP 1:1
 */
@Data
public class CareRecruitmentShape {
    private Long rcrtPrtcShpSn;
    private Long petPrtcRcrtSn;
    private String prtcShpCd;
    private LocalDateTime regDt;
}
