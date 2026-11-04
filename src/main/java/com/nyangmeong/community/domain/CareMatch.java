package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [펫돌봄 매칭 VO]
 * - PET_PRTC_MTCH 1:1
 */
@Data
public class CareMatch {
    private Long petPrtcMtchSn;
    private Long petPrtcRcrtSn;
    private Long otoChatRoomSn;
    private String mtchDlngSttsCd;
    private String idmptDmndId;
    private LocalDateTime rcvIdntyDt;
    private String rcvYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
