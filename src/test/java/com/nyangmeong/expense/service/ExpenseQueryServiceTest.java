package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.expense.domain.ExpenseCategory;
import com.nyangmeong.expense.domain.ExpenseSort;
import com.nyangmeong.expense.dto.ExpenseMonthSummary;
import com.nyangmeong.expense.dto.ExpensePetOption;
import com.nyangmeong.expense.dto.ExpenseSearchCond;
import com.nyangmeong.expense.dto.ExpenseStatisticsDayItem;
import com.nyangmeong.expense.dto.ExpenseStatisticsPetItem;
import com.nyangmeong.expense.dto.ExpenseStatisticsResponse;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 가계부 월 통계·목록·합계·월 피커·필터 반려동물]
 */
class ExpenseQueryServiceTest {

    private static final Long MBR = 1L;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    private final ExpenseMapper expenseMapper = mock(ExpenseMapper.class);
    private final PetService petService = mock(PetService.class);
    private final ExpenseQueryService service = new ExpenseQueryService(expenseMapper, petService);

    private static ExpenseStatisticsDayItem day(LocalDate ymd, long amt, int cnt) {
        ExpenseStatisticsDayItem item = new ExpenseStatisticsDayItem();
        item.setExpndYmd(ymd);
        item.setTotalExpndAmt(amt);
        item.setExpndCnt(cnt);
        return item;
    }

    private static ExpenseStatisticsPetItem petItem(Long petSn, String petNm, boolean deleted, long amt) {
        ExpenseStatisticsPetItem item = new ExpenseStatisticsPetItem();
        item.setPetSn(petSn);
        item.setPetNm(petNm);
        item.setPetDelYn(deleted);
        item.setTotalExpndAmt(amt);
        return item;
    }

