package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.dto.HealthCreatininePoint;
import com.nyangmeong.health.dto.HealthDailySumPoint;
import com.nyangmeong.health.dto.HealthIndicatorResponse;
import com.nyangmeong.health.dto.HealthWeightPoint;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;

/**
 * [검사 - 메인 건강 지표]
 * - 비교 대상 없음·오늘 기록 없음·기록 전부 없음 계산 (FN-MAN-005·006)
 */
class HealthIndicatorServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 14, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private final DailyHealthRecordMapper dailyMapper = mock(DailyHealthRecordMapper.class);
    private final InspectionValueMapper inspectionMapper = mock(InspectionValueMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final HealthIndicatorService service = new HealthIndicatorService(dailyMapper, inspectionMapper,
            petAuthorizer);

    private HealthWeightPoint weightPoint(int day, String nvl) {
        HealthWeightPoint p = new HealthWeightPoint();
        p.setRcdDt(LocalDateTime.of(2026, 11, day, 9, 0));
        p.setRcdNvl(new BigDecimal(nvl));
        return p;
    }

    private HealthDailySumPoint sumPoint(LocalDate ymd, String sum) {
        HealthDailySumPoint p = new HealthDailySumPoint();
        p.setRcdYmd(ymd);
        p.setSumNvl(new BigDecimal(sum));
        return p;
    }

    private HealthCreatininePoint creaPoint(int day, String nvl) {
        HealthCreatininePoint p = new HealthCreatininePoint();
        p.setSchdlDt(LocalDateTime.of(2026, 10, day, 10, 0));
        p.setRsltNvl(new BigDecimal(nvl));
        p.setUnitNm("mg/dL");
        p.setRefLlmtNvl(new BigDecimal("0.5"));
        p.setRefUplmtNvl(new BigDecimal("1.8"));
        return p;
    }

    private void emptyAll() {
        when(dailyMapper.selectWeightPoints(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(dailyMapper.selectDailySumPoints(anyLong(), anyString(), any(), anyInt())).thenReturn(List.of());
        when(inspectionMapper.selectCreatininePoints(anyLong(), anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    void 기록이_전부_없으면_기준일만_있고_위젯은_null이다() {
        emptyAll();

        HealthIndicatorResponse res = service.get(MBR, PET, NOW);

        assertThat(res.baseYmd()).isEqualTo(TODAY);
        assertThat(res.inqDt()).isEqualTo(NOW);
        assertThat(res.weight()).isNull();
        assertThat(res.meal()).isNull();
        assertThat(res.water()).isNull();
        assertThat(res.walk()).isNull();
        assertThat(res.creatinine()).isNull();
    }

    @Test
    void 체중은_마지막_점이_최신값이고_직전_기록과의_차이를_준다() {
        emptyAll();
        when(dailyMapper.selectWeightPoints(eq(PET), eq("HLTC0014"), eq(5)))
                .thenReturn(List.of(weightPoint(1, "4.0"), weightPoint(2, "4.2"), weightPoint(3, "4.5")));

        HealthIndicatorResponse.WeightIndicator weight = service.get(MBR, PET, NOW).weight();

        assertThat(weight.latestNvl()).isEqualByComparingTo("4.5");
        assertThat(weight.prevDiff()).isEqualByComparingTo("0.3");
        assertThat(weight.points()).hasSize(3);
    }

    @Test
    void 체중_기록이_하나뿐이면_대비는_null이다() {
        emptyAll();
        when(dailyMapper.selectWeightPoints(eq(PET), eq("HLTC0014"), eq(5)))
                .thenReturn(List.of(weightPoint(3, "4.5")));

        HealthIndicatorResponse.WeightIndicator weight = service.get(MBR, PET, NOW).weight();

        assertThat(weight.latestNvl()).isEqualByComparingTo("4.5");
        assertThat(weight.prevDiff()).isNull();
    }

    @Test
    void 오늘_기록이_있으면_오늘_합계와_직전_기록일_합계의_차이를_준다() {
        emptyAll();
        when(dailyMapper.selectDailySumPoints(eq(PET), eq("HLTC0015"), eq(TODAY), eq(5)))
                .thenReturn(List.of(sumPoint(TODAY.minusDays(3), "100"), sumPoint(TODAY, "250")));

        HealthIndicatorResponse.DailySumIndicator meal = service.get(MBR, PET, NOW).meal();

        assertThat(meal.todaySum()).isEqualByComparingTo("250");
        assertThat(meal.prevDaySum()).isEqualByComparingTo("100");
        assertThat(meal.prevDiff()).isEqualByComparingTo("150");
        assertThat(meal.points()).hasSize(2);
    }

    @Test
    void 오늘_기록이_없으면_오늘_합계는_null이고_마지막_기록일이_직전_기록일이다() {
        emptyAll();
        when(dailyMapper.selectDailySumPoints(eq(PET), eq("HLTC0016"), eq(TODAY), eq(5)))
                .thenReturn(List.of(sumPoint(TODAY.minusDays(5), "300"), sumPoint(TODAY.minusDays(2), "400")));

        HealthIndicatorResponse.DailySumIndicator water = service.get(MBR, PET, NOW).water();

        assertThat(water.todaySum()).isNull();
        assertThat(water.prevDaySum()).isEqualByComparingTo("400");
        assertThat(water.prevDiff()).isNull();
        assertThat(water.points()).hasSize(2);
    }

    @Test
    void 기록이_오늘_하루뿐이면_직전_기록일_합계와_대비가_null이다() {
        emptyAll();
        when(dailyMapper.selectDailySumPoints(eq(PET), eq("HLTC0018"), eq(TODAY), eq(5)))
                .thenReturn(List.of(sumPoint(TODAY, "30")));

        HealthIndicatorResponse.DailySumIndicator walk = service.get(MBR, PET, NOW).walk();

        assertThat(walk.todaySum()).isEqualByComparingTo("30");
        assertThat(walk.prevDaySum()).isNull();
        assertThat(walk.prevDiff()).isNull();
    }

    @Test
    void 크레아티닌은_마지막_검사의_값과_참고범위_지난_검사와의_차이를_준다() {
        emptyAll();
        when(inspectionMapper.selectCreatininePoints(eq(PET), eq("HLTC0030"), eq(5)))
                .thenReturn(List.of(creaPoint(1, "1.0"), creaPoint(20, "1.4")));

        HealthIndicatorResponse.CreatinineIndicator crea = service.get(MBR, PET, NOW).creatinine();

        assertThat(crea.latestNvl()).isEqualByComparingTo("1.4");
        assertThat(crea.unitNm()).isEqualTo("mg/dL");
        assertThat(crea.refLlmtNvl()).isEqualByComparingTo("0.5");
        assertThat(crea.refUplmtNvl()).isEqualByComparingTo("1.8");
        assertThat(crea.prevDiff()).isEqualByComparingTo("0.4");
        assertThat(crea.points()).hasSize(2);
    }

    @Test
    void 남의_반려동물이면_권한_예외가_나고_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, PET);

        assertThatThrownBy(() -> service.get(MBR, PET, NOW)).isInstanceOf(CustomException.class);

        verify(dailyMapper, never()).selectWeightPoints(anyLong(), anyString(), anyInt());
        verify(inspectionMapper, never()).selectCreatininePoints(anyLong(), anyString(), anyInt());
    }
}
