package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * [관리자 펫돌봄 매칭 상세 응답]
 * - 모집글 정보 + 매칭 목록 (최신순, 없으면 빈 목록)
 * - 채팅방 번호·채팅 메시지는 주지 않는다 (결정 14·26)
 */
@Data
public class AdminCareMatchResponse {
    private Long petPrtcRcrtSn;
    private Long pstSn;
    private String pstTtl;
    private String rcrtSttsCd;
    private Long mbrSn;
    private String mbrNcnm;
    private BigDecimal cstAmt;
    private String cstUnitCd;
    private LocalDateTime regDt;
    private List<Match> mtchs;

    /**
     * [매칭]
     * - ctptMbrSn·ctptNcnm : 돌봄 제공자(채팅방 상대방)
     */
    @Data
    public static class Match {
        private Long petPrtcMtchSn;
        private Long ctptMbrSn;
        private String ctptNcnm;
        private String mtchDlngSttsCd;
        private Boolean rcvYn;
        private LocalDateTime rcvIdntyDt;
        private LocalDateTime regDt;
        private List<ConditionVersion> cndVers;
        private List<StatusHistory> sttsHstry;
        private List<CompletionRequest> cmptnDmnds;
    }

    /**
     * [합의 조건 버전] 버전 내림차순
     */
    @Data
    public static class ConditionVersion {
        @JsonIgnore
        private Long petPrtcMtchSn;
        private Long cndVerNo;
        private String cstUnitCd;
        private BigDecimal acrdCstAmt;
        private String acrdPrtcCn;
        private LocalDateTime acrdPrtcBgngDt;
        private LocalDateTime acrdPrtcEndDt;
        private LocalDateTime regDt;
    }

    /**
     * [매칭 상태 이력] 오래된 순
     */
    @Data
    public static class StatusHistory {
        @JsonIgnore
        private Long petPrtcMtchSn;
        private String bfrSttsCd;
        private String aftrSttsCd;
        private Long cndVerNo;
        private String chgRsn;
        private LocalDateTime regDt;
    }

    /**
     * [돌봄 완료 요청 회차] 회차 순
     */
    @Data
    public static class CompletionRequest {
        @JsonIgnore
        private Long petPrtcMtchSn;
        @JsonIgnore
        private Long prtcCmptnDmndCyclSn;
        private Long dmndCycl;
        private String cmptnCn;
        private String spclMttrCn;
        private LocalDateTime prtcEndDt;
        private String prtcrIdntyRsltCd;
        private LocalDateTime prtcrIdntyDt;
        private String splmntDmndRsn;
        private LocalDateTime regDt;
        private List<Photo> imgs;
    }

    /**
     * [돌봄 완료 사진] 정렬 순서
     */
    @Data
    public static class Photo {
        @JsonIgnore
        private Long prtcCmptnDmndCyclSn;
        private Long atchFileSn;
        private Long sortSeq;
    }
}
