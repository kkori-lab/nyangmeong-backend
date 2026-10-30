package com.nyangmeong.expense.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.global.external.ReceiptOcrResponse;

/**
 * [영수증 OCR 결과 판정]
 * - 인식 판정 (FN-AIM-010-01·05)
 */
final class ReceiptOcrConverter {

    private static final Pattern USAGE_COUNT = Pattern.compile("(\\d+)\\s*회");

    private ReceiptOcrConverter() {
    }

    /**
     * 인식 상태 판정
     */
    static ReceiptStatus classify(ReceiptOcrResponse ocr) {
        long lines = ocr.sections().stream().mapToLong(section -> section.items().size()).sum();
        long drugs = ocr.sections().stream().mapToLong(section -> section.drugs().size()).sum();
        boolean hasHeader = ocr.hsptlNm() != null || ocr.ckupYmd() != null || ocr.recptAmt() != null;
        if (!hasHeader && lines == 0 && drugs == 0) {
            return ReceiptStatus.NOT_RECOGNIZED;
        }
        boolean completeHeader = ocr.hsptlNm() != null && ocr.ckupYmd() != null && ocr.recptAmt() != null;
        boolean completeLines = lines > 0 && ocr.sections().stream()
                .flatMap(section -> section.items().stream())
                .allMatch(line -> line.itemNm() != null);
        boolean completeDrugs = ocr.sections().stream()
                .flatMap(section -> section.drugs().stream())
                .allMatch(drug -> drug.mdcnNm() != null && drug.mdcnDays() != null
                        && parseDailyCount(drug.mdcnUsage()) != null);
        return completeHeader && completeLines && completeDrugs ? ReceiptStatus.RECOGNIZED : ReceiptStatus.PARTIAL;
    }

    /**
     * 용법의 1일 횟수
     */
    static Integer parseDailyCount(String mdcnUsage) {
        if (mdcnUsage == null) {
            return null;
        }
        Matcher matcher = USAGE_COUNT.matcher(mdcnUsage);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
