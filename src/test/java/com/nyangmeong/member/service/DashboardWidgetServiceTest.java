package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.dto.DashboardWidgetSaveRequest;
import com.nyangmeong.member.dto.DashboardWidgetSummary;
import com.nyangmeong.member.mapper.DashboardWidgetMapper;

/**
 * [검사 - 메인 위젯 순서]
 * - 사용 중인 위젯 코드를 전부 한 번씩 담았을 때만 그 순서대로 저장한다
 * - 빠지거나 겹치거나 없는 코드면 INVALID_INPUT_VALUE 이고 아무것도 저장하지 않는다
 */
class DashboardWidgetServiceTest {

    private static final long MBR_SN = 7L;
    private static final List<String> CODES = List.of("MANC0001", "MANC0002", "MANC0003", "MANC0004", "MANC0005",
            "MANC0006", "MANC0007");

    DashboardWidgetMapper mapper = mock(DashboardWidgetMapper.class);
    DashboardWidgetService service;

    @BeforeEach
    void setUp() {
        service = new DashboardWidgetService(mapper);
        when(mapper.selectWidgetList(MBR_SN, "MANG01")).thenReturn(CODES.stream().map(c -> {
            DashboardWidgetSummary w = new DashboardWidgetSummary();
            w.setDshbdWdgtCd(c);
            return w;
        }).toList());
    }

    @Test
    void 전체_코드를_담으면_요청_순서대로_저장한다() {
        List<String> order = List.of("MANC0005", "MANC0001", "MANC0002", "MANC0003", "MANC0004", "MANC0006",
                "MANC0007");

        service.saveWidgets(MBR_SN, new DashboardWidgetSaveRequest(order));

        InOrder in = inOrder(mapper);
        for (int i = 0; i < order.size(); i++) {
            in.verify(mapper).upsertWidgetSort(MBR_SN, order.get(i), (i + 1) * 10);
        }
    }

    @Test
    void 기본_순서를_조회한다() {
        assertThat(service.getWidgets(MBR_SN)).hasSize(7);
    }

    @Test
    void 코드가_빠지면_거절하고_저장하지_않는다() {
        assertRejected(CODES.subList(0, 6));
    }

    @Test
    void 코드가_겹치면_거절하고_저장하지_않는다() {
        assertRejected(List.of("MANC0001", "MANC0001", "MANC0003", "MANC0004", "MANC0005", "MANC0006", "MANC0007"));
    }

    @Test
    void 없는_코드면_거절하고_저장하지_않는다() {
        assertRejected(List.of("MANC0001", "MANC0002", "MANC0003", "MANC0004", "MANC0005", "MANC0006", "MANC9999"));
    }

    private void assertRejected(List<String> order) {
        Throwable t = catchThrowable(() -> service.saveWidgets(MBR_SN, new DashboardWidgetSaveRequest(order)));

        assertThat(t).isInstanceOf(CustomException.class);
        assertThat(((CustomException) t).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).upsertWidgetSort(anyLong(), anyString(), anyInt());
    }
}
