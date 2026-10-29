package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CheckupRecord;
import com.nyangmeong.health.domain.CheckupStatus;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.Schedule;
import com.nyangmeong.health.domain.ScheduleKind;
import com.nyangmeong.health.domain.ScheduleSource;
import com.nyangmeong.health.dto.CheckupCreateRequest;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSearchCond;
import com.nyangmeong.health.dto.CheckupUpdateRequest;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupMapper;

/**
 * [검사 - 검진기록]
 */
class CheckupServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long OTHER_PET = 20L;
    private static final Long CKUP = 500L;
    private static final Long SCHDL = 100L;
    private static final String TYPE = "HLTC0001";
    private static final String HOSPITAL = ScheduleKind.HOSPITAL.getCode();
    private static final String COMPLETED = CheckupStatus.COMPLETED.getCode();
    private static final String DELETED = CheckupStatus.DELETED.getCode();
    private static final LocalDate YMD = LocalDate.of(2026, 11, 3);

    private final CheckupMapper checkupMapper = mock(CheckupMapper.class);
    private final CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CheckupService service = new CheckupService(checkupMapper, careScheduleMapper, petAuthorizer);

    @BeforeEach
    void setUp() {
        when(careScheduleMapper.existsChildCode(HealthCodeGroup.CHECKUP_TYPE.getCode(), TYPE)).thenReturn(true);
        /** 등록 때 생성되는 번호 */
        doAnswer(call -> {
            CheckupRecord record = call.getArgument(0);
            record.setCkupRcdSn(CKUP);
            return 1;
        }).when(checkupMapper).insertCheckup(any());
        doAnswer(call -> {
            Schedule schedule = call.getArgument(0);
            schedule.setSchdlSn(777L);
            return 1;
        }).when(careScheduleMapper).insertSchedule(any());
        when(checkupMapper.selectDetail(CKUP, COMPLETED)).thenReturn(detail(PET, SCHDL, YMD));
    }

    private CheckupResponse detail(Long petSn, Long schdlSn, LocalDate ymd) {
        CheckupResponse response = new CheckupResponse();
        response.setCkupRcdSn(CKUP);
        response.setPetSn(petSn);
        response.setSchdlSn(schdlSn);
        response.setCkupYmd(ymd);
        response.setHsptlNm("튼튼동물병원");
        response.setCkupTypeCd(TYPE);
        response.setCkupCn("내용");
        response.setCkupMemoCn("메모");
        return response;
    }

    private CheckupCreateRequest create(LocalDate ymd, Long schdlSn) {
        return new CheckupCreateRequest(ymd, " 튼튼동물병원 ", TYPE, "내용", " ", schdlSn);
    }

    private Schedule hospitalSchedule(Long schdlSn, Long petSn, LocalDate ymd, String hrInptYn) {
        Schedule schedule = new Schedule();
        schedule.setSchdlSn(schdlSn);
        schedule.setPetSn(petSn);
        schedule.setSchdlKndCd(HOSPITAL);
        schedule.setSchdlSrcCd(ScheduleSource.DIRECT.getCode());
        schedule.setSchdlDt("Y".equals(hrInptYn) ? ymd.atTime(14, 30) : ymd.atStartOfDay());
        schedule.setHrInptYn(hrInptYn);
        schedule.setCkupTypeCd(TYPE);
        schedule.setHsptlNm("원래병원");
        return schedule;
    }

    private void assertInvalid(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 목록은_남의_반려동물이면_권한_오류로_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.FORBIDDEN)).when(petAuthorizer).checkCaregiver(MBR, OTHER_PET);

        assertThatThrownBy(() -> service.search(MBR, OTHER_PET, new CheckupSearchCond(), new PageQuery(1, 10, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(checkupMapper, never()).selectList(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void 같은_날짜_병원_일정이_있으면_그_일정에_연결하고_일정을_만들지_않는다() {
        when(checkupMapper.selectHospitalScheduleSn(PET, YMD, HOSPITAL)).thenReturn(SCHDL);

        service.create(MBR, PET, create(YMD, null));

        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).insertCheckup(record.capture());
        assertThat(record.getValue().getSchdlSn()).isEqualTo(SCHDL);
        assertThat(record.getValue().getHsptlNm()).isEqualTo("튼튼동물병원");
        assertThat(record.getValue().getCkupMemoCn()).isNull();
        assertThat(record.getValue().getCkupRcdSttsCd()).isEqualTo(COMPLETED);
        verify(careScheduleMapper, never()).insertSchedule(any());
        verify(checkupMapper).insertStatusHistory(CKUP, MBR, null, COMPLETED);
    }

    @Test
    void 같은_날짜_병원_일정이_없으면_병원_일정을_하나_만들어_연결한다() {
        when(checkupMapper.selectHospitalScheduleSn(PET, YMD, HOSPITAL)).thenReturn(null);

        service.create(MBR, PET, create(YMD, null));

        ArgumentCaptor<Schedule> schedule = ArgumentCaptor.forClass(Schedule.class);
        verify(careScheduleMapper).insertSchedule(schedule.capture());
        assertThat(schedule.getValue().getSchdlKndCd()).isEqualTo(HOSPITAL);
        assertThat(schedule.getValue().getSchdlSrcCd()).isEqualTo(ScheduleSource.CHECKUP.getCode());
        assertThat(schedule.getValue().getSchdlDt()).isEqualTo(YMD.atStartOfDay());
        assertThat(schedule.getValue().getHrInptYn()).isEqualTo("N");
        assertThat(schedule.getValue().getHsptlNm()).isEqualTo("튼튼동물병원");
        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).insertCheckup(record.capture());
        assertThat(record.getValue().getSchdlSn()).isEqualTo(777L);
    }

    @Test
    void 병원_일정_조회는_병원_종류_코드로만_해서_미용_일정만_있으면_새로_만든다() {
        when(checkupMapper.selectHospitalScheduleSn(eq(PET), eq(YMD), eq(HOSPITAL))).thenReturn(null);

        service.create(MBR, PET, create(YMD, null));

        verify(checkupMapper).selectHospitalScheduleSn(PET, YMD, HOSPITAL);
        verify(careScheduleMapper).insertSchedule(any());
    }

    @Test
    void 삭제한_검진기록과_같은_날짜로_다시_등록하면_남아_있던_병원_일정에_연결한다() {
        when(checkupMapper.selectHospitalScheduleSn(PET, YMD, HOSPITAL)).thenReturn(SCHDL);
        when(checkupMapper.selectDetail(CKUP, COMPLETED)).thenReturn(detail(PET, SCHDL, YMD));

        service.delete(MBR, PET, CKUP);
        service.create(MBR, PET, create(YMD, null));

        verify(checkupMapper).updateStatus(eq(CKUP), eq(DELETED), anyString());
        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).insertCheckup(record.capture());
        assertThat(record.getValue().getSchdlSn()).isEqualTo(SCHDL);
        verify(careScheduleMapper, never()).insertSchedule(any());
        verify(careScheduleMapper, never()).deleteSchedule(anyLong(), anyString());
    }

    @Test
    void 일정번호가_있으면_일정_날짜를_검진일로_옮기고_시각은_유지한다() {
        Schedule target = hospitalSchedule(SCHDL, PET, YMD.minusDays(7), "Y");
        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(target);
        when(checkupMapper.existsLinkedCheckup(SCHDL, COMPLETED)).thenReturn(false);

        service.create(MBR, PET, create(YMD, SCHDL));

        ArgumentCaptor<Schedule> moved = ArgumentCaptor.forClass(Schedule.class);
        verify(careScheduleMapper).updateSchedule(moved.capture(), eq("1"));
        assertThat(moved.getValue().getSchdlDt()).isEqualTo(LocalDateTime.of(2026, 11, 3, 14, 30));
        assertThat(moved.getValue().getHsptlNm()).isEqualTo("원래병원");
        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).insertCheckup(record.capture());
        assertThat(record.getValue().getSchdlSn()).isEqualTo(SCHDL);
        verify(checkupMapper, never()).selectHospitalScheduleSn(anyLong(), any(), anyString());
    }

    @Test
    void 옮겨_연결할_일정이_8일_떨어졌으면_400() {
        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(hospitalSchedule(SCHDL, PET, YMD.minusDays(8), "N"));

        assertInvalid(() -> service.create(MBR, PET, create(YMD, SCHDL)));
        verify(checkupMapper, never()).insertCheckup(any());
        verify(careScheduleMapper, never()).updateSchedule(any(), anyString());
    }

    @Test
    void 옮겨_연결할_일정이_이미_검진기록에_연결됐으면_400() {
        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(hospitalSchedule(SCHDL, PET, YMD, "N"));
        when(checkupMapper.existsLinkedCheckup(SCHDL, COMPLETED)).thenReturn(true);

        assertInvalid(() -> service.create(MBR, PET, create(YMD, SCHDL)));
        verify(checkupMapper, never()).insertCheckup(any());
    }

    @Test
    void 옮겨_연결할_일정이_다른_반려동물_것이거나_병원_일정이_아니면_400() {
        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(hospitalSchedule(SCHDL, OTHER_PET, YMD, "N"));
        assertInvalid(() -> service.create(MBR, PET, create(YMD, SCHDL)));

        Schedule grooming = hospitalSchedule(SCHDL, PET, YMD, "N");
        grooming.setSchdlKndCd(ScheduleKind.GROOMING.getCode());
        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(grooming);
        assertInvalid(() -> service.create(MBR, PET, create(YMD, SCHDL)));

        when(careScheduleMapper.selectSchedule(SCHDL)).thenReturn(null);
        assertInvalid(() -> service.create(MBR, PET, create(YMD, SCHDL)));
    }

    @Test
    void 검진유형이_공통코드에_없으면_400() {
        when(careScheduleMapper.existsChildCode(HealthCodeGroup.CHECKUP_TYPE.getCode(), "HLTC9999")).thenReturn(false);

        assertInvalid(() -> service.create(MBR, PET,
                new CheckupCreateRequest(YMD, "병원", "HLTC9999", null, null, null)));
        verify(checkupMapper, never()).insertCheckup(any());
    }

    @Test
    void 메모만_수정하면_일정_연결은_그대로다() {
        service.update(MBR, PET, CKUP, new CheckupUpdateRequest(null, null, null, null, null, "새 메모"));

        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).updateCheckup(record.capture(), eq("1"));
        assertThat(record.getValue().getSchdlSn()).isEqualTo(SCHDL);
        assertThat(record.getValue().getCkupMemoCn()).isEqualTo("새 메모");
        assertThat(record.getValue().getCkupCn()).isEqualTo("내용");
        verify(checkupMapper, never()).selectHospitalScheduleSn(anyLong(), any(), anyString());
        verify(careScheduleMapper, never()).insertSchedule(any());
        verify(checkupMapper, never()).updateLinkedCareItemsPet(anyLong(), anyLong(), anyString());
    }

    @Test
    void 검진일을_바꾸면_새_날짜_병원_일정에_연결하고_원래_일정은_남긴다() {
        LocalDate next = YMD.plusDays(30);
        when(checkupMapper.selectHospitalScheduleSn(PET, next, HOSPITAL)).thenReturn(null);

        service.update(MBR, PET, CKUP, new CheckupUpdateRequest(null, next, null, null, null, null));

        ArgumentCaptor<Schedule> schedule = ArgumentCaptor.forClass(Schedule.class);
        verify(careScheduleMapper).insertSchedule(schedule.capture());
        assertThat(schedule.getValue().getSchdlDt()).isEqualTo(next.atStartOfDay());
        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).updateCheckup(record.capture(), eq("1"));
        assertThat(record.getValue().getSchdlSn()).isEqualTo(777L);
        verify(careScheduleMapper, never()).deleteSchedule(anyLong(), anyString());
    }

    @Test
    void 반려동물을_바꾸면_케어_항목과_반복_예방을_옮기고_바꿀_반려동물도_권한을_확인한다() {
        when(checkupMapper.selectHospitalScheduleSn(OTHER_PET, YMD, HOSPITAL)).thenReturn(300L);
        when(checkupMapper.selectDetail(CKUP, COMPLETED)).thenReturn(detail(PET, SCHDL, YMD))
                .thenReturn(detail(OTHER_PET, 300L, YMD));

        CheckupResponse response = service.update(MBR, PET, CKUP,
                new CheckupUpdateRequest(OTHER_PET, null, null, null, null, null));

        verify(petAuthorizer).checkCaregiver(MBR, OTHER_PET);
        ArgumentCaptor<CheckupRecord> record = ArgumentCaptor.forClass(CheckupRecord.class);
        verify(checkupMapper).updateCheckup(record.capture(), eq("1"));
        assertThat(record.getValue().getSchdlSn()).isEqualTo(300L);
        verify(checkupMapper).updateLinkedCareItemsPet(CKUP, OTHER_PET, "1");
        verify(checkupMapper).updateLinkedRepeatPreventionsPet(CKUP, OTHER_PET, "1");
        assertThat(response.getPetSn()).isEqualTo(OTHER_PET);
    }

    @Test
    void 수정은_다른_반려동물의_검진기록이면_404() {
        assertThatThrownBy(() -> service.update(MBR, OTHER_PET, CKUP,
                new CheckupUpdateRequest(null, null, null, null, null, "메모")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_NOT_FOUND);
        verify(checkupMapper, never()).updateCheckup(any(), anyString());
    }

    @Test
    void 삭제는_상태를_삭제로_바꾸고_이력과_연결_케어_항목을_지우되_일정은_건드리지_않는다() {
        service.delete(MBR, PET, CKUP);

        verify(checkupMapper).updateStatus(CKUP, DELETED, "1");
        verify(checkupMapper).insertStatusHistory(CKUP, MBR, COMPLETED, DELETED);
        verify(checkupMapper).deleteLinkedCareItems(CKUP, "1");
        verify(checkupMapper).deleteLinkedRepeatPreventions(CKUP, "1");
        verify(careScheduleMapper, never()).deleteSchedule(anyLong(), anyString());
    }

    @Test
    void 삭제는_없거나_이미_삭제된_검진기록이면_404() {
        when(checkupMapper.selectDetail(CKUP, COMPLETED)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(MBR, PET, CKUP))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_NOT_FOUND);
        verify(checkupMapper, never()).updateStatus(anyLong(), anyString(), anyString());
    }
}
