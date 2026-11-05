package com.nyangmeong.pet.service;

import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.pet.domain.PointDealType;
import com.nyangmeong.pet.dto.AdminPointAdjustRequest;
import com.nyangmeong.pet.dto.AdminPointLedgerRow;
import com.nyangmeong.pet.dto.AdminPointSearchCond;
import com.nyangmeong.pet.mapper.PointMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 포인트 서비스]
 * - SCR-ADM-072 (FN-ADM-072-01·02)
 * - 잔액·원장 기록은 PointService, 여기는 요청 검증·감사 로그
 */
@Service
@RequiredArgsConstructor
public class AdminPointService {

    private static final String TARGET_TABLE = "PNT_GNLGR";

    private final PointService pointService;
    private final PointMapper pointMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 포인트 원장 검색 (최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminPointLedgerRow> search(AdminPointSearchCond cond, PageQuery pq) {
        AdminPointSearchCond c = cond == null ? new AdminPointSearchCond() : cond;
        c.setMbrNcnm(blankToNull(c.getMbrNcnm()));
        c.setPntDlngTypeCd(blankToNull(c.getPntDlngTypeCd()));
        if (c.getPntDlngTypeCd() != null
                && Arrays.stream(PointDealType.values()).noneMatch(t -> t.is(c.getPntDlngTypeCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (c.getFromDt() != null && c.getToDt() != null && c.getFromDt().isAfter(c.getToDt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        return PageResponse.of(new PageInfo<>(pointMapper.selectAdminAllLedgerList(c)));
    }

    /**
     * 포인트 수동 조정 (거래 유형 관리자 조정, 사유는 메모)
     */
    @Transactional
    public AdminPointLedgerRow adjust(Long actorSn, AdminPointAdjustRequest req) {
        String memoCn = req.memoCn() == null ? "" : req.memoCn().trim();
        if (req.dlngPnt() == null || req.dlngPnt() == 0 || Math.abs(req.dlngPnt()) > AdminPointAdjustRequest.MAX_PNT
                || memoCn.isEmpty() || memoCn.length() > 200) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String sttsCd = pointMapper.selectMemberSttsCd(req.mbrSn());
        if (sttsCd == null || MemberStatus.WITHDRAWN.is(sttsCd)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Long pntGnlgrSn = pointService.adjustByAdmin(actorSn, req.mbrSn(), req.dlngPnt(), memoCn);
        AdminPointLedgerRow row = pointMapper.selectAdminLedgerRow(pntGnlgrSn);
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, pntGnlgrSn, "포인트 조정", null, row);
        return row;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
