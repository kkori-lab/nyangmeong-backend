package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.pet.domain.Breed;
import com.nyangmeong.pet.dto.AdminBreedCreateRequest;
import com.nyangmeong.pet.dto.AdminBreedSummary;
import com.nyangmeong.pet.dto.AdminBreedUpdateRequest;
import com.nyangmeong.pet.mapper.BreedMapper;

/**
 * [검사 - 관리자 품종 목록·추가·수정]
 */
class AdminBreedServiceTest {

    private static final String DOG = "PETC0001";
    private static final String CAT = "PETC0002";

    private final BreedMapper breedMapper = mock(BreedMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminBreedService service = new AdminBreedService(breedMapper, adminTaskLogService);

    private Breed stored(Long vrtySn, String vrtyNm, String useYn) {
        Breed breed = new Breed();
        breed.setVrtySn(vrtySn);
        breed.setVrtyCd("DOGV0001");
        breed.setPetSeCd(DOG);
        breed.setVrtyNm(vrtyNm);
        breed.setSortSeq(10);
        breed.setUseYn(useYn);
        return breed;
    }

    private void givenInsertSetsKey(long vrtySn) {
        doAnswer(invocation -> {
            invocation.<Breed>getArgument(0).setVrtySn(vrtySn);
            return 1;
        }).when(breedMapper).insert(any());
    }

    @Test
    void 종_코드가_없으면_전체_목록() {
        service.search(" ");

        verify(breedMapper).selectAdminList(isNull());
    }

    @Test
    void 모르는_종_코드는_400() {
        assertThatThrownBy(() -> service.search("PETC9999"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 추가하면_접두_최대_번호_다음_코드를_매긴다() {
        when(breedMapper.selectMaxCodeNo("DOGV")).thenReturn(23);
        givenInsertSetsKey(38L);

        AdminBreedSummary created = service.create(1L, new AdminBreedCreateRequest(DOG, "  비숑  ", 15, null));

        assertThat(created.vrtyCd()).isEqualTo("DOGV0024");
        assertThat(created.vrtyNm()).isEqualTo("비숑");
        assertThat(created.sortSeq()).isEqualTo(15);
        assertThat(created.useYn()).isTrue();
        assertThat(created.vrtySn()).isEqualTo(38L);
    }

    @Test
    void 고양이_첫_품종은_CATV0001_정렬은_마지막_다음() {
        when(breedMapper.selectMaxCodeNo("CATV")).thenReturn(0);
        when(breedMapper.selectMaxSortSeq(CAT)).thenReturn(0);
        givenInsertSetsKey(1L);

        AdminBreedSummary created = service.create(1L, new AdminBreedCreateRequest(CAT, "샴", null, false));

        assertThat(created.vrtyCd()).isEqualTo("CATV0001");
        assertThat(created.sortSeq()).isEqualTo(10);
        assertThat(created.useYn()).isFalse();
    }

    @Test
    void 같은_종에_공백만_다른_이름은_409() {
        when(breedMapper.countSameName(DOG, "말티즈", null)).thenReturn(1);

        assertThatThrownBy(() -> service.create(1L, new AdminBreedCreateRequest(DOG, "말 티즈", null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_VRTY_NM);
        verify(breedMapper, never()).insert(any());
    }

    @Test
    void 이름이_비거나_50자를_넘으면_400() {
        assertThatThrownBy(() -> service.create(1L, new AdminBreedCreateRequest(DOG, "   ", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.create(1L, new AdminBreedCreateRequest(DOG, "가".repeat(51), null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 자기_이름으로_수정은_중복이_아니다() {
        when(breedMapper.selectAdminBreed(3L)).thenReturn(stored(3L, "말티즈", "Y"));

        service.update(1L, 3L, new AdminBreedUpdateRequest("말티즈", null, false));

        verify(breedMapper).countSameName(DOG, "말티즈", 3L);
        ArgumentCaptor<Breed> captor = ArgumentCaptor.forClass(Breed.class);
        verify(breedMapper).update(captor.capture());
        assertThat(captor.getValue().getUseYn()).isEqualTo("N");
        assertThat(captor.getValue().getSortSeq()).isNull();
    }

    @Test
    void 다른_품종과_같은_이름으로_수정은_409() {
        when(breedMapper.selectAdminBreed(3L)).thenReturn(stored(3L, "푸들", "Y"));
        when(breedMapper.countSameName(eq(DOG), anyString(), eq(3L))).thenReturn(1);

        assertThatThrownBy(() -> service.update(1L, 3L, new AdminBreedUpdateRequest("말티즈", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_VRTY_NM);
        verify(breedMapper, never()).update(any());
    }

    @Test
    void 없는_품종_수정은_404() {
        assertThatThrownBy(() -> service.update(1L, 99L, new AdminBreedUpdateRequest(null, 5, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.BREED_NOT_FOUND);
    }

    @Test
    void 아무_칸도_없는_수정은_400() {
        assertThatThrownBy(() -> service.update(1L, 3L, new AdminBreedUpdateRequest(null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(breedMapper, never()).selectAdminBreed(anyLong());
    }
}
