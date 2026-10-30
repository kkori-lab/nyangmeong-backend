package com.nyangmeong.pet.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.BrupLinkSttsHstry;
import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.LinkChangeType;
import com.nyangmeong.pet.domain.Pet;
import com.nyangmeong.pet.domain.PetCodeGroup;
import com.nyangmeong.pet.domain.PetColor;
import com.nyangmeong.pet.dto.CaregiverSummary;
import com.nyangmeong.pet.dto.PetCreateRequest;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.dto.PetUpdateRequest;
import com.nyangmeong.pet.mapper.PetMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 서비스]
 * - 내 반려동물 목록 (SCR-MYP-001·002), 다른 도메인도 이 목록을 씀
 * - 등록·수정·상세·대표 지정 (SCR-MYP-003, FN-MYP-010-05)
 * - 양육자 목록·연동 해제 (SCR-MYP-004·006), 연동 양육자 회원 번호는 다른 도메인도 씀
 * - 삭제 (SCR-MYP-002) : 위층 데이터(건강기록·AI 대화)는 PET.DEL_YN 으로 가려지므로 건드리지 않음 (docs/api/issues.md 마이페이지 3번)
 * - 정렬 = 내 양육관계 등록순 (docs/api/issues.md 마이페이지 1번 결정 전까지)
 */
@Service
@RequiredArgsConstructor
public class PetService {

    /** 계정당 주양육 반려동물 상한 */
    private static final int MAX_PET_COUNT = 10;

    /** 삭제 해제 알림 이동 경로 */
    private static final String PET_LIST_PATH = "/mypage/pets";

    private final PetMapper petMapper;
    private final PetAuthorizer petAuthorizer;
    private final CommonCodeService commonCodeService;
    private final PixelCharacterMapper pixelCharacterMapper;
    private final EventPublisher eventPublisher;

    /**
     * 내 반려동물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PetSummary> searchMyPets(Long mbrSn) {
        return petMapper.selectList(mbrSn, BrupRole.CO_PARENT.getCode());
    }

    /**
     * 반려동물 상세 조회
     */
    @Transactional(readOnly = true)
    public PetResponse getPet(Long mbrSn, Long petSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return toResponse(mbrSn, loadPet(petSn));
    }

    /**
     * 반려동물 등록
     */
    @Transactional
    public PetResponse createPet(Long mbrSn, PetCreateRequest req) {
        int primaryCount = petMapper.countPrimaryPets(mbrSn, BrupRole.PRIMARY.getCode());
        if (primaryCount >= MAX_PET_COUNT) {
            throw new CustomException(ErrorCode.PET_LIMIT_EXCEEDED);
        }

        Pet pet = new Pet();
        pet.setPetNm(req.petNm());
        pet.setPetSeCd(req.petSeCd());
        pet.setVrtySn(req.vrtySn());
        pet.setVrtyDrctInptNm(emptyToNull(req.vrtyDrctInptNm()));
        pet.setPetBrdt(req.petBrdt());
        pet.setChipNo(emptyToNull(req.chipNo()));
        pet.setPetGndrCd(req.petGndrCd());
        pet.setNutrYn(Boolean.TRUE.equals(req.nutrYn()) ? "Y" : "N");
        pet.setPetBdwg(req.petBdwg());
        pet.setGoalBdwg(req.goalBdwg());
        pet.setMinBdwg(req.minBdwg());
        pet.setMaxBdwg(req.maxBdwg());
        pet.setColrCd(req.colrCd());
        pet.setColrDrctInptNm(emptyToNull(req.colrDrctInptNm()));
        pet.setChrcCn(emptyToNull(req.chrcCn()));
        pet.setAlgyCn(normalizeList(req.algyCn()));
        pet.setDssCn(normalizeList(req.dssCn()));
        checkPet(pet);

        String actorId = String.valueOf(mbrSn);
        pet.setRgtrId(actorId);
        pet.setMdfrId(actorId);
        petMapper.insertPet(pet);

        BrupRel rel = new BrupRel();
        rel.setPetSn(pet.getPetSn());
        rel.setMbrSn(mbrSn);
        rel.setBrupRoleCd(BrupRole.PRIMARY.getCode());
        /** 첫 반려동물 = 대표 (FN-MYP-010-05) */
        rel.setRprsPetYn(primaryCount == 0 ? "Y" : "N");
        rel.setMdfrId(actorId);
        petMapper.insertBrupRel(rel);

        return toResponse(mbrSn, loadPet(pet.getPetSn()));
    }

