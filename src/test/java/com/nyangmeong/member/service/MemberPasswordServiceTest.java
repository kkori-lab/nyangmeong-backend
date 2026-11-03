package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.session.LoginSessionManager;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.PasswordChangeRequest;
import com.nyangmeong.member.dto.PasswordVerifyRequest;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 로그인 회원의 비밀번호 확인·변경]
 * - 확인: 맞으면 통과, 틀리면 CURRENT_PASSWORD_MISMATCH, SNS 전용·비활성 회원 거절
 * - 변경: 성공하면 새 해시 저장과 모든 세션 폐기, 거절되면 저장도 폐기도 하지 않는다
 * - 해시는 실제 BCrypt 로 만들어 비교한다
 */
class MemberPasswordServiceTest {

    private static final long MBR_SN = 7L;
    private static final String CURRENT = "oldpass12";

    MemberMapper memberMapper = mock(MemberMapper.class);
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    LoginSessionManager loginSessionManager = mock(LoginSessionManager.class);
    MemberPasswordService service;

    @BeforeEach
    void setUp() {
        service = new MemberPasswordService(memberMapper, passwordEncoder, loginSessionManager);
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(MemberStatus.NORMAL, passwordEncoder.encode(CURRENT)));
        when(memberMapper.updatePassword(eq(MBR_SN), anyString(), anyString(), anyString())).thenReturn(1);
    }

    private Member member(MemberStatus status, String pswdHash) {
        Member member = new Member();
        member.setMbrSn(MBR_SN);
        member.setMbrSttsCd(status.getCode());
        member.setPswdHash(pswdHash);
        return member;
    }

    private PasswordChangeRequest change(String current, String pswd, String pswdCnfrm) {
        return new PasswordChangeRequest(current, pswd, pswdCnfrm);
    }

    private ErrorCode errorOf(Throwable thrown) {
        assertThat(thrown).isInstanceOf(CustomException.class);
        return ((CustomException) thrown).getErrorCode();
    }

    @Test
    void 확인은_현재_비밀번호가_맞으면_통과한다() {
        service.verify(MBR_SN, new PasswordVerifyRequest(CURRENT));
    }

    @Test
    void 확인은_틀리면_CURRENT_PASSWORD_MISMATCH다() {
        Throwable thrown = catchThrowable(() -> service.verify(MBR_SN, new PasswordVerifyRequest("wrongpass1")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.CURRENT_PASSWORD_MISMATCH);
    }

    @Test
    void 확인은_SNS_전용_회원이면_SNS_ONLY_MEMBER다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(MemberStatus.NORMAL, null));

        Throwable thrown = catchThrowable(() -> service.verify(MBR_SN, new PasswordVerifyRequest(CURRENT)));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.SNS_ONLY_MEMBER);
    }

    @Test
    void 확인은_정지_회원이면_MEMBER_INACTIVE다() {
        when(memberMapper.selectBySn(MBR_SN))
                .thenReturn(member(MemberStatus.SUSPENDED, passwordEncoder.encode(CURRENT)));

        Throwable thrown = catchThrowable(() -> service.verify(MBR_SN, new PasswordVerifyRequest(CURRENT)));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.MEMBER_INACTIVE);
    }

    @Test
    void 확인은_회원이_없으면_MEMBER_NOT_FOUND다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(null);

        Throwable thrown = catchThrowable(() -> service.verify(MBR_SN, new PasswordVerifyRequest(CURRENT)));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 변경하면_새_해시를_저장하고_모든_세션을_폐기한다() {
        service.change(MBR_SN, change(CURRENT, "newpass34", "newpass34"));

        org.mockito.ArgumentCaptor<String> hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(memberMapper).updatePassword(eq(MBR_SN), hash.capture(), eq(MemberStatus.WITHDRAWN.getCode()),
                eq(String.valueOf(MBR_SN)));
        assertThat(passwordEncoder.matches("newpass34", hash.getValue())).isTrue();
        assertThat(hash.getValue()).isNotEqualTo("newpass34");
        verify(loginSessionManager).revokeAll(MBR_SN);
    }

    @Test
    void 새_비밀번호와_확인값이_다르면_PASSWORD_MISMATCH이고_저장하지_않는다() {
        Throwable thrown = catchThrowable(() -> service.change(MBR_SN, change(CURRENT, "newpass34", "newpass35")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        verify(memberMapper, never()).updatePassword(anyLong(), anyString(), anyString(), anyString());
        verify(loginSessionManager, never()).revokeAll(any());
    }

    @Test
    void 현재_비밀번호가_틀리면_CURRENT_PASSWORD_MISMATCH이고_저장도_폐기도_하지_않는다() {
        Throwable thrown = catchThrowable(() -> service.change(MBR_SN, change("wrongpass1", "newpass34", "newpass34")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        verify(memberMapper, never()).updatePassword(anyLong(), anyString(), anyString(), anyString());
        verify(loginSessionManager, never()).revokeAll(any());
    }

    @Test
    void 변경은_SNS_전용_회원이면_SNS_ONLY_MEMBER다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(MemberStatus.NORMAL, null));

        Throwable thrown = catchThrowable(() -> service.change(MBR_SN, change(CURRENT, "newpass34", "newpass34")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.SNS_ONLY_MEMBER);
        verify(memberMapper, never()).updatePassword(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 변경은_정지_회원이면_MEMBER_INACTIVE다() {
        when(memberMapper.selectBySn(MBR_SN))
                .thenReturn(member(MemberStatus.SUSPENDED, passwordEncoder.encode(CURRENT)));

        Throwable thrown = catchThrowable(() -> service.change(MBR_SN, change(CURRENT, "newpass34", "newpass34")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.MEMBER_INACTIVE);
        verify(loginSessionManager, never()).revokeAll(any());
    }

    @Test
    void 바뀐_행이_없으면_MEMBER_NOT_FOUND이고_세션을_폐기하지_않는다() {
        when(memberMapper.updatePassword(eq(MBR_SN), anyString(), anyString(), anyString())).thenReturn(0);

        Throwable thrown = catchThrowable(() -> service.change(MBR_SN, change(CURRENT, "newpass34", "newpass34")));

        assertThat(errorOf(thrown)).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(loginSessionManager, never()).revokeAll(any());
    }
}
