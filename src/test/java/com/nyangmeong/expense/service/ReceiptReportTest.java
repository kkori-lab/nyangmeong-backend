package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptOcrResult;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;
import com.nyangmeong.expense.dto.ReceiptReportResponse;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.OcrClient;
import com.nyangmeong.global.external.OcrProperties;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;
import com.nyangmeong.global.file.FileStore;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - Python AI 서버의 영수증 분석 결과·상태 보고] (FN-AIM-010-01·013-04)
 */
class ReceiptReportTest {

    private final ReceiptMapper mapper = mock(ReceiptMapper.class);
    private final ReceiptService service = new ReceiptService(mapper, mock(FileStore.class),
            mock(PetAuthorizer.class));
    private final ReceiptAnalyzer analyzer = new ReceiptAnalyzer(service, mock(OcrClient.class),
            new OcrProperties(), mock(CaregiverPetFake.class), JsonMapper.builder().build());

    private final String analyzing = ReceiptStatus.ANALYZING.getCode();

    @BeforeEach
    void setUp() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(stored(ReceiptStatus.ANALYZING));
        when(mapper.updateAnalyzed(eq(3L), eq(analyzing), anyString(), any())).thenReturn(1);
    }

    private Receipt stored(ReceiptStatus status) {
        Receipt receipt = new Receipt();
        receipt.setRecptSn(3L);
        receipt.setMbrSn(1L);
        receipt.setOcrPrcsSttsCd(status.getCode());
        return receipt;
    }

    private ReceiptOcrResponse fullOcr() {
        return new ReceiptOcrResponse("냥멍동물병원", LocalDate.of(2026, 10, 20), 10_000L, null,
                List.of(new Section("미르", 10_000L, null, List.of(new Line("진찰료", 10_000L)), List.of())));
    }

    private ReceiptStatusHistory history() {
        ArgumentCaptor<ReceiptStatusHistory> captor = ArgumentCaptor.forClass(ReceiptStatusHistory.class);
        verify(mapper).insertStatusHistory(captor.capture());
        return captor.getValue();
    }

    @Test
    void 인식_결과는_OCR_응답으로_상태를_판정하고_응답을_저장한다() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.RECOGNIZED, fullOcr());

        assertThat(response.recptSn()).isEqualTo(3L);
        String expected = ReceiptOcrConverter.classify(fullOcr()).getCode();
        assertThat(response.ocrPrcsSttsCd()).isEqualTo(expected);
        assertThat(expected).isIn(ReceiptStatus.RECOGNIZED.getCode(), ReceiptStatus.PARTIAL.getCode());
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateAnalyzed(eq(3L), eq(analyzing), eq(expected), json.capture());
        assertThat(json.getValue()).contains("냥멍동물병원");
        assertThat(history().getAftrSttsCd()).isEqualTo(expected);
    }

    @Test
    void 인식_결과인데_추출값이_없으면_인식_실패() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.RECOGNIZED,
                new ReceiptOcrResponse(null, null, null, null, List.of()));

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.NOT_RECOGNIZED.getCode());
    }

    @Test
    void 인식_결과에_OCR_응답이_없으면_400_INVALID_INPUT_VALUE() {
        assertThatThrownBy(() -> analyzer.report(1L, 3L, ReceiptOcrResult.RECOGNIZED, null))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updateAnalyzed(any(), any(), any(), any());
    }

    @Test
    void 인식_실패는_인식_실패로_저장한다() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.UNRECOGNIZED, null);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.NOT_RECOGNIZED.getCode());
        verify(mapper).updateAnalyzed(eq(3L), eq(analyzing), eq(ReceiptStatus.NOT_RECOGNIZED.getCode()), isNull());
        assertThat(history().getChgRsn()).isNull();
    }

    @Test
    void 시간_초과는_일시_오류와_사유_TIMEOUT() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.TIMEOUT, fullOcr());

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        verify(mapper).updateAnalyzed(eq(3L), eq(analyzing), eq(ReceiptStatus.TEMPORARY_ERROR.getCode()), isNull());
        assertThat(history().getChgRsn()).isEqualTo("TIMEOUT");
    }

    @Test
    void API_오류는_일시_오류와_사유_API_ERROR() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.API_ERROR, null);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(history().getChgRsn()).isEqualTo("API_ERROR");
    }

    @Test
    void 취소는_취소로_저장한다() {
        ReceiptReportResponse response = analyzer.report(1L, 3L, ReceiptOcrResult.CANCELED, null);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.CANCELED.getCode());
        ReceiptStatusHistory history = history();
        assertThat(history.getBfrSttsCd()).isEqualTo(analyzing);
        assertThat(history.getAftrSttsCd()).isEqualTo(ReceiptStatus.CANCELED.getCode());
    }

    @Test
    void 이미_취소한_영수증은_409_RECEIPT_ALREADY_CANCELED() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(stored(ReceiptStatus.CANCELED));

        assertThatThrownBy(() -> analyzer.report(1L, 3L, ReceiptOcrResult.UNRECOGNIZED, null))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ALREADY_CANCELED);
        verify(mapper, never()).updateAnalyzed(any(), any(), any(), any());
    }

    @Test
    void 이미_결과가_반영된_영수증은_409() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(stored(ReceiptStatus.RECOGNIZED));

        assertThatThrownBy(() -> analyzer.report(1L, 3L, ReceiptOcrResult.RECOGNIZED, fullOcr()))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 동시에_상태가_바뀌면_409_ALREADY_PROCESSED() {
        when(mapper.updateAnalyzed(eq(3L), eq(analyzing), anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> analyzer.report(1L, 3L, ReceiptOcrResult.CANCELED, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
        verify(mapper, never()).insertStatusHistory(any());
    }

    @Test
    void 남의_영수증이나_없는_영수증은_404_RECEIPT_NOT_FOUND() {
        when(mapper.selectOwned(3L, 2L)).thenReturn(null);

        assertThatThrownBy(() -> analyzer.report(2L, 3L, ReceiptOcrResult.CANCELED, null))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }
}