    /**
     * 반려동물 수정
     */
    @Transactional
    public PetResponse updatePet(Long mbrSn, Long petSn, PetUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        Pet pet = loadPet(petSn);

        if (req.petNm() != null) {
            pet.setPetNm(req.petNm());
        }
        if (req.petSeCd() != null) {
            pet.setPetSeCd(req.petSeCd());
        }
        if (req.vrtySn() != null && hasText(req.vrtyDrctInptNm())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (req.vrtySn() != null) {
            pet.setVrtySn(req.vrtySn());
            pet.setVrtyDrctInptNm(null);
        }
        if (req.vrtyDrctInptNm() != null) {
            pet.setVrtyDrctInptNm(emptyToNull(req.vrtyDrctInptNm()));
            if (pet.getVrtyDrctInptNm() != null) {
                pet.setVrtySn(null);
            }
        }
        if (req.petBrdt() != null) {
            pet.setPetBrdt(req.petBrdt());
        }
        if (req.chipNo() != null) {
            pet.setChipNo(emptyToNull(req.chipNo()));
        }
        if (req.petGndrCd() != null) {
            pet.setPetGndrCd(req.petGndrCd());
        }
        if (req.nutrYn() != null) {
            pet.setNutrYn(req.nutrYn() ? "Y" : "N");
        }
        if (req.petBdwg() != null) {
            pet.setPetBdwg(req.petBdwg());
        }
        if (req.goalBdwg() != null) {
            pet.setGoalBdwg(req.goalBdwg());
        }
        if (req.minBdwg() != null) {
            pet.setMinBdwg(req.minBdwg());
        }
        if (req.maxBdwg() != null) {
            pet.setMaxBdwg(req.maxBdwg());
        }
        if (req.colrCd() != null) {
            pet.setColrCd(req.colrCd());
        }
        if (req.colrDrctInptNm() != null) {
            pet.setColrDrctInptNm(emptyToNull(req.colrDrctInptNm()));
        }
        if (req.chrcCn() != null) {
            pet.setChrcCn(emptyToNull(req.chrcCn()));
        }
        if (req.algyCn() != null) {
            pet.setAlgyCn(normalizeList(req.algyCn()));
        }
        if (req.dssCn() != null) {
            pet.setDssCn(normalizeList(req.dssCn()));
        }
        checkPet(pet);

        petMapper.updatePet(pet, String.valueOf(mbrSn));
        return toResponse(mbrSn, loadPet(petSn));
    }

    /**
     * 대표 반려동물 지정
     */
    @Transactional
    public void designateRepresentative(Long mbrSn, Long petSn) {
        /** 공동양육 동물은 대표 불가 (FN-MYP-010-05) */
        petAuthorizer.checkPrimary(mbrSn, petSn);
        String actorId = String.valueOf(mbrSn);
        petMapper.updateRepresentativeOff(mbrSn, actorId);
        petMapper.updateRepresentativeOn(mbrSn, petSn, actorId);
    }

    /**
     * 반려동물 삭제
     * - 주양육자만 (FN-MYP-002-05), 캐릭터·보유 아이템 함께 삭제, 지출은 유지 (FN-MYP-002-07)
     * - 공동양육 연동 자동 해제·알림 (FN-MYP-002-08), 대표였으면 남은 주양육 동물 첫 마리로 승계
     */
    @Transactional
    public void deletePet(Long mbrSn, Long petSn) {
        petAuthorizer.checkPrimary(mbrSn, petSn);
        Pet pet = loadPet(petSn);
        BrupRel own = petMapper.selectBrupRel(mbrSn, petSn);
        String actorId = String.valueOf(mbrSn);

        if (petMapper.updatePetDelete(petSn, actorId) == 0) {
            throw new CustomException(ErrorCode.PET_NOT_FOUND);
        }
        pixelCharacterMapper.updateHeldItemDeleteByPetSn(petSn);
        pixelCharacterMapper.updateDeleteByPetSn(petSn, actorId);

        for (BrupRel coParent : petMapper.selectLinkedCoParentRels(petSn, BrupRole.CO_PARENT.getCode())) {
            if (petMapper.updateBrupRelUnlink(coParent.getBrupRelSn(), actorId) == 0) {
                continue;
            }
            BrupLinkSttsHstry history = new BrupLinkSttsHstry();
            history.setBrupRelSn(coParent.getBrupRelSn());
            history.setMbrSn(coParent.getMbrSn());
            history.setLinkChgTypeCd(LinkChangeType.PET_DELETED.getCode());
            petMapper.insertLinkHistory(history);
            eventPublisher.publish(new NotificationEvent(coParent.getMbrSn(), NotificationType.CO_PARENTING,
                    pet.getPetNm() + "의 기록이 정리되어 공동양육 연동이 해제되었어요", null, PET_LIST_PATH));
        }

        if (own != null && "Y".equals(own.getRprsPetYn())) {
            petMapper.updateRepresentativeOff(mbrSn, actorId);
            searchMyPets(mbrSn).stream()
                    .filter(summary -> BrupRole.PRIMARY.is(summary.getBrupRoleCd()))
                    .findFirst()
                    .ifPresent(next -> petMapper.updateRepresentativeOn(mbrSn, next.getPetSn(), actorId));
        }
    }

    /**
     * 양육자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CaregiverSummary> searchCaregivers(Long mbrSn, Long petSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        BrupRel rel = petMapper.selectBrupRel(mbrSn, petSn);
        return visibleCaregivers(mbrSn, petSn, rel.getBrupRoleCd());
    }

    /**
     * 공동양육 연동 해제
     * - 주양육자 = 강제 해제 (FN-MYP-004-01), 공동양육자 = 본인만 자진 해제 (FN-MYP-006-02)
     */
    @Transactional
    public void unlinkCaregiver(Long mbrSn, Long petSn, Long brupRelSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        BrupRel target = petMapper.selectBrupRelBySn(brupRelSn);
        /** 주양육자 행은 해제 대상 아님 */
        if (target == null || !petSn.equals(target.getPetSn()) || !BrupRole.CO_PARENT.is(target.getBrupRoleCd())) {
            throw new CustomException(ErrorCode.CAREGIVER_NOT_FOUND);
        }

        LinkChangeType type;
        if (BrupRole.PRIMARY.is(petMapper.selectBrupRel(mbrSn, petSn).getBrupRoleCd())) {
            type = LinkChangeType.FORCED_UNLINKED;
        } else if (mbrSn.equals(target.getMbrSn())) {
            type = LinkChangeType.SELF_UNLINKED;
        } else {
            throw new CustomException(ErrorCode.PET_PRIMARY_ONLY);
        }

        if (petMapper.updateBrupRelUnlink(brupRelSn, String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.CAREGIVER_NOT_FOUND);
        }
        BrupLinkSttsHstry history = new BrupLinkSttsHstry();
        history.setBrupRelSn(brupRelSn);
        history.setMbrSn(target.getMbrSn());
        history.setLinkChgTypeCd(type.getCode());
        petMapper.insertLinkHistory(history);
    }

    /**
     * 연동 양육자 회원 번호 조회
     * - 주양육자 + 연동 중 공동양육자, 삭제된 반려동물은 빈 목록
     */
    @Transactional(readOnly = true)
    public List<Long> searchCaregiverMbrSns(Long petSn) {
        return petMapper.selectCaregiverMbrSns(petSn);
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
     * 상세 응답 조립
     */
    private PetResponse toResponse(Long mbrSn, Pet pet) {
        BrupRel rel = petMapper.selectBrupRel(mbrSn, pet.getPetSn());
        List<CaregiverSummary> caregivers = visibleCaregivers(mbrSn, pet.getPetSn(), rel.getBrupRoleCd());
        String vrtyNm = pet.getVrtySn() != null ? petMapper.selectVrtyNm(pet.getVrtySn()) : null;
        return PetResponse.from(pet, vrtyNm, rel, caregivers);
    }

    /**
     * 역할별로 보이는 양육자 목록
     * - 공동양육자에게는 주양육자와 본인만, 서로 모르는 사이일 수 있음 (FN-MYP-004-04)
     */
    private List<CaregiverSummary> visibleCaregivers(Long mbrSn, Long petSn, String myRoleCd) {
        List<CaregiverSummary> caregivers = petMapper.selectCaregivers(petSn, BrupRole.PRIMARY.getCode(),
                LinkChangeType.INVITATION_ACCEPTED.getCode());
        if (BrupRole.PRIMARY.is(myRoleCd)) {
            return caregivers;
        }
        return caregivers.stream()
                .filter(c -> BrupRole.PRIMARY.is(c.getBrupRoleCd()) || mbrSn.equals(c.getMbrSn()))
                .toList();
    }

    /**
     * 저장 직전 값 검사
     */
    private void checkPet(Pet pet) {
        checkCode(PetCodeGroup.PET_SE, pet.getPetSeCd());
        checkCode(PetCodeGroup.GENDER, pet.getPetGndrCd());
        checkCode(PetCodeGroup.COLOR, pet.getColrCd());

        if ((pet.getVrtySn() == null) == (pet.getVrtyDrctInptNm() == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (pet.getVrtySn() != null && !petMapper.existsVrty(pet.getVrtySn(), pet.getPetSeCd())) {
            throw new CustomException(ErrorCode.BREED_NOT_FOUND);
        }

        if (PetColor.DIRECT_INPUT.is(pet.getColrCd())) {
            if (pet.getColrDrctInptNm() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        } else {
            pet.setColrDrctInptNm(null);
        }

        if (pet.getMinBdwg().compareTo(pet.getMaxBdwg()) >= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 공통코드 그룹 소속 검사
     */
    private void checkCode(PetCodeGroup group, String code) {
        boolean exists = commonCodeService.search(group.getCode()).stream()
                .map(CommonCodeSummary::getComCd)
                .anyMatch(c -> c.equals(code));
        if (!exists) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 쉼표 목록 정리
     */
    private String normalizeList(String value) {
        if (value == null) {
            return null;
        }
        String joined = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    /**
     * 빈 문자열 비움 처리
     */
    private String emptyToNull(String value) {
        return hasText(value) ? value : null;
    }

    /**
     * 값 있음 여부
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
