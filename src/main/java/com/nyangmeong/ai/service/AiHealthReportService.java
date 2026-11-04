package com.nyangmeong.ai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BinaryOperator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.domain.AiHealthReport;
import com.nyangmeong.ai.dto.HealthReportContextResponse;
import com.nyangmeong.ai.dto.HealthReportDailySummary;
import com.nyangmeong.ai.dto.HealthReportInputResponse;
import com.nyangmeong.ai.dto.HealthReportInspectionSummary;
import com.nyangmeong.ai.dto.HealthReportResponse;
import com.nyangmeong.ai.dto.HealthReportSaveRequest;
import com.nyangmeong.ai.dto.HealthReportStoolSummary;
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

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * [AI 건강분석 리포트 서비스]
 * - SCR-MAN-001 AI 건강분석 리포트 위젯 : Python AI 서버가 입력을 받고 결과를 저장 (docs/api/ai.md)
 * - 새로고침은 기준일 하루 1회, 그날 첫 생성 뒤 자동 재생성은 5회까지
 */
@Service
@RequiredArgsConstructor
public class AiHealthReportService {

    /** 기준일 포함 분석 기간(일) */
    static final int ANALYSIS_DAYS = 14;
    /** 기준일 하루 새로고침 상한 */
    static final int MAX_REFRESH = 1;
    /** 기준일 하루 자동 재생성 상한 */
    static final int MAX_AUTO_REGENERATION = 5;

    private static final TypeReference<List<String>> TIPS_TYPE = new TypeReference<>() {
    };

    private final AiHealthReportMapper aiHealthReportMapper;
    private final PetAuthorizer petAuthorizer;
    private final PetService petService;
    private final DailyHealthRecordService dailyHealthRecordService;
    private final CheckupService checkupService;
    private final InspectionValueService inspectionValueService;
    private final ObjectMapper objectMapper;

