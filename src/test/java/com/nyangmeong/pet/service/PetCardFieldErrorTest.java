package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.pet.domain.JntBrupInvtn;
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptRequest;
import com.nyangmeong.pet.mapper.CaregiverInvitationMapper;
import com.nyangmeong.pet.mapper.PetMapper;

/**
 * [검사 - 반려동물 쓰기 AI 도구의 서비스 검사 400 칸 이름]
 * - accept_caregiver_invitation : 초대 대상이 아닌 반려동물을 고르면 data의 field로 petSns
 */
class PetCardFieldErrorTest {

    private static final Long SENDER = 1L;
    private static final Long RECEIVER = 2L;
    private static final Long INVITATION = 50L;
    private static final Long PET_A = 100L;
    private static final String TARGET_EMAIL = "sooyoung@example.com";

    @Test
    void 초대_수락_대상_아닌_반려동물이면_petSns() {
        CaregiverInvitationMapper invitationMapper = mock(CaregiverInvitationMapper.class);
        MemberService memberService = mock(MemberService.class);
        FieldCryptoService crypto = mock(FieldCryptoService.class);
        CaregiverInvitationService service = new CaregiverInvitationService(invitationMapper, mock(PetMapper.class),
                memberService, crypto, mock(TokenHashUtil.class), mock(EventPublisher.class),
                mock(SystemSettingService.class), "http://front.test");

        JntBrupInvtn invitation = new JntBrupInvtn();
        invitation.setJntBrupInvtnSn(INVITATION);
        invitation.setSndrMbrSn(SENDER);
        invitation.setInvtnSttsCd("PETC0020");
        invitation.setInvtnTrgtEmlHmac("h:" + TARGET_EMAIL);
        invitation.setInvtnExpryDt(LocalDateTime.now().plusDays(3));
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation);
        when(invitationMapper.selectTargetPetSns(INVITATION)).thenReturn(List.of(PET_A));
        when(memberService.getMember(RECEIVER)).thenReturn(
                new MemberResponse(TARGET_EMAIL, "수영", "01012345678", null, "USRC0001", true, List.of(), null, null));
        when(crypto.emailHmac(anyString())).thenAnswer(inv -> "h:" + inv.<String>getArgument(0));

        CustomException e = catchThrowableOfType(CustomException.class, () -> service.acceptInvitation(RECEIVER,
                INVITATION, new CaregiverInvitationAcceptRequest(List.of(PET_A, 999L))));

        assertThat(e).isNotNull();
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(e.getFields()).containsExactly("petSns");
    }
}
