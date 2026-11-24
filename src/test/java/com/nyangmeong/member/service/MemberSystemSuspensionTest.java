package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 처리자 없는 회원 정지 (채팅 상대 신고 누적)]
 */
class MemberSystemSuspensionTest {

    private static final Long MBR_SN = 1L;
    private static final String NORMAL = MemberStatus.NORMAL.getCode();
    private static final String SUSPENDED = MemberStatus.SUSPENDED.getCode();

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);
    private final MemberSuspensionService service = new MemberSuspensionService(memberMapper, adminTaskLogService,
            loginSessionManager);

    private Member member(String mbrSttsCd, MemberAuthority authority) {
        Member member = new Member();
        member.setMbrSn(MBR_SN);
        member.setMbrSttsCd(mbrSttsCd);
        member.setMbrAuthrtCd(authority.getCode());
        return member;
    }

    @Test
    void 정상_회원을_7일_정지하고_처리자_없는_이력을_남기고_세션을_끊는다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        when(memberMapper.updateSuspend(MBR_SN, SUSPENDED, NORMAL, LocalDate.now().plusDays(7).atStartOfDay(),
                "SYSTEM")).thenReturn(1);

        assertThat(service.suspendBySystem(MBR_SN, 7, "신고 누적")).isTrue();

        verify(memberMapper).insertStatusHistory(MBR_SN, NORMAL, SUSPENDED, "신고 누적");
        verify(loginSessionManager).revokeAll(MBR_SN);
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 이미_정지된_회원이나_관리자는_예외_없이_건너뛴다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(SUSPENDED, MemberAuthority.USER));
        assertThat(service.suspendBySystem(MBR_SN, 7, "신고 누적")).isFalse();

        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.ADMIN));
        assertThat(service.suspendBySystem(MBR_SN, 7, "신고 누적")).isFalse();

        verify(memberMapper, never()).updateSuspend(anyLong(), anyString(), anyString(), any(), anyString());
        verify(loginSessionManager, never()).revokeAll(anyLong());
    }

    @Test
    void 동시에_상태가_바뀌어_갱신되지_않으면_이력을_남기지_않는다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        when(memberMapper.updateSuspend(any(), any(), any(), any(), any())).thenReturn(0);

        assertThat(service.suspendBySystem(MBR_SN, 7, "신고 누적")).isFalse();

        verify(memberMapper, never()).insertStatusHistory(any(), any(), any(), any());
    }
}
