package com.nyangmeong.health.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.InspectionArticle;
import com.nyangmeong.health.domain.InspectionValue;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.InspectionValueCreateRequest;
import com.nyangmeong.health.dto.InspectionValueCreateRequest.InspectionValueItemRequest;
import com.nyangmeong.health.dto.InspectionValueResponse;
import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest;
import com.nyangmeong.health.mapper.InspectionValueMapper;

import lombok.RequiredArgsConstructor;

/**
 * [검사 수치 서비스]
 * - 검진상세 혈액검사 결과 목록·선택 삭제 (SCR-HLT-005, FN-HLT-005-02)
 * - 검사 수치 저장(인라인 추가·PDF 분석 결과)·수정 (SCR-HLT-007, FN-HLT-007-02·03·05)
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
        if (cond.getCkupRcdSn() == null) {
            return searchAll(mbrSn, petSn, cond);
        }
        CheckupResponse checkup = checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        List<InspectionValueSummary> rows = inspectionValueMapper.selectList(cond);
        if (checkup.getCkupYmd() != null) {
            rows.forEach(row -> row.setSchdlDt(checkup.getCkupYmd().atStartOfDay()));
        }
        return rows;
    }

    /**
     * 반려동물 전체 검사 수치 조회 (검진일 오름차순, 같은 검진기록 안에서는 정렬순서)
     */
    private List<InspectionValueSummary> searchAll(Long mbrSn, Long petSn, InspectionValueSearchCond cond) {
        Map<Long, CheckupSummary> checkups = checkupService.searchAllCompleted(mbrSn, petSn).stream()
                .collect(Collectors.toMap(CheckupSummary::getCkupRcdSn, Function.identity()));
        if (checkups.isEmpty()) {
            return List.of();
        }
        List<InspectionValueSummary> rows = new ArrayList<>(
                inspectionValueMapper.selectListByCheckups(List.copyOf(checkups.keySet()), cond));
        rows.forEach(row -> {
            LocalDate ckupYmd = checkups.get(row.getCkupRcdSn()).getCkupYmd();
            if (ckupYmd != null) {
                row.setSchdlDt(ckupYmd.atStartOfDay());
            }
        });
        rows.sort(Comparator.comparing(InspectionValueSummary::getSchdlDt,
                Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(InspectionValueSummary::getCkupRcdSn));
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

    /**
     * 검사 수치 저장
     * - 같은 검진기록에 이미 있는 항목·요청 안 중복 항목은 건너뜀
     * - 삭제된 같은 항목은 새 값으로 복구 (UK_INSP_NVL_1)
     * - 저장·복구된 행만 반환
     */
    @Transactional
    public List<InspectionValueResponse> create(Long mbrSn, Long petSn, InspectionValueCreateRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        List<InspectionValue> values = req.values().stream().map(item -> toValue(req.ckupRcdSn(), item)).toList();
        String actorId = String.valueOf(mbrSn);

        int sortSeq = inspectionValueMapper.selectMaxSortSeq(req.ckupRcdSn());
        Set<String> seen = new HashSet<>();
        List<InspectionValueResponse> saved = new ArrayList<>();
        for (InspectionValue value : values) {
            if (!seen.add(value.getInspArtclCd() + "|" + value.getInspArtclDrctInptNm())
                    || findSameArticle(value) != null) {
                continue;
            }
            value.setSortSeq(++sortSeq);
            value.setRgtrId(actorId);
            value.setMdfrId(actorId);
            if (inspectionValueMapper.restoreValue(value, actorId) == 0) {
                inspectionValueMapper.insertValue(value);
            }
            saved.add(InspectionValueResponse.from(findSameArticle(value)));
        }
        return saved;
    }

    /**
     * 검사 수치 수정
     * - 보낸 항목만 수정, 검사 항목은 변경 불가
     */
    @Transactional
    public InspectionValueResponse update(Long mbrSn, Long petSn, Long inspNvlSn, InspectionValueUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        InspectionValue value = getOwn(petSn, inspNvlSn);
        if (req.rsltNvl() != null) {
            value.setRsltNvl(req.rsltNvl());
        }
        if (req.unitNm() != null) {
            value.setUnitNm(requiredText(req.unitNm()));
        }
        if (req.refLlmtNvl() != null) {
            value.setRefLlmtNvl(req.refLlmtNvl());
        }
        if (req.refUplmtNvl() != null) {
            value.setRefUplmtNvl(req.refUplmtNvl());
        }
        if (req.memoCn() != null) {
            value.setMemoCn(emptyToNull(req.memoCn()));
        }
        checkRefPair(value.getRefLlmtNvl(), value.getRefUplmtNvl());
        inspectionValueMapper.updateValue(value, String.valueOf(mbrSn));
        return InspectionValueResponse.from(inspectionValueMapper.selectValue(inspNvlSn));
    }

    /**
     * 요청 1행 → 검사 수치
     * - 직접입력명이 있으면 코드는 비우거나 직접입력(HLTC0035)만, 없으면 12종 코드 필수
     */
    private InspectionValue toValue(Long ckupRcdSn, InspectionValueItemRequest item) {
        String code = emptyToNull(item.inspArtclCd());
        String name = emptyToNull(item.inspArtclDrctInptNm());
        InspectionValue value = new InspectionValue();
        value.setCkupRcdSn(ckupRcdSn);
        if (name != null) {
            if (code != null && !InspectionArticle.DIRECT.is(code)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            value.setInspArtclCd(InspectionArticle.DIRECT.getCode());
            value.setInspArtclDrctInptNm(name);
        } else {
            boolean known = code != null && Arrays.stream(InspectionArticle.values())
                    .anyMatch(article -> article != InspectionArticle.DIRECT && article.is(code));
            if (!known) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            value.setInspArtclCd(code);
            value.setInspArtclDrctInptNm("");
        }
        checkRefPair(item.refLlmtNvl(), item.refUplmtNvl());
        value.setRsltNvl(item.rsltNvl());
        value.setUnitNm(requiredText(item.unitNm()));
        value.setRefLlmtNvl(item.refLlmtNvl());
        value.setRefUplmtNvl(item.refUplmtNvl());
        value.setMemoCn(emptyToNull(item.memoCn()));
        return value;
    }

    private InspectionValue findSameArticle(InspectionValue value) {
        return inspectionValueMapper.selectValueByArticle(value.getCkupRcdSn(), value.getInspArtclCd(),
                value.getInspArtclDrctInptNm());
    }

    /**
     * 경로 반려동물의 검사 수치 조회
     */
    private InspectionValue getOwn(Long petSn, Long inspNvlSn) {
        InspectionValue value = inspectionValueMapper.selectValue(inspNvlSn);
        if (value == null || !checkupService.existsCheckup(petSn, value.getCkupRcdSn())) {
            throw new CustomException(ErrorCode.INSPECTION_VALUE_NOT_FOUND);
        }
        return value;
    }

    /**
     * 참고범위 짝 검증 : 하한·상한은 함께 있거나 둘 다 없음 (CK_INSP_NVL_REF_PAIR)
     */
    private void checkRefPair(BigDecimal llmt, BigDecimal uplmt) {
        if ((llmt == null) != (uplmt == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String requiredText(String value) {
        String text = emptyToNull(value);
        if (text == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return text;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
