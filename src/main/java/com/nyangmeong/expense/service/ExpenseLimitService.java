package com.nyangmeong.expense.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.expense.dto.ExpenseLimitResponse;
import com.nyangmeong.expense.dto.ExpenseLimitSaveRequest;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [월 지출 한도 (FN-MYP-030-13)]
 * - 계정 전체 한도 하나
 * - 처음 저장하면 한도 알림 수신을 켜고, 이후 저장은 수신 설정을 그대로 둠(끄기는 SCR-COM-008)
 */
@Service
@RequiredArgsConstructor
public class ExpenseLimitService {

    private final ExpenseMapper expenseMapper;

    /**
     * 한도 저장
     */
    @Transactional
    public ExpenseLimitResponse save(Long mbrSn, ExpenseLimitSaveRequest req) {
        if (expenseMapper.updateLimit(mbrSn, req.mmExpndLimAmt(), String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return new ExpenseLimitResponse(req.mmExpndLimAmt(),
                "Y".equals(expenseMapper.selectLimitNotiRcptnYn(mbrSn)));
    }
}
