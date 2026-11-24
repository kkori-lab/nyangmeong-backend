package com.nyangmeong.ai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.health.service.CareItemService;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

import lombok.RequiredArgsConstructor;

/**
 * [AI 등록 카드 컨텍스트 서비스]
 * - SCR-AIM-012 Python AI 서버가 반려동물 결정·같은 이름 케어 항목 안내에 쓴다
 * - 반려동물은 공동양육 포함 회원이 양육하는 전부
 * - 종은 코드가 아니라 이름(PETG01)으로 준다. AI가 코드 값을 두지 않게
 */
@Service
@RequiredArgsConstructor
public class AiRegisterContextService {

    private static final String PET_SE_CODE_GROUP = "PETG01";

    private final PetService petService;
    private final CareItemService careItemService;
    private final CommonCodeService commonCodeService;

    /**
     * 등록 카드 컨텍스트 조회
     */
    @Transactional(readOnly = true)
    public List<RegisterContextPet> search(Long mbrSn) {
        List<PetSummary> pets = petService.searchMyPets(mbrSn);
        if (pets.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> careArtclNms = careItemService
                .searchNamesByPets(pets.stream().map(PetSummary::getPetSn).toList());
        Map<String, String> petSeNms = commonCodeService.search(PET_SE_CODE_GROUP).stream()
                .collect(Collectors.toMap(CommonCodeSummary::getComCd, CommonCodeSummary::getComCdNm,
                        (first, second) -> first));
        return pets.stream()
                .map(pet -> new RegisterContextPet(pet.getPetSn(), pet.getPetNm(),
                        pet.getPetSeCd() == null ? null : petSeNms.get(pet.getPetSeCd()), vrtyNm(pet),
                        careArtclNms.getOrDefault(pet.getPetSn(), List.of())))
                .toList();
    }

    /**
     * 품종 이름. 없으면 직접 입력 품종, 둘 다 없으면 null
     */
    private String vrtyNm(PetSummary pet) {
        if (pet.getVrtyNm() != null && !pet.getVrtyNm().isBlank()) {
            return pet.getVrtyNm();
        }
        if (pet.getVrtyDrctInptNm() != null && !pet.getVrtyDrctInptNm().isBlank()) {
            return pet.getVrtyDrctInptNm();
        }
        return null;
    }
}
