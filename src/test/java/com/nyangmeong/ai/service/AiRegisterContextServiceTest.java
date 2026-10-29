package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - AI 등록 카드 컨텍스트 조회]
 */
class AiRegisterContextServiceTest {

    private final PetService petService = mock(PetService.class);
    private final CareArticleNameFake careArticleNameFake = mock(CareArticleNameFake.class);
    private final AiRegisterContextService service = new AiRegisterContextService(petService, careArticleNameFake);

    @Test
    void 양육하는_반려동물_전부와_각자의_케어_항목_이름을_준다() {
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet(10L, "콩이"), pet(20L, "보리")));
        when(careArticleNameFake.searchNames(List.of(10L, 20L))).thenReturn(Map.of(10L, List.of("심장사상충", "목욕")));

        List<RegisterContextPet> result = service.search(1L);

        assertThat(result).containsExactly(
                new RegisterContextPet(10L, "콩이", List.of("심장사상충", "목욕")),
                new RegisterContextPet(20L, "보리", List.of()));
    }

    @Test
    void 반려동물이_없으면_빈_배열이다() {
        when(petService.searchMyPets(1L)).thenReturn(List.of());

        assertThat(service.search(1L)).isEmpty();
        verify(careArticleNameFake, never()).searchNames(anyList());
    }

    @Test
    void 가짜는_케어_항목_이름을_주지_않는다() {
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet(10L, "콩이")));
        AiRegisterContextService withFake = new AiRegisterContextService(petService, new CareArticleNameFake());

        assertThat(withFake.search(1L)).containsExactly(new RegisterContextPet(10L, "콩이", List.of()));
    }

    private PetSummary pet(Long petSn, String petNm) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setPetNm(petNm);
        return pet;
    }
}
