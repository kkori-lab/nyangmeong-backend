package com.nyangmeong.pet.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.nyangmeong.pet.domain.InvitationStatus;
import com.nyangmeong.pet.domain.InvtnTrgtPet;
import com.nyangmeong.pet.domain.JntBrupInvtn;
import com.nyangmeong.pet.domain.JntBrupInvtnSttsHstry;
import com.nyangmeong.pet.domain.LinkChangeType;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationConflictResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateResult;
import com.nyangmeong.pet.dto.CaregiverInvitationPetSummary;
import com.nyangmeong.pet.dto.CaregiverInvitationResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationSummary;
import com.nyangmeong.pet.mapper.CaregiverInvitationMapper;
import com.nyangmeong.pet.mapper.PetMapper;

/**
 * [공동양육 초대 서비스]
 * - 이메일 초대 발송·보낸 초대·받은 초대 상세·수락·거절 (SCR-MYP-005)
 * - 초대는 반려동물 단위가 아니라 PetAuthorizer 대신 반려동물마다 주양육자·5명 상한 검사 (docs/api/issues.md API 5번)
 * - 링크는 초대 번호 기준, 수락 권한은 로그인 계정 이메일 일치로 판단
 */
@Service
public class CaregiverInvitationService {

    /** 초대 유효 기간 (FN-MYP-005-04) */
    private static final int VALID_DAYS = 7;
    /** 반려동물당 공동양육자 상한 */
    private static final int MAX_CAREGIVER_COUNT = 5;
    private static final int TOKEN_BYTES = 32;
    private static final String INVITATION_PATH = "/mypage/invitations/";
    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaregiverInvitationMapper invitationMapper;
    private final PetMapper petMapper;
    private final MemberService memberService;
    private final FieldCryptoService fieldCryptoService;
    private final TokenHashUtil tokenHashUtil;
    private final EventPublisher eventPublisher;
    private final String frontBaseUrl;

    public CaregiverInvitationService(CaregiverInvitationMapper invitationMapper, PetMapper petMapper,
            MemberService memberService, FieldCryptoService fieldCryptoService, TokenHashUtil tokenHashUtil,
            EventPublisher eventPublisher, @Value("${app.front-base-url:http://localhost:5173}") String frontBaseUrl) {
        this.invitationMapper = invitationMapper;
        this.petMapper = petMapper;
        this.memberService = memberService;
        this.fieldCryptoService = fieldCryptoService;
        this.tokenHashUtil = tokenHashUtil;
        this.eventPublisher = eventPublisher;
        this.frontBaseUrl = frontBaseUrl;
    }

