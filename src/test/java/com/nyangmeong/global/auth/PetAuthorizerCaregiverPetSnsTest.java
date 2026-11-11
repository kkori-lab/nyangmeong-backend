package com.nyangmeong.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 양육 중인 반려동물 번호 목록]
 */
class PetAuthorizerCaregiverPetSnsTest {

    private final PetAuthMapper petAuthMapper = mock(PetAuthMapper.class);
    private final PetAuthorizer petAuthorizer = new PetAuthorizer(petAuthMapper);

    @Test
    void 주양육자와_공동양육자_역할_코드로_조회한다() {
        List<String> roles = List.of(BrupRole.PRIMARY.getCode(), BrupRole.CO_PARENT.getCode());
        when(petAuthMapper.selectCaregiverPetSns(1L, List.of(10L, 93L), roles)).thenReturn(List.of(10L));

        assertThat(petAuthorizer.searchCaregiverPetSns(1L, List.of(10L, 93L))).containsExactly(10L);
    }

    @Test
    void 볼_수_없는_번호가_있어도_예외를_던지지_않는다() {
        when(petAuthMapper.selectCaregiverPetSns(eq(1L), eq(List.of(93L)), anyList()))
                .thenReturn(List.of());

        assertThat(petAuthorizer.searchCaregiverPetSns(1L, List.of(93L))).isEmpty();
        verify(petAuthMapper, never()).existsPet(anyLong());
    }
}
