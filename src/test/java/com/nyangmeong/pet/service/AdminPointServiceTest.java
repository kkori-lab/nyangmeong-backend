package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.pet.domain.PointDealType;
import com.nyangmeong.pet.domain.PointLedger;
import com.nyangmeong.pet.dto.AdminPointAdjustRequest;
import com.nyangmeong.pet.dto.AdminPointLedgerRow;
import com.nyangmeong.pet.dto.AdminPointSearchCond;
import com.nyangmeong.pet.mapper.CareMissionMapper;
import com.nyangmeong.pet.mapper.PointMapper;

/**
 * [검사 - 관리자 포인트 원장 검색·수동 조정]
 */
class AdminPointServiceTest {

    private static final Long ADMIN = 1L;
    private static final Long MBR = 7L;
    private static final Long LEDGER_SN = 500L;

    private final PointMapper pointMapper = mock(PointMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final PointService pointService = new PointService(pointMapper, mock(CareMissionMapper.class));
    private final AdminPointService service = new AdminPointService(pointService, pointMapper, adminTaskLogService);

    @BeforeEach
    void setUp() {
        when(pointMapper.selectMemberSttsCd(MBR)).thenReturn(MemberStatus.NORMAL.getCode());
        when(pointMapper.selectHldPnt(MBR)).thenReturn(300);
        when(pointMapper.updateHldPnt(anyLong(), anyInt(), anyString())).thenReturn(1);
        doAnswer(inv -> {
            inv.<PointLedger>getArgument(0).setPntGnlgrSn(LEDGER_SN);
            return 1;
        }).when(pointMapper).insertLedger(any());
        when(pointMapper.selectAdminLedgerRow(LEDGER_SN)).thenReturn(new AdminPointLedgerRow());
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    @Test
    void 지급은_보유_포인트와_관리자_조정_원장을_같이_남기고_감사_로그를_쓴다() {
        AdminPointLedgerRow row = service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 100, "  이벤트 지급  "));

        verify(pointMapper).updateHldPnt(MBR, 100, String.valueOf(ADMIN));
        ArgumentCaptor<PointLedger> captor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointMapper).insertLedger(captor.capture());
        PointLedger ledger = captor.getValue();
        assertThat(ledger.getPntDlngTypeCd()).isEqualTo(PointDealType.ADMIN_ADJUST.getCode());
        assertThat(ledger.getDlngPnt()).isEqualTo(100);
        assertThat(ledger.getDlngAftrBlncPnt()).isEqualTo(300);
        assertThat(ledger.getMemoCn()).isEqualTo("이벤트 지급");
        assertThat(ledger.getIdmptDmndId()).startsWith("ADJ-");
        verify(adminTaskLogService).record(eq(ADMIN), eq(AdminTaskType.CREATE), eq("PNT_GNLGR"), eq(LEDGER_SN),
                eq("포인트 조정"), isNull(), eq(row));
    }

    @Test
    void 차감은_음수로_기록한다() {
        service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, -50, "중복 지급 회수"));

        verify(pointMapper).updateHldPnt(MBR, -50, String.valueOf(ADMIN));
    }

    @Test
    void 잔액보다_큰_차감은_POINT_NOT_ENOUGH이고_원장을_남기지_않는다() {
        when(pointMapper.updateHldPnt(MBR, -1000, String.valueOf(ADMIN))).thenReturn(0);

        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, -1000, "회수")),
                ErrorCode.POINT_NOT_ENOUGH);
        verify(pointMapper, never()).insertLedger(any());
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 영_포인트_상한_초과_사유_공백은_INVALID_INPUT_VALUE() {
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 0, "사유")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 100_001, "사유")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, -100_001, "사유")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 10, "   ")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 10, "가".repeat(201))),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(pointMapper, never()).updateHldPnt(anyLong(), anyInt(), anyString());
    }

    @Test
    void 상한과_사유_200자는_조정한다() {
        when(pointMapper.selectHldPnt(MBR)).thenReturn(200_000);

        service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, 100_000, "가".repeat(200)));
        service.adjust(ADMIN, new AdminPointAdjustRequest(MBR, -100_000, "회수"));

        verify(pointMapper).updateHldPnt(MBR, 100_000, String.valueOf(ADMIN));
        verify(pointMapper).updateHldPnt(MBR, -100_000, String.valueOf(ADMIN));
    }

    @Test
    void 없는_회원과_탈퇴_회원은_MEMBER_NOT_FOUND() {
        when(pointMapper.selectMemberSttsCd(99L)).thenReturn(null);
        when(pointMapper.selectMemberSttsCd(8L)).thenReturn(MemberStatus.WITHDRAWN.getCode());
        when(pointMapper.selectMemberSttsCd(9L)).thenReturn(MemberStatus.SUSPENDED.getCode());

        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(99L, 10, "사유")),
                ErrorCode.MEMBER_NOT_FOUND);
        assertError(() -> service.adjust(ADMIN, new AdminPointAdjustRequest(8L, 10, "사유")),
                ErrorCode.MEMBER_NOT_FOUND);
        service.adjust(ADMIN, new AdminPointAdjustRequest(9L, 10, "사유"));
        verify(pointMapper).updateHldPnt(9L, 10, String.valueOf(ADMIN));
    }

    @Test
    void 검색은_공백_조건을_비우고_넘긴다() {
        AdminPointSearchCond cond = new AdminPointSearchCond();
        cond.setMbrNcnm("  초코 ");
        cond.setPntDlngTypeCd(" ");

        service.search(cond, new PageQuery(null, null, null));

        ArgumentCaptor<AdminPointSearchCond> captor = ArgumentCaptor.forClass(AdminPointSearchCond.class);
        verify(pointMapper).selectAdminAllLedgerList(captor.capture());
        assertThat(captor.getValue().getMbrNcnm()).isEqualTo("초코");
        assertThat(captor.getValue().getPntDlngTypeCd()).isNull();
    }

    @Test
    void 모르는_거래_유형과_뒤집힌_기간은_INVALID_INPUT_VALUE() {
        AdminPointSearchCond badType = new AdminPointSearchCond();
        badType.setPntDlngTypeCd("MYPC9999");
        AdminPointSearchCond badRange = new AdminPointSearchCond();
        badRange.setFromDt(LocalDate.of(2026, 11, 5));
        badRange.setToDt(LocalDate.of(2026, 11, 4));
        PageQuery pq = new PageQuery(null, null, null);

        assertError(() -> service.search(badType, pq), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.search(badRange, pq), ErrorCode.INVALID_INPUT_VALUE);
        verify(pointMapper, never()).selectAdminAllLedgerList(any());
    }

    @Test
    void 같은_날짜_기간과_관리자_조정_유형은_검색한다() {
        AdminPointSearchCond cond = new AdminPointSearchCond();
        cond.setPntDlngTypeCd(PointDealType.ADMIN_ADJUST.getCode());
        cond.setFromDt(LocalDate.of(2026, 11, 5));
        cond.setToDt(LocalDate.of(2026, 11, 5));

        service.search(cond, new PageQuery(null, null, null));

        verify(pointMapper).selectAdminAllLedgerList(cond);
    }
}
