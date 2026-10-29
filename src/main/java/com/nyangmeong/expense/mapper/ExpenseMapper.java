package com.nyangmeong.expense.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.expense.domain.Expense;
import com.nyangmeong.expense.domain.ExpenseSort;
import com.nyangmeong.expense.dto.ExpenseLineRequest;
import com.nyangmeong.expense.dto.ExpensePetOption;
import com.nyangmeong.expense.dto.ExpenseSearchCond;
import com.nyangmeong.expense.dto.ExpenseStatisticsCategoryItem;
import com.nyangmeong.expense.dto.ExpenseStatisticsDayItem;
import com.nyangmeong.expense.dto.ExpenseStatisticsPetItem;
import com.nyangmeong.expense.dto.ExpenseSummary;

/**
 * [가계부 지출 Mapper]
 * - EXPND, 한도는 MBR : member service 가 없어 직접 읽고 씀
 * - 지출은 회원별 : 공동양육자끼리도 서로의 지출을 보지 않음
 */
@Mapper
public interface ExpenseMapper {

    /**
     * 지출 등록
     */
    void insert(Expense expense);

    /**
     * 멱등 요청 ID 로 내 지출 조회
     */
    List<ExpenseSummary> selectByIdmptDmndIds(@Param("mbrSn") Long mbrSn,
            @Param("idmptDmndIds") List<String> idmptDmndIds);

    /**
     * 같은 날짜·카테고리·금액·반려동물의 내 지출 조회
     */
    List<ExpenseSummary> selectDuplicates(@Param("mbrSn") Long mbrSn, @Param("expndYmd") LocalDate expndYmd,
            @Param("expndClsfCd") String expndClsfCd, @Param("lines") List<ExpenseLineRequest> lines);

    /**
     * 기간 내 지출 합계
     */
    long selectTotalAmt(@Param("mbrSn") Long mbrSn, @Param("startYmd") LocalDate startYmd,
            @Param("endYmd") LocalDate endYmd);

    /**
     * 알림 받을 월 지출 한도 조회 : 한도가 없거나 알림 수신을 끄면 null
     */
    Long selectNotifiedLimitAmt(@Param("mbrSn") Long mbrSn);

    /**
     * 조건에 맞는 내 지출 목록
     */
    List<ExpenseSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("cond") ExpenseSearchCond cond,
            @Param("sort") ExpenseSort sort);

    /**
     * 조건에 맞는 내 지출 건수
     */
    int selectCount(@Param("mbrSn") Long mbrSn, @Param("cond") ExpenseSearchCond cond);

    /**
     * 조건에 맞는 내 지출 합계
     */
    long selectSumAmt(@Param("mbrSn") Long mbrSn, @Param("cond") ExpenseSearchCond cond);

    /**
     * 내 지출이 있는 연월(yyyy-MM, 최신순)
     */
    List<String> selectMonths(@Param("mbrSn") Long mbrSn, @Param("endYmd") LocalDate endYmd);

    /**
     * 내 지출이 남은 반려동물(삭제된 반려동물 포함)
     */
    List<ExpensePetOption> selectPetOptions(@Param("mbrSn") Long mbrSn);

    /**
     * 기간 내 날짜별 합계
     */
    List<ExpenseStatisticsDayItem> selectDayItems(@Param("mbrSn") Long mbrSn, @Param("startYmd") LocalDate startYmd,
            @Param("endYmd") LocalDate endYmd);

    /**
     * 기간 내 카테고리별 합계 : 기타는 마지막
     */
    List<ExpenseStatisticsCategoryItem> selectCategoryItems(@Param("mbrSn") Long mbrSn,
            @Param("startYmd") LocalDate startYmd, @Param("endYmd") LocalDate endYmd,
            @Param("etcClsfCd") String etcClsfCd);

    /**
     * 기간 내 반려동물별 합계 : 공용 제외
     */
    List<ExpenseStatisticsPetItem> selectPetItems(@Param("mbrSn") Long mbrSn, @Param("startYmd") LocalDate startYmd,
            @Param("endYmd") LocalDate endYmd);

    /**
     * 기간 내 공용 지출 합계
     */
    long selectCommonTotalAmt(@Param("mbrSn") Long mbrSn, @Param("startYmd") LocalDate startYmd,
            @Param("endYmd") LocalDate endYmd);

    /**
     * 기간 내 금액 큰 지출 3건
     */
    List<ExpenseSummary> selectTopExpenses(@Param("mbrSn") Long mbrSn, @Param("startYmd") LocalDate startYmd,
            @Param("endYmd") LocalDate endYmd);

    /**
     * 기준일 이전 내 지출 존재 여부
     */
    boolean existsBefore(@Param("mbrSn") Long mbrSn, @Param("ymd") LocalDate ymd);

    /**
     * 내 지출 한 건 : 없거나 남의 지출이면 null
     */
    Expense selectMine(@Param("mbrSn") Long mbrSn, @Param("expndSn") Long expndSn);

    /**
     * 내 지출 한 건(목록 항목 형태)
     */
    ExpenseSummary selectSummary(@Param("mbrSn") Long mbrSn, @Param("expndSn") Long expndSn);

    /**
     * 지출 수정
     */
    int update(Expense expense);

    /**
     * 내 지출 삭제 : 지운 건수
     */
    int deleteMine(@Param("mbrSn") Long mbrSn, @Param("expndSns") List<Long> expndSns);

    /**
     * 조건에 맞는 내 지출 삭제 : 지운 건수
     */
    int deleteByCond(@Param("mbrSn") Long mbrSn, @Param("cond") ExpenseSearchCond cond);

    /**
     * 월 지출 한도 저장 : 처음 저장이면 한도 알림 수신을 켬
     */
    int updateLimit(@Param("mbrSn") Long mbrSn, @Param("mmExpndLimAmt") Long mmExpndLimAmt,
            @Param("actorId") String actorId);

    /**
     * 한도 알림 수신 여부(Y/N)
     */
    String selectLimitNotiRcptnYn(@Param("mbrSn") Long mbrSn);

    /**
     * 월 지출 한도 : 미설정이면 null
     */
    Long selectLimitAmt(@Param("mbrSn") Long mbrSn);
}
