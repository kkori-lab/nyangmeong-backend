package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberUpdateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.PasswordResetAccount;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 회원 서비스]
 * - 이메일·번호·닉네임 중복 판단, 회원 저장 값, 닉네임 설정, 회원정보 수정(번호·프로필 사진)
 */
class MemberServiceTest {

    private static final String EMAIL = "newbie@example.com";
    private static final String PHONE = "01012345678";

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final FieldCryptoService crypto = cryptoService();
    private final FileStore fileStore = mock(FileStore.class);
    private final MemberService service = new MemberService(memberMapper, crypto, systemSettingService, fileStore);

    /** Mockito 는 Long 반환을 0으로 채우므로 "쓰는 회원 없음"을 null 로 명시 */
    @BeforeEach
    void stubNoOwner() {
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(null);
        when(memberMapper.selectMbrSnByPhoneHmac(anyString())).thenReturn(null);
    }

    private static FieldCryptoService cryptoService() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService fieldCryptoService = new FieldCryptoService(properties);
        fieldCryptoService.initializeKeys();
        return fieldCryptoService;
    }

    private Member stored(Long mbrSn, String statusCode, String ncnm, String ncnmStngCmptnYn) {
        Member member = new Member();
        member.setMbrSn(mbrSn);
        member.setMbrSttsCd(statusCode);
        member.setJoinPathCd(JoinPath.EMAIL.getCode());
        member.setAcntEmlEnc(crypto.encrypt(EMAIL));
        member.setMblTelnoEnc(crypto.encrypt(PHONE));
        member.setMbrNcnm(ncnm);
        member.setNcnmStngCmptnYn(ncnmStngCmptnYn);
        return member;
    }

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private void givenInsertAssignsSn(long mbrSn) {
        doAnswer(invocation -> {
            invocation.<Member>getArgument(0).setMbrSn(mbrSn);
            return 1;
        }).when(memberMapper).insert(any());
    }

    @Test
    void 없는_이메일은_가입할_수_있다() {
        service.checkEmailAvailable(crypto.emailHmac(EMAIL));
    }

    @Test
    void 정상_회원_이메일은_중복이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.NORMAL.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void 정지_회원_이메일도_중복이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.SUSPENDED.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    void 탈퇴_보관_회원_이메일은_복구_안내이다() {
        when(memberMapper.selectByEmailHmac(anyString()))
                .thenReturn(stored(1L, MemberStatus.WITHDRAWN.getCode(), "a", "Y"));

        assertThatThrownBy(() -> service.checkEmailAvailable("h"))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_AVAILABLE));
    }

    @Test
    void 휴대전화번호는_정규화해_중복을_확인한다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThat(service.getPhoneAvailability("+82 10-1234-5678", null).normalizedMblTelno()).isEqualTo(PHONE);
        assertThat(service.getPhoneAvailability("010-1234-5678", null).available()).isFalse();
        assertThat(service.getPhoneAvailability("010-1234-5678", 6L).available()).isFalse();
    }

    @Test
    void 로그인_회원_본인의_번호는_사용_가능하다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThat(service.getPhoneAvailability("01012345678", 5L).available()).isTrue();
    }

    @Test
    void 쓰는_사람이_없는_번호는_사용_가능하다() {
        assertThat(service.getPhoneAvailability("01012345678", null).available()).isTrue();
    }

    @Test
    void 형식이_틀린_번호는_거절한다() {
        assertThatThrownBy(() -> service.getPhoneAvailability("02-123-4567", null))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_PHONE_NUMBER));
    }

    @Test
    void 가입_때_다른_회원이_쓰는_번호는_거절한다() {
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        assertThatThrownBy(() -> service.checkPhoneAvailable(PHONE))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
    }

    @Test
    void 닉네임은_다른_회원이_쓰면_사용할_수_없다() {
        when(memberMapper.selectMbrSnByNickname("초코맘")).thenReturn(5L);

        assertThat(service.getNicknameAvailability("초코맘", null).available()).isFalse();
        assertThat(service.getNicknameAvailability("초코맘", 6L).available()).isFalse();
        assertThat(service.getNicknameAvailability("초코맘", 5L).available()).isTrue();
        assertThat(service.getNicknameAvailability("새닉네임", null).available()).isTrue();
    }

    @Test
    void 가입하면_정상_일반회원으로_저장하고_최초_상태_이력을_남긴다() {
        givenInsertAssignsSn(11L);

        Member created = service.create(EMAIL, PHONE, "hash");

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insert(saved.capture());
        Member member = saved.getValue();
        assertThat(created.getMbrSn()).isEqualTo(11L);
        assertThat(member.getMbrSttsCd()).isEqualTo(MemberStatus.NORMAL.getCode());
        assertThat(member.getMbrAuthrtCd()).isEqualTo(MemberAuthority.USER.getCode());
        assertThat(member.getJoinPathCd()).isEqualTo(JoinPath.EMAIL.getCode());
        assertThat(member.getPswdHash()).isEqualTo("hash");
        assertThat(member.getAcntEmlHmac()).isEqualTo(crypto.emailHmac(EMAIL));
        assertThat(crypto.decrypt(member.getAcntEmlEnc())).isEqualTo(EMAIL);
        assertThat(member.getMblTelnoHmac()).isEqualTo(crypto.hmac(PHONE));
        assertThat(crypto.decrypt(member.getMblTelnoEnc())).isEqualTo(PHONE);
        assertThat(member.getLastLgnDt()).isNull();
        verify(memberMapper).insertStatusHistory(11L, null, MemberStatus.NORMAL.getCode(), "회원가입");
    }

    @Test
    void 기본_닉네임은_한글_숫자_10글자이다() {
        givenInsertAssignsSn(11L);

        String nickname = service.create(EMAIL, PHONE, "hash").getMbrNcnm();

        assertThat(nickname).matches("^냥멍\\d{8}$").hasSize(10);
    }

    @Test
    void 기본_닉네임이_겹치면_다시_만든다() {
        givenInsertAssignsSn(11L);
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(1L, 1L, null);

        assertThat(service.create(EMAIL, PHONE, "hash").getMbrNcnm()).startsWith("냥멍");
        verify(memberMapper, org.mockito.Mockito.times(3)).selectMbrSnByNickname(anyString());
    }

    @Test
    void 기본_닉네임을_계속_못_만들면_서버_오류이다() {
        when(memberMapper.selectMbrSnByNickname(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.INTERNAL_SERVER_ERROR));
        verify(memberMapper, never()).insert(any());
    }

    @Test
    void 동시_가입으로_유일_키가_부딪히면_키_이름으로_오류를_가른다() {
        when(memberMapper.insert(any())).thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_1'"))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_3'"))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_2'"));

        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.EMAIL_ALREADY_REGISTERED));
        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
        assertThatThrownBy(() -> service.create(EMAIL, PHONE, "hash"))
                .satisfies(e -> assertCode(e, ErrorCode.NICKNAME_ALREADY_USED));
        verify(memberMapper, never()).insertStatusHistory(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void 닉네임_설정은_저장과_완료_표시를_함께_한다() {
        Member member = stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N");
        Member updated = stored(5L, MemberStatus.NORMAL.getCode(), "초코맘", "Y");
        when(memberMapper.selectBySn(5L)).thenReturn(member, updated);
        when(memberMapper.selectSnsOffrCds(5L)).thenReturn(List.of());

        MemberResponse response = service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘"));

        verify(memberMapper).updateNickname(5L, "초코맘", "5");
        assertThat(response.mbrNcnm()).isEqualTo("초코맘");
        assertThat(response.ncnmStngCmptnYn()).isTrue();
        assertThat(response.acntEml()).isEqualTo(EMAIL);
        assertThat(response.mblTelno()).isEqualTo(PHONE);
    }

    @Test
    void 기본_닉네임_그대로도_확정할_수_있다() {
        Member member = stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N");
        when(memberMapper.selectBySn(5L)).thenReturn(member);
        when(memberMapper.selectMbrSnByNickname("냥멍12345678")).thenReturn(5L);

        service.updateNickname(5L, new MemberNicknameUpdateRequest("냥멍12345678"));

        verify(memberMapper).updateNickname(5L, "냥멍12345678", "5");
    }

    @Test
    void 다른_회원이_쓰는_닉네임은_저장하지_않는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.NORMAL.getCode(), "냥멍12345678", "N"));
        when(memberMapper.selectMbrSnByNickname("초코맘")).thenReturn(9L);

        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.NICKNAME_ALREADY_USED));
        verify(memberMapper, never()).updateNickname(anyLong(), anyString(), anyString());
    }

    @Test
    void 정지_탈퇴_회원의_닉네임은_바꾸지_않는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.WITHDRAWN.getCode(), "a", "N"));

        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_INACTIVE));
        verify(memberMapper, never()).updateNickname(anyLong(), anyString(), anyString());
    }

    @Test
    void 없는_회원의_닉네임은_바꾸지_않는다() {
        assertThatThrownBy(() -> service.updateNickname(5L, new MemberNicknameUpdateRequest("초코맘")))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 회원_응답은_이메일과_번호를_복호화하고_SNS_연동을_담는다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.NORMAL.getCode(), "초코맘", "Y"));
        when(memberMapper.selectSnsOffrCds(5L)).thenReturn(List.of(JoinPath.KAKAO.getCode()));

        MemberResponse response = service.getMember(5L);

        assertThat(response.snsAcntLinks()).hasSize(1);
        assertThat(response.snsAcntLinks().get(0).snsOffrCd()).isEqualTo(JoinPath.KAKAO.getCode());
        assertThat(response.joinPathCd()).isEqualTo(JoinPath.EMAIL.getCode());
        verify(memberMapper).selectSnsOffrCds(eq(5L));
    }

    @Test
    void 회원_응답은_가입일과_최근_로그인을_담는다() {
        Member member = stored(5L, MemberStatus.NORMAL.getCode(), "초코맘", "Y");
        member.setRegDt(LocalDateTime.of(2026, 8, 14, 9, 0));
        member.setLastLgnDt(LocalDateTime.of(2026, 10, 21, 10, 42));
        when(memberMapper.selectBySn(5L)).thenReturn(member);

        MemberResponse response = service.getMember(5L);

        assertThat(response.joinDt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 9, 0));
        assertThat(response.lastLgnDt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 10, 42));
        assertThat(response.mblTelno()).isEqualTo(PHONE);
    }

    private static MultipartFile png() {
        return new MockMultipartFile("profilePhoto", "me.png", "image/png", new byte[] {1, 2, 3});
    }

    private void givenNormalMember(long mbrSn) {
        when(memberMapper.selectBySn(mbrSn)).thenReturn(stored(mbrSn, MemberStatus.NORMAL.getCode(), "초코맘", "Y"));
    }

    @Test
    void 회원정보_수정은_정규화한_번호를_암호문과_HMAC으로_저장한다() {
        givenNormalMember(5L);

        service.updateMember(5L, new MemberUpdateRequest("+82 10-9876-5432", null, null));

        ArgumentCaptor<String> enc = ArgumentCaptor.forClass(String.class);
        verify(memberMapper).updateProfile(eq(5L), enc.capture(), eq(crypto.hmac("01098765432")), isNull(),
                eq(false), eq("5"));
        assertThat(crypto.decrypt(enc.getValue())).isEqualTo("01098765432");
    }

    @Test
    void 회원정보_수정은_다른_회원이_쓰는_번호를_저장하지_않는다() {
        givenNormalMember(5L);
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac("01098765432"))).thenReturn(9L);

        Throwable thrown = catchThrowable(
                () -> service.updateMember(5L, new MemberUpdateRequest("010-9876-5432", null, null)));

        assertCode(thrown, ErrorCode.PHONE_ALREADY_USED);
        verify(memberMapper, never()).updateProfile(anyLong(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void 회원정보_수정은_본인_번호를_그대로_보내도_저장한다() {
        givenNormalMember(5L);
        when(memberMapper.selectMbrSnByPhoneHmac(crypto.hmac(PHONE))).thenReturn(5L);

        service.updateMember(5L, new MemberUpdateRequest(PHONE, null, null));

        verify(memberMapper).updateProfile(eq(5L), anyString(), eq(crypto.hmac(PHONE)), isNull(), eq(false), eq("5"));
    }

    @Test
    void 회원정보_수정은_형식이_틀린_번호를_거절한다() {
        givenNormalMember(5L);

        Throwable thrown = catchThrowable(
                () -> service.updateMember(5L, new MemberUpdateRequest("12345", null, null)));

        assertCode(thrown, ErrorCode.INVALID_PHONE_NUMBER);
    }

    @Test
    void 회원정보_수정은_사진을_공개_이미지로_저장한다() {
        givenNormalMember(5L);
        MultipartFile photo = png();
        when(fileStore.storePublic(photo, "5")).thenReturn(77L);

        service.updateMember(5L, new MemberUpdateRequest(null, photo, null));

        verify(memberMapper).updateProfile(eq(5L), isNull(), isNull(), eq(77L), eq(false), eq("5"));
    }

    @Test
    void 회원정보_수정은_기본_이미지로_되돌리면_사진을_비운다() {
        givenNormalMember(5L);

        service.updateMember(5L, new MemberUpdateRequest(null, null, true));

        verify(memberMapper).updateProfile(eq(5L), isNull(), isNull(), isNull(), eq(true), eq("5"));
        verify(fileStore, never()).storePublic(any(MultipartFile.class), anyString());
    }

    @Test
    void 회원정보_수정은_사진과_되돌리기를_같이_보내면_400이다() {
        givenNormalMember(5L);

        Throwable thrown = catchThrowable(
                () -> service.updateMember(5L, new MemberUpdateRequest(null, png(), true)));

        assertCode(thrown, ErrorCode.BAD_REQUEST);
        verify(fileStore, never()).storePublic(any(MultipartFile.class), anyString());
    }

    @Test
    void 회원정보_수정은_보낸_값이_없으면_바꾸지_않는다() {
        givenNormalMember(5L);

        MemberResponse response = service.updateMember(5L, new MemberUpdateRequest(null, null, false));

        assertThat(response.mbrNcnm()).isEqualTo("초코맘");
        verify(memberMapper, never()).updateProfile(anyLong(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void 회원정보_수정은_정지_탈퇴_회원이면_거절한다() {
        when(memberMapper.selectBySn(5L)).thenReturn(stored(5L, MemberStatus.WITHDRAWN.getCode(), "초코맘", "Y"));

        Throwable thrown = catchThrowable(
                () -> service.updateMember(5L, new MemberUpdateRequest(PHONE, null, null)));

        assertCode(thrown, ErrorCode.MEMBER_INACTIVE);
    }

    @Test
    void 회원정보_수정_중_번호_유일_키가_부딪히면_409이다() {
        givenNormalMember(5L);
        when(memberMapper.updateProfile(anyLong(), any(), any(), any(), anyBoolean(), any()))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_3'"));

        Throwable thrown = catchThrowable(
                () -> service.updateMember(5L, new MemberUpdateRequest("01098765432", null, null)));

        assertCode(thrown, ErrorCode.PHONE_ALREADY_USED);
    }

    @Test
    void 비밀번호_찾기_없는_이메일은_미가입이다() {
        assertThat(service.resolvePasswordResetAccount(crypto.hmac(EMAIL)).certRslt())
                .isEqualTo(EmailCertificationResponse.MEMBER_NOT_FOUND);
    }

    @Test
    void 비밀번호_찾기_비밀번호가_있는_회원은_재설정_대상이다() {
        Member member = stored(1L, MemberStatus.NORMAL.getCode(), "냥멍", "Y");
        member.setPswdHash("hash");
        when(memberMapper.selectByEmailHmac(crypto.hmac(EMAIL))).thenReturn(member);

        PasswordResetAccount account = service.resolvePasswordResetAccount(crypto.hmac(EMAIL));

        assertThat(account.resettable()).isTrue();
        assertThat(account.snsOffrCds()).isNull();
    }

    @Test
    void 비밀번호_찾기_정지_회원도_재설정_대상이다() {
        Member member = stored(1L, MemberStatus.SUSPENDED.getCode(), "냥멍", "Y");
        member.setPswdHash("hash");
        when(memberMapper.selectByEmailHmac(crypto.hmac(EMAIL))).thenReturn(member);

        PasswordResetAccount account = service.resolvePasswordResetAccount(crypto.hmac(EMAIL));
        assertThat(account.resettable()).isTrue();
        assertThat(account.mbrSn()).isEqualTo(1L);
    }

    @Test
    void 비밀번호_변경은_바뀐_행이_없으면_재설정_불가이다() {
        when(memberMapper.updatePassword(eq(1L), eq("HASH"), eq(MemberStatus.WITHDRAWN.getCode()), anyString()))
                .thenReturn(1)
                .thenReturn(0);

        service.changePassword(1L, "HASH");

        assertThatThrownBy(() -> service.changePassword(1L, "HASH"))
                .satisfies(e -> assertCode(e, ErrorCode.PASSWORD_RESET_NOT_AVAILABLE));
    }

    @Test
    void 비밀번호_찾기_비밀번호가_없는_회원은_연결된_SNS만_안내한다() {
        Member member = stored(1L, MemberStatus.NORMAL.getCode(), "냥멍", "Y");
        member.setJoinPathCd(JoinPath.KAKAO.getCode());
        when(memberMapper.selectByEmailHmac(crypto.hmac(EMAIL))).thenReturn(member);
        when(memberMapper.selectSnsOffrCds(1L)).thenReturn(List.of(JoinPath.KAKAO.getCode()));

        PasswordResetAccount account = service.resolvePasswordResetAccount(crypto.hmac(EMAIL));

        assertThat(account.certRslt()).isEqualTo(EmailCertificationResponse.SNS_ONLY);
        assertThat(account.snsOffrCds()).containsExactly(JoinPath.KAKAO.getCode());
    }

    @Test
    void 비밀번호_찾기_탈퇴_보관_기간_안이면_복구_안내_지나면_미가입이다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        Member keeping = stored(1L, MemberStatus.WITHDRAWN.getCode(), "냥멍", "Y");
        keeping.setWhdwlDt(LocalDateTime.now().minusDays(29));
        when(memberMapper.selectByEmailHmac(crypto.hmac(EMAIL))).thenReturn(keeping);
        assertThat(service.resolvePasswordResetAccount(crypto.hmac(EMAIL)).certRslt())
                .isEqualTo(EmailCertificationResponse.MEMBER_WITHDRAWN);

        Member expired = stored(1L, MemberStatus.WITHDRAWN.getCode(), "냥멍", "Y");
        expired.setWhdwlDt(LocalDateTime.now().minusDays(31));
        when(memberMapper.selectByEmailHmac(crypto.hmac(EMAIL))).thenReturn(expired);
        assertThat(service.resolvePasswordResetAccount(crypto.hmac(EMAIL)).certRslt())
                .isEqualTo(EmailCertificationResponse.MEMBER_NOT_FOUND);
    }

    private Member withdrawn(int daysAgo) {
        Member member = stored(7L, MemberStatus.WITHDRAWN.getCode(), "냥멍", "Y");
        member.setWhdwlDt(LocalDateTime.now().minusDays(daysAgo));
        return member;
    }

    @Test
    void 복구_대상은_보관_기간_안의_탈퇴_회원이다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        Member keeping = withdrawn(29);
        when(memberMapper.selectByEmailHmac("h")).thenReturn(keeping);

        assertThat(service.getRecoverableMember("h")).isSameAs(keeping);
        assertThat(service.getRecoveryExpiryDt(keeping)).isEqualTo(keeping.getWhdwlDt().plusDays(30));
    }

    @Test
    void 복구_대상이_아닌_회원과_미가입은_회원_없음이다() {
        when(memberMapper.selectByEmailHmac("normal"))
                .thenReturn(stored(1L, MemberStatus.NORMAL.getCode(), "a", "Y"));
        when(memberMapper.selectByEmailHmac("suspended"))
                .thenReturn(stored(2L, MemberStatus.SUSPENDED.getCode(), "a", "Y"));
        when(memberMapper.selectByEmailHmac("none")).thenReturn(null);

        for (String hmac : List.of("normal", "suspended", "none")) {
            assertThatThrownBy(() -> service.getRecoverableMember(hmac))
                    .satisfies(e -> assertCode(e, ErrorCode.MEMBER_NOT_FOUND));
        }
    }

    @Test
    void 보관_기간이_지났거나_탈퇴일시를_모르면_복구_기간_경과이다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.selectByEmailHmac("expired")).thenReturn(withdrawn(31));
        Member unknown = stored(8L, MemberStatus.WITHDRAWN.getCode(), "냥멍", "Y");
        when(memberMapper.selectByEmailHmac("unknown")).thenReturn(unknown);

        assertThatThrownBy(() -> service.getRecoverableMember("expired"))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_EXPIRED));
        assertThatThrownBy(() -> service.getRecoverableMember("unknown"))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_EXPIRED));
    }

    @Test
    void 복구하면_정상으로_바꾸고_탈퇴에서_정상_이력을_한_번_남긴다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.updateRecover(7L, "USRC0001", "USRC0003", 30, "7")).thenReturn(1);

        service.recover(withdrawn(3), null);

        verify(memberMapper).insertStatusHistory(7L, "USRC0003", "USRC0001", "재가입 복구");
        verify(memberMapper, never()).updatePhone(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 보관_번호가_없던_회원은_받은_번호를_암호화해_저장한다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.updateRecover(anyLong(), anyString(), anyString(), anyInt(), anyString())).thenReturn(1);
        when(memberMapper.updatePhone(eq(7L), anyString(), anyString(), eq("7"))).thenReturn(1);

        service.recover(withdrawn(3), PHONE);

        ArgumentCaptor<String> enc = ArgumentCaptor.forClass(String.class);
        verify(memberMapper).updatePhone(eq(7L), enc.capture(), eq(crypto.hmac(PHONE)), eq("7"));
        assertThat(crypto.decrypt(enc.getValue())).isEqualTo(PHONE);
    }

    @Test
    void 상태_변경이_0행이면_기간_경과이고_이력도_번호도_남기지_않는다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.updateRecover(anyLong(), anyString(), anyString(), anyInt(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.recover(withdrawn(3), PHONE))
                .satisfies(e -> assertCode(e, ErrorCode.MEMBER_RECOVERY_EXPIRED));

        verify(memberMapper, never()).insertStatusHistory(anyLong(), anyString(), anyString(), anyString());
        verify(memberMapper, never()).updatePhone(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 번호_저장이_0행이면_이미_처리된_요청이다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.updateRecover(anyLong(), anyString(), anyString(), anyInt(), anyString())).thenReturn(1);
        when(memberMapper.updatePhone(anyLong(), anyString(), anyString(), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.recover(withdrawn(3), PHONE))
                .satisfies(e -> assertCode(e, ErrorCode.ALREADY_PROCESSED));
    }

    @Test
    void 번호_저장_중_다른_회원과_겹치면_번호_중복이다() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        when(memberMapper.updateRecover(anyLong(), anyString(), anyString(), anyInt(), anyString())).thenReturn(1);
        when(memberMapper.updatePhone(anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'MBR.UK_MBR_3'"));

        assertThatThrownBy(() -> service.recover(withdrawn(3), PHONE))
                .satisfies(e -> assertCode(e, ErrorCode.PHONE_ALREADY_USED));
    }

    private AdminMemberSummary adminRow(String email) {
        AdminMemberSummary row = new AdminMemberSummary();
        row.setMbrSn(1L);
        row.setAcntEmlEnc(crypto.encrypt(email));
        return row;
    }

    @Test
    void 관리자_목록은_이메일을_앞_3자만_남긴다() {
        when(memberMapper.selectAdminList(any(), any(), any()))
                .thenReturn(List.of(adminRow("choco@gmail.com"), adminRow("ab@naver.com")));

        PageResponse<AdminMemberSummary> page = service.searchForAdmin(new AdminMemberSearchCond(),
                new PageQuery(null, null, null));

        assertThat(page.content().get(0).getMaskedAcntEml()).isEqualTo("cho***@gmail.com");
        assertThat(page.content().get(1).getMaskedAcntEml()).isEqualTo("ab***@naver.com");
    }

    @Test
    void 관리자_검색어에_골뱅이가_있으면_이메일_HMAC으로_찾는다() {
        when(memberMapper.selectAdminList(any(), any(), any())).thenReturn(List.of());
        AdminMemberSearchCond cond = new AdminMemberSearchCond();
        cond.setQ(" Choco@Gmail.com ");

        service.searchForAdmin(cond, new PageQuery(null, null, null));

        verify(memberMapper).selectAdminList(cond, crypto.hmac("choco@gmail.com"), null);
    }

    @Test
    void 관리자_검색어에_골뱅이가_없으면_닉네임으로_찾는다() {
        when(memberMapper.selectAdminList(any(), any(), any())).thenReturn(List.of());
        AdminMemberSearchCond cond = new AdminMemberSearchCond();
        cond.setQ("초코");

        service.searchForAdmin(cond, new PageQuery(null, null, null));

        verify(memberMapper).selectAdminList(cond, null, "초코");
    }

    @Test
    void 관리자_검색어가_비면_검색_조건을_넣지_않는다() {
        when(memberMapper.selectAdminList(any(), any(), any())).thenReturn(List.of());
        AdminMemberSearchCond cond = new AdminMemberSearchCond();
        cond.setQ("  ");

        service.searchForAdmin(cond, new PageQuery(null, null, null));

        verify(memberMapper).selectAdminList(cond, null, null);
    }

    @Test
    void 관리자_목록은_시작일이_종료일보다_늦으면_거절한다() {
        AdminMemberSearchCond cond = new AdminMemberSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 10, 30));
        cond.setEndYmd(LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> service.searchForAdmin(cond, new PageQuery(null, null, null)))
                .satisfies(e -> assertCode(e, ErrorCode.INVALID_INPUT_VALUE));
        verify(memberMapper, never()).selectAdminList(any(), any(), any());
    }
}
