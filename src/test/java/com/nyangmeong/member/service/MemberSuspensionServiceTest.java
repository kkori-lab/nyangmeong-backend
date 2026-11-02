package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.domain.SuspensionType;
import com.nyangmeong.member.dto.AdminMemberReleaseRequest;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.dto.AdminSuspensionSearchCond;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 회원 정지 서비스]
 */
class MemberSuspensionServiceTest {

    private static final Long ACTOR_SN = 9L;
    private static final Long MBR_SN = 1L;
    private static final String NORMAL = MemberStatus.NORMAL.getCode();
    private static final String SUSPENDED = MemberStatus.SUSPENDED.getCode();

    private MemberMapper memberMapper;
    private AdminTaskLogService adminTaskLogService;
    private LoginSessionManager loginSessionManager;
    private MemberSuspensionService service;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        adminTaskLogService = mock(AdminTaskLogService.class);
        loginSessionManager = mock(LoginSessionManager.class);
        service = new MemberSuspensionService(memberMapper, adminTaskLogService, loginSessionManager);
    }

    private Member member(String mbrSttsCd, MemberAuthority authority) {
        Member member = new Member();
        member.setMbrSn(MBR_SN);
        member.setMbrSttsCd(mbrSttsCd);
        member.setMbrAuthrtCd(authority.getCode());
        return member;
    }

    private void stubUpdateSuspend(int count) {
        when(memberMapper.updateSuspend(eq(MBR_SN), eq(SUSPENDED), eq(NORMAL), any(), anyString())).thenReturn(count);
    }

    @Test
    void 기간_정지는_오늘에서_며칠_뒤_0시에_끝난다() {
        for (int dayCnt : List.of(7, 30)) {
            when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
            LocalDateTime endDt = LocalDate.now().plusDays(dayCnt).atStartOfDay();
            when(memberMapper.updateSuspend(MBR_SN, SUSPENDED, NORMAL, endDt, "9")).thenReturn(1);

            service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(dayCnt, "욕설 반복"));

            verify(memberMapper).updateSuspend(MBR_SN, SUSPENDED, NORMAL, endDt, "9");
        }
    }

    @Test
    void 영구_정지는_종료_일시를_비운다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        when(memberMapper.updateSuspend(MBR_SN, SUSPENDED, NORMAL, null, "9")).thenReturn(1);

        service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(null, "광고 도배"));

        verify(memberMapper).updateSuspend(MBR_SN, SUSPENDED, NORMAL, null, "9");
    }

    @Test
    void 정지하면_이력과_감사_로그를_남기고_세션을_폐기한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        stubUpdateSuspend(1);

        service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(7, "욕설 반복"));

        verify(memberMapper).insertProcessedStatusHistory(MBR_SN, ACTOR_SN, NORMAL, SUSPENDED, "욕설 반복");
        verify(loginSessionManager).revokeAll(MBR_SN);
        verify(adminTaskLogService).record(eq(ACTOR_SN), eq(AdminTaskType.PROCESS), eq("MBR"), eq(MBR_SN),
                eq("회원 정지"), any(), any());
    }

    @Test
    void 정지_기간이_선택지에_없으면_400() {
        for (int dayCnt : List.of(0, 1, 8, 31, -7)) {
            Throwable thrown = catchThrowable(
                    () -> service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(dayCnt, "욕설 반복")));

            assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
        verify(memberMapper, never()).updateSuspend(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void 없는_회원은_정지하지_못한다() {
        Throwable thrown = catchThrowable(
                () -> service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(7, "욕설 반복")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 관리자는_정지하지_못한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.ADMIN));

        assertNotSuspendable(ACTOR_SN);
    }

    @Test
    void 본인은_정지하지_못한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));

        assertNotSuspendable(MBR_SN);
    }

    @Test
    void 탈퇴_회원은_정지하지_못한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(MemberStatus.WITHDRAWN.getCode(), MemberAuthority.USER));

        assertNotSuspendable(ACTOR_SN);
    }

    @Test
    void 이미_정지된_회원은_다시_정지하지_못한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(SUSPENDED, MemberAuthority.USER));

        assertNotSuspendable(ACTOR_SN);
    }

    @Test
    void 동시에_상태가_바뀌면_이력을_남기지_않는다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        stubUpdateSuspend(0);

        Throwable thrown = catchThrowable(
                () -> service.suspend(ACTOR_SN, MBR_SN, new AdminMemberSuspendRequest(7, "욕설 반복")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_SUSPENDABLE);
        verify(memberMapper, never()).insertProcessedStatusHistory(any(), any(), any(), any(), any());
        verify(loginSessionManager, never()).revokeAll(any());
    }

    private void assertNotSuspendable(Long actorSn) {
        Throwable thrown = catchThrowable(
                () -> service.suspend(actorSn, MBR_SN, new AdminMemberSuspendRequest(7, "욕설 반복")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_SUSPENDABLE);
        verify(memberMapper, never()).updateSuspend(any(), any(), any(), any(), any());
    }

    @Test
    void 해제하면_정상으로_바꾸고_처리자와_사유를_남긴다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(SUSPENDED, MemberAuthority.USER));
        when(memberMapper.updateRelease(MBR_SN, NORMAL, SUSPENDED, null, "9")).thenReturn(1);

        service.release(ACTOR_SN, MBR_SN, new AdminMemberReleaseRequest("이의 제기 인용"));

        verify(memberMapper).insertProcessedStatusHistory(MBR_SN, ACTOR_SN, SUSPENDED, NORMAL, "이의 제기 인용");
        verify(adminTaskLogService).record(eq(ACTOR_SN), eq(AdminTaskType.PROCESS), eq("MBR"), eq(MBR_SN),
                eq("회원 정지 해제"), any(), any());
    }

    @Test
    void 정지_중이_아니면_해제하지_못한다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));

        Throwable thrown = catchThrowable(
                () -> service.release(ACTOR_SN, MBR_SN, new AdminMemberReleaseRequest("이의 제기 인용")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED);
        verify(memberMapper, never()).updateRelease(any(), any(), any(), any(), any());
    }

    @Test
    void 동시에_해제되면_이력을_남기지_않는다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(SUSPENDED, MemberAuthority.USER));

        Throwable thrown = catchThrowable(
                () -> service.release(ACTOR_SN, MBR_SN, new AdminMemberReleaseRequest("이의 제기 인용")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED);
        verify(memberMapper, never()).insertProcessedStatusHistory(any(), any(), any(), any(), any());
    }

    @Test
    void 없는_회원은_해제하지_못한다() {
        Throwable thrown = catchThrowable(
                () -> service.release(ACTOR_SN, MBR_SN, new AdminMemberReleaseRequest("이의 제기 인용")));

        assertThat(((BaseException) thrown).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 만료_해제는_처리자_없이_이력을_남기고_바뀐_건수만_센다() {
        when(memberMapper.selectExpiredSuspendedMbrSns(eq(SUSPENDED), any())).thenReturn(List.of(1L, 2L));
        when(memberMapper.updateRelease(eq(1L), eq(NORMAL), eq(SUSPENDED), any(), eq("SYSTEM"))).thenReturn(1);
        when(memberMapper.updateRelease(eq(2L), eq(NORMAL), eq(SUSPENDED), any(), eq("SYSTEM"))).thenReturn(0);

        int released = service.releaseExpired();

        assertThat(released).isEqualTo(1);
        verify(memberMapper).insertStatusHistory(1L, SUSPENDED, NORMAL, "정지 기간 만료");
        verify(memberMapper, never()).insertStatusHistory(2L, SUSPENDED, NORMAL, "정지 기간 만료");
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 정지_회원_목록은_종류를_영구_여부로_넘긴다() {
        AdminSuspensionSearchCond cond = new AdminSuspensionSearchCond();
        cond.setQ(" 나비 ");
        cond.setType(SuspensionType.PERIOD);
        when(memberMapper.selectSuspensionList(SUSPENDED, "나비", false)).thenReturn(List.of());

        service.searchSuspensions(cond, new PageQuery(1, 20, null));

        verify(memberMapper).selectSuspensionList(SUSPENDED, "나비", false);
    }

    @Test
    void 정지_회원_목록의_조건은_비울_수_있다() {
        when(memberMapper.selectSuspensionList(eq(SUSPENDED), isNull(), isNull())).thenReturn(List.of());

        service.searchSuspensions(new AdminSuspensionSearchCond(), new PageQuery(1, 20, null));

        verify(memberMapper).selectSuspensionList(SUSPENDED, null, null);
    }
}
