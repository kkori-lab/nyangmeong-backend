package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptFailReason;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.global.external.OcrClient;
import com.nyangmeong.global.external.OcrProperties;
import com.nyangmeong.global.external.OcrTimeoutException;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.file.StoredFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * [영수증 OCR 분석]
 * - 요청 안에서 동기 분석, 제한 시간(app.ocr.timeout) 초과·API 오류 -> 일시 오류 (FN-AIM-008-07·010-01)
 * - 추출 0건 -> 인식 실패, 일부 -> 부분 인식, 실패해도 원본 유지 (FN-AIM-010-05·06)
 * - 결과 다시 열기 : 저장된 OCR 응답을 오늘 날짜·현재 반려동물 기준으로 다시 가공 (FN-AIM-013-06)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptAnalyzer {

    private final ReceiptService receiptService;
    private final OcrClient ocrClient;
    private final OcrProperties ocrProperties;
    private final CaregiverPetFake caregiverPetFake;
    private final ObjectMapper objectMapper;

    /** 저장된 OCR 응답으로 결과를 다시 주는 상태 */
    private static final List<ReceiptStatus> RESULT_KEPT = List.of(ReceiptStatus.RECOGNIZED, ReceiptStatus.PARTIAL,
            ReceiptStatus.SAVED);

    /**
     * 영수증 업로드와 분석
     */
    public ReceiptResponse create(Long mbrSn, Long petSn, MultipartFile file, String ocrDmndSrcCd) {
        Long recptSn = receiptService.create(mbrSn, petSn, file, ocrDmndSrcCd);
        return run(mbrSn, recptSn, petSn);
    }

    /**
     * 분석 결과 다시 열기
     */
    public ReceiptResponse get(Long mbrSn, Long recptSn, Long petSn) {
        Receipt receipt = receiptService.get(mbrSn, recptSn, petSn);
        ReceiptStatus status = ReceiptStatus.from(receipt.getOcrPrcsSttsCd());
        if (status == ReceiptStatus.TEMPORARY_ERROR) {
            return ReceiptResponse.empty(recptSn, status.getCode(), receiptService.getLastChgRsn(recptSn));
        }
        if (!RESULT_KEPT.contains(status) || receipt.getOcrRspnsJson() == null) {
            return ReceiptResponse.empty(recptSn, status.getCode(), null);
        }
        ReceiptOcrResponse ocr = objectMapper.readValue(receipt.getOcrRspnsJson(), ReceiptOcrResponse.class);
        return ReceiptOcrConverter.toResponse(recptSn, status, ocr, petSn, caregiverPetFake.search(mbrSn),
                LocalDate.now());
    }

    /**
     * 다시 분석
     */
    public ReceiptResponse analyze(Long mbrSn, Long recptSn, Long petSn) {
        receiptService.restart(mbrSn, recptSn, petSn);
        return run(mbrSn, recptSn, petSn);
    }

    /**
     * OCR 호출과 결과 반영
     */
    private ReceiptResponse run(Long mbrSn, Long recptSn, Long petSn) {
        ReceiptOcrResponse ocr;
        try {
            ocr = recognize(receiptService.getImage(mbrSn, recptSn));
        } catch (OcrTimeoutException e) {
            return fail(mbrSn, recptSn, ReceiptFailReason.TIMEOUT, e);
        } catch (RuntimeException e) {
            return fail(mbrSn, recptSn, ReceiptFailReason.API_ERROR, e);
        }

        ReceiptStatus status = ReceiptOcrConverter.classify(ocr);
        if (!receiptService.finish(mbrSn, recptSn, status, null, objectMapper.writeValueAsString(ocr))) {
            return ReceiptResponse.empty(recptSn, ReceiptStatus.CANCELED.getCode(), null);
        }
        if (status == ReceiptStatus.NOT_RECOGNIZED) {
            return ReceiptResponse.empty(recptSn, status.getCode(), null);
        }
        return ReceiptOcrConverter.toResponse(recptSn, status, ocr, petSn, caregiverPetFake.search(mbrSn),
                LocalDate.now());
    }

    /**
     * 제한 시간 안의 OCR 호출
     * - 시간 초과 시 호출 스레드를 기다리지 않음 : try-with-resources 의 close 는 작업 종료까지 대기
     */
    private ReceiptOcrResponse recognize(StoredFile image) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<ReceiptOcrResponse> future = executor
                    .submit(() -> ocrClient.recognize(image.content(), image.contentType()));
            return future.get(ocrProperties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new OcrTimeoutException("OCR 분석 시간 초과");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 일시 오류 반영
     */
    private ReceiptResponse fail(Long mbrSn, Long recptSn, ReceiptFailReason reason, RuntimeException e) {
        log.warn("[Receipt] OCR 분석 실패 recptSn={} reason={}", recptSn, reason, e);
        if (!receiptService.finish(mbrSn, recptSn, ReceiptStatus.TEMPORARY_ERROR, reason.name(), null)) {
            return ReceiptResponse.empty(recptSn, ReceiptStatus.CANCELED.getCode(), null);
        }
        return ReceiptResponse.empty(recptSn, ReceiptStatus.TEMPORARY_ERROR.getCode(), reason.name());
    }
}
