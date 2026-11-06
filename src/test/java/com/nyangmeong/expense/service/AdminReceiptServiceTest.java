package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.expense.dto.AdminReceiptListResponse;
import com.nyangmeong.expense.dto.AdminReceiptSearchCond;
import com.nyangmeong.expense.dto.AdminReceiptSummary;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 영수증 OCR 현황]
 */
class AdminReceiptServiceTest {

    private final ReceiptMapper receiptMapper = mock(ReceiptMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final AdminReceiptService service = new AdminReceiptService(receiptMapper, fileStore);

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 건수는_성공_실패_분석중_일시오류_코드로_센다() {
        AdminReceiptSummary summary = new AdminReceiptSummary();
        summary.setTotCnt(5);
        summary.setFailCnt(2);
        AdminReceiptSearchCond cond = new AdminReceiptSearchCond();
        when(receiptMapper.selectAdminReceiptSummary(any(), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(summary);
        when(receiptMapper.selectAdminReceiptList(any())).thenReturn(List.of());

        AdminReceiptListResponse res = service.search(cond, new PageQuery(null, null, null));

        verify(receiptMapper).selectAdminReceiptSummary(cond, List.of("AIMC0002", "AIMC0003", "AIMC0007"),
                "AIMC0004", "AIMC0001", "AIMC0005");
        assertThat(res.summary().getTotCnt()).isEqualTo(5);
        assertThat(res.summary().getFailCnt()).isEqualTo(2);
        assertThat(res.content()).isEmpty();
    }

    @Test
    void 빈_문자열_필터는_없는_것으로_본다() {
        AdminReceiptSearchCond cond = new AdminReceiptSearchCond();
        cond.setOcrDmndSrcCd(" ");
        cond.setOcrPrcsSttsCd("");
        when(receiptMapper.selectAdminReceiptList(any())).thenReturn(List.of());

        AdminReceiptListResponse res = service.search(cond, new PageQuery(null, null, null));

        assertThat(cond.getOcrDmndSrcCd()).isNull();
        assertThat(cond.getOcrPrcsSttsCd()).isNull();
        assertThat(res.summary().getTotCnt()).isZero();
    }

    @Test
    void 모르는_출처나_상태_코드는_400() {
        AdminReceiptSearchCond src = new AdminReceiptSearchCond();
        src.setOcrDmndSrcCd("TMP00031");
        assertError(() -> service.search(src, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);

        AdminReceiptSearchCond stts = new AdminReceiptSearchCond();
        stts.setOcrPrcsSttsCd("AIMC0099");
        assertError(() -> service.search(stts, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(receiptMapper, never()).selectAdminReceiptList(any());
    }

    @Test
    void 시작일이_종료일보다_뒤면_400() {
        AdminReceiptSearchCond cond = new AdminReceiptSearchCond();
        cond.setFromYmd(LocalDate.of(2026, 11, 5));
        cond.setToYmd(LocalDate.of(2026, 11, 4));

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 원본_이미지는_소유와_관계없이_연다() {
        StoredFile file = new StoredFile(new byte[] { 1 }, "image/png", "r.png");
        when(receiptMapper.selectAtchFileSn(7L)).thenReturn(70L);
        when(fileStore.load(70L)).thenReturn(file);

        assertThat(service.getImage(7L)).isSameAs(file);
    }

    @Test
    void 없는_영수증_이미지는_404() {
        when(receiptMapper.selectAtchFileSn(8L)).thenReturn(null);

        assertError(() -> service.getImage(8L), ErrorCode.RECEIPT_NOT_FOUND);
    }
}
