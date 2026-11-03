package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.CareRecruitmentDetail;

/**
 * [펫돌봄 모집 응답]
 * - 상세·등록·수정 공통, 상세 주소·정확한 좌표는 담지 않는다(FN-CMU-004-10)
 * - aplcntCnt : 「지금 n명과 채팅 중」, 작성자에게만 준다
 */
public record CareRecruitmentResponse(
        Long petPrtcRcrtSn,
        Long pstSn,
        String pstTtl,
        String pstCn,
        Long mbrSn,
        String mbrNcnm,
        Long mbrAtchFileSn,
        LocalDateTime regDt,
        Integer inqCnt,
        String rcrtSttsCd,
        List<CareRecruitmentPetItem> pets,
        List<String> prtcShpCds,
        LocalDateTime prtcBgngDt,
        LocalDateTime prtcEndDt,
        boolean schdlCnsltnPsbltyYn,
        String cstUnitCd,
        Integer cstAmt,
        Long rgnSn,
        String rlsRgnNm,
        List<PostImageSummary> imgs,
        Integer rcmdtnCnt,
        Integer aplcntCnt,
        Boolean liked,
        Boolean owner) {

    public static CareRecruitmentResponse of(CareRecruitmentDetail detail, List<CareRecruitmentPetItem> pets,
            List<String> prtcShpCds, List<PostImageSummary> imgs, boolean liked, boolean owner) {
        return new CareRecruitmentResponse(detail.getPetPrtcRcrtSn(), detail.getPstSn(), detail.getPstTtl(),
                detail.getPstCn(), detail.getMbrSn(), detail.getMbrNcnm(), detail.getMbrAtchFileSn(),
                detail.getPstRegDt(), detail.getInqCnt(), detail.getRcrtSttsCd(), pets, prtcShpCds,
                detail.getPrtcBgngDt(), detail.getPrtcEndDt(), "Y".equals(detail.getSchdlCnsltnPsbltyYn()),
                detail.getCstUnitCd(), detail.getCstAmt(), detail.getRgnSn(), detail.getRlsRgnNm(), imgs,
                detail.getRcmdtnCnt(), owner ? detail.getAplcntCnt() : null, liked, owner);
    }
}
