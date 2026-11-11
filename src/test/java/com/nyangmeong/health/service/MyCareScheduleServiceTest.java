package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CheckupStatus;
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.dto.MyCareScheduleSearchCond;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 회원 단위 케어 캘린더 일정]
 */
class MyCareScheduleServiceTest {

    private static final Long MBR = 1L;
    private static final String COMPLETED = CheckupStatus.COMPLETED.getCode();

    private final CareScheduleMapper mapper = mock(CareScheduleMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CareScheduleService service = new CareScheduleService(mapper, petAuthorizer,
            mock(CareMissionService.class));

    private static MyCareScheduleSearchCond cond(String petSns) {
        MyCareScheduleSearchCond cond = new MyCareScheduleSearchCond();
        cond.setPetSns(petSns);
        return cond;
    }

    private static String joined(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    @Test
    void 볼_수_없는_반려동물_번호는_오류_없이_빠진다() {
        MyCareScheduleSearchCond cond = cond("10,93,11");
        List<CareScheduleSummary> rows = List.of(new CareScheduleSummary());
        when(petAuthorizer.searchCaregiverPetSns(MBR, List.of(10L, 93L, 11L))).thenReturn(List.of(10L, 11L));
        when(mapper.selectListByPets(List.of(10L, 11L), cond, COMPLETED)).thenReturn(rows);

        assertThat(service.searchMine(MBR, cond)).isSameAs(rows);
    }

    @Test
    void 같은_번호는_한_번만_넘긴다() {
        MyCareScheduleSearchCond cond = cond("10,10,11");
        when(petAuthorizer.searchCaregiverPetSns(MBR, List.of(10L, 11L))).thenReturn(List.of(10L, 11L));

        service.searchMine(MBR, cond);

        verify(mapper).selectListByPets(List.of(10L, 11L), cond, COMPLETED);
    }

    @Test
    void petSns를_생략하면_볼_수_있는_반려동물_전체() {
        MyCareScheduleSearchCond cond = cond(null);
        when(petAuthorizer.searchCaregiverPetSns(MBR, List.of())).thenReturn(List.of(10L, 11L, 12L));

        service.searchMine(MBR, cond);

        verify(mapper).selectListByPets(List.of(10L, 11L, 12L), cond, COMPLETED);
    }

    @Test
    void petSns가_빈_문자열이면_볼_수_있는_반려동물_전체() {
        MyCareScheduleSearchCond cond = cond("");
        when(petAuthorizer.searchCaregiverPetSns(MBR, List.of())).thenReturn(List.of(10L, 11L));

        service.searchMine(MBR, cond);

        verify(mapper).selectListByPets(List.of(10L, 11L), cond, COMPLETED);
    }

    @Test
    void 볼_수_있는_반려동물이_0마리면_DB를_조회하지_않고_빈_목록() {
        when(petAuthorizer.searchCaregiverPetSns(MBR, List.of(93L))).thenReturn(List.of());

        assertThat(service.searchMine(MBR, cond("93"))).isEmpty();
        verify(mapper, never()).selectListByPets(anyList(), any(), anyString());
    }

    @Test
    void 반려동물_번호가_50개를_넘으면_INVALID_INPUT_VALUE() {
        assertThatThrownBy(() -> service.searchMine(MBR, cond(joined(51))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(petAuthorizer, never()).searchCaregiverPetSns(anyLong(), anyList());
    }

    @Test
    void 반려동물_번호가_50개면_조회한다() {
        when(petAuthorizer.searchCaregiverPetSns(anyLong(), anyList())).thenReturn(List.of(1L));

        service.searchMine(MBR, cond(joined(50)));

        verify(mapper).selectListByPets(anyList(), any(), anyString());
    }

    @Test
    void Long_범위를_넘는_번호는_INVALID_INPUT_VALUE() {
        assertThatThrownBy(() -> service.searchMine(MBR, cond("99999999999999999999")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
