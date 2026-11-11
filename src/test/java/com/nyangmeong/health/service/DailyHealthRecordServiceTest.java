package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.DailyHealthRecord;
import com.nyangmeong.health.dto.DailyHealthRecordCreateRequest;
import com.nyangmeong.health.dto.DailyHealthRecordResponse;
import com.nyangmeong.health.dto.DailyHealthRecordSearchCond;
import com.nyangmeong.health.dto.DailyHealthRecordSummary;
import com.nyangmeong.health.dto.DailyHealthRecordUpdateRequest;
import com.nyangmeong.health.mapper.DailyHealthRecordMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 일상 건강기록 등록·조회·수정·삭제]
 * - 지표별 값 검증, 케어미션 반영, 체중 동기화 (FN-HLT-002-11·13)
 */
class DailyHealthRecordServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long OTHER_PET = 20L;
    private static final Long RCD = 700L;
    private static final String WEIGHT = "HLTC0014";
    private static final String MEAL = "HLTC0015";
    private static final String WATER = "HLTC0016";
    private static final String STOOL = "HLTC0017";
    private static final String WALK = "HLTC0018";
    private static final String NORMAL = "HLTC0019";
    private static final BigDecimal PET_KG = new BigDecimal("5.000");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 29, 8, 15);

    private final DailyHealthRecordMapper mapper = mock(DailyHealthRecordMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final PetService petService = mock(PetService.class);
    private final DailyHealthRecordService service = new DailyHealthRecordService(mapper, petAuthorizer,
            careMissionService, petService);

    @BeforeEach
    void setUp() {
        when(mapper.existsChildCode("HLTG10", NORMAL)).thenReturn(true);
        when(mapper.insertRecord(any())).thenAnswer(call -> {
            DailyHealthRecord record = call.getArgument(0);
            record.setDailyHlthRcdSn(RCD);
            return 1;
        });
        when(mapper.selectSummary(RCD)).thenReturn(new DailyHealthRecordSummary());
        when(petService.getPet(MBR, PET)).thenReturn(pet(PET_KG));
    }

    private static PetResponse pet(BigDecimal petBdwg) {
        return new PetResponse(PET, null, null, null, null, null, null, null, petBdwg, null, null, null, null,
                null, null, null, false, false, null, null, null, List.of());
    }

    private static DailyHealthRecord record(String indicator, BigDecimal value, String stool) {
        DailyHealthRecord record = new DailyHealthRecord();
        record.setDailyHlthRcdSn(RCD);
        record.setPetSn(PET);
        record.setDailyRcdIdctCd(indicator);
        record.setRcdNvl(value);
        record.setCrsdSttsCd(stool);
        record.setRcdDt(AT);
        return record;
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 체중을_등록하면_케어미션에_반영하고_최신_체중으로_프로필을_맞춘다() {
        when(mapper.selectLatestWeight(PET, WEIGHT)).thenReturn(new BigDecimal("4.200"));

        DailyHealthRecordResponse response = service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, new BigDecimal("4.2"), null));

        assertThat(response).isNotNull();
        verify(petAuthorizer).checkCaregiver(MBR, PET);
        verify(careMissionService).reflect(PET, CareMission.WEIGHT_RECORD, MBR);
        verify(petService).syncWeight(PET, new BigDecimal("4.200"), MBR);
    }

    @Test
    void 식사량을_등록하면_체중_동기화는_하지_않는다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(MEAL, AT, new BigDecimal("80"), null));

        verify(careMissionService).reflect(PET, CareMission.MEAL_RECORD, MBR);
        verify(petService, never()).syncWeight(anyLong(), any(), anyLong());
    }

    @Test
    void 배변상태는_상태_코드로_등록하고_수치는_받지_않는다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT, null, NORMAL));
        verify(careMissionService).reflect(PET, CareMission.STOOL_RECORD, MBR);

        assertInvalid(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT, null, null)));
        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(STOOL, AT, BigDecimal.ONE, NORMAL)));
        assertInvalid(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT, null, "HLTC9999")));
    }

    @Test
    void 수치_지표는_수치가_필수이고_0_이하나_상태_코드는_400이다() {
        assertInvalid(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT, null, null)));
        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, BigDecimal.ZERO, null)));
        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, BigDecimal.ONE, NORMAL)));
        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest("HLTC0099", AT, BigDecimal.ONE, null)));
        verify(mapper, never()).insertRecord(any());
    }

    @Test
    void 체중은_200kg까지_등록하고_넘으면_400이다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT, new BigDecimal("200"), null));
        verify(mapper).insertRecord(any());

        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, new BigDecimal("200.001"), null)));
        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, new BigDecimal("999"), null)));
        verify(mapper).insertRecord(any());
    }

    @Test
    void 체중_상한은_식사량에_적용하지_않는다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(MEAL, AT, new BigDecimal("250"), null));

        verify(mapper).insertRecord(any());
    }

    @Test
    void 체중을_200kg_넘게_수정하면_400이고_프로필을_맞추지_않는다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(WEIGHT, new BigDecimal("4.000"), null));

        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, new BigDecimal("999"), null)));
        verify(mapper, never()).updateRecord(any(), anyString());
        verify(petService, never()).syncWeight(anyLong(), any(), anyLong());
    }

    @Test
    void 산책은_120분까지_등록하고_넘으면_400이다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WALK, AT, new BigDecimal("120"), null));
        verify(mapper).insertRecord(any());

        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WALK, AT, new BigDecimal("120.01"), null)));
        verify(mapper).insertRecord(any());
    }

    @Test
    void 산책을_120분_넘게_수정하면_400이다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(WALK, new BigDecimal("30"), null));

        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, new BigDecimal("121"), null)));
        verify(mapper, never()).updateRecord(any(), anyString());
    }

    @Test
    void 산책_상한은_식사량에_적용하지_않는다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(MEAL, AT, new BigDecimal("250"), null));

        verify(mapper).insertRecord(any());
    }

    @Test
    void 음수량은_체중x100ml까지_등록하고_넘으면_400이다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WATER, AT, new BigDecimal("500"), null));
        verify(mapper).insertRecord(any());

        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WATER, AT, new BigDecimal("500.01"), null)));
        verify(mapper).insertRecord(any());
    }

    @Test
    void 식사량은_체중x50g까지_등록하고_넘으면_400이다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(MEAL, AT, new BigDecimal("250"), null));
        verify(mapper).insertRecord(any());

        assertInvalid(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(MEAL, AT, new BigDecimal("250.01"), null)));
        verify(mapper).insertRecord(any());
    }

    @Test
    void 음수량과_식사량을_상한_넘게_수정하면_400이다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(WATER, new BigDecimal("100"), null));
        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, new BigDecimal("500.01"), null)));

        when(mapper.selectRecord(PET, RCD)).thenReturn(record(MEAL, new BigDecimal("80"), null));
        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, new BigDecimal("250.01"), null)));
        verify(mapper, never()).updateRecord(any(), anyString());
    }

    @Test
    void 체중_비례_상한은_체중과_산책에_적용하지_않는다() {
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT, new BigDecimal("200"), null));
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WALK, AT, new BigDecimal("120"), null));

        verify(mapper, times(2)).insertRecord(any());
        verify(petService, never()).getPet(anyLong(), anyLong());
    }

    @Test
    void 권한이_없으면_등록하지_않는다() {
        doThrow(new CustomException(ErrorCode.FORBIDDEN)).when(petAuthorizer).checkCaregiver(MBR, OTHER_PET);

        assertThatThrownBy(() -> service.create(MBR, OTHER_PET,
                new DailyHealthRecordCreateRequest(WEIGHT, AT, BigDecimal.ONE, null)))
                .isInstanceOf(CustomException.class);
        verify(mapper, never()).insertRecord(any());
    }

    @Test
    void 목록은_지표_코드와_기간을_검사하고_경로_반려동물로_조회한다() {
        DailyHealthRecordSearchCond bad = new DailyHealthRecordSearchCond();
        bad.setDailyRcdIdctCd("HLTC0099");
        assertInvalid(() -> service.search(MBR, PET, bad));

        DailyHealthRecordSearchCond reversed = new DailyHealthRecordSearchCond();
        reversed.setFromYmd(LocalDate.of(2026, 10, 29));
        reversed.setToYmd(LocalDate.of(2026, 10, 28));
        assertInvalid(() -> service.search(MBR, PET, reversed));

        DailyHealthRecordSearchCond ok = new DailyHealthRecordSearchCond();
        ok.setDailyRcdIdctCd(WEIGHT);
        ok.setSize(1);
        service.search(MBR, PET, ok);
        assertThat(ok.getPetSn()).isEqualTo(PET);
        verify(mapper).selectList(ok);
    }

    @Test
    void 체중_기록을_고치면_남은_최신_체중으로_동기화하고_시각만_고쳐도_값은_유지한다() {
        DailyHealthRecord stored = record(WEIGHT, new BigDecimal("4.000"), null);
        when(mapper.selectRecord(PET, RCD)).thenReturn(stored);
        when(mapper.selectSummary(RCD)).thenReturn(new DailyHealthRecordSummary());
        when(mapper.selectLatestWeight(PET, WEIGHT)).thenReturn(new BigDecimal("4.500"));

        service.update(MBR, PET, RCD, new DailyHealthRecordUpdateRequest(AT.plusHours(1), null, null));

        assertThat(stored.getRcdNvl()).isEqualByComparingTo("4.000");
        assertThat(stored.getRcdDt()).isEqualTo(AT.plusHours(1));
        verify(mapper).updateRecord(stored, String.valueOf(MBR));
        verify(petService).syncWeight(PET, new BigDecimal("4.500"), MBR);
        verify(careMissionService, never()).reflect(anyLong(), any(), anyLong());
    }

    @Test
    void 지표와_맞지_않는_필드로_수정하면_400이다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(MEAL, new BigDecimal("80"), null));
        assertInvalid(() -> service.update(MBR, PET, RCD, new DailyHealthRecordUpdateRequest(null, null, NORMAL)));

        when(mapper.selectRecord(PET, RCD)).thenReturn(record(STOOL, null, NORMAL));
        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, BigDecimal.ONE, null)));
        assertInvalid(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(null, null, "HLTC9999")));
        verify(mapper, never()).updateRecord(any(), anyString());
    }

    @Test
    void 다른_반려동물의_기록은_404이다() {
        when(mapper.selectRecord(OTHER_PET, RCD)).thenReturn(null);

        assertThatThrownBy(() -> service.update(MBR, OTHER_PET, RCD,
                new DailyHealthRecordUpdateRequest(AT, null, null))).isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DAILY_HEALTH_RECORD_NOT_FOUND));
        assertThatThrownBy(() -> service.delete(MBR, OTHER_PET, RCD)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DAILY_HEALTH_RECORD_NOT_FOUND));
        verify(mapper, never()).deleteRecord(anyLong(), anyString());
    }

    @Test
    void 체중_기록을_지우면_남은_최신_체중으로_동기화하고_남은_기록이_없으면_하지_않는다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(WEIGHT, new BigDecimal("4.000"), null));
        when(mapper.selectLatestWeight(PET, WEIGHT)).thenReturn(new BigDecimal("3.900"));

        service.delete(MBR, PET, RCD);

        verify(mapper).deleteRecord(RCD, String.valueOf(MBR));
        verify(petService).syncWeight(eq(PET), eq(new BigDecimal("3.900")), eq(MBR));

        when(mapper.selectLatestWeight(PET, WEIGHT)).thenReturn(null);
        service.delete(MBR, PET, RCD);
        verify(petService).syncWeight(eq(PET), any(), eq(MBR));
    }

    @Test
    void 등록_서비스_검사_400은_틀린_칸_이름을_담는다() {
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest("HLTC9999", AT,
                new BigDecimal("1"), null)), "dailyRcdIdctCd");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT,
                new BigDecimal("1"), NORMAL)), "rcdNvl");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT, null, null)),
                "crsdSttsCd");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(STOOL, AT, null,
                "HLTC9999")), "crsdSttsCd");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT, null, null)),
                "rcdNvl");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT, null,
                NORMAL)), "crsdSttsCd");
        assertFields(() -> service.create(MBR, PET, new DailyHealthRecordCreateRequest(WEIGHT, AT,
                new BigDecimal("0"), null)), "rcdNvl");
    }

    @Test
    void 기록_시각은_지금_더하기_5분까지_등록하고_넘으면_400이다() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        service.create(MBR, PET, new DailyHealthRecordCreateRequest(WALK, now, new BigDecimal("30"), null));
        service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WALK, now.plusMinutes(5), new BigDecimal("30"), null));
        service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WALK, now.minusDays(3), new BigDecimal("30"), null));
        verify(mapper, times(3)).insertRecord(any());

        assertFields(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WALK, now.plusMinutes(6), new BigDecimal("30"), null)), "rcdDt");
        assertFields(() -> service.create(MBR, PET,
                new DailyHealthRecordCreateRequest(WALK, now.plusDays(1), new BigDecimal("30"), null)), "rcdDt");
        verify(mapper, times(3)).insertRecord(any());
    }

    @Test
    void 기록_시각을_미래로_수정하면_400이고_지난_시각은_저장한다() {
        when(mapper.selectRecord(PET, RCD)).thenReturn(record(WALK, new BigDecimal("30"), null));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        assertFields(() -> service.update(MBR, PET, RCD,
                new DailyHealthRecordUpdateRequest(now.plusMinutes(6), null, null)), "rcdDt");
        verify(mapper, never()).updateRecord(any(), anyString());

        service.update(MBR, PET, RCD, new DailyHealthRecordUpdateRequest(now.minusDays(1), null, null));
        verify(mapper).updateRecord(any(), anyString());
    }

    private static void assertFields(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String... fields) {
        assertThatThrownBy(call).isInstanceOfSatisfying(CustomException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
            assertThat(e.getFields()).containsExactly(fields);
        });
    }
}
