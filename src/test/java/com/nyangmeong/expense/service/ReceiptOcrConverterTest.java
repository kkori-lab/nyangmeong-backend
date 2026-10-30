package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Drug;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;

/**
 * [검사 - 영수증 OCR 결과 판정]
 */
class ReceiptOcrConverterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private Section section(String sectionNm, Long sbttlAmt, List<Line> items, List<Drug> drugs) {
        return new Section(sectionNm, sbttlAmt, null, items, drugs);
    }

    @Test
    void 추출_0건이면_인식_실패() {
        ReceiptOcrResponse empty = new ReceiptOcrResponse(null, null, null, null, List.of());
        ReceiptOcrResponse emptySection = new ReceiptOcrResponse(null, null, null, null,
                List.of(section(null, null, List.of(), List.of())));

        assertThat(ReceiptOcrConverter.classify(empty)).isEqualTo(ReceiptStatus.NOT_RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(emptySection)).isEqualTo(ReceiptStatus.NOT_RECOGNIZED);
    }

    @Test
    void 필수_항목을_모두_읽으면_인식_성공_일부만이면_부분_인식() {
        Section full = section(null, 40_000L, List.of(new Line("진찰료", 10_000L)),
                List.of(new Drug("아포퀠", 7, "1일 2회", 15_000L)));
        ReceiptOcrResponse recognized = new ReceiptOcrResponse("냥멍동물병원", TODAY, 40_000L, null, List.of(full));
        ReceiptOcrResponse noDate = new ReceiptOcrResponse("냥멍동물병원", null, 40_000L, null, List.of(full));
        ReceiptOcrResponse noUsage = new ReceiptOcrResponse("냥멍동물병원", TODAY, 40_000L, null,
                List.of(section(null, 40_000L, List.of(new Line("진찰료", 10_000L)),
                        List.of(new Drug("아포퀠", 7, "용법 미상", 15_000L)))));
        ReceiptOcrResponse headerOnly = new ReceiptOcrResponse("냥멍동물병원", null, null, null, List.of());

        assertThat(ReceiptOcrConverter.classify(recognized)).isEqualTo(ReceiptStatus.RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(noDate)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(noUsage)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(headerOnly)).isEqualTo(ReceiptStatus.PARTIAL);
    }

    @Test
    void 용법에서_1일_횟수를_읽는다() {
        assertThat(ReceiptOcrConverter.parseDailyCount("1일 2회")).isEqualTo(2);
        assertThat(ReceiptOcrConverter.parseDailyCount("1일2회")).isEqualTo(2);
        assertThat(ReceiptOcrConverter.parseDailyCount("용법 미상")).isNull();
        assertThat(ReceiptOcrConverter.parseDailyCount(null)).isNull();
    }
}
