package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.ai.dto.RegisterContextPet;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.health.service.CareItemService;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - AI 등록 카드 컨텍스트 조회]
 */
class AiRegisterContextServiceTest {

    private final PetService petService = mock(PetService.class);
    private final CareItemService careItemService = mock(CareItemService.class);
    private final CommonCodeService commonCodeService = mock(CommonCodeService.class);
    private final AiRegisterContextService service = new AiRegisterContextService(petService, careItemService,
            commonCodeService);

    @BeforeEach
    void setUp() {
        when(commonCodeService.search("PETG01")).thenReturn(List.of(code("PETC0001", "강아지"), code("PETC0002", "고양이")));
    }

    @Test
    void 양육하는_반려동물_전부와_각자의_케어_항목_이름을_준다() {
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet(10L, "콩이"), pet(20L, "보리")));
        when(careItemService.searchNamesByPets(List.of(10L, 20L))).thenReturn(Map.of(10L, List.of("심장사상충", "목욕")));

        List<RegisterContextPet> result = service.search(1L);

        assertThat(result).containsExactly(
                new RegisterContextPet(10L, "콩이", null, null, List.of("심장사상충", "목욕")),
                new RegisterContextPet(20L, "보리", null, null, List.of()));
    }

    @Test
    void 같은_이름_반려동물을_종_이름과_품종_이름으로_구별한다() {
        PetSummary dog = pet(10L, "까미");
        dog.setPetSeCd("PETC0001");
        dog.setVrtyNm("말티즈");
        PetSummary cat = pet(20L, "까미");
        cat.setPetSeCd("PETC0002");
        cat.setVrtyNm("코리안 숏헤어");
        when(petService.searchMyPets(1L)).thenReturn(List.of(dog, cat));
        when(careItemService.searchNamesByPets(List.of(10L, 20L))).thenReturn(Map.of());

        assertThat(service.search(1L)).containsExactly(
                new RegisterContextPet(10L, "까미", "강아지", "말티즈", List.of()),
                new RegisterContextPet(20L, "까미", "고양이", "코리안 숏헤어", List.of()));
    }

    @Test
    void 품종이_없으면_직접_입력_품종을_준다() {
        PetSummary pet = pet(10L, "콩이");
        pet.setPetSeCd("PETC0001");
        pet.setVrtyDrctInptNm("믹스견");
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet));
        when(careItemService.searchNamesByPets(List.of(10L))).thenReturn(Map.of());

        assertThat(service.search(1L).get(0).vrtyNm()).isEqualTo("믹스견");
    }

    @Test
    void 품종도_직접_입력도_없고_모르는_종_코드면_null이다() {
        PetSummary pet = pet(10L, "콩이");
        pet.setPetSeCd("PETC9999");
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet));
        when(careItemService.searchNamesByPets(List.of(10L))).thenReturn(Map.of());

        RegisterContextPet result = service.search(1L).get(0);

        assertThat(result.petSeNm()).isNull();
        assertThat(result.vrtyNm()).isNull();
    }

    @Test
    void 반려동물이_없으면_빈_배열이다() {
        when(petService.searchMyPets(1L)).thenReturn(List.of());

        assertThat(service.search(1L)).isEmpty();
        verify(careItemService, never()).searchNamesByPets(anyList());
        verify(commonCodeService, never()).search(anyString());
    }

    private PetSummary pet(Long petSn, String petNm) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setPetNm(petNm);
        return pet;
    }

    private CommonCodeSummary code(String comCd, String comCdNm) {
        CommonCodeSummary code = new CommonCodeSummary();
        code.setComCd(comCd);
        code.setComCdNm(comCdNm);
        return code;
    }
}