    /**
     * 공동양육 초대 발송
     */
    @Transactional
    public CaregiverInvitationCreateResult createInvitation(Long mbrSn, CaregiverInvitationCreateRequest req) {
        List<Long> petSns = req.petSns().stream().distinct().toList();
        List<Pet> pets = petSns.stream().map(petSn -> loadPrimaryPet(mbrSn, petSn)).toList();

        MemberResponse sender = memberService.getMember(mbrSn);
        String targetEmail = fieldCryptoService.normalizeEmail(req.invtnTrgtEml());
        String targetHmac = fieldCryptoService.emailHmac(targetEmail);
        if (targetHmac.equals(fieldCryptoService.emailHmac(sender.acntEml()))) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_SELF);
        }

        Member receiver = memberService.findForLogin(targetHmac);
        LocalDateTime now = LocalDateTime.now();
        for (Pet pet : pets) {
            if (receiver != null && petMapper.selectBrupRel(receiver.getMbrSn(), pet.getPetSn()) != null) {
                return CaregiverInvitationCreateResult.fail(ErrorCode.CAREGIVER_ALREADY_LINKED,
                        new CaregiverInvitationConflictResponse(pet.getPetSn(), pet.getPetNm(), null));
            }
            LocalDateTime pendingExpryDt = invitationMapper.selectPendingExpryDt(targetHmac, pet.getPetSn(),
                    InvitationStatus.PENDING.getCode(), now);
            if (pendingExpryDt != null) {
                return CaregiverInvitationCreateResult.fail(ErrorCode.CAREGIVER_INVITATION_DUPLICATED,
                        new CaregiverInvitationConflictResponse(pet.getPetSn(), pet.getPetNm(), pendingExpryDt));
            }
            checkCaregiverLimit(pet.getPetSn());
        }

        JntBrupInvtn invitation = new JntBrupInvtn();
        invitation.setSndrMbrSn(mbrSn);
        invitation.setRcvrMbrSn(receiver != null ? receiver.getMbrSn() : null);
        invitation.setInvtnSttsCd(InvitationStatus.PENDING.getCode());
        invitation.setInvtnTrgtEmlEnc(fieldCryptoService.encrypt(targetEmail));
        invitation.setInvtnTrgtEmlHmac(targetHmac);
        invitation.setRegDt(now);
        invitation.setInvtnExpryDt(now.plusDays(VALID_DAYS));
        invitation.setInvtnTknHash(tokenHashUtil.hash(createToken()));
        invitation.setMdfrId(String.valueOf(mbrSn));
        invitationMapper.insertInvitation(invitation);
        for (Long petSn : petSns) {
            InvtnTrgtPet targetPet = new InvtnTrgtPet();
            targetPet.setJntBrupInvtnSn(invitation.getJntBrupInvtnSn());
            targetPet.setPetSn(petSn);
            invitationMapper.insertTargetPet(targetPet);
        }
        insertHistory(invitation.getJntBrupInvtnSn(), mbrSn, null, InvitationStatus.PENDING);

        publishInvitation(invitation, receiver, sender.mbrNcnm(), targetEmail, pets);

        List<CaregiverInvitationPetSummary> petSummaries = loadPets(invitation.getJntBrupInvtnSn(), now);
        return CaregiverInvitationCreateResult.success(new CaregiverInvitationResponse(
                invitation.getJntBrupInvtnSn(), sender.mbrNcnm(), invitation.getInvtnSttsCd(),
                invitation.getInvtnExpryDt(), maskEmail(targetEmail), false, petSummaries));
    }

    /**
     * 내가 보낸 초대 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CaregiverInvitationSummary> searchSentInvitations(Long mbrSn) {
        List<CaregiverInvitationSummary> invitations = invitationMapper.selectSentList(mbrSn,
                InvitationStatus.PENDING.getCode(), InvitationStatus.ACCEPTED.getCode(),
                InvitationStatus.REJECTED.getCode(), InvitationStatus.EXPIRED.getCode(), LocalDateTime.now());
        invitations.forEach(invitation -> invitation.setInvtnTrgtEml(
                fieldCryptoService.decrypt(invitation.getInvtnTrgtEml())));
        return invitations;
    }

    /**
     * 받은 초대 상세 조회
     */
    @Transactional(readOnly = true)
    public CaregiverInvitationResponse getInvitation(Long mbrSn, Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = loadInvitation(jntBrupInvtnSn);
        LocalDateTime now = LocalDateTime.now();
        InvitationStatus status = effectiveStatus(invitation, now);
        boolean emailMatched = isInvitedMember(mbrSn, invitation);
        String emailMask = maskEmail(fieldCryptoService.decrypt(invitation.getInvtnTrgtEmlEnc()));

        /** 만료·처리 여부를 이메일 일치보다 먼저 (FN-MYP-005-10) */
        if (status != InvitationStatus.PENDING || !emailMatched) {
            return new CaregiverInvitationResponse(jntBrupInvtnSn, null, status.getCode(),
                    invitation.getInvtnExpryDt(), emailMask, emailMatched, null);
        }
        String sndrMbrNcnm = memberService.getMember(invitation.getSndrMbrSn()).mbrNcnm();
        return new CaregiverInvitationResponse(jntBrupInvtnSn, sndrMbrNcnm, status.getCode(),
                invitation.getInvtnExpryDt(), emailMask, true, loadPets(jntBrupInvtnSn, now));
    }

    /**
     * 공동양육 초대 수락
     */
    @Transactional
    public void acceptInvitation(Long mbrSn, Long jntBrupInvtnSn, CaregiverInvitationAcceptRequest req) {
        JntBrupInvtn invitation = loadInvitation(jntBrupInvtnSn);
        checkRespondable(mbrSn, invitation);

        List<Long> acceptedPetSns = req.petSns().stream().distinct().toList();
        if (!invitationMapper.selectTargetPetSns(jntBrupInvtnSn).containsAll(acceptedPetSns)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        respond(mbrSn, jntBrupInvtnSn, InvitationStatus.ACCEPTED, acceptedPetSns);

        String actorId = String.valueOf(mbrSn);
        for (Long petSn : acceptedPetSns) {
            loadPet(petSn);
            if (petMapper.selectBrupRel(mbrSn, petSn) != null) {
                throw new CustomException(ErrorCode.CAREGIVER_ALREADY_LINKED);
            }
            checkCaregiverLimit(petSn);
            Long brupRelSn = link(mbrSn, petSn, actorId);

            BrupLinkSttsHstry history = new BrupLinkSttsHstry();
            history.setBrupRelSn(brupRelSn);
            history.setMbrSn(mbrSn);
            history.setLinkChgTypeCd(LinkChangeType.INVITATION_ACCEPTED.getCode());
            petMapper.insertLinkHistory(history);
        }
    }

    /**
     * 공동양육 초대 거절
     */
    @Transactional
    public void rejectInvitation(Long mbrSn, Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = loadInvitation(jntBrupInvtnSn);
        checkRespondable(mbrSn, invitation);
        respond(mbrSn, jntBrupInvtnSn, InvitationStatus.REJECTED, List.of());
    }

    /**
     * 초대 조회
     */
    private JntBrupInvtn loadInvitation(Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = invitationMapper.selectInvitation(jntBrupInvtnSn);
        if (invitation == null) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_NOT_FOUND);
        }
        return invitation;
    }

    /**
     * 삭제되지 않은 반려동물 조회
     */
    private Pet loadPet(Long petSn) {
        Pet pet = petMapper.selectPet(petSn);
        if (pet == null) {
            throw new CustomException(ErrorCode.PET_NOT_FOUND);
        }
        return pet;
    }

    /**
     * 내가 주양육자인 반려동물 조회
     */
    private Pet loadPrimaryPet(Long mbrSn, Long petSn) {
        Pet pet = loadPet(petSn);
        BrupRel rel = petMapper.selectBrupRel(mbrSn, petSn);
        if (rel == null || !BrupRole.PRIMARY.is(rel.getBrupRoleCd())) {
            throw new CustomException(ErrorCode.PET_PRIMARY_ONLY);
        }
        return pet;
    }

    /**
     * 공동양육자 상한 검사
     */
    private void checkCaregiverLimit(Long petSn) {
        if (petMapper.countLinkedCaregivers(petSn, BrupRole.CO_PARENT.getCode()) >= MAX_CAREGIVER_COUNT) {
            throw new CustomException(ErrorCode.CAREGIVER_LIMIT_EXCEEDED);
        }
    }

    /**
     * 수락·거절 가능 여부 검사
     */
    private void checkRespondable(Long mbrSn, JntBrupInvtn invitation) {
        InvitationStatus status = effectiveStatus(invitation, LocalDateTime.now());
        if (status == InvitationStatus.ACCEPTED || status == InvitationStatus.REJECTED) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        }
        if (status == InvitationStatus.EXPIRED) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_EXPIRED);
        }
        if (!isInvitedMember(mbrSn, invitation)) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_EMAIL_MISMATCH);
        }
    }

    /**
     * 초대 응답 저장
     */
    private void respond(Long mbrSn, Long jntBrupInvtnSn, InvitationStatus status, List<Long> acceptedPetSns) {
        /** 동시에 응답한 경우 먼저 바꾼 쪽만 */
        if (invitationMapper.updateResponse(jntBrupInvtnSn, mbrSn, InvitationStatus.PENDING.getCode(),
                status.getCode(), String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        }
        invitationMapper.updateTargetPetsAcceptance(jntBrupInvtnSn, acceptedPetSns);
        insertHistory(jntBrupInvtnSn, mbrSn, InvitationStatus.PENDING, status);
    }

    /**
     * 공동양육 연동
     * - BRUP_REL (PET_SN, MBR_SN) 유일 : 해제했던 행은 되살림
     */
    private Long link(Long mbrSn, Long petSn, String actorId) {
        BrupRel unlinked = petMapper.selectBrupRelIncludingUnlinked(mbrSn, petSn);
        if (unlinked != null) {
            petMapper.updateBrupRelRelink(unlinked.getBrupRelSn(), BrupRole.CO_PARENT.getCode(), actorId);
            return unlinked.getBrupRelSn();
        }
        BrupRel rel = new BrupRel();
        rel.setPetSn(petSn);
        rel.setMbrSn(mbrSn);
        rel.setBrupRoleCd(BrupRole.CO_PARENT.getCode());
        rel.setRprsPetYn("N");
        rel.setMdfrId(actorId);
        petMapper.insertBrupRel(rel);
        return rel.getBrupRelSn();
    }

    /**
     * 초대 상태 이력 등록
     */
    private void insertHistory(Long jntBrupInvtnSn, Long mbrSn, InvitationStatus before, InvitationStatus after) {
        JntBrupInvtnSttsHstry history = new JntBrupInvtnSttsHstry();
        history.setJntBrupInvtnSn(jntBrupInvtnSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before != null ? before.getCode() : null);
        history.setAftrSttsCd(after.getCode());
        invitationMapper.insertStatusHistory(history);
    }

    /**
     * 초대 알림·메일 발행
     * - 주소로 보내는 메일 이벤트 : CertificationMailEvent 뿐이라 함께 씀
     */
    private void publishInvitation(JntBrupInvtn invitation, Member receiver, String sndrMbrNcnm,
            String targetEmail, List<Pet> pets) {
        String petNames = pets.stream().map(Pet::getPetNm).collect(Collectors.joining(", "));
        String path = INVITATION_PATH + invitation.getJntBrupInvtnSn();
        String title = petNames + "의 공동양육자가 되어주세요";
        if (receiver != null) {
            eventPublisher.publish(new NotificationEvent(receiver.getMbrSn(), NotificationType.CO_PARENTING, title,
                    sndrMbrNcnm + "님이 초대했어요", path));
            return;
        }
        String content = sndrMbrNcnm + "님이 " + petNames + "의 공동양육자로 초대했어요.\n"
                + "냥멍케어에 가입한 뒤 이 이메일 계정으로 로그인하면 초대를 받을 수 있어요.\n\n"
                + "초대 확인하기 : " + frontBaseUrl + path + "\n"
                + "초대는 " + invitation.getInvtnExpryDt().format(EXPIRY_FORMAT) + "까지 받을 수 있어요.";
        eventPublisher.publish(new CertificationMailEvent(targetEmail, "[냥멍케어] " + title, content));
    }

    /**
     * 초대 대상 반려동물 조회
     */
    private List<CaregiverInvitationPetSummary> loadPets(Long jntBrupInvtnSn, LocalDateTime now) {
        return invitationMapper.selectInvitationPets(jntBrupInvtnSn, InvitationStatus.PENDING.getCode(),
                InvitationStatus.ACCEPTED.getCode(), InvitationStatus.REJECTED.getCode(),
                InvitationStatus.EXPIRED.getCode(), now);
    }

    /**
     * 만료 반영 상태
     */
    private InvitationStatus effectiveStatus(JntBrupInvtn invitation, LocalDateTime now) {
        for (InvitationStatus status : InvitationStatus.values()) {
            if (status.is(invitation.getInvtnSttsCd())) {
                if (status == InvitationStatus.PENDING && !invitation.getInvtnExpryDt().isAfter(now)) {
                    return InvitationStatus.EXPIRED;
                }
                return status;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 로그인 계정 이메일과 초대 이메일 일치 여부
     */
    private boolean isInvitedMember(Long mbrSn, JntBrupInvtn invitation) {
        String myHmac = fieldCryptoService.emailHmac(memberService.getMember(mbrSn).acntEml());
        return invitation.getInvtnTrgtEmlHmac().equals(myHmac);
    }

    /**
     * 아이디 첫 글자와 도메인만 남긴 이메일 (FN-MYP-005-09)
     */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 초대 토큰 원문
     */
    private String createToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
