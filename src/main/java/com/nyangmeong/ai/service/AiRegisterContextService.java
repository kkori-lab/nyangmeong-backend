package com.nyangmeong.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

import lombok.RequiredArgsConstructor;

/**
 * [AI 등록 카드 컨텍스트 서비스]
 * - SCR-AIM-012 Python AI 서버가 반려동물 결정·같은 이름 케어 항목 안내에 쓴다
 * - 반려동물은 공동양육 포함 회원이 양육하는 전부
 */
@Service
@RequiredArgsConstructor
public class AiRegisterContextService {

    private final PetService petService;
    private final CareArticleNameFake careArticleNameFake;

    /**
     * 등록 카드 컨텍스트 조회
     */
    @Transactional(readOnly = true)
    public List<RegisterContextPet> search(Long mbrSn) {
        List<PetSummary> pets = petService.searchMyPets(mbrSn);
        if (pets.isEmpty()) {
            return List.of();
        }
        Map<Long, List<String>> careArtclNms = careArticleNameFake
                .searchNames(pets.stream().map(PetSummary::getPetSn).toList());
        return pets.stream()
                .map(pet -> new RegisterContextPet(pet.getPetSn(), pet.getPetNm(),
                        careArtclNms.getOrDefault(pet.getPetSn(), List.of())))
                .toList();
    }
}
