package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.expense.domain.ExpenseCategory;
import com.nyangmeong.expense.domain.ExpenseSort;
import com.nyangmeong.expense.dto.ExpenseMonthSummary;
import com.nyangmeong.expense.dto.ExpensePetOption;
import com.nyangmeong.expense.dto.ExpenseSearchCond;
import com.nyangmeong.expense.dto.ExpenseStatisticsDayItem;
import com.nyangmeong.expense.dto.ExpenseStatisticsResponse;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.dto.ExpenseTotalResponse;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

import lombok.RequiredArgsConstructor;

/**
 * [가계부 조회 (SCR-MYP-030)]
 * - 월 통계·목록·합계·월 피커·필터 반려동물
 * - 지출은 회원별 : 내가 등록한 지출만
 */
@Service
@RequiredArgsConstructor
public class ExpenseQueryService {

    static final int PAGE_SIZE = 20;

    private final ExpenseMapper expenseMapper;
    private final PetService petService;

    /**
     * 월 통계 (FN-MYP-030-01·02·04·05·06·14)
     */
    @Transactional(readOnly = true)
    public ExpenseStatisticsResponse getStatistics(Long mbrSn, String yearMonth) {
        YearMonth month = parseMonth(yearMonth);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<ExpenseStatisticsDayItem> days = expenseMapper.selectDayItems(mbrSn, start, end);
        long totalAmt = days.stream().mapToLong(ExpenseStatisticsDayItem::getTotalExpndAmt).sum();
        Long lastMonthAmt = null;
        if (expenseMapper.existsBefore(mbrSn, start)) {
            YearMonth lastMonth = month.minusMonths(1);
            lastMonthAmt = expenseMapper.selectTotalAmt(mbrSn, lastMonth.atDay(1), lastMonth.atEndOfMonth());
        }
        return new ExpenseStatisticsResponse(totalAmt, expenseMapper.selectLimitAmt(mbrSn), lastMonthAmt, days,
                expenseMapper.selectCategoryItems(mbrSn, start, end, ExpenseCategory.ETC.getCode()),
                expenseMapper.selectPetItems(mbrSn, start, end),
                expenseMapper.selectCommonTotalAmt(mbrSn, start, end),
                expenseMapper.selectTopExpenses(mbrSn, start, end));
    }

    /**
     * 지출 목록 (FN-MYP-030-09·10·11·12·17·20)
     */
    @Transactional(readOnly = true)
    public PageResponse<ExpenseSummary> search(Long mbrSn, ExpenseSearchCond cond, PageQuery pq) {
        ExpenseSearchCond normalized = normalize(cond);
        ExpenseSort sort = ExpenseSort.from(pq.sort());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<ExpenseSummary> rows = expenseMapper.selectList(mbrSn, normalized, sort);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 조건별 건수·합계 (FN-MYP-030-09·18·19)
     */
    @Transactional(readOnly = true)
    public ExpenseTotalResponse getTotal(Long mbrSn, ExpenseSearchCond cond) {
        ExpenseSearchCond normalized = normalize(cond);
        return new ExpenseTotalResponse(expenseMapper.selectCount(mbrSn, normalized),
                expenseMapper.selectSumAmt(mbrSn, normalized));
    }

    /**
     * 지출이 있는 연월 (FN-MYP-030-16)
     * - 미래 달 제외
     */
    @Transactional(readOnly = true)
    public List<ExpenseMonthSummary> searchMonths(Long mbrSn) {
        LocalDate endOfThisMonth = YearMonth.now(ExpenseService.ZONE).atEndOfMonth();
        return expenseMapper.selectMonths(mbrSn, endOfThisMonth).stream().map(ExpenseMonthSummary::new).toList();
    }

    /**
     * 필터 반려동물 (FN-MYP-030-10)
     * - 지금 연동된 반려동물 + 내 지출이 남은 반려동물, 반려동물 등록순
     */
    @Transactional(readOnly = true)
    public List<ExpensePetOption> searchPets(Long mbrSn) {
        List<PetSummary> linkedPets = petService.searchMyPets(mbrSn);
        Set<Long> linkedPetSns = linkedPets.stream().map(PetSummary::getPetSn).collect(Collectors.toSet());

        Map<Long, ExpensePetOption> options = new TreeMap<>();
        for (ExpensePetOption option : expenseMapper.selectPetOptions(mbrSn)) {
            option.setPetLinked(linkedPetSns.contains(option.getPetSn()));
            options.put(option.getPetSn(), option);
        }
        for (PetSummary pet : linkedPets) {
            options.computeIfAbsent(pet.getPetSn(), petSn -> {
                ExpensePetOption option = new ExpensePetOption();
                option.setPetSn(petSn);
                option.setPetNm(pet.getPetNm());
                option.setPetDelYn(false);
                option.setPetLinked(true);
                return option;
            });
        }
        return List.copyOf(options.values());
    }

    /**
     * 검색 조건 검사·정리
     * - petSn 과 공용은 함께 쓸 수 없음, 시작이 종료보다 뒤면 서로 바꿈 (FN-MYP-030-17)
     */
    static ExpenseSearchCond normalize(ExpenseSearchCond cond) {
        ExpenseSearchCond normalized = new ExpenseSearchCond();
        if (cond == null) {
            return normalized;
        }
        boolean common = Boolean.TRUE.equals(cond.getCommon());
        if (common && cond.getPetSn() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String clsfCd = cond.getExpndClsfCd() == null || cond.getExpndClsfCd().isBlank() ? null
                : cond.getExpndClsfCd();
        if (clsfCd != null && !ExpenseCategory.exists(clsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDate start = cond.getStartYmd();
        LocalDate end = cond.getEndYmd();
        if (start != null && end != null && start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        normalized.setStartYmd(start);
        normalized.setEndYmd(end);
        normalized.setPetSn(cond.getPetSn());
        normalized.setCommon(common);
        normalized.setExpndClsfCd(clsfCd);
        return normalized;
    }

    /**
     * 통계 연월 : yyyy-MM, 미래 달은 400
     */
    private static YearMonth parseMonth(String yearMonth) {
        if (yearMonth == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        YearMonth month;
        try {
            month = YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (month.isAfter(YearMonth.now(ExpenseService.ZONE))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return month;
    }
}
