package com.nyangmeong.pet.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
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
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptResult;
import com.nyangmeong.pet.dto.CaregiverInvitationConflictResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateResult;
import com.nyangmeong.pet.dto.CaregiverInvitationLinkedPet;
import com.nyangmeong.pet.dto.CaregiverInvitationPendingPet;
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
    private final SystemSettingService systemSettingService;
    private final String frontBaseUrl;

    public CaregiverInvitationService(CaregiverInvitationMapper invitationMapper, PetMapper petMapper,
            MemberService memberService, FieldCryptoService fieldCryptoService, TokenHashUtil tokenHashUtil,
            EventPublisher eventPublisher, SystemSettingService systemSettingService,
            @Value("${app.front-base-url:http://localhost:5173}") String frontBaseUrl) {
        this.invitationMapper = invitationMapper;
        this.petMapper = petMapper;
        this.memberService = memberService;
        this.fieldCryptoService = fieldCryptoService;
        this.tokenHashUtil = tokenHashUtil;
        this.eventPublisher = eventPublisher;
        this.systemSettingService = systemSettingService;
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
        /** 고른 동물을 모두 검사해 충돌을 모은다. 하나라도 있으면 초대를 만들지 않는다 */
        List<CaregiverInvitationLinkedPet> linkedPets = new ArrayList<>();
        List<CaregiverInvitationPendingPet> pendingPets = new ArrayList<>();
        for (Pet pet : pets) {
            if (receiver != null && petMapper.selectBrupRel(receiver.getMbrSn(), pet.getPetSn()) != null) {
                linkedPets.add(new CaregiverInvitationLinkedPet(pet.getPetSn(), pet.getPetNm()));
                continue;
            }
            LocalDateTime pendingExpryDt = invitationMapper.selectPendingExpryDt(targetHmac, pet.getPetSn(),
                    InvitationStatus.PENDING.getCode(), now);
            if (pendingExpryDt != null) {
                pendingPets.add(new CaregiverInvitationPendingPet(pet.getPetSn(), pet.getPetNm(), pendingExpryDt));
            }
        }
        if (!linkedPets.isEmpty() || !pendingPets.isEmpty()) {
            ErrorCode error = linkedPets.isEmpty() ? ErrorCode.CAREGIVER_INVITATION_DUPLICATED
                    : ErrorCode.CAREGIVER_ALREADY_LINKED;
            return CaregiverInvitationCreateResult.fail(error,
                    new CaregiverInvitationConflictResponse(linkedPets, pendingPets));
        }
        SystemSetting setting = systemSettingService.get();
        for (Pet pet : pets) {
            checkCaregiverLimit(pet.getPetSn(), setting);
        }

        JntBrupInvtn invitation = new JntBrupInvtn();
        invitation.setSndrMbrSn(mbrSn);
        invitation.setRcvrMbrSn(receiver != null ? receiver.getMbrSn() : null);
        invitation.setInvtnSttsCd(InvitationStatus.PENDING.getCode());
        invitation.setInvtnTrgtEmlEnc(fieldCryptoService.encrypt(targetEmail));
        invitation.setInvtnTrgtEmlHmac(targetHmac);
        invitation.setRegDt(now);
        /** 초대 유효 기간 : 운영 설정 (FN-MYP-005-04). 이미 보낸 초대의 만료일은 바뀌지 않는다 */
        invitation.setInvtnExpryDt(now.plusDays(setting.getInvtnVldDayCnt()));
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
        int remainingPetCount = PetLimit.remaining(systemSettingService.get().getPetMaxCnt().intValue(),
                petMapper.countLinkedPets(mbrSn));
        return new CaregiverInvitationResponse(jntBrupInvtnSn, sndrMbrNcnm, status.getCode(),
                invitation.getInvtnExpryDt(), emailMask, true, loadPets(jntBrupInvtnSn, now), remainingPetCount);
    }

    /**
     * 공동양육 초대 수락
     */
    @Transactional
    public CaregiverInvitationAcceptResult acceptInvitation(Long mbrSn, Long jntBrupInvtnSn,
            CaregiverInvitationAcceptRequest req) {
        JntBrupInvtn invitation = loadInvitation(jntBrupInvtnSn);
        checkRespondable(mbrSn, invitation);

        List<Long> acceptedPetSns = req.petSns().stream().distinct().toList();
        if (!invitationMapper.selectTargetPetSns(jntBrupInvtnSn).containsAll(acceptedPetSns)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "petSns");
        }
        /** 한도는 응답 기록·연동보다 먼저 : 넘으면 아무것도 바꾸지 않는다. 같은 회원의 동시 등록·수락은 회원 행 잠금으로 줄 세운다 */
        petMapper.lockMemberForPetLimit(mbrSn);
        SystemSetting setting = systemSettingService.get();
        int maxPetCount = setting.getPetMaxCnt().intValue();
        int linkedCount = petMapper.countLinkedPets(mbrSn);
        if (linkedCount + acceptedPetSns.size() > maxPetCount) {
            return CaregiverInvitationAcceptResult.limitExceeded(PetLimit.message(maxPetCount),
                    PetLimit.remaining(maxPetCount, linkedCount));
        }
        respond(mbrSn, jntBrupInvtnSn, InvitationStatus.ACCEPTED, acceptedPetSns);

        String actorId = String.valueOf(mbrSn);
        for (Long petSn : acceptedPetSns) {
            loadPet(petSn);
            if (petMapper.selectBrupRel(mbrSn, petSn) != null) {
                throw new CustomException(ErrorCode.CAREGIVER_ALREADY_LINKED);
            }
            checkCaregiverLimit(petSn, setting);
            Long brupRelSn = link(mbrSn, petSn, actorId);

            BrupLinkSttsHstry history = new BrupLinkSttsHstry();
            history.setBrupRelSn(brupRelSn);
            history.setMbrSn(mbrSn);
            history.setLinkChgTypeCd(LinkChangeType.INVITATION_ACCEPTED.getCode());
            petMapper.insertLinkHistory(history);
        }
        return CaregiverInvitationAcceptResult.success();
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
     * 보낸 공동양육 초대 취소 (대기 중인 초대만)
     * - 받는 사람 알림은 새로 보내지 않는다. 알림을 눌러 상세를 열면 취소 상태로 보인다
     */
    @Transactional
    public void cancelInvitation(Long mbrSn, Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = loadSentInvitation(mbrSn, jntBrupInvtnSn);
        InvitationStatus status = effectiveStatus(invitation, LocalDateTime.now());
        if (status == InvitationStatus.CANCELED) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_CANCELED);
        }
        if (status == InvitationStatus.EXPIRED) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_EXPIRED);
        }
        if (status != InvitationStatus.PENDING) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        }
        /** 동시에 응답했거나 만료된 경우 먼저 바꾼 쪽만 */
        if (invitationMapper.updateCancel(jntBrupInvtnSn, mbrSn, InvitationStatus.PENDING.getCode(),
                InvitationStatus.CANCELED.getCode(), LocalDateTime.now(), String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_ALREADY_PROCESSED);
        }
        insertHistory(jntBrupInvtnSn, mbrSn, InvitationStatus.PENDING, InvitationStatus.CANCELED);
    }

    /**
     * 취소한 공동양육 초대 삭제 (보낸 목록에서 숨김, 이력은 남긴다)
     */
    @Transactional
    public void deleteInvitation(Long mbrSn, Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = loadSentInvitation(mbrSn, jntBrupInvtnSn);
        if (!InvitationStatus.CANCELED.is(invitation.getInvtnSttsCd())) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_NOT_CANCELED);
        }
        if (invitationMapper.updateDelete(jntBrupInvtnSn, mbrSn, InvitationStatus.CANCELED.getCode(),
                String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_NOT_FOUND);
        }
    }

    /**
     * 내가 보낸 초대 조회 (삭제된 초대는 없는 초대)
     */
    private JntBrupInvtn loadSentInvitation(Long mbrSn, Long jntBrupInvtnSn) {
        JntBrupInvtn invitation = loadInvitation(jntBrupInvtnSn);
        if (!mbrSn.equals(invitation.getSndrMbrSn())) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_NOT_SENDER);
        }
        return invitation;
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
    private void checkCaregiverLimit(Long petSn, SystemSetting setting) {
        int max = setting.getJntBrupMaxNope().intValue();
        if (petMapper.countLinkedCaregivers(petSn, BrupRole.CO_PARENT.getCode()) >= max) {
            throw CustomException.withMessage(ErrorCode.CAREGIVER_LIMIT_EXCEEDED,
                    ErrorCode.CAREGIVER_LIMIT_EXCEEDED.message().formatted(max));
        }
    }

    /**
     * 수락·거절 가능 여부 검사
     */
    private void checkRespondable(Long mbrSn, JntBrupInvtn invitation) {
        InvitationStatus status = effectiveStatus(invitation, LocalDateTime.now());
        if (status == InvitationStatus.CANCELED) {
            throw new CustomException(ErrorCode.CAREGIVER_INVITATION_CANCELED);
        }
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
