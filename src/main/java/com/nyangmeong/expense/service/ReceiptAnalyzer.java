package com.nyangmeong.expense.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptFailReason;
import com.nyangmeong.expense.domain.ReceiptOcrResult;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.ReceiptReportResponse;
import com.nyangmeong.expense.dto.ReceiptResultResponse;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.ReceiptOcrResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * [영수증 OCR 분석 결과]
 * - 분석은 Python AI 서버, 추출 0건 -> 인식 실패, 일부 -> 부분 인식, 실패해도 원본 유지 (FN-AIM-010-05·06)
 * - 결과 다시 열기 : 저장된 OCR 응답 그대로 (FN-AIM-013-06)
 * - Python AI 서버 분석 결과 보고 -> 상태 판정·반영 (FN-AIM-010-01·013-04)
 */
@Component
@RequiredArgsConstructor
public class ReceiptAnalyzer {

    private final ReceiptService receiptService;
    private final ObjectMapper objectMapper;

    /** 저장된 OCR 응답으로 결과를 다시 주는 상태 */
    private static final List<ReceiptStatus> RESULT_KEPT = List.of(ReceiptStatus.RECOGNIZED, ReceiptStatus.PARTIAL,
            ReceiptStatus.SAVED);

    /**
     * 분석 결과 다시 열기 : 저장된 OCR 응답 그대로
     */
    public ReceiptResultResponse getResult(Long mbrSn, Long recptSn, Long petSn) {
        Receipt receipt = receiptService.get(mbrSn, recptSn, petSn);
        ReceiptStatus status = ReceiptStatus.from(receipt.getOcrPrcsSttsCd());
        String failRsn = status == ReceiptStatus.TEMPORARY_ERROR ? receiptService.getLastChgRsn(recptSn) : null;
        ReceiptOcrResponse ocr = RESULT_KEPT.contains(status) && receipt.getOcrRspnsJson() != null
                ? objectMapper.readValue(receipt.getOcrRspnsJson(), ReceiptOcrResponse.class)
                : null;
        return new ReceiptResultResponse(recptSn, petSn, status.getCode(), failRsn, ocr);
    }

    /**
     * Python AI 서버의 분석 결과·상태 반영
     * - RECOGNIZED -> OCR 응답으로 인식성공·부분인식·인식실패 판정, ocr 없으면 400
     */
    public ReceiptReportResponse report(Long mbrSn, Long recptSn, ReceiptOcrResult result, ReceiptOcrResponse ocr) {
        if (result == ReceiptOcrResult.RECOGNIZED && ocr == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        ReceiptStatus status = switch (result) {
            case RECOGNIZED -> ReceiptOcrConverter.classify(ocr);
            case UNRECOGNIZED -> ReceiptStatus.NOT_RECOGNIZED;
            case TIMEOUT, API_ERROR -> ReceiptStatus.TEMPORARY_ERROR;
            case CANCELED -> ReceiptStatus.CANCELED;
        };
        String chgRsn = switch (result) {
            case TIMEOUT -> ReceiptFailReason.TIMEOUT.name();
            case API_ERROR -> ReceiptFailReason.API_ERROR.name();
            default -> null;
        };
        boolean keepsOcr = result == ReceiptOcrResult.RECOGNIZED || result == ReceiptOcrResult.UNRECOGNIZED;
        String ocrRspnsJson = keepsOcr && ocr != null ? objectMapper.writeValueAsString(ocr) : null;
        receiptService.report(mbrSn, recptSn, status, chgRsn, ocrRspnsJson);
        return new ReceiptReportResponse(recptSn, status.getCode());
    }

}
