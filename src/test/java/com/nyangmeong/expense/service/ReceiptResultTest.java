package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptSource;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.ReceiptResultResponse;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;
import com.nyangmeong.global.file.FileStore;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - AI 대화창 업로드 출처·분석 결과 다시 열기] (FN-AIM-013-04·06)
 */
class ReceiptResultTest {

    private final ReceiptMapper mapper = mock(ReceiptMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final ReceiptService service = new ReceiptService(mapper, fileStore, petAuthorizer);

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final ReceiptAnalyzer analyzer = new ReceiptAnalyzer(service, objectMapper);

    private final MockMultipartFile image = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    private String uploadedSource(String ocrDmndSrcCd) {
        ReceiptMapper uploadMapper = mock(ReceiptMapper.class);
        doAnswer(invocation -> {
            invocation.<Receipt>getArgument(0).setRecptSn(3L);
            return null;
        }).when(uploadMapper).insert(any());
        new ReceiptService(uploadMapper, fileStore, petAuthorizer).create(1L, 10L, image, ocrDmndSrcCd);
        ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
        verify(uploadMapper).insert(captor.capture());
        return captor.getValue().getOcrDmndSrcCd();
    }

    private Receipt stored(ReceiptStatus status, String ocrRspnsJson) {
        Receipt receipt = new Receipt();
        receipt.setRecptSn(3L);
        receipt.setMbrSn(1L);
        receipt.setOcrPrcsSttsCd(status.getCode());
        receipt.setOcrRspnsJson(ocrRspnsJson);
        return receipt;
    }

    private String recognizedJson() {
        return objectMapper.writeValueAsString(new ReceiptOcrResponse("냥멍동물병원", LocalDate.now(), 10_000L, null,
                List.of(new Section("미르", 10_000L, null, List.of(new Line("진찰료", 10_000L)), List.of()))));
    }

    @Test
    void AI_대화창_출처를_주면_AI_대화창으로_생략하면_건강기록으로_등록한다() {
        assertThat(uploadedSource(ReceiptSource.CHAT.getCode())).isEqualTo("AIMC0031");
        assertThat(uploadedSource(null)).isEqualTo(ReceiptSource.HEALTH.getCode());
        assertThat(uploadedSource(ReceiptSource.HEALTH.getCode())).isEqualTo(ReceiptSource.HEALTH.getCode());
    }

    @Test
    void 인식_성공_영수증은_저장된_OCR_응답을_그대로_돌려준다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(stored(ReceiptStatus.RECOGNIZED, recognizedJson()));

        ReceiptResultResponse response = analyzer.getResult(1L, 3L, 10L);

        assertThat(response.recptSn()).isEqualTo(3L);
        assertThat(response.petSn()).isEqualTo(10L);
        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.RECOGNIZED.getCode());
        assertThat(response.failRsn()).isNull();
        assertThat(response.ocr().hsptlNm()).isEqualTo("냥멍동물병원");
        assertThat(response.ocr().recptAmt()).isEqualTo(10_000L);
        assertThat(response.ocr().sections()).singleElement()
                .satisfies(section -> assertThat(section.sectionNm()).isEqualTo("미르"));
        verify(petAuthorizer).checkCaregiver(1L, 10L);
    }

    @Test
    void 부분_인식과_저장_완료도_OCR_응답을_돌려준다() {
        for (ReceiptStatus status : List.of(ReceiptStatus.PARTIAL, ReceiptStatus.SAVED)) {
            when(mapper.selectOwned(3L, 1L)).thenReturn(stored(status, recognizedJson()));

            ReceiptResultResponse response = analyzer.getResult(1L, 3L, 10L);

            assertThat(response.ocrPrcsSttsCd()).isEqualTo(status.getCode());
            assertThat(response.ocr()).isNotNull();
        }
    }

    @Test
    void 일시_오류_영수증은_상태와_마지막_사유만_돌려준다() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(stored(ReceiptStatus.TEMPORARY_ERROR, null));
        when(mapper.selectLastChgRsn(3L)).thenReturn("TIMEOUT");

        ReceiptResultResponse response = analyzer.getResult(1L, 3L, 10L);

        assertThat(response.ocrPrcsSttsCd()).isEqualTo(ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(response.failRsn()).isEqualTo("TIMEOUT");
        assertThat(response.ocr()).isNull();
    }

    @Test
    void 결과가_없는_상태는_상태만_돌려준다() {
        for (ReceiptStatus status : List.of(ReceiptStatus.ANALYZING, ReceiptStatus.CANCELED,
                ReceiptStatus.NOT_RECOGNIZED)) {
            when(mapper.selectOwned(3L, 1L)).thenReturn(stored(status, recognizedJson()));

            ReceiptResultResponse response = analyzer.getResult(1L, 3L, 10L);

            assertThat(response.ocrPrcsSttsCd()).isEqualTo(status.getCode());
            assertThat(response.failRsn()).isNull();
            assertThat(response.ocr()).isNull();
        }
    }

    @Test
    void 남의_영수증이나_없는_영수증은_RECEIPT_NOT_FOUND() {
        when(mapper.selectOwned(3L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> analyzer.getResult(1L, 3L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }

    @Test
    void 연동되지_않은_반려동물이면_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(1L, 99L);

        assertThatThrownBy(() -> analyzer.getResult(1L, 3L, 99L))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_ACCESS_DENIED);
    }
}
