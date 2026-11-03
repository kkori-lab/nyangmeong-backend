package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 관리자 작업 로그 조회]
 */
class AdminTaskLogQueryTest {

    private final AdminTaskLogMapper adminTaskLogMapper = mock(AdminTaskLogMapper.class);
    private final AdminTaskLogService service = new AdminTaskLogService(adminTaskLogMapper,
            JsonMapper.builder().build());

    @Test
    void 조건을_그대로_넘겨_목록을_돌려준다() {
        AdminTaskLogSearchCond cond = new AdminTaskLogSearchCond();
        cond.setMbrSn(9L);
        cond.setTaskTypeCd("SYSC0002");
        AdminTaskLogSummary row = new AdminTaskLogSummary();
        row.setMngrTaskLogSn(1L);
        when(adminTaskLogMapper.selectList(cond)).thenReturn(List.of(row));

        var result = service.search(cond, new PageQuery(null, null, null));
        PageHelper.clearPage();

        assertThat(result.content()).containsExactly(row);
        verify(adminTaskLogMapper).selectList(cond);
    }

    @Test
    void 시작일이_종료일보다_뒤면_INVALID_INPUT_VALUE() {
        AdminTaskLogSearchCond cond = new AdminTaskLogSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 10, 31));
        cond.setEndYmd(LocalDate.of(2026, 10, 30));

        assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verifyNoInteractions(adminTaskLogMapper);
    }

    @Test
    void 시작일과_종료일이_같으면_조회한다() {
        AdminTaskLogSearchCond cond = new AdminTaskLogSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 10, 30));
        cond.setEndYmd(LocalDate.of(2026, 10, 30));
        when(adminTaskLogMapper.selectList(cond)).thenReturn(List.of());

        service.search(cond, new PageQuery(null, null, null));
        PageHelper.clearPage();

        verify(adminTaskLogMapper).selectList(cond);
    }

    @Test
    void 상세는_전후_값을_JSON_객체로_돌려준다() {
        AdminTaskLog log = new AdminTaskLog();
        log.setMngrTaskLogSn(1L);
        log.setMbrSn(9L);
        log.setChgBfrCn("{\"aiCnvrsTurnCnt\":6}");
        log.setChgAftrCn("{\"aiCnvrsTurnCnt\":8}");
        log.setCntnIpAddr("203.0.113.7");
        when(adminTaskLogMapper.selectById(1L)).thenReturn(log);
        when(adminTaskLogMapper.selectNcnm(9L)).thenReturn("관리자01");

        AdminTaskLogDetailResponse detail = service.getDetail(1L);

        assertThat(detail.mbrNcnm()).isEqualTo("관리자01");
        assertThat(detail.chgBfrCn().get("aiCnvrsTurnCnt").asInt()).isEqualTo(6);
        assertThat(detail.chgAftrCn().get("aiCnvrsTurnCnt").asInt()).isEqualTo(8);
        assertThat(detail.cntnIpAddr()).isEqualTo("203.0.113.7");
    }

    @Test
    void 전후_값이_없는_로그인_로그는_null() {
        AdminTaskLog log = new AdminTaskLog();
        log.setMngrTaskLogSn(2L);
        log.setMbrSn(9L);
        when(adminTaskLogMapper.selectById(2L)).thenReturn(log);

        AdminTaskLogDetailResponse detail = service.getDetail(2L);

        assertThat(detail.chgBfrCn()).isNull();
        assertThat(detail.chgAftrCn()).isNull();
    }

    @Test
    void 없는_번호는_NOT_FOUND() {
        assertThatThrownBy(() -> service.getDetail(99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
