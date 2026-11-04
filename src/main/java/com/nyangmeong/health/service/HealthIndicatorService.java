package com.nyangmeong.health.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.health.domain.DailyRecordIndicator;
import com.nyangmeong.health.domain.InspectionArticle;
import com.nyangmeong.health.dto.HealthCreatininePoint;
import com.nyangmeong.health.dto.HealthDailySumPoint;
import com.nyangmeong.health.dto.HealthIndicatorResponse;
import com.nyangmeong.health.dto.HealthIndicatorResponse.CreatinineIndicator;
import com.nyangmeong.health.dto.HealthIndicatorResponse.DailySumIndicator;
import com.nyangmeong.health.dto.HealthIndicatorResponse.WeightIndicator;
import com.nyangmeong.health.dto.HealthWeightPoint;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;

import lombok.RequiredArgsConstructor;

/**
 * [메인 건강 지표 서비스]
 * - 메인 건강 대시보드 위젯 5종 값과 기준일 (SCR-HLT-002 메인 건강 지표, FN-MAN-005·006)
 * - 기록이 없는 지표는 null, 비교 대상이 없으면 대비 값은 null
 */
@Service
@RequiredArgsConstructor
public class HealthIndicatorService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int POINT_SIZE = 5;

    private final DailyHealthRecordMapper dailyHealthRecordMapper;
    private final InspectionValueMapper inspectionValueMapper;
    private final PetAuthorizer petAuthorizer;

    /**
     * 메인 건강 지표 조회
     */
    @Transactional(readOnly = true)
    public HealthIndicatorResponse get(Long mbrSn, Long petSn) {
        return get(mbrSn, petSn, LocalDateTime.now(ZONE));
    }

    HealthIndicatorResponse get(Long mbrSn, Long petSn, LocalDateTime now) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        LocalDate baseYmd = now.toLocalDate();
        return new HealthIndicatorResponse(baseYmd, now, weight(petSn),
                dailySum(petSn, DailyRecordIndicator.MEAL, baseYmd),
                dailySum(petSn, DailyRecordIndicator.WATER, baseYmd),
                dailySum(petSn, DailyRecordIndicator.WALK, baseYmd), creatinine(petSn));
    }

    private WeightIndicator weight(Long petSn) {
        List<HealthWeightPoint> points = dailyHealthRecordMapper.selectWeightPoints(petSn,
                DailyRecordIndicator.WEIGHT.getCode(), POINT_SIZE);
        if (points.isEmpty()) {
            return null;
        }
        BigDecimal latest = points.get(points.size() - 1).getRcdNvl();
        BigDecimal prev = points.size() > 1 ? points.get(points.size() - 2).getRcdNvl() : null;
        return new WeightIndicator(latest, diff(latest, prev), points);
    }

    private DailySumIndicator dailySum(Long petSn, DailyRecordIndicator indicator, LocalDate baseYmd) {
        List<HealthDailySumPoint> points = dailyHealthRecordMapper.selectDailySumPoints(petSn, indicator.getCode(),
                baseYmd, POINT_SIZE);
        if (points.isEmpty()) {
            return null;
        }
        HealthDailySumPoint last = points.get(points.size() - 1);
        BigDecimal todaySum = null;
        BigDecimal prevDaySum;
        if (baseYmd.equals(last.getRcdYmd())) {
            todaySum = last.getSumNvl();
            prevDaySum = points.size() > 1 ? points.get(points.size() - 2).getSumNvl() : null;
        } else {
            prevDaySum = last.getSumNvl();
        }
        return new DailySumIndicator(todaySum, prevDaySum, diff(todaySum, prevDaySum), points);
    }

    private CreatinineIndicator creatinine(Long petSn) {
        List<HealthCreatininePoint> points = inspectionValueMapper.selectCreatininePoints(petSn,
                InspectionArticle.CREA.getCode(), POINT_SIZE);
        if (points.isEmpty()) {
            return null;
        }
        HealthCreatininePoint last = points.get(points.size() - 1);
        BigDecimal prev = points.size() > 1 ? points.get(points.size() - 2).getRsltNvl() : null;
        return new CreatinineIndicator(last.getRsltNvl(), last.getUnitNm(), last.getRefLlmtNvl(),
                last.getRefUplmtNvl(), diff(last.getRsltNvl(), prev), points);
    }

    private BigDecimal diff(BigDecimal current, BigDecimal prev) {
        if (current == null || prev == null) {
            return null;
        }
        return current.subtract(prev);
    }
}
