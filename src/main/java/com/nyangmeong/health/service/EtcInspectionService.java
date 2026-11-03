package com.nyangmeong.health.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.EtcInspection;
import com.nyangmeong.health.dto.EtcInspectionCreateRequest;
import com.nyangmeong.health.dto.EtcInspectionResponse;
import com.nyangmeong.health.dto.EtcInspectionSearchCond;
import com.nyangmeong.health.dto.EtcInspectionSummary;
import com.nyangmeong.health.dto.EtcInspectionUpdateRequest;
import com.nyangmeong.health.mapper.EtcInspectionMapper;

import lombok.RequiredArgsConstructor;

/**
 * [기타검사 서비스]
 * - 기타검사 목록·입력·수정·삭제 (SCR-HLT-005, FN-HLT-005-02·11~14)
 */
@Service
@RequiredArgsConstructor
public class EtcInspectionService {

    private final EtcInspectionMapper etcInspectionMapper;
    private final CheckupService checkupService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 기타검사 목록 조회
     */
    @Transactional(readOnly = true)
    public List<EtcInspectionSummary> search(Long mbrSn, Long petSn, EtcInspectionSearchCond cond) {
        checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        return etcInspectionMapper.selectList(cond);
    }

    /**
     * 기타검사 입력
     */
    @Transactional
    public EtcInspectionResponse create(Long mbrSn, Long petSn, EtcInspectionCreateRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        String actorId = String.valueOf(mbrSn);

        EtcInspection inspection = new EtcInspection();
        inspection.setCkupRcdSn(req.ckupRcdSn());
        inspection.setInspNm(requiredText(req.inspNm()));
        inspection.setInspRsltCn(requiredText(req.inspRsltCn()));
        inspection.setMemoCn(emptyToNull(req.memoCn()));
        inspection.setRgtrId(actorId);
        inspection.setMdfrId(actorId);
        etcInspectionMapper.insertInspection(inspection);
        return EtcInspectionResponse.from(etcInspectionMapper.selectInspection(inspection.getEtcInspSn()));
    }

    /**
     * 기타검사 수정
     */
    @Transactional
    public EtcInspectionResponse update(Long mbrSn, Long petSn, Long etcInspSn, EtcInspectionUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        EtcInspection inspection = getOwn(petSn, etcInspSn);
        if (req.inspNm() != null) {
            inspection.setInspNm(requiredText(req.inspNm()));
        }
        if (req.inspRsltCn() != null) {
            inspection.setInspRsltCn(requiredText(req.inspRsltCn()));
        }
        if (req.memoCn() != null) {
            inspection.setMemoCn(emptyToNull(req.memoCn()));
        }
        etcInspectionMapper.updateInspection(inspection, String.valueOf(mbrSn));
        return EtcInspectionResponse.from(etcInspectionMapper.selectInspection(etcInspSn));
    }

    /**
     * 기타검사 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long etcInspSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwn(petSn, etcInspSn);
        etcInspectionMapper.deleteInspection(etcInspSn, String.valueOf(mbrSn));
    }

    /**
     * 경로 반려동물의 기타검사 조회
     */
    private EtcInspection getOwn(Long petSn, Long etcInspSn) {
        EtcInspection inspection = etcInspectionMapper.selectInspection(etcInspSn);
        if (inspection == null || !checkupService.existsCheckup(petSn, inspection.getCkupRcdSn())) {
            throw new CustomException(ErrorCode.ETC_INSPECTION_NOT_FOUND);
        }
        return inspection;
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
