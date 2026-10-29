package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.pet.domain.BrupLinkSttsHstry;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.InvtnTrgtPet;
import com.nyangmeong.pet.domain.JntBrupInvtn;
import com.nyangmeong.pet.domain.JntBrupInvtnSttsHstry;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateResult;
import com.nyangmeong.pet.dto.CaregiverInvitationPetSummary;
import com.nyangmeong.pet.dto.CaregiverInvitationResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationSummary;
import com.nyangmeong.pet.mapper.CaregiverInvitationMapper;
import com.nyangmeong.pet.mapper.PetMapper;

/**
 * [검사 - 공동양육 초대 발송·보낸 초대·받은 초대·수락·거절]
 */
class CaregiverInvitationServiceTest {

    private static final Long SENDER = 1L;
    private static final Long RECEIVER = 2L;
    private static final Long OTHER = 3L;
    private static final Long INVITATION = 50L;
    private static final Long PET_A = 100L;
    private static final Long PET_B = 101L;
    private static final String SENDER_EMAIL = "owner@example.com";
    private static final String TARGET_EMAIL = "sooyoung@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String PENDING = "PETC0020";
    private static final String ACCEPTED = "PETC0021";
    private static final String REJECTED = "PETC0022";
    private static final String EXPIRED = "PETC0023";
    private static final String PRIMARY = "PETC0018";
    private static final String CO_PARENT = "PETC0019";
    private static final String LINK_ACCEPTED = "PETC0024";

