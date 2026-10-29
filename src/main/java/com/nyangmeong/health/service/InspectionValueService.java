package com.nyangmeong.health.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.mapper.InspectionValueMapper;

import lombok.RequiredArgsConstructor;

/**
 * [검사 수치 서비스]
 * - 검진상세 혈액검사 결과 목록·선택 삭제 (SCR-HLT-005, FN-HLT-005-02)
 * - 저장(인라인 추가·PDF 분석 결과)은 SCR-HLT-007
 */
@Service
@RequiredArgsConstructor
public class InspectionValueService {

    private final InspectionValueMapper inspectionValueMapper;
    private final CheckupService checkupService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 검사 수치 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InspectionValueSummary> search(Long mbrSn, Long petSn, InspectionValueSearchCond cond) {
        CheckupResponse checkup = checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        List<InspectionValueSummary> rows = inspectionValueMapper.selectList(cond);
        if (checkup.getCkupYmd() != null) {
            rows.forEach(row -> row.setSchdlDt(checkup.getCkupYmd().atStartOfDay()));
        }
        return rows;
    }

    /**
     * 검사 수치 선택 삭제
     * - 다른 반려동물·삭제된 수치가 섞이면 전체 거절
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, List<Long> inspNvlSns) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        if (inspNvlSns == null || inspNvlSns.isEmpty() || inspNvlSns.stream().anyMatch(Objects::isNull)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> distinct = List.copyOf(new HashSet<>(inspNvlSns));
        List<Long> ckupRcdSns = inspectionValueMapper.selectCheckupSns(distinct);
        Set<Long> checkups = new HashSet<>(ckupRcdSns);
        if (ckupRcdSns.size() != distinct.size()
                || !checkups.stream().allMatch(ckupRcdSn -> checkupService.existsCheckup(petSn, ckupRcdSn))) {
            throw new CustomException(ErrorCode.INSPECTION_VALUE_NOT_FOUND);
        }
        inspectionValueMapper.deleteValues(distinct, String.valueOf(mbrSn));
    }
}
