package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.dto.AdminLostReportResponse;
import com.nyangmeong.community.dto.AdminLostReportSearchCond;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 실종제보 목록·상세]
 */
class AdminLostReportServiceTest {

    private static final String NORMAL = "CMUC0003";
    private static final String MISSING = "CMUC0006";
    private static final String FOUND = "CMUC0007";
    private static final String EXPIRED = "CMUC0008";
    private static final Long LOST = 5L;
    private static final Long PST = 9L;

    private final LostReportMapper lostReportMapper = mock(LostReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final AdminLostReportService service = new AdminLostReportService(lostReportMapper, postMapper);

    private LostReportDetail detail(String sttsCd, LocalDateTime expryCrtrDt) {
        LostReportDetail detail = new LostReportDetail();
        detail.setLostTpofSn(LOST);
        detail.setPstSn(PST);
        detail.setPstTtl("푸들 초코를 찾습니다");
        detail.setLostTpofSttsCd(sttsCd);
        detail.setExpryCrtrDt(expryCrtrDt);
        detail.setDscvSwtcDt(FOUND.equals(sttsCd) ? LocalDateTime.of(2026, 10, 2, 9, 0) : null);
        detail.setMbrSn(2L);
        detail.setMbrNcnm("초코맘");
        detail.setPstCn("본문");
        detail.setPstRegDt(LocalDateTime.of(2026, 10, 29, 10, 0));
        return detail;
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 상태_필터를_코드와_함께_넘긴다() {
        AdminLostReportSearchCond cond = new AdminLostReportSearchCond();
        cond.setLostTpofSttsCd(EXPIRED);

        service.search(cond, new PageQuery(null, null, null));

        verify(lostReportMapper).selectAdminList(EXPIRED, NORMAL, MISSING, FOUND, EXPIRED);
    }

    @Test
    void 빈_상태는_전체() {
        AdminLostReportSearchCond cond = new AdminLostReportSearchCond();
        cond.setLostTpofSttsCd(" ");

        service.search(cond, new PageQuery(null, null, null));

        verify(lostReportMapper).selectAdminList(null, NORMAL, MISSING, FOUND, EXPIRED);
    }

    @Test
    void 모르는_상태_코드는_400() {
        AdminLostReportSearchCond cond = new AdminLostReportSearchCond();
        cond.setLostTpofSttsCd("CMUC0003");

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(lostReportMapper, never()).selectAdminList(any(), any(), any(), any(), any());
    }

    @Test
    void 실종_글은_본문_사진_만료_기준일을_준다() {
        LocalDateTime expry = LocalDateTime.now().plusDays(10);
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(MISSING, expry));
        when(postMapper.selectImages(PST)).thenReturn(List.of());

        AdminLostReportResponse res = service.get(LOST);

        assertThat(res.lostTpofSttsCd()).isEqualTo(MISSING);
        assertThat(res.pstCn()).isEqualTo("본문");
        assertThat(res.mbrNcnm()).isEqualTo("초코맘");
        assertThat(res.expryCrtrDt()).isEqualTo(expry);
        assertThat(res.imgs()).isEmpty();
    }

    @Test
    void 기간_지난_실종_글은_만료로_내린다() {
        LocalDateTime expry = LocalDateTime.now().minusDays(1);
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(MISSING, expry));
        when(postMapper.selectImages(PST)).thenReturn(List.of());

        AdminLostReportResponse res = service.get(LOST);

        assertThat(res.lostTpofSttsCd()).isEqualTo(EXPIRED);
        assertThat(res.expryCrtrDt()).isEqualTo(expry);
        assertThat(res.pstCn()).isEqualTo("본문");
    }

    @Test
    void 찾았어요_글은_제목_상태_처리_시각만_준다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(FOUND, LocalDateTime.now()));

        AdminLostReportResponse res = service.get(LOST);

        assertThat(res.lostTpofSn()).isEqualTo(LOST);
        assertThat(res.pstSn()).isEqualTo(PST);
        assertThat(res.pstTtl()).isEqualTo("푸들 초코를 찾습니다");
        assertThat(res.lostTpofSttsCd()).isEqualTo(FOUND);
        assertThat(res.dscvSwtcDt()).isNotNull();
        assertThat(res.mbrSn()).isNull();
        assertThat(res.mbrNcnm()).isNull();
        assertThat(res.regDt()).isNull();
        assertThat(res.pstCn()).isNull();
        assertThat(res.expryCrtrDt()).isNull();
        assertThat(res.imgs()).isNull();
        verify(postMapper, never()).selectImages(anyLong());
    }

    @Test
    void 조회수를_올리지_않는다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, LocalDateTime.now().plusDays(1)));
        when(postMapper.selectImages(PST)).thenReturn(List.of());

        service.get(LOST);

        verify(postMapper, never()).insertViewIgnore(anyLong(), anyLong());
        verify(postMapper, never()).updateInqCnt(anyLong());
    }

    @Test
    void 없는_번호는_404() {
        assertError(() -> service.get(LOST), ErrorCode.LOST_REPORT_NOT_FOUND);
    }
}