    /**
     * 리포트 분석 입력과 저장본 조회
     */
    @Transactional(readOnly = true)
    public HealthReportContextResponse getContext(Long mbrSn, Long petSn, LocalDate baseYmd) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        PetResponse pet = petService.getPet(mbrSn, petSn);
        AiHealthReport saved = aiHealthReportMapper.selectByPet(petSn);
        boolean isSameDay = saved != null && baseYmd.equals(saved.getBaseYmd());
        boolean canRefresh = !isSameDay || saved.getRfrshNmtm() < MAX_REFRESH;
        boolean canAutoRegenerate = !isSameDay || saved.getAutoRgnrNmtm() < MAX_AUTO_REGENERATION;
        return new HealthReportContextResponse(buildInput(mbrSn, petSn, pet, baseYmd), toResponse(saved),
                canRefresh, canAutoRegenerate);
    }

    /**
     * 리포트 저장
     */
    @Transactional
    public void save(Long mbrSn, Long petSn, HealthReportSaveRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        AiHealthReport saved = aiHealthReportMapper.selectByPetForUpdate(petSn);
        if (saved != null && req.baseYmd().isBefore(saved.getBaseYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean isSameDay = saved != null && req.baseYmd().equals(saved.getBaseYmd());
        int rfrshNmtm = isSameDay ? saved.getRfrshNmtm() : 0;
        int autoRgnrNmtm = isSameDay ? saved.getAutoRgnrNmtm() : 0;
        if (Boolean.TRUE.equals(req.rfrshYn())) {
            if (rfrshNmtm >= MAX_REFRESH) {
                throw new CustomException(ErrorCode.HEALTH_REPORT_REFRESH_LIMIT);
            }
            rfrshNmtm++;
        } else if (isSameDay) {
            if (autoRgnrNmtm >= MAX_AUTO_REGENERATION) {
                throw new CustomException(ErrorCode.HEALTH_REPORT_AUTO_LIMIT);
            }
            autoRgnrNmtm++;
        }

        String actorId = String.valueOf(mbrSn);
        AiHealthReport report = new AiHealthReport();
        report.setPetSn(petSn);
        report.setBaseYmd(req.baseYmd());
        report.setRptTtl(req.rptTtl());
        report.setAnlsCn(objectMapper.writeValueAsString(req.tips()));
        report.setInptHash(req.inputHash().toLowerCase());
        report.setRfrshNmtm(rfrshNmtm);
        report.setAutoRgnrNmtm(autoRgnrNmtm);
        report.setCrtDt(LocalDateTime.now());
        report.setRgtrId(actorId);
        report.setMdfrId(actorId);
        aiHealthReportMapper.upsert(report);
    }

    /**
     * 분석 입력 조립
     */
    private HealthReportInputResponse buildInput(Long mbrSn, Long petSn, PetResponse pet, LocalDate baseYmd) {
        DailyHealthRecordSearchCond cond = new DailyHealthRecordSearchCond();
        cond.setFromYmd(baseYmd.minusDays(ANALYSIS_DAYS - 1L));
        cond.setToYmd(baseYmd);
        List<DailyHealthRecordSummary> records = new ArrayList<>(dailyHealthRecordService.search(mbrSn, petSn, cond));
        records.sort(Comparator.comparing(DailyHealthRecordSummary::getRcdDt)
                .thenComparing(DailyHealthRecordSummary::getDailyHlthRcdSn));

        List<HealthReportStoolSummary> stools = records.stream()
                .filter(r -> DailyRecordIndicator.STOOL.getCode().equals(r.getDailyRcdIdctCd()))
                .map(r -> new HealthReportStoolSummary(r.getRcdDt().toLocalDate(), r.getCrsdSttsCdNm()))
                .toList();

        CheckupSummary latest = checkupService.searchAllCompleted(mbrSn, petSn).stream()
                .filter(c -> c.getCkupYmd() != null && !c.getCkupYmd().isAfter(baseYmd))
                .max(Comparator.comparing(CheckupSummary::getCkupYmd).thenComparing(CheckupSummary::getCkupRcdSn))
                .orElse(null);

        return new HealthReportInputResponse(pet.petNm(), pet.petSeCd(),
                pet.vrtyNm() != null ? pet.vrtyNm() : pet.vrtyDrctInptNm(), ageInMonths(pet.petBrdt(), baseYmd),
                pet.nutrYn(), pet.goalBdwg(),
                daily(records, DailyRecordIndicator.WEIGHT, (earlier, later) -> later),
                daily(records, DailyRecordIndicator.MEAL, BigDecimal::add),
                daily(records, DailyRecordIndicator.WATER, BigDecimal::add),
                daily(records, DailyRecordIndicator.WALK, BigDecimal::add),
                stools, latest != null ? latest.getCkupYmd() : null, outOfRange(mbrSn, petSn, latest));
    }

    /**
     * 지표별 하루 값
     */
    private List<HealthReportDailySummary> daily(List<DailyHealthRecordSummary> records,
            DailyRecordIndicator indicator, BinaryOperator<BigDecimal> merge) {
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        records.stream()
                .filter(r -> indicator.getCode().equals(r.getDailyRcdIdctCd()) && r.getRcdNvl() != null)
                .forEach(r -> byDay.merge(r.getRcdDt().toLocalDate(), r.getRcdNvl(), merge));
        return byDay.entrySet().stream()
                .map(e -> new HealthReportDailySummary(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * 검진 1회의 참고 범위 밖 검사 수치
     */
    private List<HealthReportInspectionSummary> outOfRange(Long mbrSn, Long petSn, CheckupSummary checkup) {
        if (checkup == null) {
            return List.of();
        }
        InspectionValueSearchCond cond = new InspectionValueSearchCond();
        cond.setCkupRcdSn(checkup.getCkupRcdSn());
        return inspectionValueService.search(mbrSn, petSn, cond).stream()
                .filter(AiHealthReportService::isOutOfRange)
                .map(v -> new HealthReportInspectionSummary(inspectionName(v), v.getRsltNvl(), v.getUnitNm(),
                        v.getRefLlmtNvl(), v.getRefUplmtNvl()))
                .toList();
    }

    /**
     * 검사 항목 이름
     */
    private static String inspectionName(InspectionValueSummary v) {
        String drctInptNm = v.getInspArtclDrctInptNm();
        return drctInptNm != null && !drctInptNm.isBlank() ? drctInptNm : v.getInspArtclCdNm();
    }

    /**
     * 참고 범위 밖 여부
     */
    private static boolean isOutOfRange(InspectionValueSummary v) {
        if (v.getRsltNvl() == null) {
            return false;
        }
        boolean isBelow = v.getRefLlmtNvl() != null && v.getRsltNvl().compareTo(v.getRefLlmtNvl()) < 0;
        boolean isAbove = v.getRefUplmtNvl() != null && v.getRsltNvl().compareTo(v.getRefUplmtNvl()) > 0;
        return isBelow || isAbove;
    }

    /**
     * 기준일 만 개월 수
     */
    private static Integer ageInMonths(LocalDate petBrdt, LocalDate baseYmd) {
        if (petBrdt == null || petBrdt.isAfter(baseYmd)) {
            return 0;
        }
        return (int) Period.between(petBrdt, baseYmd).toTotalMonths();
    }

    /**
     * 저장본 응답 변환
     */
    private HealthReportResponse toResponse(AiHealthReport saved) {
        if (saved == null) {
            return null;
        }
        return new HealthReportResponse(saved.getRptTtl(), objectMapper.readValue(saved.getAnlsCn(), TIPS_TYPE),
                saved.getInptHash(), saved.getCrtDt());
    }
}
