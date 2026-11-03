package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.util.List;

/**
 * [영수증 OCR 추출값]
 * - 읽지 못한 칸은 null, 목록은 빈 목록
 * - 반려동물 구역이 없는 영수증 -> sectionNm null 구역 1개
 * - expndPlcusNm : 사용처(영수증 상호), 앞 80자. 모든 출처에서 채운다 (FN-MYP-031-03)
 * - expndClsfCd : 추정 지출 카테고리 MYPG05(MYPC0012~0018), 불확실하면 null
 * - ckupYmd·recptAmt : 가계부 출처에서는 결제일·결제 금액
 */
public record ReceiptOcrResponse(String hsptlNm, LocalDate ckupYmd, Long recptAmt, Long btmDscntAmt,
        List<Section> sections, String expndPlcusNm, String expndClsfCd) {

    public ReceiptOcrResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    /**
     * 사용처·추정 카테고리 없는 추출값
     */
    public ReceiptOcrResponse(String hsptlNm, LocalDate ckupYmd, Long recptAmt, Long btmDscntAmt,
            List<Section> sections) {
        this(hsptlNm, ckupYmd, recptAmt, btmDscntAmt, sections, null, null);
    }

    /**
     * [반려동물 구역]
     * - dscntAmt : 구역 안에 찍힌 할인
     */
    public record Section(String sectionNm, Long sbttlAmt, Long dscntAmt, List<Line> items, List<Drug> drugs) {

        public Section {
            items = items == null ? List.of() : List.copyOf(items);
            drugs = drugs == null ? List.of() : List.copyOf(drugs);
        }
    }

    /**
     * [진료 품목]
     */
    public record Line(String itemNm, Long itemAmt) {
    }

    /**
     * [처방 약품]
     * - mdcnUsage : 용법 원문 (예: 1일 2회)
     */
    public record Drug(String mdcnNm, Integer mdcnDays, String mdcnUsage, Long mdcnAmt) {
    }
}
