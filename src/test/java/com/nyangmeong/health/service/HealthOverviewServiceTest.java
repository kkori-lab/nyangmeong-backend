package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.CheckupAttachmentSearchCond;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.dto.PrescriptionSearchCond;
import com.nyangmeong.health.dto.PrescriptionSummary;
import com.nyangmeong.health.dto.RepeatPreventionSearchCond;
import com.nyangmeong.health.dto.RepeatPreventionSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupAttachmentMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;
import com.nyangmeong.health.mapper.PrescriptionMapper;
import com.nyangmeong.health.mapper.RepeatPreventionMapper;

/**
 * [검사 - 종합 건강기록 반려동물 전체 조회]
 * - ckupRcdSn 을 생략하면 완료 검진기록 전체로 첨부·검사 수치·처방약·반복 예방을 읽음 (FN-HLT-011-03·06~09)
 */
class HealthOverviewServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP_OLD = 500L;
    private static final Long CKUP_NEW = 501L;

    private final CheckupService checkupService = mock(CheckupService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);

    private final InspectionValueMapper inspectionMapper = mock(InspectionValueMapper.class);
    private final InspectionValueService inspectionService = new InspectionValueService(inspectionMapper,
            checkupService, petAuthorizer);

    private final PrescriptionMapper prescriptionMapper = mock(PrescriptionMapper.class);
    private final PrescriptionService prescriptionService = new PrescriptionService(prescriptionMapper,
            checkupService, mock(CareItemService.class), petAuthorizer);

    private final CheckupAttachmentMapper attachmentMapper = mock(CheckupAttachmentMapper.class);
    private final CheckupAttachmentService attachmentService = new CheckupAttachmentService(attachmentMapper,
            inspectionMapper, mock(CareScheduleMapper.class), checkupService, petAuthorizer, mock(FileStore.class),
            new InspectionAnalyzer());

    private final RepeatPreventionMapper repeatMapper = mock(RepeatPreventionMapper.class);
    private final RepeatPreventionService repeatService = new RepeatPreventionService(repeatMapper,
            mock(CareScheduleMapper.class), checkupService, mock(CareItemService.class), petAuthorizer);

    private CheckupSummary checkup(Long sn, LocalDate ymd, String hospital) {
        CheckupSummary summary = new CheckupSummary();
        summary.setCkupRcdSn(sn);
        summary.setCkupYmd(ymd);
        summary.setHsptlNm(hospital);
        return summary;
    }

    /** 검진일 최신순으로 오는 전체 검진기록 : 501(9월) → 500(5월) */
    private void givenCheckups() {
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of(
                checkup(CKUP_NEW, LocalDate.of(2026, 9, 9), "늘봄동물병원"),
                checkup(CKUP_OLD, LocalDate.of(2026, 5, 10), "행복동물병원")));
    }

    private void givenNoCheckups() {
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of());
    }

    private InspectionValueSummary value(Long sn, Long ckupRcdSn) {
        InspectionValueSummary value = new InspectionValueSummary();
        value.setInspNvlSn(sn);
        value.setCkupRcdSn(ckupRcdSn);
        value.setRsltNvl(BigDecimal.ONE);
        return value;
    }

    @Test
    void 검사_수치는_검진일_오름차순으로_주고_행마다_검진일을_채운다() {
        givenCheckups();
        // 매퍼는 검진기록 번호순(500 → 501)
        when(inspectionMapper.selectListByCheckups(any(), any())).thenReturn(List.of(value(1L, CKUP_OLD),
                value(2L, CKUP_OLD), value(3L, CKUP_NEW)));

        List<InspectionValueSummary> rows = inspectionService.search(MBR, PET, new InspectionValueSearchCond());

        assertThat(rows).extracting(InspectionValueSummary::getInspNvlSn).containsExactly(1L, 2L, 3L);
        assertThat(rows.get(0).getSchdlDt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 0, 0));
        assertThat(rows.get(2).getSchdlDt()).isEqualTo(LocalDateTime.of(2026, 9, 9, 0, 0));
    }

    @Test
    void 검사_수치는_번호가_큰_검진기록이_더_이른_검진일이면_날짜순으로_바꿔_준다() {
        when(checkupService.searchAllCompleted(MBR, PET)).thenReturn(List.of(
                checkup(CKUP_OLD, LocalDate.of(2026, 9, 9), "늘봄동물병원"),
                checkup(CKUP_NEW, LocalDate.of(2026, 5, 10), "행복동물병원")));
        when(inspectionMapper.selectListByCheckups(any(), any()))
                .thenReturn(List.of(value(1L, CKUP_OLD), value(2L, CKUP_NEW)));

        List<InspectionValueSummary> rows = inspectionService.search(MBR, PET, new InspectionValueSearchCond());

        assertThat(rows).extracting(InspectionValueSummary::getInspNvlSn).containsExactly(2L, 1L);
    }

    @Test
    void 검진기록이_없으면_조회_없이_빈_목록이다() {
        givenNoCheckups();

        assertThat(inspectionService.search(MBR, PET, new InspectionValueSearchCond())).isEmpty();
        assertThat(prescriptionService.search(MBR, PET, new PrescriptionSearchCond())).isEmpty();
        assertThat(repeatService.search(MBR, PET, new RepeatPreventionSearchCond())).isEmpty();
        assertThat(attachmentService.search(MBR, PET, new CheckupAttachmentSearchCond(),
                new PageQuery(null, 4, null)).content()).isEmpty();

        verify(inspectionMapper, never()).selectListByCheckups(any(), any());
        verify(prescriptionMapper, never()).selectListByCheckups(any(), any(), any());
        verify(repeatMapper, never()).selectListByCheckups(any());
        verify(attachmentMapper, never()).selectListByCheckups(any(), any());
    }

    @Test
    void 검진기록_번호를_주면_기존처럼_그_검진기록_하나만_조회한다() {
        InspectionValueSearchCond cond = new InspectionValueSearchCond();
        cond.setCkupRcdSn(CKUP_OLD);
        when(checkupService.getCheckup(MBR, PET, CKUP_OLD)).thenReturn(new CheckupResponse());

        inspectionService.search(MBR, PET, cond);

        verify(checkupService).getCheckup(MBR, PET, CKUP_OLD);
        verify(inspectionMapper).selectList(cond);
        verify(checkupService, never()).searchAllCompleted(any(), any());
    }

    @Test
    void 처방약은_전체_검진기록_번호로_복용중_번호를_넘겨_조회한다() {
        givenCheckups();
        PrescriptionSummary taking = new PrescriptionSummary();
        taking.setPrscSn(7L);
        PrescriptionSearchCond cond = new PrescriptionSearchCond();
        cond.setPrscSttsCd(PrescriptionStatus.TAKING.getCode());
        when(prescriptionMapper.selectListByCheckups(any(), eq(cond), eq(PrescriptionStatus.TAKING.getCode())))
                .thenReturn(List.of(taking));

        List<PrescriptionSummary> rows = prescriptionService.search(MBR, PET, cond);

        assertThat(rows).extracting(PrescriptionSummary::getPrscSn).containsExactly(7L);
        verify(prescriptionMapper).selectListByCheckups(eq(List.of(CKUP_NEW, CKUP_OLD)), eq(cond),
                eq(PrescriptionStatus.TAKING.getCode()));
    }

    @Test
    void 첨부는_전체_검진기록으로_조회하고_전체_건수를_준다() {
        givenCheckups();
        when(attachmentMapper.selectListByCheckups(any(), any()))
                .thenReturn(List.of(new CheckupAttachmentSummary()));

        PageResponse<CheckupAttachmentSummary> page = attachmentService.search(MBR, PET,
                new CheckupAttachmentSearchCond(), new PageQuery(null, 4, null));

        assertThat(page.content()).hasSize(1);
        verify(attachmentMapper).selectListByCheckups(eq(List.of(CKUP_NEW, CKUP_OLD)), any());
    }

    @Test
    void 반복_예방은_시작일_최신순이고_출처_검진기록의_병원명을_채운다() {
        givenCheckups();
        when(repeatMapper.selectListByCheckups(any())).thenReturn(List.of(
                prevention(1L, CKUP_OLD, LocalDate.of(2026, 5, 10)),
                prevention(2L, CKUP_NEW, LocalDate.of(2026, 9, 9))));

        List<RepeatPreventionSummary> rows = repeatService.search(MBR, PET, new RepeatPreventionSearchCond());

        assertThat(rows).extracting(RepeatPreventionSummary::getReptPrvntMngSn).containsExactly(2L, 1L);
        assertThat(rows).extracting(RepeatPreventionSummary::getHsptlNm).containsExactly("늘봄동물병원", "행복동물병원");
    }

    @Test
    void 다른_회원의_반려동물은_전체_조회도_거절한다() {
        doThrow(new CustomException(ErrorCode.FORBIDDEN)).when(checkupService).searchAllCompleted(MBR, PET);

        assertThatThrownBy(() -> repeatService.search(MBR, PET, new RepeatPreventionSearchCond()))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> inspectionService.search(MBR, PET, new InspectionValueSearchCond()))
                .isInstanceOf(CustomException.class);
    }

    private RepeatPreventionSummary prevention(Long sn, Long ckupRcdSn, LocalDate bgngYmd) {
        RepeatPreventionSummary summary = new RepeatPreventionSummary();
        summary.setReptPrvntMngSn(sn);
        summary.setCkupRcdSn(ckupRcdSn);
        summary.setBgngYmd(bgngYmd);
        return summary;
    }
}
