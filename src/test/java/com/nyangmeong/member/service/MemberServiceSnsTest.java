package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.SnsSignupClaims;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - SNS 회원 등록]
 * - 비밀번호 없이 가입 경로를 제공자로 저장하고, 최초 상태 이력과 SNS 연동 1행을 함께 남긴다
 * - 같은 제공자 계정이 이미 연동돼 있으면 SNS_ACCOUNT_ALREADY_LINKED
 */
class MemberServiceSnsTest {

    private static final String KAKAO = JoinPath.KAKAO.getCode();
    private static final String ACCOUNT_ID = KAKAO + ":acnt-1";
    private static final String EMAIL = "sns@example.com";
    private static final String PHONE = "01012345678";

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final FieldCryptoService crypto = cryptoService();
    private final FileStore fileStore = mock(FileStore.class);
    private final MemberService service = new MemberService(memberMapper, crypto, systemSettingService, fileStore);

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private SnsSignupClaims claims() {
        return new SnsSignupClaims(KAKAO, ACCOUNT_ID, EMAIL, PHONE, true, 0L, Long.MAX_VALUE);
    }

    private void givenInsertAssigns(long mbrSn) {
        doAnswer(invocation -> {
            ((Member) invocation.getArgument(0)).setMbrSn(mbrSn);
            return 1;
        }).when(memberMapper).insert(any(Member.class));
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(null);
    }

    @Test
    void SNS_회원은_비밀번호_없이_제공자_가입_경로로_저장하고_연동과_최초_이력을_남긴다() {
        givenInsertAssigns(21L);

        Member member = service.createWithSns(claims(), PHONE);

        ArgumentCaptor<Member> inserted = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insert(inserted.capture());
        Member saved = inserted.getValue();
        assertThat(saved.getPswdHash()).isNull();
        assertThat(saved.getJoinPathCd()).isEqualTo(KAKAO);
        assertThat(saved.getMbrSttsCd()).isEqualTo(MemberStatus.NORMAL.getCode());
        assertThat(crypto.decrypt(saved.getAcntEmlEnc())).isEqualTo(EMAIL);
        assertThat(saved.getAcntEmlHmac()).isEqualTo(crypto.emailHmac(EMAIL));
        assertThat(crypto.decrypt(saved.getMblTelnoEnc())).isEqualTo(PHONE);
        assertThat(saved.getMbrNcnm()).startsWith("냥멍");
        verify(memberMapper, times(1)).insertSnsLink(21L, KAKAO, ACCOUNT_ID);
        verify(memberMapper).insertStatusHistory(eq(21L), isNull(), eq(MemberStatus.NORMAL.getCode()), anyString());
        assertThat(member.getMbrSn()).isEqualTo(21L);
    }

    @Test
    void 같은_제공자_계정이_이미_연동돼_있으면_SNS_ACCOUNT_ALREADY_LINKED다() {
        givenInsertAssigns(21L);
        when(memberMapper.insertSnsLink(21L, KAKAO, ACCOUNT_ID))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'UK_SNS_ACNT_LINK_1'"));

        assertThatThrownBy(() -> service.createWithSns(claims(), PHONE))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SNS_ACCOUNT_ALREADY_LINKED));
    }

    @Test
    void 연동된_회원_조회는_제공자와_계정_ID로_찾는다() {
        Member linked = new Member();
        linked.setMbrSn(21L);
        when(memberMapper.selectBySnsAccount(KAKAO, ACCOUNT_ID)).thenReturn(linked);

        assertThat(service.findBySnsAccount(KAKAO, ACCOUNT_ID)).isSameAs(linked);
        assertThat(service.findBySnsAccount(KAKAO, "other")).isNull();
    }
}
