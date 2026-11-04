package com.nyangmeong.expense.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.expense.domain.ReceiptSource;
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
     * 인식 상태 판정 : 건강 일지·AI 대화창 기준
     */
    static ReceiptStatus classify(ReceiptOcrResponse ocr) {
        return classify(ocr, ReceiptSource.HEALTH);
    }

    /**
     * 인식 상태 판정
     * - 머리 값 : 병원명·사용처·날짜·금액 중 하나라도 있으면 인식 실패가 아니다
     * - 가계부 출처 완전 인식 : 사용처·결제일·금액, 품목 0개 허용 (펫샵 영수증은 병원명·품목이 없을 수 있음)
     * - 그 밖의 출처 완전 인식 : 병원명·검진일·금액, 품목 1개 이상
     */
    static ReceiptStatus classify(ReceiptOcrResponse ocr, ReceiptSource source) {
        long lines = ocr.sections().stream().mapToLong(section -> section.items().size()).sum();
        long drugs = ocr.sections().stream().mapToLong(section -> section.drugs().size()).sum();
        boolean hasHeader = ocr.hsptlNm() != null || ocr.expndPlcusNm() != null || ocr.ckupYmd() != null
                || ocr.recptAmt() != null;
        if (!hasHeader && lines == 0 && drugs == 0) {
            return ReceiptStatus.NOT_RECOGNIZED;
        }
        boolean expense = source == ReceiptSource.EXPENSE;
        String place = expense ? ocr.expndPlcusNm() : ocr.hsptlNm();
        boolean completeHeader = place != null && ocr.ckupYmd() != null && ocr.recptAmt() != null;
        boolean completeLines = (expense || lines > 0) && ocr.sections().stream()
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
