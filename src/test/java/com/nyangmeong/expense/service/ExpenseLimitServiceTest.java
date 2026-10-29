package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.nyangmeong.expense.dto.ExpenseLimitResponse;
import com.nyangmeong.expense.dto.ExpenseLimitSaveRequest;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 월 지출 한도 저장]
 * - 처음 저장이면 알림 수신을 켜는 판단은 SQL(CASE) 이 함
 */
class ExpenseLimitServiceTest {

    private static final Long MBR = 1L;

    private final ExpenseMapper expenseMapper = mock(ExpenseMapper.class);
    private final ExpenseLimitService service = new ExpenseLimitService(expenseMapper);

    @Test
    void 저장하면_한도와_현재_알림_수신_여부를_돌려준다() {
        when(expenseMapper.updateLimit(MBR, 500_000L, "1")).thenReturn(1);
        when(expenseMapper.selectLimitNotiRcptnYn(MBR)).thenReturn("Y");

        ExpenseLimitResponse res = service.save(MBR, new ExpenseLimitSaveRequest(500_000L));

        assertThat(res.mmExpndLimAmt()).isEqualTo(500_000L);
        assertThat(res.expndLimNotiRcptnYn()).isTrue();
        verify(expenseMapper).updateLimit(MBR, 500_000L, "1");
    }

    @Test
    void 알림을_끈_회원은_한도를_바꿔도_꺼진_채로_돌려준다() {
        when(expenseMapper.updateLimit(MBR, 300_000L, "1")).thenReturn(1);
        when(expenseMapper.selectLimitNotiRcptnYn(MBR)).thenReturn("N");

        assertThat(service.save(MBR, new ExpenseLimitSaveRequest(300_000L)).expndLimNotiRcptnYn()).isFalse();
    }

    @Test
    void 회원이_없으면_404다() {
        when(expenseMapper.updateLimit(MBR, 1L, "1")).thenReturn(0);

        assertThatThrownBy(() -> service.save(MBR, new ExpenseLimitSaveRequest(1L)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 한도_SQL은_처음_저장일_때만_수신을_켜고_한도보다_먼저_계산한다() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/expense/ExpenseMapper.xml"));
        Matcher update = Pattern.compile("<update id=\"updateLimit\".*?</update>", Pattern.DOTALL).matcher(xml);
        assertThat(update.find()).isTrue();
        String sql = update.group();

        assertThat(sql).contains("CASE WHEN MM_EXPND_LIM_AMT IS NULL THEN 'Y'", "ELSE EXPND_LIM_NOTI_RCPTN_YN END");
        assertThat(sql.indexOf("EXPND_LIM_NOTI_RCPTN_YN =")).isLessThan(sql.indexOf("MM_EXPND_LIM_AMT = #{"));
    }
}
