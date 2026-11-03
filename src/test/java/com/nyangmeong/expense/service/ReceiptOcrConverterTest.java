package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.expense.domain.ReceiptSource;
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

    private ReceiptOcrResponse shop(String expndPlcusNm, LocalDate ymd, Long amt, List<Section> sections) {
        return new ReceiptOcrResponse(null, ymd, amt, null, sections, expndPlcusNm, "MYPC0013");
    }

    @Test
    void 가계부_출처는_사용처_결제일_금액이면_품목이_없어도_인식_성공() {
        ReceiptOcrResponse noItems = shop("펫월드", TODAY, 32_000L, List.of());
        ReceiptOcrResponse emptySection = shop("펫월드", TODAY, 32_000L,
                List.of(section(null, null, List.of(), List.of())));
        ReceiptOcrResponse withItems = shop("펫월드", TODAY, 32_000L,
                List.of(section(null, 32_000L, List.of(new Line("사료", 32_000L)), List.of())));

        assertThat(ReceiptOcrConverter.classify(noItems, ReceiptSource.EXPENSE)).isEqualTo(ReceiptStatus.RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(emptySection, ReceiptSource.EXPENSE))
                .isEqualTo(ReceiptStatus.RECOGNIZED);
        assertThat(ReceiptOcrConverter.classify(withItems, ReceiptSource.EXPENSE))
                .isEqualTo(ReceiptStatus.RECOGNIZED);
    }

    @Test
    void 가계부_출처는_사용처_결제일_금액_중_하나가_없거나_품목명이_없으면_부분_인식() {
        ReceiptOcrResponse noPlace = shop(null, TODAY, 32_000L, List.of());
        ReceiptOcrResponse noDate = shop("펫월드", null, 32_000L, List.of());
        ReceiptOcrResponse noAmt = shop("펫월드", TODAY, null, List.of());
        ReceiptOcrResponse noItemNm = shop("펫월드", TODAY, 32_000L,
                List.of(section(null, 32_000L, List.of(new Line(null, 32_000L)), List.of())));
        ReceiptOcrResponse hospitalOnly = new ReceiptOcrResponse("냥멍동물병원", TODAY, 32_000L, null, List.of(),
                null, null);

        for (ReceiptOcrResponse ocr : List.of(noPlace, noDate, noAmt, noItemNm, hospitalOnly)) {
            assertThat(ReceiptOcrConverter.classify(ocr, ReceiptSource.EXPENSE)).isEqualTo(ReceiptStatus.PARTIAL);
        }
    }

    @Test
    void 사용처만_읽힌_영수증은_인식_실패가_아니라_부분_인식() {
        ReceiptOcrResponse placeOnly = shop("펫월드", null, null, List.of());

        assertThat(ReceiptOcrConverter.classify(placeOnly, ReceiptSource.EXPENSE)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(placeOnly, ReceiptSource.HEALTH)).isEqualTo(ReceiptStatus.PARTIAL);
        assertThat(ReceiptOcrConverter.classify(placeOnly, ReceiptSource.CHAT)).isEqualTo(ReceiptStatus.PARTIAL);
    }

    @Test
    void 건강_일지와_대화창_출처는_병원명과_품목_기준_그대로() {
        ReceiptOcrResponse shopFull = shop("펫월드", TODAY, 32_000L,
                List.of(section(null, 32_000L, List.of(new Line("사료", 32_000L)), List.of())));
        ReceiptOcrResponse hospitalNoItems = new ReceiptOcrResponse("냥멍동물병원", TODAY, 32_000L, null, List.of(),
                "냥멍동물병원", null);
        Section full = section("미르", 32_000L, List.of(new Line("진찰료", 32_000L)), List.of());
        ReceiptOcrResponse hospitalFull = new ReceiptOcrResponse("냥멍동물병원", TODAY, 32_000L, null,
                List.of(full), "냥멍동물병원", "MYPC0012");

        for (ReceiptSource source : List.of(ReceiptSource.HEALTH, ReceiptSource.CHAT)) {
            assertThat(ReceiptOcrConverter.classify(shopFull, source)).isEqualTo(ReceiptStatus.PARTIAL);
            assertThat(ReceiptOcrConverter.classify(hospitalNoItems, source)).isEqualTo(ReceiptStatus.PARTIAL);
            assertThat(ReceiptOcrConverter.classify(hospitalFull, source)).isEqualTo(ReceiptStatus.RECOGNIZED);
        }
    }
}
