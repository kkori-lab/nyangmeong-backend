package com.nyangmeong.expense.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.expense.domain.Expense;
import com.nyangmeong.expense.dto.ExpenseLineRequest;
import com.nyangmeong.expense.dto.ExpenseSummary;

/**
 * [가계부 지출 Mapper]
 * - EXPND, 한도는 MBR : member service 가 없어 직접 읽음
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
}
