package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.AdminAccountSummary;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 회원 권한 서비스]
 */
class MemberAuthorityServiceTest {

    private static final Long ACTOR_SN = 9L;
    private static final Long MBR_SN = 1L;
    private static final String ADMIN = MemberAuthority.ADMIN.getCode();
    private static final String USER = MemberAuthority.USER.getCode();
    private static final String NORMAL = MemberStatus.NORMAL.getCode();
    private static final String SUSPENDED = MemberStatus.SUSPENDED.getCode();
    private static final String WITHDRAWN = MemberStatus.WITHDRAWN.getCode();

    private MemberMapper memberMapper;
    private FieldCryptoService fieldCryptoService;
    private AdminTaskLogService adminTaskLogService;
    private MemberAuthorityService service;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        fieldCryptoService = mock(FieldCryptoService.class);
        adminTaskLogService = mock(AdminTaskLogService.class);
        service = new MemberAuthorityService(memberMapper, fieldCryptoService, adminTaskLogService);
    }

    private Member member(String mbrSttsCd, MemberAuthority authority) {
        Member member = new Member();
        member.setMbrSn(MBR_SN);
        member.setMbrSttsCd(mbrSttsCd);
        member.setMbrAuthrtCd(authority.getCode());
        return member;
    }

    private void stubAdmins(Long... mbrSns) {
        when(memberMapper.selectNormalAdminSnsForUpdate(ADMIN, NORMAL)).thenReturn(List.of(mbrSns));
    }

    private ErrorCode errorOf(Runnable action) {
        Throwable thrown = catchThrowable(action::run);
        assertThat(thrown).isInstanceOf(BaseException.class);
        return ((BaseException) thrown).getErrorCode();
    }

    @Test
    void 목록은_이메일을_마스킹하고_탈퇴를_뺀다() {
        AdminAccountSummary row = new AdminAccountSummary();
        row.setAcntEmlEnc("암호문");
        List<AdminAccountSummary> rows = new ArrayList<>(List.of(row));
        when(memberMapper.selectAdminAccountList(ADMIN, WITHDRAWN, "MBR", "관리자 지정")).thenReturn(rows);
        when(fieldCryptoService.decrypt("암호문")).thenReturn("admin@nyangmeong.dev");

        service.searchAdmins(new PageQuery(null, null, null));

        assertThat(row.getMaskedAcntEml()).isEqualTo("adm***@nyangmeong.dev");
    }

    @Test
    void 정상_일반회원을_지정하고_감사_로그를_남긴다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        when(memberMapper.updateAuthority(MBR_SN, ADMIN, USER, NORMAL, "9")).thenReturn(1);

        service.appointAdmin(ACTOR_SN, MBR_SN);

        verify(memberMapper).updateAuthority(MBR_SN, ADMIN, USER, NORMAL, "9");
        verify(adminTaskLogService).record(eq(ACTOR_SN), eq(AdminTaskType.UPDATE), eq("MBR"), eq(MBR_SN),
                eq("관리자 지정"), any(), any());
    }

    @Test
    void 없는_회원은_지정할_수_없다() {
        assertThat(errorOf(() -> service.appointAdmin(ACTOR_SN, MBR_SN))).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 정지_탈퇴_이미_관리자는_지정할_수_없다() {
        for (Member target : List.of(member(SUSPENDED, MemberAuthority.USER), member(WITHDRAWN, MemberAuthority.USER),
                member(NORMAL, MemberAuthority.ADMIN))) {
            when(memberMapper.selectBySn(MBR_SN)).thenReturn(target);

            assertThat(errorOf(() -> service.appointAdmin(ACTOR_SN, MBR_SN)))
                    .isEqualTo(ErrorCode.MEMBER_NOT_APPOINTABLE);
        }
        verify(memberMapper, never()).updateAuthority(any(), any(), any(), any(), any());
    }

    @Test
    void 조회_뒤_상태가_바뀌면_지정하지_않고_로그도_남기지_않는다() {
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.USER));
        when(memberMapper.updateAuthority(MBR_SN, ADMIN, USER, NORMAL, "9")).thenReturn(0);

        assertThat(errorOf(() -> service.appointAdmin(ACTOR_SN, MBR_SN))).isEqualTo(ErrorCode.MEMBER_NOT_APPOINTABLE);
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 관리자를_해제하고_감사_로그를_남긴다() {
        stubAdmins(MBR_SN, ACTOR_SN);
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.ADMIN));
        when(memberMapper.updateAuthority(eq(MBR_SN), eq(USER), eq(ADMIN), isNull(), anyString())).thenReturn(1);

        service.releaseAdmin(ACTOR_SN, MBR_SN);

        verify(memberMapper).updateAuthority(MBR_SN, USER, ADMIN, null, "9");
        verify(adminTaskLogService).record(eq(ACTOR_SN), eq(AdminTaskType.UPDATE), eq("MBR"), eq(MBR_SN),
                eq("관리자 해제"), any(), any());
    }

    @Test
    void 잠근_뒤에_대상을_읽는다() {
        stubAdmins(MBR_SN, ACTOR_SN);
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.ADMIN));
        when(memberMapper.updateAuthority(eq(MBR_SN), eq(USER), eq(ADMIN), isNull(), anyString())).thenReturn(1);

        service.releaseAdmin(ACTOR_SN, MBR_SN);

        InOrder order = inOrder(memberMapper);
        order.verify(memberMapper).selectNormalAdminSnsForUpdate(ADMIN, NORMAL);
        order.verify(memberMapper).selectBySn(MBR_SN);
    }

    @Test
    void 본인은_해제할_수_없다() {
        assertThat(errorOf(() -> service.releaseAdmin(ACTOR_SN, ACTOR_SN))).isEqualTo(ErrorCode.ADMIN_NOT_RELEASABLE);
        verify(memberMapper, never()).updateAuthority(any(), any(), any(), any(), any());
    }

    @Test
    void 마지막_정상_관리자는_해제할_수_없다() {
        stubAdmins(MBR_SN);
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(NORMAL, MemberAuthority.ADMIN));

        assertThat(errorOf(() -> service.releaseAdmin(ACTOR_SN, MBR_SN))).isEqualTo(ErrorCode.ADMIN_NOT_RELEASABLE);
        verify(memberMapper, never()).updateAuthority(any(), any(), any(), any(), any());
    }

    @Test
    void 정지된_관리자는_정상_관리자가_남아_있으면_해제한다() {
        stubAdmins(ACTOR_SN);
        when(memberMapper.selectBySn(MBR_SN)).thenReturn(member(SUSPENDED, MemberAuthority.ADMIN));
        when(memberMapper.updateAuthority(eq(MBR_SN), eq(USER), eq(ADMIN), isNull(), anyString())).thenReturn(1);

        service.releaseAdmin(ACTOR_SN, MBR_SN);

        verify(memberMapper).updateAuthority(MBR_SN, USER, ADMIN, null, "9");
    }

    @Test
    void 일반회원과_탈퇴_회원은_이미_처리된_요청이다() {
        stubAdmins(ACTOR_SN);
        for (Member target : List.of(member(NORMAL, MemberAuthority.USER), member(WITHDRAWN, MemberAuthority.ADMIN))) {
            when(memberMapper.selectBySn(MBR_SN)).thenReturn(target);

            assertThat(errorOf(() -> service.releaseAdmin(ACTOR_SN, MBR_SN))).isEqualTo(ErrorCode.ALREADY_PROCESSED);
        }
        verify(memberMapper, never()).updateAuthority(any(), any(), any(), any(), any());
    }

    @Test
    void 없는_회원은_해제할_수_없다() {
        stubAdmins(ACTOR_SN);

        assertThat(errorOf(() -> service.releaseAdmin(ACTOR_SN, MBR_SN))).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
