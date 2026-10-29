package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.RepeatPrevention;
import com.nyangmeong.health.dto.RepeatPreventionCreateRequest;
import com.nyangmeong.health.dto.RepeatPreventionResponse;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.RepeatPreventionMapper;

/**
 * [검사 - 반복 예방 관리]
 * - 등록 시 정기관리 항목 추가, 해지 시 삭제 (FN-HLT-005-05)
 */
class RepeatPreventionServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final Long PRVNT = 80L;
    private static final String HEARTWORM = "HLTC0012";
    private static final LocalDate BGNG = LocalDate.of(2026, 9, 23);

    private final RepeatPreventionMapper mapper = mock(RepeatPreventionMapper.class);
    private final CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
    private final CheckupService checkupService = mock(CheckupService.class);
    private final CareItemService careItemService = mock(CareItemService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final RepeatPreventionService service = new RepeatPreventionService(mapper, careScheduleMapper,
            checkupService, careItemService, petAuthorizer);

    @BeforeEach
    void setUp() {
        when(careScheduleMapper.existsChildCode(HealthCodeGroup.REPEAT_PREVENTION_ARTICLE.getCode(), HEARTWORM))
                .thenReturn(true);
        when(mapper.selectCodeName(HEARTWORM)).thenReturn("심장사상충 예방");
        doAnswer(call -> {
            RepeatPrevention prevention = call.getArgument(0);
            prevention.setReptPrvntMngSn(PRVNT);
            return 1;
        }).when(mapper).insertPrevention(any());
        when(careItemService.createPreventionItem(eq(MBR), any(), eq("심장사상충 예방"))).thenReturn(BGNG);
    }

    @Test
    void 등록하면_정기관리_항목을_추가하고_다음_예정일은_시작일이다() {
        RepeatPreventionResponse response = service.create(MBR, PET,
                new RepeatPreventionCreateRequest(CKUP, HEARTWORM, CareCycle.MONTH_1.getCode(), 15, BGNG));

        assertThat(response.reptPrvntMngSn()).isEqualTo(PRVNT);
        assertThat(response.nxtmPrnmntYmd()).isEqualTo(BGNG);
        assertThat(response.cycleDrctInptDayCnt()).isNull();
        verify(checkupService).getCheckup(MBR, PET, CKUP);
        verify(careItemService).createPreventionItem(eq(MBR),
                argThat(prevention -> PET.equals(prevention.getPetSn()) && PRVNT.equals(prevention.getReptPrvntMngSn())
                        && BGNG.equals(prevention.getBgngYmd())),
                eq("심장사상충 예방"));
    }

    @Test
    void 직접입력_주기에_일수가_없으면_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new RepeatPreventionCreateRequest(CKUP, HEARTWORM, CareCycle.CUSTOM.getCode(), null, BGNG)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertPrevention(any());
    }

    @Test
    void 알_수_없는_예방_항목은_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new RepeatPreventionCreateRequest(CKUP, "HLTC9999", CareCycle.MONTH_1.getCode(), null, BGNG)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 해지하면_관리_행과_케어_항목을_삭제한다() {
        RepeatPrevention prevention = new RepeatPrevention();
        prevention.setReptPrvntMngSn(PRVNT);
        prevention.setCkupRcdSn(CKUP);
        when(mapper.selectPrevention(PRVNT)).thenReturn(prevention);
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);

        service.delete(MBR, PET, PRVNT);

        verify(mapper).deletePrevention(PRVNT, "1");
        verify(careItemService).deletePreventionItem(PRVNT, "1");
    }

    @Test
    void 없는_반복_예방_해지는_404() {
        assertThatThrownBy(() -> service.delete(MBR, PET, PRVNT))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPEAT_PREVENTION_NOT_FOUND);
        verify(careItemService, never()).deletePreventionItem(anyLong(), anyString());
    }
}
