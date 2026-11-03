package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * [펫돌봄 모집 목록 항목]
 * - pets·prtcShpCds 는 목록 조회 뒤 서비스가 채운다
 * - aplcntCnt : 메시지가 1건 이상 오간 이 글의 채팅방 수(「채팅 n명」)
 */
@Data
public class CareRecruitmentSummary {
    private Long petPrtcRcrtSn;
    private Long pstSn;
    private String pstTtl;
    private String pstCn;
    private Long thumbnailAtchFileSn;
    private String mbrNcnm;
    private Long mbrAtchFileSn;
    private String rlsRgnNm;
    private String cstUnitCd;
    private Integer cstAmt;
    private Integer aplcntCnt;
    private String rcrtSttsCd;
    private LocalDateTime prtcBgngDt;
    private LocalDateTime prtcEndDt;
    private Boolean schdlCnsltnPsbltyYn;
    private LocalDateTime regDt;
    /** 내가 쓴 글인지 (프론트의 「내 글」 배지) */
    private Boolean owner;
    private List<CareRecruitmentPetItem> pets;
    private List<String> prtcShpCds;
}
