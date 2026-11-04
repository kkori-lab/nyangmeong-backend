package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.pet.domain.Breed;
import com.nyangmeong.pet.domain.PetSe;
import com.nyangmeong.pet.dto.AdminBreedCreateRequest;
import com.nyangmeong.pet.dto.AdminBreedSummary;
import com.nyangmeong.pet.dto.AdminBreedUpdateRequest;
import com.nyangmeong.pet.mapper.BreedMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 품종 서비스]
 * - SCR-ADM-101 (FN-ADM-101-01)
 * - 삭제 없음 : 사용 안 함이면 공용 품종 목록·새 등록 선택에서 빠짐, 이미 등록된 반려동물은 그대로
 * - 품종 코드는 서버가 매김 : 강아지 DOGV·고양이 CATV + 그 접두 최대 번호 + 1 (네 자리)
 */
@Service
@RequiredArgsConstructor
public class AdminBreedService {

    private static final String TARGET_TABLE = "VRTY";
    private static final int NAME_MAX_LENGTH = 50;
    private static final int SORT_SEQ_STEP = 10;

    private final BreedMapper breedMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 관리자 품종 목록 조회 (사용 안 함 포함, 종·정렬 순서·번호순)
     */
    @Transactional(readOnly = true)
    public List<AdminBreedSummary> search(String petSeCd) {
        boolean all = petSeCd == null || petSeCd.isBlank();
        if (!all) {
            prefixOf(petSeCd);
        }
        return breedMapper.selectAdminList(all ? null : petSeCd).stream().map(AdminBreedSummary::from).toList();
    }

    /**
     * 품종 추가
     */
    @Transactional
    public AdminBreedSummary create(Long actorSn, AdminBreedCreateRequest req) {
        String prefix = prefixOf(req.petSeCd());
        String vrtyNm = normalizeName(req.vrtyNm());
        checkUniqueName(req.petSeCd(), vrtyNm, null);

        String actorId = String.valueOf(actorSn);
        Breed breed = new Breed();
        breed.setVrtyCd(prefix + String.format("%04d", breedMapper.selectMaxCodeNo(prefix) + 1));
        breed.setPetSeCd(req.petSeCd());
        breed.setVrtyNm(vrtyNm);
        breed.setSortSeq(req.sortSeq() != null ? req.sortSeq()
                : breedMapper.selectMaxSortSeq(req.petSeCd()) + SORT_SEQ_STEP);
        breed.setUseYn(toYn(req.useYn() == null || req.useYn()));
        breed.setRgtrId(actorId);
        breed.setMdfrId(actorId);
        try {
            breedMapper.insert(breed);
        } catch (DuplicateKeyException e) {
            // 같은 품종 코드를 동시에 매긴 경우 (UK_VRTY_1)
            throw new CustomException(ErrorCode.DUPLICATE_VRTY_NM);
        }
        AdminBreedSummary created = AdminBreedSummary.from(breed);
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, breed.getVrtySn(), "품종 추가", null,
                created);
        return created;
    }

    /**
     * 품종 수정 (보낸 칸만, 종·코드는 안 바꿈)
     */
    @Transactional
    public AdminBreedSummary update(Long actorSn, Long vrtySn, AdminBreedUpdateRequest req) {
        if (req.vrtyNm() == null && req.sortSeq() == null && req.useYn() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Breed before = getOrThrow(vrtySn);
        String vrtyNm = req.vrtyNm() == null ? null : normalizeName(req.vrtyNm());
        if (vrtyNm != null) {
            checkUniqueName(before.getPetSeCd(), vrtyNm, vrtySn);
        }

        Breed breed = new Breed();
        breed.setVrtySn(vrtySn);
        breed.setVrtyNm(vrtyNm);
        breed.setSortSeq(req.sortSeq());
        breed.setUseYn(req.useYn() == null ? null : toYn(req.useYn()));
        breed.setMdfrId(String.valueOf(actorSn));
        breedMapper.update(breed);

        AdminBreedSummary updated = AdminBreedSummary.from(getOrThrow(vrtySn));
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, vrtySn, "품종 수정",
                AdminBreedSummary.from(before), updated);
        return updated;
    }

    private Breed getOrThrow(Long vrtySn) {
        Breed breed = breedMapper.selectAdminBreed(vrtySn);
        if (breed == null) {
            throw new CustomException(ErrorCode.BREED_NOT_FOUND);
        }
        return breed;
    }

    /**
     * 구분 코드의 품종 코드 접두 (모르는 구분이면 400)
     */
    private String prefixOf(String petSeCd) {
        if (PetSe.DOG.is(petSeCd)) {
            return "DOGV";
        }
        if (PetSe.CAT.is(petSeCd)) {
            return "CATV";
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * 앞뒤 공백 지운 뒤 1~50자 (아니면 400)
     */
    private String normalizeName(String vrtyNm) {
        String name = vrtyNm.strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return name;
    }

    /**
     * 같은 구분에 공백을 무시하고 같은 이름이 있으면 409 (사용 안 함 포함)
     */
    private void checkUniqueName(String petSeCd, String vrtyNm, Long excludeVrtySn) {
        String nameKey = vrtyNm.replaceAll("\\s", "");
        if (breedMapper.countSameName(petSeCd, nameKey, excludeVrtySn) > 0) {
            throw new CustomException(ErrorCode.DUPLICATE_VRTY_NM);
        }
    }

    private static String toYn(boolean value) {
        return value ? "Y" : "N";
    }
}
