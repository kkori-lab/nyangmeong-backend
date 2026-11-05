package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.dto.MemberWithdrawRequest;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 회원탈퇴]
 * - 문구가 완전히 같을 때만 상태 변경·이력·세션 폐기, 다르면 아무것도 바꾸지 않는다
 * - 정상 회원이 아니면(바뀐 행 0) MEMBER_INACTIVE 이고 이력도 세션 폐기도 하지 않는다
 */
class MemberWithdrawServiceTest {

    private static final long MBR_SN = 7L;
    private static final String PHRASE = "정말로 탈퇴하겠습니다.";

    MemberMapper memberMapper = mock(MemberMapper.class);
    LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);
    MemberWithdrawService service;

    @BeforeEach
    void setUp() {
        service = new MemberWithdrawService(memberMapper, loginSessionManager);
        when(memberMapper.updateWithdraw(eq(MBR_SN), anyString(), anyString(), anyString())).thenReturn(1);
    }

    @Test
    void 문구가_일치하면_탈퇴_상태_이력_세션_폐기를_한다() {
        service.withdraw(MBR_SN, new MemberWithdrawRequest(true, PHRASE));

        verify(memberMapper).updateWithdraw(MBR_SN, MemberStatus.NORMAL.getCode(), MemberStatus.WITHDRAWN.getCode(),
                "7");
        verify(memberMapper).insertStatusHistory(MBR_SN, MemberStatus.NORMAL.getCode(),
                MemberStatus.WITHDRAWN.getCode(), "회원 탈퇴");
        verify(loginSessionManager).revokeAll(MBR_SN);
    }

    @Test
    void 문구가_조금이라도_다르면_거절하고_아무것도_바꾸지_않는다() {
        String[] wrongs = { "정말로 탈퇴하겠습니다", "정말로탈퇴하겠습니다.", "정말로 탈퇴하겠습니다. ", " 정말로 탈퇴하겠습니다.", "", "탈퇴" };
        for (String wrong : wrongs) {
            Throwable t = catchThrowable(() -> service.withdraw(MBR_SN, new MemberWithdrawRequest(true, wrong)));

            assertThat(t).isInstanceOfSatisfying(CustomException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_PHRASE_MISMATCH));
        }
        verifyNoInteractions(loginSessionManager);
        verify(memberMapper, never()).updateWithdraw(anyLong(), anyString(), anyString(), anyString());
        verify(memberMapper, never()).insertStatusHistory(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 정상_회원이_아니면_MEMBER_INACTIVE이고_이력도_세션_폐기도_하지_않는다() {
        when(memberMapper.updateWithdraw(eq(MBR_SN), anyString(), anyString(), anyString())).thenReturn(0);

        Throwable t = catchThrowable(() -> service.withdraw(MBR_SN, new MemberWithdrawRequest(true, PHRASE)));

        assertThat(t).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_INACTIVE));
        verify(memberMapper, never()).insertStatusHistory(anyLong(), anyString(), anyString(), anyString());
        verifyNoInteractions(loginSessionManager);
    }
}
