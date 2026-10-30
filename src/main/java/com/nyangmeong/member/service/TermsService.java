package com.nyangmeong.member.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.domain.Terms;
import com.nyangmeong.member.dto.TermsAgreeRequest;
import com.nyangmeong.member.dto.TermsResponse;
import com.nyangmeong.member.dto.TermsSummary;
import com.nyangmeong.member.mapper.TermsMapper;

import lombok.RequiredArgsConstructor;

/**
 * [약관 서비스]
 * - 사용 중인 약관 조회와 가입 때 약관별 동의 검사·저장
 * - 필수 여부는 TRMS.ESNTL_AGRE_YN 으로 판단하고 약관 키를 코드에 박지 않는다
 */
@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsMapper termsMapper;

    /**
     * 사용 중인 약관 목록
     */
    @Transactional(readOnly = true)
    public List<TermsSummary> search() {
        return termsMapper.selectUsableList();
    }

    /**
     * 약관 전문
     */
    @Transactional(readOnly = true)
    public TermsResponse getTerms(Long trmsSn) {
        Terms terms = termsMapper.selectUsableBySn(trmsSn);
        if (terms == null) {
            throw new CustomException(ErrorCode.TERMS_NOT_FOUND);
        }
        return TermsResponse.from(terms);
    }

    /**
     * 가입 약관 동의 검사 : 약관별 동의 여부(보내지 않은 선택 약관은 미동의)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> checkAgreements(List<TermsAgreeRequest> items) {
        List<TermsSummary> usable = termsMapper.selectUsableList();
        Set<Long> usableSns = new HashSet<>();
        usable.forEach(terms -> usableSns.add(terms.getTrmsSn()));

        Map<Long, Boolean> agreed = new LinkedHashMap<>();
        for (TermsAgreeRequest item : items) {
            if (!usableSns.contains(item.trmsSn())) {
                throw new CustomException(ErrorCode.TERMS_NOT_FOUND);
            }
            if (agreed.put(item.trmsSn(), Boolean.TRUE.equals(item.agreYn())) != null) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
        }

        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (TermsSummary terms : usable) {
            boolean isAgreed = Boolean.TRUE.equals(agreed.get(terms.getTrmsSn()));
            if (terms.isEsntlAgreYn() && !isAgreed) {
                throw new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
            result.put(terms.getTrmsSn(), isAgreed);
        }
        return result;
    }

    /**
     * 복구 때 다시 받아야 할 현재 필수 약관 : 이미 같은 버전으로 동의한 것은 빠진다
     */
    @Transactional(readOnly = true)
    public List<TermsSummary> searchMissingEssential(Long mbrSn) {
        return termsMapper.selectMissingEssential(mbrSn);
    }

    /**
     * 복구 약관 동의 검사 : 빠진 필수 약관을 모두 동의해야 한다, 그 밖의 항목은 무시
     * - 돌려주는 값은 저장할 약관 번호
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public List<Long> checkRecoveryAgreements(List<TermsSummary> missing, List<TermsAgreeRequest> items) {
        Set<Long> agreed = new HashSet<>();
        if (items != null) {
            items.stream().filter(item -> Boolean.TRUE.equals(item.agreYn())).forEach(item -> agreed.add(item.trmsSn()));
        }
        List<Long> result = new ArrayList<>();
        for (TermsSummary terms : missing) {
            if (!agreed.contains(terms.getTrmsSn())) {
                throw new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            }
            result.add(terms.getTrmsSn());
        }
        return result;
    }

    /**
     * 복구 필수 약관 동의 저장
     */
    @Transactional
    public void createRecoveryAgreements(Long mbrSn, List<Long> trmsSns) {
        trmsSns.forEach(trmsSn -> termsMapper.upsertAgreement(mbrSn, trmsSn, String.valueOf(mbrSn)));
    }

    /**
     * 약관별 동의 저장
     */
    @Transactional
    public void createAgreements(Long mbrSn, Map<Long, Boolean> agreements) {
        agreements.forEach((trmsSn, isAgreed) -> termsMapper.insertAgreement(mbrSn, trmsSn, isAgreed ? "Y" : "N",
                String.valueOf(mbrSn)));
    }
}