    private static PetSummary pet(Long petSn, String petNm) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setPetNm(petNm);
        return pet;
    }

    @Test
    void 월_통계는_공용을_반려동물별과_따로_주고_삭제된_반려동물도_남긴다() {
        when(expenseMapper.selectDayItems(MBR, OCT_1, OCT_31))
                .thenReturn(List.of(day(LocalDate.of(2026, 10, 3), 120_000L, 1), day(OCT_31, 30_000L, 2)));
        when(expenseMapper.selectPetItems(MBR, OCT_1, OCT_31))
                .thenReturn(List.of(petItem(10L, "뭉치", false, 120_000L), petItem(11L, "모리", true, 20_000L)));
        when(expenseMapper.selectCommonTotalAmt(MBR, OCT_1, OCT_31)).thenReturn(10_000L);
        when(expenseMapper.selectLimitAmt(MBR)).thenReturn(500_000L);
        when(expenseMapper.existsBefore(MBR, OCT_1)).thenReturn(true);
        when(expenseMapper.selectTotalAmt(MBR, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(100_000L);

        ExpenseStatisticsResponse res = service.getStatistics(MBR, "2026-10");

        assertThat(res.totalExpndAmt()).isEqualTo(150_000L);
        assertThat(res.commonTotalExpndAmt()).isEqualTo(10_000L);
        assertThat(res.pets()).extracting(ExpenseStatisticsPetItem::getPetSn).containsExactly(10L, 11L);
        assertThat(res.pets().get(1).getPetDelYn()).isTrue();
        assertThat(res.pets().get(1).getPetNm()).isEqualTo("모리");
        assertThat(res.mmExpndLimAmt()).isEqualTo(500_000L);
        assertThat(res.lastMonthTotalExpndAmt()).isEqualTo(100_000L);
        verify(expenseMapper).selectCategoryItems(MBR, OCT_1, OCT_31, ExpenseCategory.ETC.getCode());
    }

    @Test
    void 첫_달이면_지난달_대비가_없고_한도_미설정이면_한도가_null이다() {
        when(expenseMapper.existsBefore(MBR, OCT_1)).thenReturn(false);
        when(expenseMapper.selectLimitAmt(MBR)).thenReturn(null);

        ExpenseStatisticsResponse res = service.getStatistics(MBR, "2026-10");

        assertThat(res.lastMonthTotalExpndAmt()).isNull();
        assertThat(res.mmExpndLimAmt()).isNull();
        assertThat(res.totalExpndAmt()).isZero();
        verify(expenseMapper, never()).selectTotalAmt(eq(MBR), any(), any());
    }

    @Test
    void 통계_연월이_틀리거나_미래면_400이다() {
        String nextMonth = YearMonth.now(ExpenseService.ZONE).plusMonths(1).toString();

        for (String yearMonth : new String[] {"2026-13", "202610", nextMonth, null}) {
            assertThatThrownBy(() -> service.getStatistics(MBR, yearMonth)).isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Test
    void 목록은_공용_조건과_정렬을_넘기고_시작이_종료보다_뒤면_바꾼다() {
        ExpenseSearchCond cond = new ExpenseSearchCond();
        cond.setStartYmd(OCT_31);
        cond.setEndYmd(OCT_1);
        cond.setCommon(true);

        service.search(MBR, cond, new PageQuery(2, null, "amount"));

        ArgumentCaptor<ExpenseSearchCond> captor = ArgumentCaptor.forClass(ExpenseSearchCond.class);
        verify(expenseMapper).selectList(eq(MBR), captor.capture(), eq(ExpenseSort.AMOUNT));
        assertThat(captor.getValue().getStartYmd()).isEqualTo(OCT_1);
        assertThat(captor.getValue().getEndYmd()).isEqualTo(OCT_31);
        assertThat(captor.getValue().getCommon()).isTrue();
    }

    @Test
    void 정렬을_비우면_최신순이고_없는_정렬_값은_400이다() {
        service.search(MBR, new ExpenseSearchCond(), new PageQuery(null, null, null));
        verify(expenseMapper).selectList(eq(MBR), any(), eq(ExpenseSort.LATEST));

        assertThatThrownBy(() -> service.search(MBR, new ExpenseSearchCond(), new PageQuery(null, null, "regDt")))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 반려동물과_공용을_함께_거르거나_없는_카테고리면_400이다() {
        ExpenseSearchCond both = new ExpenseSearchCond();
        both.setPetSn(10L);
        both.setCommon(true);
        ExpenseSearchCond badCategory = new ExpenseSearchCond();
        badCategory.setExpndClsfCd("MYPC9999");

        assertThatThrownBy(() -> service.getTotal(MBR, both)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.getTotal(MBR, badCategory)).isInstanceOf(CustomException.class);
        verify(expenseMapper, never()).selectCount(any(), any());
    }

    @Test
    void 합계는_같은_조건의_건수와_금액이다() {
        when(expenseMapper.selectCount(eq(MBR), any())).thenReturn(3);
        when(expenseMapper.selectSumAmt(eq(MBR), any())).thenReturn(45_000L);

        assertThat(service.getTotal(MBR, null)).satisfies(total -> {
            assertThat(total.expndCnt()).isEqualTo(3);
            assertThat(total.totalExpndAmt()).isEqualTo(45_000L);
        });
    }

    @Test
    void 월_피커는_이번_달까지만_조회한다() {
        LocalDate endOfThisMonth = YearMonth.now(ExpenseService.ZONE).atEndOfMonth();
        when(expenseMapper.selectMonths(MBR, endOfThisMonth)).thenReturn(List.of("2026-10", "2026-08"));

        assertThat(service.searchMonths(MBR)).extracting(ExpenseMonthSummary::yearMonth)
                .containsExactly("2026-10", "2026-08");
    }

    @Test
    void 필터_반려동물은_연동된_동물과_지출이_남은_삭제_연동해제_동물을_등록순으로_준다() {
        when(petService.searchMyPets(MBR)).thenReturn(List.of(pet(12L, "두부"), pet(10L, "뭉치")));
        ExpensePetOption linked = new ExpensePetOption();
        linked.setPetSn(10L);
        linked.setPetNm("뭉치");
        linked.setPetDelYn(false);
        ExpensePetOption unlinked = new ExpensePetOption();
        unlinked.setPetSn(5L);
        unlinked.setPetNm("나비");
        unlinked.setPetDelYn(false);
        ExpensePetOption deleted = new ExpensePetOption();
        deleted.setPetSn(11L);
        deleted.setPetNm("모리");
        deleted.setPetDelYn(true);
        when(expenseMapper.selectPetOptions(MBR)).thenReturn(List.of(unlinked, linked, deleted));

        List<ExpensePetOption> options = service.searchPets(MBR);

        assertThat(options).extracting(ExpensePetOption::getPetSn).containsExactly(5L, 10L, 11L, 12L);
        assertThat(options).extracting(ExpensePetOption::getPetLinked).containsExactly(false, true, false, true);
        assertThat(options).extracting(ExpensePetOption::getPetDelYn).containsExactly(false, false, true, false);
        assertThat(options.get(3).getPetNm()).isEqualTo("두부");
    }
}
