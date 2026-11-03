package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.Breed;
import com.nyangmeong.pet.dto.BreedSummary;
import com.nyangmeong.pet.mapper.BreedMapper;

/**
 * [검사 - 품종 조회·확인]
 */
class BreedServiceTest {

    private final BreedMapper breedMapper = mock(BreedMapper.class);
    private final BreedService service = new BreedService(breedMapper);

    private Breed breed(Long vrtySn, String petSeCd, String vrtyNm) {
        Breed breed = new Breed();
        breed.setVrtySn(vrtySn);
        breed.setVrtyCd("DOGV000" + vrtySn);
        breed.setPetSeCd(petSeCd);
        breed.setVrtyNm(vrtyNm);
        breed.setSortSeq(10);
        return breed;
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 구분별_품종을_조회한다() {
        when(breedMapper.selectList("PETC0001")).thenReturn(List.of(breed(1L, "PETC0001", "말티즈")));

        List<BreedSummary> res = service.getBreeds("PETC0001");

        assertThat(res).extracting(BreedSummary::vrtyNm).containsExactly("말티즈");
    }

    @Test
    void 알_수_없는_구분이면_400이고_조회하지_않는다() {
        assertError(() -> service.getBreeds("PETC9999"), ErrorCode.INVALID_INPUT_VALUE);
        verify(breedMapper, never()).selectList("PETC9999");
    }

    @Test
    void 다른_구분의_품종이거나_없는_품종이면_BREED_NOT_FOUND() {
        when(breedMapper.selectBreed(1L)).thenReturn(breed(1L, "PETC0001", "말티즈"));

        assertThat(service.getBreedOrThrow(1L, "PETC0001").getVrtyNm()).isEqualTo("말티즈");
        assertError(() -> service.getBreedOrThrow(1L, "PETC0002"), ErrorCode.BREED_NOT_FOUND);
        assertError(() -> service.getBreedOrThrow(99L, "PETC0001"), ErrorCode.BREED_NOT_FOUND);
    }
}