    private final CaregiverInvitationMapper invitationMapper = mock(CaregiverInvitationMapper.class);
    private final PetMapper petMapper = mock(PetMapper.class);
    private final MemberService memberService = mock(MemberService.class);
    private final FieldCryptoService crypto = mock(FieldCryptoService.class);
    private final TokenHashUtil tokenHashUtil = mock(TokenHashUtil.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final CaregiverInvitationService service = new CaregiverInvitationService(invitationMapper, petMapper,
            memberService, crypto, tokenHashUtil, eventPublisher, "http://front.test");

    @BeforeEach
    void setUp() {
        when(crypto.normalizeEmail(anyString())).thenAnswer(inv -> inv.<String>getArgument(0).trim().toLowerCase());
        when(crypto.emailHmac(anyString())).thenAnswer(inv -> "h:" + inv.<String>getArgument(0).trim().toLowerCase());
        when(crypto.encrypt(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
        when(crypto.decrypt(anyString())).thenAnswer(inv -> inv.<String>getArgument(0).substring(4));
        when(tokenHashUtil.hash(anyString())).thenReturn("token-hash");

        when(memberService.getMember(SENDER)).thenReturn(member(SENDER_EMAIL, "김주인"));
        when(memberService.getMember(RECEIVER)).thenReturn(member(TARGET_EMAIL, "수영"));
        when(memberService.getMember(OTHER)).thenReturn(member(OTHER_EMAIL, "다른사람"));

        when(petMapper.selectPet(PET_A)).thenReturn(pet(PET_A, "뭉치"));
        when(petMapper.selectPet(PET_B)).thenReturn(pet(PET_B, "나비"));
        when(petMapper.selectBrupRel(SENDER, PET_A)).thenReturn(rel(PET_A, SENDER, PRIMARY));
        when(petMapper.selectBrupRel(SENDER, PET_B)).thenReturn(rel(PET_B, SENDER, PRIMARY));
        when(petMapper.countLinkedCaregivers(anyLong(), eq(CO_PARENT))).thenReturn(2);

        doAnswer(inv -> {
            inv.<JntBrupInvtn>getArgument(0).setJntBrupInvtnSn(INVITATION);
            return 1;
        }).when(invitationMapper).insertInvitation(any());
        when(invitationMapper.updateResponse(eq(INVITATION), anyLong(), eq(PENDING), anyString(), anyString()))
                .thenReturn(1);
        when(invitationMapper.selectTargetPetSns(INVITATION)).thenReturn(List.of(PET_A, PET_B));
    }

    @Test
    void 가입_회원에게_보낸_초대는_초대와_대상_반려동물_이력을_저장하고_인앱_알림을_발행한다() {
        when(memberService.findForLogin("h:" + TARGET_EMAIL)).thenReturn(memberVo(RECEIVER));
        when(invitationMapper.selectInvitationPets(eq(INVITATION), eq(PENDING), eq(ACCEPTED), eq(REJECTED),
                eq(EXPIRED), any())).thenReturn(List.of(petSummary(PET_A, "뭉치", PENDING)));

        CaregiverInvitationCreateResult result = service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest(" Sooyoung@Example.com ", List.of(PET_A, PET_B)));

        assertThat(result.succeeded()).isTrue();
        ArgumentCaptor<JntBrupInvtn> invitation = ArgumentCaptor.forClass(JntBrupInvtn.class);
        verify(invitationMapper).insertInvitation(invitation.capture());
        JntBrupInvtn saved = invitation.getValue();
        assertThat(saved.getSndrMbrSn()).isEqualTo(SENDER);
        assertThat(saved.getRcvrMbrSn()).isEqualTo(RECEIVER);
        assertThat(saved.getInvtnSttsCd()).isEqualTo(PENDING);
        assertThat(saved.getInvtnTrgtEmlEnc()).isEqualTo("enc:" + TARGET_EMAIL);
        assertThat(saved.getInvtnTrgtEmlHmac()).isEqualTo("h:" + TARGET_EMAIL);
        assertThat(saved.getInvtnExpryDt()).isEqualTo(saved.getRegDt().plusDays(7));
        assertThat(saved.getInvtnTknHash()).isEqualTo("token-hash");

        ArgumentCaptor<InvtnTrgtPet> targets = ArgumentCaptor.forClass(InvtnTrgtPet.class);
        verify(invitationMapper, times(2)).insertTargetPet(targets.capture());
        assertThat(targets.getAllValues()).extracting(InvtnTrgtPet::getPetSn).containsExactly(PET_A, PET_B);
        assertThat(targets.getAllValues()).extracting(InvtnTrgtPet::getJntBrupInvtnSn)
                .containsOnly(INVITATION);

        ArgumentCaptor<JntBrupInvtnSttsHstry> history = ArgumentCaptor.forClass(JntBrupInvtnSttsHstry.class);
        verify(invitationMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(PENDING);

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(RECEIVER);
        assertThat(event.getValue().type()).isEqualTo(NotificationType.CO_PARENTING);
        assertThat(event.getValue().notiTtl()).isEqualTo("뭉치, 나비의 공동양육자가 되어주세요");
        assertThat(event.getValue().mvmnPath()).isEqualTo("/mypage/invitations/50");
        verify(eventPublisher, never()).publish(any(CertificationMailEvent.class));

        CaregiverInvitationResponse response = result.response();
        assertThat(response.jntBrupInvtnSn()).isEqualTo(INVITATION);
        assertThat(response.invtnTrgtEmlMask()).isEqualTo("s***@example.com");
        assertThat(response.pets()).hasSize(1);
    }

    @Test
    void 미가입_이메일에는_초대_링크가_든_메일을_발행한다() {
        service.createInvitation(SENDER, new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A)));

        ArgumentCaptor<JntBrupInvtn> invitation = ArgumentCaptor.forClass(JntBrupInvtn.class);
        verify(invitationMapper).insertInvitation(invitation.capture());
        assertThat(invitation.getValue().getRcvrMbrSn()).isNull();

        ArgumentCaptor<CertificationMailEvent> mail = ArgumentCaptor.forClass(CertificationMailEvent.class);
        verify(eventPublisher).publish(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(TARGET_EMAIL);
        assertThat(mail.getValue().mailCn()).contains("http://front.test/mypage/invitations/50");
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 같은_반려동물을_두_번_보내도_대상은_한_번만_저장한다() {
        service.createInvitation(SENDER, new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A, PET_A)));

        verify(invitationMapper, times(1)).insertTargetPet(any());
    }

    @Test
    void 주양육자가_아닌_반려동물은_초대할_수_없다() {
        when(petMapper.selectBrupRel(SENDER, PET_B)).thenReturn(rel(PET_B, SENDER, CO_PARENT));

        assertThatThrownBy(() -> service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A, PET_B))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
        verify(invitationMapper, never()).insertInvitation(any());
    }

