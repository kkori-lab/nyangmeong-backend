package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.CheckupCreateRequest;
import com.nyangmeong.health.dto.EtcInspectionCreateRequest;
import com.nyangmeong.health.dto.PrescriptionCreateRequest;
import com.nyangmeong.health.dto.ReceiptCheckupCommand;
import com.nyangmeong.health.dto.RepeatPreventionCreateRequest;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupMapper;
import com.nyangmeong.health.mapper.EtcInspectionMapper;
import com.nyangmeong.health.mapper.PrescriptionMapper;
import com.nyangmeong.health.mapper.RepeatPreventionMapper;
import com.nyangmeong.pet.service.PointService;

/**
 * [검사 - 등록 AI 도구의 서비스 검사 400 칸 이름]
 * - draft_checkup·draft_prescription·draft_etc_inspection·draft_repeat_prevention : 서비스가 거절한 칸을 data의 field로 알림
 * - 영수증 저장의 검진기록 등록은 칸 이름이 달라(pets[i]…) 비운다
 */
class HealthCreateFieldErrorTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final LocalDate YMD = LocalDate.of(2026, 11, 1);

    private final CheckupService checkupService = mock(CheckupService.class);

    private void assertFields(Runnable call, String... fields) {
        CustomException e = catchThrowableOfType(CustomException.class, call::run);
        assertThat(e).isNotNull();
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(e.getFields()).containsExactly(fields);
    }

    // 검진기록

    private CheckupService checkup(boolean typeExists) {
        CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
        when(careScheduleMapper.existsChildCode(anyString(), anyString())).thenReturn(typeExists);
        return new CheckupService(mock(CheckupMapper.class), careScheduleMapper, mock(PetAuthorizer.class),
                mock(PointService.class));
    }

    @Test
    void 검진_등록_병원명이_공백만이면_hsptlNm() {
        CheckupService service = checkup(true);
        assertFields(() -> service.create(MBR, PET, new CheckupCreateRequest(YMD, " ", "HLTC0001", null, null, null)),
                "hsptlNm");
    }

    @Test
    void 검진_등록_검진유형이_없는_코드면_ckupTypeCd() {
        CheckupService service = checkup(false);
        assertFields(() -> service.create(MBR, PET,
                new CheckupCreateRequest(YMD, "튼튼동물병원", "HLTC9999", null, null, null)), "ckupTypeCd");
    }

    @Test
    void 검진_등록_옮겨_연결할_일정이_없으면_schdlSn() {
        CheckupService service = checkup(true);
        assertFields(() -> service.create(MBR, PET,
                new CheckupCreateRequest(YMD, "튼튼동물병원", "HLTC0001", null, null, 77L)), "schdlSn");
    }

    @Test
    void 영수증_저장의_검진기록_등록은_칸_이름을_싣지_않는다() {
        CheckupService service = checkup(false);
        assertFields(() -> service.createFromReceipt(MBR, PET,
                new ReceiptCheckupCommand(3L, YMD, "튼튼동물병원", "HLTC9999", null, null, null)));
    }

    // 처방약

    private PrescriptionService prescription() {
        return new PrescriptionService(mock(PrescriptionMapper.class), checkupService, mock(CareItemService.class),
                mock(PetAuthorizer.class));
    }

    private PrescriptionCreateRequest prescriptionReq(String mdcnNm, LocalDate end, String prscSttsCd) {
        return new PrescriptionCreateRequest(CKUP, mdcnNm, YMD, end, 2, prscSttsCd, null);
    }

    @Test
    void 처방_추가_약_이름이_공백만이면_mdcnNm() {
        PrescriptionService service = prescription();
        assertFields(() -> service.create(MBR, PET, prescriptionReq(" ", YMD, PrescriptionStatus.TAKING.getCode())),
                "mdcnNm");
    }

    @Test
    void 처방_추가_상태가_없는_코드면_prscSttsCd() {
        PrescriptionService service = prescription();
        assertFields(() -> service.create(MBR, PET, prescriptionReq("약", YMD, "HLTC9999")), "prscSttsCd");
    }

    @Test
    void 처방_추가_종료일이_시작일보다_앞이면_mdcnEndYmd() {
        PrescriptionService service = prescription();
        assertFields(() -> service.create(MBR, PET,
                prescriptionReq("약", YMD.minusDays(1), PrescriptionStatus.TAKING.getCode())), "mdcnEndYmd");
    }

    // 기타검사

    @Test
    void 기타검사_입력_검사명이나_결과가_공백만이면_그_칸() {
        EtcInspectionService service = new EtcInspectionService(mock(EtcInspectionMapper.class), checkupService,
                mock(PetAuthorizer.class));
        assertFields(() -> service.create(MBR, PET, new EtcInspectionCreateRequest(CKUP, " ", "정상", null)),
                "inspNm");
        assertFields(() -> service.create(MBR, PET, new EtcInspectionCreateRequest(CKUP, "초음파", " ", null)),
                "inspRsltCn");
    }

    // 반복 예방

    private RepeatPreventionService prevention(boolean articleExists) {
        CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
        when(careScheduleMapper.existsChildCode(anyString(), anyString())).thenReturn(articleExists);
        return new RepeatPreventionService(mock(RepeatPreventionMapper.class), careScheduleMapper, checkupService,
                mock(CareItemService.class), mock(PetAuthorizer.class));
    }

    @Test
    void 반복_예방_등록_항목이_없는_코드면_reptPrvntArtclCd() {
        RepeatPreventionService service = prevention(false);
        assertFields(() -> service.create(MBR, PET,
                new RepeatPreventionCreateRequest(CKUP, "HLTC9999", CareCycle.CUSTOM.getCode(), 30, YMD)),
                "reptPrvntArtclCd");
    }

    @Test
    void 반복_예방_등록_주기가_없는_코드면_reptCycleCd() {
        RepeatPreventionService service = prevention(true);
        assertFields(() -> service.create(MBR, PET, new RepeatPreventionCreateRequest(CKUP, "HLTC0030", "HLTC9999",
                null, YMD)), "reptCycleCd");
    }

    @Test
    void 반복_예방_등록_직접입력_주기에_일수가_없으면_cycleDrctInptDayCnt() {
        RepeatPreventionService service = prevention(true);
        assertFields(() -> service.create(MBR, PET,
                new RepeatPreventionCreateRequest(CKUP, "HLTC0030", CareCycle.CUSTOM.getCode(), null, YMD)),
                "cycleDrctInptDayCnt");
    }
}
