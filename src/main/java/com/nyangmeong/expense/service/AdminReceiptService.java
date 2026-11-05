package com.nyangmeong.expense.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.expense.domain.ReceiptSource;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.AdminReceiptListResponse;
import com.nyangmeong.expense.dto.AdminReceiptRow;
import com.nyangmeong.expense.dto.AdminReceiptSearchCond;
import com.nyangmeong.expense.dto.AdminReceiptSummary;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 영수증 OCR 현황 서비스]
 * - SCR-ADM-082 (FN-ADM-082-01) 건수 묶음·목록·원본 이미지
 * - 분석 결과 내용은 주지 않는다 (결정 26)
 */
@Service
@RequiredArgsConstructor
public class AdminReceiptService {

    /** 성공으로 세는 상태 : 인식성공·부분인식·저장완료 */
    private static final List<String> SUCCESS_CDS = List.of(ReceiptStatus.RECOGNIZED.getCode(),
            ReceiptStatus.PARTIAL.getCode(), ReceiptStatus.SAVED.getCode());

    private final ReceiptMapper receiptMapper;
    private final FileStore fileStore;

    /**
     * 건수 묶음과 목록 (목록은 최신순)
     */
    @Transactional(readOnly = true)
    public AdminReceiptListResponse search(AdminReceiptSearchCond cond, PageQuery pq) {
        AdminReceiptSearchCond c = cond == null ? new AdminReceiptSearchCond() : cond;
        c.setOcrDmndSrcCd(blankToNull(c.getOcrDmndSrcCd()));
        c.setOcrPrcsSttsCd(blankToNull(c.getOcrPrcsSttsCd()));
        if (c.getOcrDmndSrcCd() != null
                && Arrays.stream(ReceiptSource.values()).noneMatch(s -> s.is(c.getOcrDmndSrcCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (c.getOcrPrcsSttsCd() != null
                && Arrays.stream(ReceiptStatus.values()).noneMatch(s -> s.is(c.getOcrPrcsSttsCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (c.getFromYmd() != null && c.getToYmd() != null && c.getFromYmd().isAfter(c.getToYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        AdminReceiptSummary summary = receiptMapper.selectAdminReceiptSummary(c, SUCCESS_CDS,
                ReceiptStatus.NOT_RECOGNIZED.getCode(), ReceiptStatus.ANALYZING.getCode(),
                ReceiptStatus.TEMPORARY_ERROR.getCode());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminReceiptRow> rows = receiptMapper.selectAdminReceiptList(c);
        return AdminReceiptListResponse.of(summary == null ? new AdminReceiptSummary() : summary,
                PageResponse.of(new PageInfo<>(rows)));
    }

    /**
     * 원본 이미지 (회원 소유 검사 없음)
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long recptSn) {
        Long atchFileSn = receiptMapper.selectAtchFileSn(recptSn);
        if (atchFileSn == null) {
            throw new CustomException(ErrorCode.RECEIPT_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 인식 실패 영수증 수 조회
     */
    @Transactional(readOnly = true)
    public long countOcrFailed() {
        AdminReceiptSummary summary = receiptMapper.selectAdminReceiptSummary(new AdminReceiptSearchCond(),
                SUCCESS_CDS, ReceiptStatus.NOT_RECOGNIZED.getCode(), ReceiptStatus.ANALYZING.getCode(),
                ReceiptStatus.TEMPORARY_ERROR.getCode());
        return summary == null ? 0 : summary.getFailCnt();
    }
}
