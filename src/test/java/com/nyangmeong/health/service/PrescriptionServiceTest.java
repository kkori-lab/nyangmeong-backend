package com.nyangmeong.health.service;

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

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.PrescriptionCreateRequest;
import com.nyangmeong.health.dto.PrescriptionResponse;
import com.nyangmeong.health.dto.PrescriptionUpdateRequest;
import com.nyangmeong.health.mapper.PrescriptionMapper;

/**
 * [검사 - 처방약·투약 항목 연동]
 * - 복용중 + 주기 체크 ON만 투약 항목 유지 (FN-HLT-005-03)
 */
class PrescriptionServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final Long PRSC = 70L;
    private static final String TAKING = PrescriptionStatus.TAKING.getCode();
    private static final String ENDED = PrescriptionStatus.ENDED.getCode();
    private static final LocalDate BGNG = LocalDate.of(2026, 8, 5);
    private static final LocalDate END = LocalDate.of(2026, 8, 12);

    private final PrescriptionMapper mapper = mock(PrescriptionMapper.class);
    private final CheckupService checkupService = mock(CheckupService.class);
    private final CareItemService careItemService = mock(CareItemService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final PrescriptionService service = new PrescriptionService(mapper, checkupService, careItemService,
            petAuthorizer);

    @BeforeEach
    void setUp() {
        doAnswer(call -> {
            Prescription prescription = call.getArgument(0);
            prescription.setPrscSn(PRSC);
            return 1;
        }).when(mapper).insertPrescription(any());
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
    }

    private Prescription stored(String prscSttsCd, String careRfltYn) {
        Prescription prescription = new Prescription();
        prescription.setPrscSn(PRSC);
        prescription.setCkupRcdSn(CKUP);
        prescription.setMdcnNm("소염제");
        prescription.setMdcnBgngYmd(BGNG);
        prescription.setMdcnEndYmd(END);
        prescription.setDayMdcnNmtm(2);
        prescription.setPrscSttsCd(prscSttsCd);
        prescription.setCareRfltYn(careRfltYn);
        when(mapper.selectPrescriptionForUpdate(PRSC)).thenReturn(prescription);
        return prescription;
    }

    private PrescriptionUpdateRequest update(String prscSttsCd, Boolean careRfltYn) {
        return new PrescriptionUpdateRequest(null, null, null, null, prscSttsCd, careRfltYn);
    }

    @Test
    void 복용중_주기_체크_ON으로_추가하면_투약_항목을_만든다() {
        PrescriptionResponse response = service.create(MBR, PET,
                new PrescriptionCreateRequest(CKUP, " 소염제 ", BGNG, END, 2, TAKING, true));

        assertThat(response.prscSn()).isEqualTo(PRSC);
        assertThat(response.mdcnNm()).isEqualTo("소염제");
        assertThat(response.careRfltYn()).isTrue();
        verify(checkupService).getCheckup(MBR, PET, CKUP);
        verify(mapper).insertStatusHistory(PRSC, MBR, null, TAKING);
        verify(careItemService).createMedicationItem(eq(MBR), eq(PET), any());
    }

    @Test
    void 주기_체크를_보내지_않으면_OFF로_저장하고_투약_항목을_만들지_않는다() {
        PrescriptionResponse response = service.create(MBR, PET,
                new PrescriptionCreateRequest(CKUP, "소염제", BGNG, END, 2, TAKING, null));

        assertThat(response.careRfltYn()).isFalse();
        verify(careItemService, never()).createMedicationItem(anyLong(), anyLong(), any());
    }

    @Test
    void 종료일이_시작일보다_앞서면_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new PrescriptionCreateRequest(CKUP, "소염제", END, BGNG, 2, TAKING, true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertPrescription(any());
    }

    @Test
    void 알_수_없는_복용_상태는_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new PrescriptionCreateRequest(CKUP, "소염제", BGNG, END, 2, "HLTC9999", true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 종료로_바꾸면_이력을_남기고_투약_항목을_삭제한다() {
        stored(TAKING, "Y");

        service.update(MBR, PET, PRSC, update(ENDED, null));

        verify(mapper).insertStatusHistory(PRSC, MBR, TAKING, ENDED);
        verify(careItemService).deleteMedicationItem(PRSC, "1");
        verify(careItemService, never()).createMedicationItem(anyLong(), anyLong(), any());
    }

    @Test
    void 주기_체크를_OFF로_바꾸면_투약_항목을_삭제한다() {
        stored(TAKING, "Y");

        service.update(MBR, PET, PRSC, update(null, false));

        verify(careItemService).deleteMedicationItem(PRSC, "1");
        verify(mapper, never()).insertStatusHistory(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void 종료된_처방을_복용중으로_되돌리고_ON이면_투약_항목을_새로_만든다() {
        stored(ENDED, "Y");

        service.update(MBR, PET, PRSC, update(TAKING, null));

        verify(mapper).insertStatusHistory(PRSC, MBR, ENDED, TAKING);
        verify(careItemService).createMedicationItem(eq(MBR), eq(PET), any());
    }

    @Test
    void 횟수만_바꾸면_항목은_그대로_두고_처방약만_고친다() {
        Prescription prescription = stored(TAKING, "Y");

        PrescriptionResponse response = service.update(MBR, PET, PRSC,
                new PrescriptionUpdateRequest(null, null, null, 3, null, null));

        assertThat(response.dayMdcnNmtm()).isEqualTo(3);
        verify(mapper).updatePrescription(prescription, "1");
        verify(careItemService).createMedicationItem(eq(MBR), eq(PET), any());
        verify(careItemService, never()).deleteMedicationItem(anyLong(), anyString());
    }

    @Test
    void 수정_결과_종료일이_시작일보다_앞서면_400() {
        stored(TAKING, "Y");

        assertThatThrownBy(() -> service.update(MBR, PET, PRSC,
                new PrescriptionUpdateRequest(null, null, LocalDate.of(2026, 8, 1), null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다른_반려동물의_처방약은_404() {
        stored(TAKING, "Y");
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(MBR, PET, PRSC))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRESCRIPTION_NOT_FOUND);
        verify(mapper, never()).deletePrescription(anyLong(), anyString());
    }

    @Test
    void 삭제하면_투약_항목도_삭제한다() {
        stored(TAKING, "Y");

        service.delete(MBR, PET, PRSC);

        verify(mapper).deletePrescription(PRSC, "1");
        verify(careItemService).deleteMedicationItem(PRSC, "1");
    }

    @Test
    void 복용_종료일이_지난_처방은_종료하고_투약_항목을_삭제한다() {
        Prescription prescription = stored(TAKING, "Y");
        when(mapper.selectExpired(any(), eq(TAKING))).thenReturn(List.of(prescription));

        assertThat(service.updateExpired()).isEqualTo(1);

        assertThat(prescription.getPrscSttsCd()).isEqualTo(ENDED);
        verify(mapper).updatePrescription(prescription, "SYSTEM");
        verify(mapper).insertStatusHistory(eq(PRSC), isNull(), eq(TAKING), eq(ENDED));
        verify(careItemService).deleteMedicationItem(PRSC, "SYSTEM");
    }
}
