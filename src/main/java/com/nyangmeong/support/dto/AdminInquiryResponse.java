package com.nyangmeong.support.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.support.domain.Inquiry;

/**
 * [관리자 1:1 문의 상세 응답]
 * - GET /api/admin/inquiries/{otoInqrySn}, 답변 등록·수정, 캐릭터 오류 처리 (FN-ADM-042-03~08)
 * - 첨부 이미지 : GET /api/admin/inquiries/{otoInqrySn}/images/{atchFileSn}
 */
public record AdminInquiryResponse(
        Long otoInqrySn,
        String inqryTypeCd,
        String inqryTypeCdNm,
        String inqryCtgr,
        String inqryPrcsSttsCd,
        String inqryPrcsSttsCdNm,
        String inqryTtl,
        String inqryCn,
        Long mbrSn,
        String mbrNcnm,
        Long prcsMbrSn,
        String prcsMbrNcnm,
        String ansCn,
        LocalDateTime ansDt,
        Long pixelCharSn,
        String charErrPrcsRsltCd,
        String charErrPrcsRsltCdNm,
        Long charImgAtchFileSn,
        boolean delYn,
        LocalDateTime regDt,
        List<InquiryImageSummary> imgs,
        List<AdminInquiryHistorySummary> hstrys) {

    /** 코드 이름·닉네임·첨부·이력 */
    public record Extra(String inqryTypeCdNm, String inqryCtgr, String inqryPrcsSttsCdNm, String mbrNcnm,
            String prcsMbrNcnm, String charErrPrcsRsltCdNm, Long charImgAtchFileSn, List<InquiryImageSummary> imgs,
            List<AdminInquiryHistorySummary> hstrys) {
    }

    public static AdminInquiryResponse from(Inquiry inquiry, Extra extra) {
        return new AdminInquiryResponse(inquiry.getOtoInqrySn(), inquiry.getInqryTypeCd(), extra.inqryTypeCdNm(),
                extra.inqryCtgr(), inquiry.getInqryPrcsSttsCd(), extra.inqryPrcsSttsCdNm(), inquiry.getInqryTtl(),
                inquiry.getInqryCn(), inquiry.getMbrSn(), extra.mbrNcnm(), inquiry.getPrcsMbrSn(),
                extra.prcsMbrNcnm(), inquiry.getAnsCn(), inquiry.getAnsDt(), inquiry.getPixelCharSn(),
                inquiry.getCharErrPrcsRsltCd(), extra.charErrPrcsRsltCdNm(), extra.charImgAtchFileSn(),
                "Y".equals(inquiry.getDelYn()), inquiry.getRegDt(), extra.imgs(), extra.hstrys());
    }
}
