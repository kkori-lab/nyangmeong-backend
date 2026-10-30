package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.LostReportDetail;

/**
 * [실종 제보 응답]
 * - 상세·등록·수정 공통
 * - 찾았어요 상태면 작성자가 아닌 회원에게 제목·상태·전환 시각만 주고 나머지는 null (FN-CMU-002-09)
 * - expryCrtrDt 는 작성자에게만 준다
 */
public record LostReportResponse(
        Long lostTpofSn,
        Long pstSn,
        String pstTtl,
        String lostTpofSttsCd,
        LocalDateTime dscvSwtcDt,
        String mbrNcnm,
        LocalDateTime regDt,
        String pstCn,
        String petSeCd,
        String petNm,
        Long vrtySn,
        String vrtyNm,
        String colrNm,
        String chrcCn,
        LocalDateTime lastWtnsDt,
        Long rgnSn,
        String rgnNm,
        String wtnsPlcNm,
        BigDecimal wtnsPlcLat,
        BigDecimal wtnsPlcLot,
        String cnpl,
        LocalDateTime expryCrtrDt,
        List<PostImageSummary> imgs,
        Integer inqCnt,
        Integer rcmdtnCnt,
        Boolean liked,
        Boolean owner) {

    /**
     * 전체 공개 응답 (품종명은 마스터명, 없으면 직접입력명)
     */
    public static LostReportResponse of(LostReportDetail detail, String cnpl, List<PostImageSummary> imgs,
            boolean liked, boolean owner) {
        String vrtyNm = detail.getVrtySn() != null ? detail.getVrtyNm() : detail.getVrtyDrctInptNm();
        return new LostReportResponse(detail.getLostTpofSn(), detail.getPstSn(), detail.getPstTtl(),
                detail.getLostTpofSttsCd(), detail.getDscvSwtcDt(), detail.getMbrNcnm(), detail.getPstRegDt(),
                detail.getPstCn(), detail.getPetSeCd(), detail.getPetNm(), detail.getVrtySn(), vrtyNm,
                detail.getColrNm(), detail.getChrcCn(), detail.getLastWtnsDt(), detail.getRgnSn(),
                detail.getRgnNm(), detail.getWtnsPlcNm(), detail.getWtnsPlcLat(), detail.getWtnsPlcLot(), cnpl,
                owner ? detail.getExpryCrtrDt() : null, imgs, detail.getInqCnt(), detail.getRcmdtnCnt(), liked,
                owner);
    }

    /**
     * 찾았어요 비공개 응답 (제목·상태·전환 시각만)
     */
    public static LostReportResponse closed(LostReportDetail detail) {
        return new LostReportResponse(detail.getLostTpofSn(), detail.getPstSn(), detail.getPstTtl(),
                detail.getLostTpofSttsCd(), detail.getDscvSwtcDt(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, false, false);
    }
}
