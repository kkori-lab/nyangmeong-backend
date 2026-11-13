package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.ai.domain.AiHealthReport;
import com.nyangmeong.ai.dto.HealthReportContextResponse;
import com.nyangmeong.ai.dto.HealthReportDailySummary;
import com.nyangmeong.ai.dto.HealthReportInputResponse;
import com.nyangmeong.ai.dto.HealthReportSaveRequest;
import com.nyangmeong.ai.mapper.AiHealthReportMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.DailyRecordIndicator;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.DailyHealthRecordSearchCond;
import com.nyangmeong.health.dto.DailyHealthRecordSummary;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.service.CheckupService;
import com.nyangmeong.health.service.DailyHealthRecordService;
import com.nyangmeong.health.service.InspectionValueService;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.service.PetService;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - AI 건강분석 리포트 입력 조회·저장]
 */
class AiHealthReportServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final LocalDate BASE = LocalDate.of(2026, 11, 4);
    private static final String HASH = "a".repeat(64);

    private final AiHealthReportMapper mapper = mock(AiHealthReportMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final PetService petService = mock(PetService.class);
    private final DailyHealthRecordService dailyService = mock(DailyHealthRecordService.class);
    private final CheckupService checkupService = mock(CheckupService.class);
    private final InspectionValueService inspectionService = mock(InspectionValueService.class);
    private final AiHealthReportService service = new AiHealthReportService(mapper, petAuthorizer, petService,
            dailyService, checkupService, inspectionService, JsonMapper.builder().build());

    private PetResponse pet(String vrtyNm, String vrtyDrctInptNm) {
        return new PetResponse(PET, "콩이", "PETC0001", null, vrtyNm, vrtyDrctInptNm, "PETC0003",
                LocalDate.of(2024, 11, 5), new BigDecimal("4.2"), "PETC0010", null, null, null, null, null,
                "PETC0020", true, true, new BigDecimal("4.000"), null, null, "Y", List.of());
    }

    private DailyHealthRecordSummary record(long sn, DailyRecordIndicator indicator, String value,
            LocalDateTime rcdDt) {
        DailyHealthRecordSummary row = new DailyHealthRecordSummary();
        row.setDailyHlthRcdSn(sn);
        row.setDailyRcdIdctCd(indicator.getCode());
        row.setRcdNvl(value != null ? new BigDecimal(value) : null);
        row.setRcdDt(rcdDt);
        return row;
    }

    private DailyHealthRecordSummary stool(long sn, String name, LocalDateTime rcdDt) {
        DailyHealthRecordSummary row = record(sn, DailyRecordIndicator.STOOL, null, rcdDt);
        row.setCrsdSttsCd("HLTC0100");
        row.setCrsdSttsCdNm(name);
        return row;
    }

    private CheckupSummary checkup(long sn, LocalDate ymd) {
        CheckupSummary row = new CheckupSummary();
        row.setCkupRcdSn(sn);
        row.setCkupYmd(ymd);
        return row;
    }

    private InspectionValueSummary inspection(String cdNm, String drctNm, String rslt, String lower, String upper) {
        InspectionValueSummary row = new InspectionValueSummary();
        row.setInspArtclCdNm(cdNm);
        row.setInspArtclDrctInptNm(drctNm);
        row.setRsltNvl(new BigDecimal(rslt));
        row.setUnitNm("mg/dL");
        row.setRefLlmtNvl(lower != null ? new BigDecimal(lower) : null);
        row.setRefUplmtNvl(upper != null ? new BigDecimal(upper) : null);
        return row;
    }

    private AiHealthReport saved(LocalDate baseYmd, int rfrshNmtm, int autoRgnrNmtm) {
        AiHealthReport report = new AiHealthReport();
        report.setPetSn(PET);
        report.setBaseYmd(baseYmd);
        report.setRptTtl("잘 지내요");
        report.setAnlsCn("[\"물을 잘 마셔요\",\"산책을 늘려요\"]");
        report.setInptHash(HASH);
        report.setRfrshNmtm(rfrshNmtm);
        report.setAutoRgnrNmtm(autoRgnrNmtm);
        report.setCrtDt(LocalDateTime.of(2026, 11, 4, 9, 0));
        return report;
    }

    private HealthReportSaveRequest request(LocalDate baseYmd, boolean rfrshYn) {
        return new HealthReportSaveRequest(baseYmd, "잘 지내요", List.of("물을 잘 마셔요"), HASH, rfrshYn);
    }

    private AiHealthReport captureUpsert() {
        ArgumentCaptor<AiHealthReport> captor = ArgumentCaptor.forClass(AiHealthReport.class);
        verify(mapper).upsert(captor.capture());
        return captor.getValue();
    }

    @Test
    void 하루_값은_체중은_마지막_기록이고_식사량_음수량_산책은_합계() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of(
                record(3, DailyRecordIndicator.WEIGHT, "4.300", BASE.atTime(20, 0)),
                record(1, DailyRecordIndicator.WEIGHT, "4.100", BASE.atTime(8, 0)),
                record(2, DailyRecordIndicator.MEAL, "50", BASE.atTime(8, 0)),
                record(4, DailyRecordIndicator.MEAL, "70", BASE.atTime(19, 0)),
                record(5, DailyRecordIndicator.WATER, "100", BASE.minusDays(1).atTime(9, 0)),
                record(6, DailyRecordIndicator.WATER, "150", BASE.minusDays(1).atTime(21, 0)),
                record(7, DailyRecordIndicator.WALK, "30", BASE.atTime(7, 0)),
                record(8, DailyRecordIndicator.WALK, "20", BASE.atTime(18, 0)),
                stool(9, "보통", BASE.minusDays(2).atTime(10, 0)),
                stool(10, "묽음", BASE.minusDays(2).atTime(15, 0))));
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());

        HealthReportInputResponse input = service.getContext(MBR, PET, BASE).input();

        assertThat(input.weights()).containsExactly(new HealthReportDailySummary(BASE, new BigDecimal("4.300")));
        assertThat(input.meals()).containsExactly(new HealthReportDailySummary(BASE, new BigDecimal("120")));
        assertThat(input.waters())
                .containsExactly(new HealthReportDailySummary(BASE.minusDays(1), new BigDecimal("250")));
        assertThat(input.walks()).containsExactly(new HealthReportDailySummary(BASE, new BigDecimal("50")));
        assertThat(input.stools()).extracting("crsdSttsCdNm").containsExactly("보통", "묽음");
        assertThat(input.stools()).extracting("ymd").containsOnly(BASE.minusDays(2));
        assertThat(input.latestCkupYmd()).isNull();
        assertThat(input.inspOutOfRange()).isEmpty();
    }

    @Test
    void 기준일_포함_14일을_조회한다() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());

        service.getContext(MBR, PET, BASE);

        ArgumentCaptor<DailyHealthRecordSearchCond> captor = ArgumentCaptor.forClass(DailyHealthRecordSearchCond.class);
        verify(dailyService).search(eq(MBR), eq(PET),
                captor.capture());
        assertThat(captor.getValue().getFromYmd()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(captor.getValue().getToYmd()).isEqualTo(BASE);
        assertThat(captor.getValue().getSize()).isNull();
    }

    @Test
    void 반려동물_정보와_만_개월_수_품종_직접_입력명() {
        when(petService.getPet(MBR, PET)).thenReturn(pet(null, "믹스"));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());

        HealthReportInputResponse input = service.getContext(MBR, PET, BASE).input();

        assertThat(input.petNm()).isEqualTo("콩이");
        assertThat(input.vrtyNm()).isEqualTo("믹스");
        assertThat(input.ageMm()).isEqualTo(23);
        assertThat(input.nutrYn()).isTrue();
        assertThat(input.goalBdwg()).isEqualByComparingTo("4");
    }

    @Test
    void 기준일_이전_가장_최근_완료_검진의_참고_범위_밖_수치만() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of(
                checkup(30, BASE.plusDays(3)),
                checkup(20, LocalDate.of(2026, 9, 1)),
                checkup(10, LocalDate.of(2026, 5, 1))));
        when(inspectionService.search(any(), any(), any())).thenReturn(List.of(
                inspection("BUN", "", "40", "7", "27"),
                inspection("ALT", null, "50", "10", "100"),
                inspection("직접입력", "레몬수치", "1", "2", "5"),
                inspection("메모", null, "3", null, null)));

        HealthReportContextResponse res = service.getContext(MBR, PET, BASE);

        assertThat(res.input().latestCkupYmd()).isEqualTo(LocalDate.of(2026, 9, 1));
        ArgumentCaptor<InspectionValueSearchCond> captor = ArgumentCaptor.forClass(InspectionValueSearchCond.class);
        verify(inspectionService).search(eq(MBR), eq(PET),
                captor.capture());
        assertThat(captor.getValue().getCkupRcdSn()).isEqualTo(20L);
        assertThat(res.input().inspOutOfRange()).extracting("inspArtclNm").containsExactly("BUN", "레몬수치");
    }

    @Test
    void 저장본이_없으면_report는_null이고_둘_다_가능() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());

        HealthReportContextResponse res = service.getContext(MBR, PET, BASE);

        assertThat(res.report()).isNull();
        assertThat(res.rfrshPsblYn()).isTrue();
        assertThat(res.autoRgnrPsblYn()).isTrue();
    }

    @Test
    void 같은_기준일에_새로고침했고_자동_재생성_5회면_둘_다_불가() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());
        when(mapper.selectByPet(PET)).thenReturn(saved(BASE, 1, 5));

        HealthReportContextResponse res = service.getContext(MBR, PET, BASE);

        assertThat(res.report().tips()).containsExactly("물을 잘 마셔요", "산책을 늘려요");
        assertThat(res.report().inputHash()).isEqualTo(HASH);
        assertThat(res.rfrshPsblYn()).isFalse();
        assertThat(res.autoRgnrPsblYn()).isFalse();
    }

    @Test
    void 저장본_기준일이_다르면_횟수와_관계없이_둘_다_가능() {
        when(petService.getPet(MBR, PET)).thenReturn(pet("말티즈", null));
        when(dailyService.search(any(), any(), any())).thenReturn(List.of());
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());
        when(mapper.selectByPet(PET)).thenReturn(saved(BASE.minusDays(1), 1, 5));

        HealthReportContextResponse res = service.getContext(MBR, PET, BASE);

        assertThat(res.rfrshPsblYn()).isTrue();
        assertThat(res.autoRgnrPsblYn()).isTrue();
    }

    @Test
    void 남의_반려동물은_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, PET);

        assertThatThrownBy(() -> service.getContext(MBR, PET, BASE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_ACCESS_DENIED);
        verify(mapper, never()).selectByPet(anyLong());
    }

    @Test
    void 첫_저장은_두_횟수_0() {
        service.save(MBR, PET, request(BASE, false));

        AiHealthReport report = captureUpsert();
        assertThat(report.getBaseYmd()).isEqualTo(BASE);
        assertThat(report.getAnlsCn()).isEqualTo("[\"물을 잘 마셔요\"]");
        assertThat(report.getRfrshNmtm()).isZero();
        assertThat(report.getAutoRgnrNmtm()).isZero();
        assertThat(report.getCrtDt()).isNotNull();
        assertThat(report.getMdfrId()).isEqualTo("1");
    }

    @Test
    void 같은_기준일_자동_저장은_자동_재생성_횟수를_올린다() {
        when(mapper.selectByPetForUpdate(PET)).thenReturn(saved(BASE, 1, 2));

        service.save(MBR, PET, request(BASE, false));

        AiHealthReport report = captureUpsert();
        assertThat(report.getRfrshNmtm()).isEqualTo(1);
        assertThat(report.getAutoRgnrNmtm()).isEqualTo(3);
    }

    @Test
    void 기준일이_바뀌면_횟수를_0부터_센다() {
        when(mapper.selectByPetForUpdate(PET)).thenReturn(saved(BASE.minusDays(1), 1, 5));

        service.save(MBR, PET, request(BASE, true));

        AiHealthReport report = captureUpsert();
        assertThat(report.getRfrshNmtm()).isEqualTo(1);
        assertThat(report.getAutoRgnrNmtm()).isZero();
    }

    @Test
    void 같은_기준일_두_번째_새로고침은_409() {
        when(mapper.selectByPetForUpdate(PET)).thenReturn(saved(BASE, 1, 0));

        assertThatThrownBy(() -> service.save(MBR, PET, request(BASE, true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HEALTH_REPORT_REFRESH_LIMIT);
        verify(mapper, never()).upsert(any());
    }

    @Test
    void 자동_재생성_6회째는_409() {
        when(mapper.selectByPetForUpdate(PET)).thenReturn(saved(BASE, 0, 5));

        assertThatThrownBy(() -> service.save(MBR, PET, request(BASE, false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HEALTH_REPORT_AUTO_LIMIT);
        verify(mapper, never()).upsert(any());
    }

    @Test
    void 저장본보다_이른_기준일은_400() {
        when(mapper.selectByPetForUpdate(PET)).thenReturn(saved(BASE, 0, 0));

        assertThatThrownBy(() -> service.save(MBR, PET, request(BASE.minusDays(1), false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).upsert(any());
    }

    @Test
    void 남의_반려동물은_저장하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, PET);

        assertThatThrownBy(() -> service.save(MBR, PET, request(BASE, false)))
                .isInstanceOf(CustomException.class);
        verify(mapper, never()).upsert(any());
    }
}
