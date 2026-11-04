package com.nyangmeong.member.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.member.dto.AdminTermsCreateRequest;
import com.nyangmeong.member.dto.AdminTermsDetail;
import com.nyangmeong.member.dto.AdminTermsSummary;
import com.nyangmeong.member.mapper.TermsMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 약관 서비스]
 * - SCR-ADM-051 (FN-ADM-051-01·02·03·04·05)
 * - 관리 대상은 TRMS 에 이미 있는 종류만 : 새 종류는 만들지 않는다 (없는 종류 404)
 * - 새 버전을 등록하면 같은 종류 이전 버전은 사용 안 함, 정렬순서는 이전 버전을 이어받는다
 * - 기존 회원 동의(MBR_TRMS_AGRE)는 건드리지 않는다 : 개정해도 재동의를 받지 않고 공지로 안내 (FN-ADM-051-05)
 */
@Service
@RequiredArgsConstructor
public class AdminTermsService {

    private static final String TARGET_TABLE = "TRMS";
    private static final int VER_MAX_LENGTH = 80;
    private static final int TTL_MAX_LENGTH = 200;
    private static final int CN_MAX_LENGTH = 4000;

    private final TermsMapper termsMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 종류별 사용 중 버전 목록
     */
    @Transactional(readOnly = true)
    public List<AdminTermsSummary> search() {
        return termsMapper.selectAdminCurrentList();
    }

    /**
     * 한 종류의 버전 이력 (최신순, 없는 종류는 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<AdminTermsSummary> searchHistory(String trmsFixIdntfKey) {
        if (trmsFixIdntfKey == null || trmsFixIdntfKey.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return termsMapper.selectAdminHistory(trmsFixIdntfKey.strip());
    }

    /**
     * 약관 상세 (사용 안 함 버전 포함)
     */
    @Transactional(readOnly = true)
    public AdminTermsDetail get(Long trmsSn) {
        AdminTermsDetail detail = termsMapper.selectAdminDetail(trmsSn);
        if (detail == null) {
            throw new CustomException(ErrorCode.TERMS_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 약관 새 버전 등록
     */
    @Transactional
    public AdminTermsDetail create(Long actorSn, AdminTermsCreateRequest req) {
        String key = req.trmsFixIdntfKey().strip();
        String verId = normalize(req.trmsVerId(), VER_MAX_LENGTH);
        String ttl = normalize(req.trmsTtl(), TTL_MAX_LENGTH);
        String cn = normalize(req.trmsCn(), CN_MAX_LENGTH);

        // 같은 종류 행을 잠가 동시에 등록해도 사용 중 버전이 하나만 남게 한다
        Integer sortSeq = termsMapper.selectLatestSortSeqForUpdate(key);
        if (sortSeq == null) {
            throw new CustomException(ErrorCode.TERMS_NOT_FOUND);
        }
        if (termsMapper.selectAdminDetailByVersion(key, verId) != null) {
            throw new CustomException(ErrorCode.DUPLICATE_TRMS_VER);
        }

        String actorId = String.valueOf(actorSn);
        termsMapper.updateDisableByKey(key, actorId);
        try {
            termsMapper.insertAdminTerms(key, verId, ttl, cn, Boolean.TRUE.equals(req.esntlAgreYn()) ? "Y" : "N",
                    sortSeq, actorId);
        } catch (DuplicateKeyException e) {
            // 같은 종류·같은 버전 (UK_TRMS_1)
            throw new CustomException(ErrorCode.DUPLICATE_TRMS_VER);
        }

        AdminTermsDetail created = termsMapper.selectAdminDetailByVersion(key, verId);
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, created.getTrmsSn(), "약관 새 버전 등록",
                null, created);
        return created;
    }

    /**
     * 앞뒤 공백 지운 뒤 1자 이상 최대 글자 수 이하 (아니면 400)
     */
    private String normalize(String value, int maxLength) {
        String stripped = value.strip();
        if (stripped.isEmpty() || stripped.length() > maxLength) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return stripped;
    }
}
