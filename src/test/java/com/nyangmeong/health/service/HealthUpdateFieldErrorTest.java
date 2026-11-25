package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CheckupStatus;
import com.nyangmeong.health.domain.DailyHealthRecord;
import com.nyangmeong.health.domain.DailyRecordIndicator;
import com.nyangmeong.health.domain.EtcInspection;
import com.nyangmeong.health.domain.InspectionValue;
import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupUpdateRequest;
import com.nyangmeong.health.dto.DailyHealthRecordUpdateRequest;
import com.nyangmeong.health.dto.EtcInspectionUpdateRequest;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest.RefRange;
import com.nyangmeong.health.dto.PrescriptionUpdateRequest;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupMapper;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.health.mapper.EtcInspectionMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;
import com.nyangmeong.health.mapper.PrescriptionMapper;
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.PetService;
import com.nyangmeong.pet.service.PointService;

/**
 * [검사 - 수정 AI 도구의 서비스 검사 400 칸 이름]
 * - draft_checkup_update·draft_prescription_update·draft_etc_inspection_update·draft_inspection_value_update·
 *   draft_daily_health_record_update : 서비스가 거절한 칸을 data의 field로 알림
 */
class HealthUpdateFieldErrorTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final Long SN = 900L;

    private final CheckupService checkupService = mock(CheckupService.class);

    private void assertFields(Runnable call, String... fields) {
        CustomException e = catchThrowableOfType(CustomException.class, call::run);
        assertThat(e).isNotNull();
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(e.getFields()).containsExactly(fields);
    }

    // 검진기록

    private CheckupService checkupWithRecord(CheckupMapper checkupMapper, CareScheduleMapper careScheduleMapper) {
        CheckupResponse current = new CheckupResponse();
        current.setCkupRcdSn(CKUP);
        current.setPetSn(PET);
        current.setHsptlNm("튼튼동물병원");
        current.setCkupTypeCd("HLTC0001");
        when(checkupMapper.selectDetail(CKUP, CheckupStatus.COMPLETED.getCode())).thenReturn(current);
        return new CheckupService(checkupMapper, careScheduleMapper, mock(PetAuthorizer.class),
                mock(PointService.class));
    }

    @Test
    void 검진_수정_병원명이_공백만이면_hsptlNm() {
        CheckupService service = checkupWithRecord(mock(CheckupMapper.class), mock(CareScheduleMapper.class));
        assertFields(() -> service.update(MBR, PET, CKUP, new CheckupUpdateRequest(" ", null, null, null)),
                "hsptlNm");
    }

    @Test
    void 검진_수정_검진유형이_없는_코드면_ckupTypeCd() {
        CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
        when(careScheduleMapper.existsChildCode(anyString(), anyString())).thenReturn(false);
        CheckupService service = checkupWithRecord(mock(CheckupMapper.class), careScheduleMapper);
        assertFields(() -> service.update(MBR, PET, CKUP, new CheckupUpdateRequest(null, "HLTC9999", null, null)),
                "ckupTypeCd");
    }

    // 처방약

    private PrescriptionService prescriptionService() {
        Prescription prescription = new Prescription();
        prescription.setCkupRcdSn(CKUP);
        prescription.setMdcnNm("약");
        prescription.setMdcnBgngYmd(LocalDate.of(2026, 11, 1));
        prescription.setMdcnEndYmd(LocalDate.of(2026, 11, 10));
        prescription.setPrscSttsCd(PrescriptionStatus.TAKING.getCode());
        PrescriptionMapper mapper = mock(PrescriptionMapper.class);
        when(mapper.selectPrescriptionForUpdate(SN)).thenReturn(prescription);
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
        return new PrescriptionService(mapper, checkupService, mock(CareItemService.class), mock(PetAuthorizer.class));
    }

    @Test
    void 처방_수정_약_이름이_공백만이면_mdcnNm() {
        PrescriptionService service = prescriptionService();
        assertFields(() -> service.update(MBR, PET, SN,
                new PrescriptionUpdateRequest(" ", null, null, null, null, null)), "mdcnNm");
    }

    @Test
    void 처방_수정_상태가_없는_코드면_prscSttsCd() {
        PrescriptionService service = prescriptionService();
        assertFields(() -> service.update(MBR, PET, SN,
                new PrescriptionUpdateRequest(null, null, null, null, "HLTC9999", null)), "prscSttsCd");
    }

    @Test
    void 처방_수정_종료일이_시작일보다_앞이면_mdcnEndYmd() {
        PrescriptionService service = prescriptionService();
        assertFields(() -> service.update(MBR, PET, SN,
                new PrescriptionUpdateRequest(null, null, LocalDate.of(2026, 10, 1), null, null, null)),
                "mdcnEndYmd");
    }

    // 기타검사

    private EtcInspectionService etcInspectionService() {
        EtcInspection inspection = new EtcInspection();
        inspection.setCkupRcdSn(CKUP);
        EtcInspectionMapper mapper = mock(EtcInspectionMapper.class);
        when(mapper.selectInspection(SN)).thenReturn(inspection);
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
        return new EtcInspectionService(mapper, checkupService, mock(PetAuthorizer.class));
    }

    @Test
    void 기타검사_수정_검사명이나_결과가_공백만이면_그_칸() {
        EtcInspectionService service = etcInspectionService();
        assertFields(() -> service.update(MBR, PET, SN, new EtcInspectionUpdateRequest(" ", null, null)), "inspNm");
        assertFields(() -> service.update(MBR, PET, SN, new EtcInspectionUpdateRequest(null, " ", null)),
                "inspRsltCn");
    }

    // 검사 수치

    private InspectionValueService inspectionValueService() {
        InspectionValue value = new InspectionValue();
        value.setCkupRcdSn(CKUP);
        InspectionValueMapper mapper = mock(InspectionValueMapper.class);
        when(mapper.selectValue(SN)).thenReturn(value);
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
        return new InspectionValueService(mapper, checkupService, mock(PetAuthorizer.class));
    }

    @Test
    void 검사_수치_수정_단위가_공백만이면_unitNm() {
        InspectionValueService service = inspectionValueService();
        assertFields(() -> service.update(MBR, PET, SN, new InspectionValueUpdateRequest(null, " ", null, null)),
                "unitNm");
    }

    @Test
    void 검사_수치_수정_참고범위가_한쪽만이거나_하한이_크면_refRange() {
        InspectionValueService service = inspectionValueService();
        assertFields(() -> service.update(MBR, PET, SN, new InspectionValueUpdateRequest(null, null,
                new RefRange(BigDecimal.ONE, null), null)), "refRange");
        assertFields(() -> service.update(MBR, PET, SN, new InspectionValueUpdateRequest(null, null,
                new RefRange(BigDecimal.TEN, BigDecimal.ONE), null)), "refRange");
    }

    // 일상 건강기록

    private DailyHealthRecordService dailyHealthRecordService(DailyRecordIndicator indicator) {
        DailyHealthRecord record = new DailyHealthRecord();
        record.setDailyRcdIdctCd(indicator.getCode());
        DailyHealthRecordMapper mapper = mock(DailyHealthRecordMapper.class);
        when(mapper.selectRecord(anyLong(), anyLong())).thenReturn(record);
        return new DailyHealthRecordService(mapper, mock(PetAuthorizer.class), mock(CareMissionService.class),
                mock(PetService.class));
    }

    @Test
    void 일지_수정_배변에_수치를_보내면_rcdNvl() {
        DailyHealthRecordService service = dailyHealthRecordService(DailyRecordIndicator.STOOL);
        assertFields(() -> service.update(MBR, PET, SN,
                new DailyHealthRecordUpdateRequest(null, BigDecimal.ONE, null)), "rcdNvl");
    }

    @Test
    void 일지_수정_배변이_아닌데_상태를_보내면_crsdSttsCd() {
        DailyHealthRecordService service = dailyHealthRecordService(DailyRecordIndicator.WEIGHT);
        assertFields(() -> service.update(MBR, PET, SN,
                new DailyHealthRecordUpdateRequest(null, null, "HLTC0020")), "crsdSttsCd");
    }
}
