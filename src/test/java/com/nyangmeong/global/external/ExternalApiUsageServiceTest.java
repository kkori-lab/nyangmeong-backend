package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 외부 API 호출 로그 기록·사용량 조회]
 */
class ExternalApiUsageServiceTest {

    private static final String SUCCESS = "SYSC0021";

    private final ExternalApiCallLogMapper mapper = mock(ExternalApiCallLogMapper.class);
    private final ExternalApiUsageService service = new ExternalApiUsageService(mapper);

    @Test
    void 호출이_없는_종류도_명세_7종_순서로_0을_돌려준다() {
        ExternalApiUsageSummary ocr = new ExternalApiUsageSummary();
        ocr.setExtlApiSeCd("SYSC0016");
        ocr.setCallCnt(200);
        ocr.setFailCnt(3);
        ocr.setAvgRspnsMs(3400L);
        when(mapper.selectUsage(any(), eq(SUCCESS))).thenReturn(List.of(ocr));

        List<ExternalApiUsageSummary> result = service.search(new ExternalApiUsageSearchCond());

        assertThat(result).extracting(ExternalApiUsageSummary::getExtlApiNm)
                .containsExactly("AI 대화", "AI 분류", "OCR", "캐릭터 생성", "PDF 분석", "카카오 로컬", "메일");
        assertThat(result.get(0).getCallCnt()).isZero();
        assertThat(result.get(0).getFailRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(2).getFailRate()).isEqualByComparingTo("1.5");
    }

    @Test
    void 종류를_고르면_그_종류만_돌려준다() {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        cond.setExtlApiSeCd("SYSC0020");
        when(mapper.selectUsage(cond, SUCCESS)).thenReturn(List.of());

        assertThat(service.search(cond)).extracting(ExternalApiUsageSummary::getExtlApiNm).containsExactly("메일");
    }

    @Test
    void 기간이_없으면_오늘까지_7일로_채운다() {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        when(mapper.selectDailyUsage(cond, SUCCESS)).thenReturn(List.of());

        service.searchDaily(cond);

        assertThat(cond.getEndYmd()).isEqualTo(LocalDate.now());
        assertThat(cond.getBgngYmd()).isEqualTo(LocalDate.now().minusDays(6));
    }

    @Test
    void 기간이_92일이면_조회한다() {
        ExternalApiUsageSearchCond cond = period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31));
        when(mapper.selectDailyUsage(cond, SUCCESS)).thenReturn(List.of());

        service.searchDaily(cond);

        verify(mapper).selectDailyUsage(cond, SUCCESS);
    }

    @Test
    void 기간이_93일이면_INVALID_INPUT_VALUE() {
        assertInvalid(period(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void 시작일이_종료일보다_뒤면_INVALID_INPUT_VALUE() {
        assertInvalid(period(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 30)));
    }

    @Test
    void 조회_종류가_7종_밖이면_INVALID_INPUT_VALUE() {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        cond.setExtlApiSeCd("SYSC0019");

        assertInvalid(cond);
    }

    @Test
    void 토큰은_AI_코드만_넘겨_조회한다() {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        when(mapper.selectDailyTokens(cond, List.of("SYSC0015", "SYSC0024"))).thenReturn(List.of());

        service.searchTokens(cond);

        verify(mapper).selectDailyTokens(cond, List.of("SYSC0015", "SYSC0024"));
    }

    @Test
    void 실패_목록은_성공_코드를_넘겨_페이지로_돌려준다() {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        ExternalApiFailureSummary row = new ExternalApiFailureSummary();
        row.setExtlApiSeCd("SYSC0016");
        when(mapper.selectFailures(cond, SUCCESS)).thenReturn(List.of(row));

        var result = service.searchFailures(cond, new PageQuery(null, null, null));
        PageHelper.clearPage();

        assertThat(result.content()).containsExactly(row);
        assertThat(row.getExtlApiNm()).isEqualTo("OCR");
    }

    @Test
    void 통지를_회원과_함께_저장한다() {
        service.record(5L, new ExternalApiCallRequest("SYSC0015", "SYSC0021", null, 2100L, 200, 120L, 80L, null));

        ArgumentCaptor<ExternalApiCallLog> log = ArgumentCaptor.forClass(ExternalApiCallLog.class);
        verify(mapper).insert(log.capture());
        assertThat(log.getValue().getMbrSn()).isEqualTo(5L);
        assertThat(log.getValue().getExtlApiSeCd()).isEqualTo("SYSC0015");
        assertThat(log.getValue().getInptTknCnt()).isEqualTo(120L);
        assertThat(log.getValue().getOtptTknCnt()).isEqualTo(80L);
    }

    @Test
    void 없는_구분이나_결과_코드는_저장하지_않는다() {
        assertInvalidRecord(new ExternalApiCallRequest("SYSC0019", "SYSC0021", null, null, null, null, null, null));
        assertInvalidRecord(new ExternalApiCallRequest("SYSC0015", "SYSC0014", null, null, null, null, null, null));
    }

    private ExternalApiUsageSearchCond period(LocalDate bgngYmd, LocalDate endYmd) {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        cond.setBgngYmd(bgngYmd);
        cond.setEndYmd(endYmd);
        return cond;
    }

    private void assertInvalid(ExternalApiUsageSearchCond cond) {
        assertThatThrownBy(() -> service.searchDaily(cond))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verifyNoInteractions(mapper);
    }

    private void assertInvalidRecord(ExternalApiCallRequest req) {
        assertThatThrownBy(() -> service.record(5L, req))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verifyNoInteractions(mapper);
    }
}