    @Test
    void 내_이메일로는_초대할_수_없다() {
        assertThatThrownBy(() -> service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest("OWNER@example.com", List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_SELF);
        verify(invitationMapper, never()).insertInvitation(any());
    }

    @Test
    void 이미_연동된_회원이면_반려동물명을_담아_실패를_돌려준다() {
        when(memberService.findForLogin("h:" + TARGET_EMAIL)).thenReturn(memberVo(RECEIVER));
        when(petMapper.selectBrupRel(RECEIVER, PET_B)).thenReturn(rel(PET_B, RECEIVER, CO_PARENT));

        CaregiverInvitationCreateResult result = service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A, PET_B)));

        assertThat(result.error()).isEqualTo(ErrorCode.CAREGIVER_ALREADY_LINKED);
        assertThat(result.conflict().petSn()).isEqualTo(PET_B);
        assertThat(result.conflict().petNm()).isEqualTo("나비");
        verify(invitationMapper, never()).insertInvitation(any());
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 같은_반려동물로_대기_중인_초대가_있으면_만료일시를_담아_실패를_돌려준다() {
        LocalDateTime expiry = LocalDateTime.of(2026, 11, 3, 10, 0);
        when(invitationMapper.selectPendingExpryDt(eq("h:" + TARGET_EMAIL), eq(PET_A), eq(PENDING), any()))
                .thenReturn(expiry);

        CaregiverInvitationCreateResult result = service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A)));

        assertThat(result.error()).isEqualTo(ErrorCode.CAREGIVER_INVITATION_DUPLICATED);
        assertThat(result.conflict().invtnExpryDt()).isEqualTo(expiry);
        assertThat(result.conflict().petNm()).isEqualTo("뭉치");
        verify(invitationMapper, never()).insertInvitation(any());
    }

    @Test
    void 공동양육자가_5명인_반려동물은_초대할_수_없다() {
        when(petMapper.countLinkedCaregivers(PET_A, CO_PARENT)).thenReturn(5);

        assertThatThrownBy(() -> service.createInvitation(SENDER,
                new CaregiverInvitationCreateRequest(TARGET_EMAIL, List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_LIMIT_EXCEEDED);
        verify(invitationMapper, never()).insertInvitation(any());
    }

    @Test
    void 보낸_초대_목록은_상태_코드를_넘기고_이메일을_복호화한다() {
        CaregiverInvitationSummary row = new CaregiverInvitationSummary();
        row.setJntBrupInvtnSn(INVITATION);
        row.setInvtnTrgtEml("enc:" + TARGET_EMAIL);
        when(invitationMapper.selectSentList(eq(SENDER), eq(PENDING), eq(ACCEPTED), eq(REJECTED), eq(EXPIRED),
                any())).thenReturn(List.of(row));

        List<CaregiverInvitationSummary> result = service.searchSentInvitations(SENDER);

        assertThat(result).singleElement().extracting(CaregiverInvitationSummary::getInvtnTrgtEml)
                .isEqualTo(TARGET_EMAIL);
    }

    @Test
    void 초대받은_계정은_대기_초대의_초대한_사람과_반려동물을_본다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));
        when(invitationMapper.selectInvitationPets(eq(INVITATION), eq(PENDING), eq(ACCEPTED), eq(REJECTED),
                eq(EXPIRED), any())).thenReturn(List.of(petSummary(PET_A, "뭉치", PENDING),
                        petSummary(PET_B, "나비", PENDING)));

        CaregiverInvitationResponse response = service.getInvitation(RECEIVER, INVITATION);

        assertThat(response.invtnSttsCd()).isEqualTo(PENDING);
        assertThat(response.emailMatched()).isTrue();
        assertThat(response.sndrMbrNcnm()).isEqualTo("김주인");
        assertThat(response.pets()).extracting(CaregiverInvitationPetSummary::getPetNm).containsExactly("뭉치", "나비");
    }

    @Test
    void 다른_계정은_가린_이메일만_보고_반려동물과_초대한_사람은_못_본다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));

        CaregiverInvitationResponse response = service.getInvitation(OTHER, INVITATION);

        assertThat(response.emailMatched()).isFalse();
        assertThat(response.invtnTrgtEmlMask()).isEqualTo("s***@example.com");
        assertThat(response.pets()).isNull();
        assertThat(response.sndrMbrNcnm()).isNull();
        verify(invitationMapper, never()).selectInvitationPets(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void 만료일시가_지난_대기_초대는_초대받은_계정에도_만료로_보이고_반려동물이_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, -1));

        CaregiverInvitationResponse response = service.getInvitation(RECEIVER, INVITATION);

        assertThat(response.invtnSttsCd()).isEqualTo(EXPIRED);
        assertThat(response.pets()).isNull();
        assertThat(response.sndrMbrNcnm()).isNull();
    }

    @Test
    void 이미_처리된_초대는_상태만_보인다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(REJECTED, 3));

        CaregiverInvitationResponse response = service.getInvitation(RECEIVER, INVITATION);

        assertThat(response.invtnSttsCd()).isEqualTo(REJECTED);
        assertThat(response.pets()).isNull();
    }

    @Test
    void 없는_초대는_404() {
        assertThatThrownBy(() -> service.getInvitation(RECEIVER, 999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_NOT_FOUND);
    }

    @Test
    void 수락은_고른_반려동물만_연동하고_나머지는_거절로_남긴다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));
        doAnswer(inv -> {
            inv.<BrupRel>getArgument(0).setBrupRelSn(700L);
            return 1;
        }).when(petMapper).insertBrupRel(any());

        service.acceptInvitation(RECEIVER, INVITATION, new CaregiverInvitationAcceptRequest(List.of(PET_A)));

        verify(invitationMapper).updateResponse(INVITATION, RECEIVER, PENDING, ACCEPTED, "2");
        verify(invitationMapper).updateTargetPetsAcceptance(INVITATION, List.of(PET_A));

        ArgumentCaptor<BrupRel> rel = ArgumentCaptor.forClass(BrupRel.class);
        verify(petMapper).insertBrupRel(rel.capture());
        assertThat(rel.getValue().getPetSn()).isEqualTo(PET_A);
        assertThat(rel.getValue().getMbrSn()).isEqualTo(RECEIVER);
        assertThat(rel.getValue().getBrupRoleCd()).isEqualTo(CO_PARENT);
        assertThat(rel.getValue().getRprsPetYn()).isEqualTo("N");

        ArgumentCaptor<BrupLinkSttsHstry> link = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        verify(petMapper).insertLinkHistory(link.capture());
        assertThat(link.getValue().getBrupRelSn()).isEqualTo(700L);
        assertThat(link.getValue().getMbrSn()).isEqualTo(RECEIVER);
        assertThat(link.getValue().getLinkChgTypeCd()).isEqualTo(LINK_ACCEPTED);

        ArgumentCaptor<JntBrupInvtnSttsHstry> history = ArgumentCaptor.forClass(JntBrupInvtnSttsHstry.class);
        verify(invitationMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(PENDING);
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(ACCEPTED);
    }

    @Test
    void 해제했던_반려동물을_다시_수락하면_기존_양육관계를_되살린다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));
        BrupRel unlinked = rel(PET_A, RECEIVER, CO_PARENT);
        unlinked.setBrupRelSn(600L);
        unlinked.setUseYn("N");
        when(petMapper.selectBrupRelIncludingUnlinked(RECEIVER, PET_A)).thenReturn(unlinked);

        service.acceptInvitation(RECEIVER, INVITATION, new CaregiverInvitationAcceptRequest(List.of(PET_A)));

        verify(petMapper).updateBrupRelRelink(600L, CO_PARENT, "2");
        verify(petMapper, never()).insertBrupRel(any());
        ArgumentCaptor<BrupLinkSttsHstry> link = ArgumentCaptor.forClass(BrupLinkSttsHstry.class);
        verify(petMapper).insertLinkHistory(link.capture());
        assertThat(link.getValue().getBrupRelSn()).isEqualTo(600L);
    }

    @Test
    void 이미_처리된_초대는_수락할_수_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(ACCEPTED, 3));

        assertThatThrownBy(() -> service.acceptInvitation(RECEIVER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        verify(invitationMapper, never()).updateResponse(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void 만료일시가_지난_초대는_수락할_수_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, -1));

        assertThatThrownBy(() -> service.acceptInvitation(RECEIVER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_EXPIRED);
        verify(petMapper, never()).insertBrupRel(any());
    }

    @Test
    void 초대_이메일과_다른_계정은_수락할_수_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));

        assertThatThrownBy(() -> service.acceptInvitation(OTHER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_EMAIL_MISMATCH);
        verify(invitationMapper, never()).updateResponse(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void 초대에_없는_반려동물은_수락할_수_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));

        assertThatThrownBy(() -> service.acceptInvitation(RECEIVER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A, 999L))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(invitationMapper, never()).updateResponse(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void 수락할_때_공동양육자가_5명이면_막는다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));
        when(petMapper.countLinkedCaregivers(PET_A, CO_PARENT)).thenReturn(5);

        assertThatThrownBy(() -> service.acceptInvitation(RECEIVER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_LIMIT_EXCEEDED);
        verify(petMapper, never()).insertBrupRel(any());
    }

    @Test
    void 동시에_먼저_응답한_쪽이_있으면_409() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));
        when(invitationMapper.updateResponse(INVITATION, RECEIVER, PENDING, ACCEPTED, "2")).thenReturn(0);

        assertThatThrownBy(() -> service.acceptInvitation(RECEIVER, INVITATION,
                new CaregiverInvitationAcceptRequest(List.of(PET_A))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        verify(petMapper, never()).insertBrupRel(any());
    }

    @Test
    void 거절은_초대의_반려동물_전체를_거절로_남긴다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(PENDING, 3));

        service.rejectInvitation(RECEIVER, INVITATION);

        verify(invitationMapper).updateResponse(INVITATION, RECEIVER, PENDING, REJECTED, "2");
        verify(invitationMapper).updateTargetPetsAcceptance(INVITATION, List.of());
        ArgumentCaptor<JntBrupInvtnSttsHstry> history = ArgumentCaptor.forClass(JntBrupInvtnSttsHstry.class);
        verify(invitationMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(REJECTED);
        verify(petMapper, never()).insertBrupRel(any());
    }

    @Test
    void 거절한_초대는_다시_거절하거나_수락할_수_없다() {
        when(invitationMapper.selectInvitation(INVITATION)).thenReturn(invitation(REJECTED, 3));

        assertThatThrownBy(() -> service.rejectInvitation(RECEIVER, INVITATION))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
    }

    private MemberResponse member(String email, String nickname) {
        return new MemberResponse(email, nickname, "01012345678", null, "USRC0001", true, List.of());
    }

    private Member memberVo(Long mbrSn) {
        Member member = new Member();
        member.setMbrSn(mbrSn);
        return member;
    }

    private Pet pet(Long petSn, String petNm) {
        Pet pet = new Pet();
        pet.setPetSn(petSn);
        pet.setPetNm(petNm);
        return pet;
    }

    private BrupRel rel(Long petSn, Long mbrSn, String roleCd) {
        BrupRel rel = new BrupRel();
        rel.setPetSn(petSn);
        rel.setMbrSn(mbrSn);
        rel.setBrupRoleCd(roleCd);
        rel.setUseYn("Y");
        return rel;
    }

    private CaregiverInvitationPetSummary petSummary(Long petSn, String petNm, String statusCd) {
        CaregiverInvitationPetSummary summary = new CaregiverInvitationPetSummary();
        summary.setPetSn(petSn);
        summary.setPetNm(petNm);
        summary.setInvtnSttsCd(statusCd);
        return summary;
    }

    /** 만료까지 남은 일수 : 음수면 이미 만료 */
    private JntBrupInvtn invitation(String statusCd, int daysLeft) {
        JntBrupInvtn invitation = new JntBrupInvtn();
        invitation.setJntBrupInvtnSn(INVITATION);
        invitation.setSndrMbrSn(SENDER);
        invitation.setInvtnSttsCd(statusCd);
        invitation.setInvtnTrgtEmlEnc("enc:" + TARGET_EMAIL);
        invitation.setInvtnTrgtEmlHmac("h:" + TARGET_EMAIL);
        invitation.setInvtnExpryDt(LocalDateTime.now().plusDays(daysLeft));
        return invitation;
    }
}
