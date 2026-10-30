package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.PrescriptionCreateRequest;
import com.nyangmeong.health.dto.PrescriptionResponse;
import com.nyangmeong.health.dto.PrescriptionSearchCond;
import com.nyangmeong.health.dto.PrescriptionSummary;
import com.nyangmeong.health.dto.PrescriptionUpdateRequest;
import com.nyangmeong.health.mapper.PrescriptionMapper;

import lombok.RequiredArgsConstructor;

/**
 * [처방약 서비스]
 * - 처방약 목록·추가·수정·삭제, 복용 종료일 경과 자동 종료 (SCR-HLT-005, FN-HLT-005-03)
 * - 복용중 + 주기 체크 ON일 때만 투약 케어 항목 유지 : 그 밖에는 삭제(완료 기록 유지)
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String SYSTEM_ID = "SYSTEM";

    private final PrescriptionMapper prescriptionMapper;
    private final CheckupService checkupService;
    private final CareItemService careItemService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 처방약 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PrescriptionSummary> search(Long mbrSn, Long petSn, PrescriptionSearchCond cond) {
        if (cond.getCkupRcdSn() == null) {
            List<Long> ckupRcdSns = checkupService.searchAllCompleted(mbrSn, petSn).stream()
                    .map(CheckupSummary::getCkupRcdSn).toList();
            if (ckupRcdSns.isEmpty()) {
                return List.of();
            }
            return prescriptionMapper.selectListByCheckups(ckupRcdSns, cond, PrescriptionStatus.TAKING.getCode());
        }
        checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        return prescriptionMapper.selectList(cond, PrescriptionStatus.TAKING.getCode());
    }

    /**
     * 처방약 추가
     */
    @Transactional
    public PrescriptionResponse create(Long mbrSn, Long petSn, PrescriptionCreateRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        String actorId = String.valueOf(mbrSn);

        Prescription prescription = new Prescription();
        prescription.setCkupRcdSn(req.ckupRcdSn());
        prescription.setMdcnNm(requiredText(req.mdcnNm()));
        prescription.setMdcnBgngYmd(req.mdcnBgngYmd());
        prescription.setMdcnEndYmd(req.mdcnEndYmd());
        prescription.setDayMdcnNmtm(req.dayMdcnNmtm());
        prescription.setPrscSttsCd(statusOf(req.prscSttsCd()).getCode());
        prescription.setCareRfltYn(Boolean.TRUE.equals(req.careRfltYn()) ? "Y" : "N");
        prescription.setRgtrId(actorId);
        prescription.setMdfrId(actorId);
        checkPeriod(prescription);

        prescriptionMapper.insertPrescription(prescription);
        prescriptionMapper.insertStatusHistory(prescription.getPrscSn(), mbrSn, null, prescription.getPrscSttsCd());
        syncMedicationItem(mbrSn, petSn, prescription, actorId);
        return PrescriptionResponse.from(prescription);
    }

    /**
     * 처방약 수정
     */
    @Transactional
    public PrescriptionResponse update(Long mbrSn, Long petSn, Long prscSn, PrescriptionUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        Prescription prescription = getOwn(petSn, prscSn);
        String actorId = String.valueOf(mbrSn);
        String bfrSttsCd = prescription.getPrscSttsCd();

        if (req.mdcnNm() != null) {
            prescription.setMdcnNm(requiredText(req.mdcnNm()));
        }
        if (req.mdcnBgngYmd() != null) {
            prescription.setMdcnBgngYmd(req.mdcnBgngYmd());
        }
        if (req.mdcnEndYmd() != null) {
            prescription.setMdcnEndYmd(req.mdcnEndYmd());
        }
        if (req.dayMdcnNmtm() != null) {
            prescription.setDayMdcnNmtm(req.dayMdcnNmtm());
        }
        if (req.prscSttsCd() != null) {
            prescription.setPrscSttsCd(statusOf(req.prscSttsCd()).getCode());
        }
        if (req.careRfltYn() != null) {
            prescription.setCareRfltYn(req.careRfltYn() ? "Y" : "N");
        }
        checkPeriod(prescription);

        prescriptionMapper.updatePrescription(prescription, actorId);
        if (!bfrSttsCd.equals(prescription.getPrscSttsCd())) {
            prescriptionMapper.insertStatusHistory(prscSn, mbrSn, bfrSttsCd, prescription.getPrscSttsCd());
        }
        syncMedicationItem(mbrSn, petSn, prescription, actorId);
        return PrescriptionResponse.from(prescription);
    }

    /**
     * 처방약 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long prscSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwn(petSn, prscSn);
        String actorId = String.valueOf(mbrSn);
        prescriptionMapper.deletePrescription(prscSn, actorId);
        careItemService.deleteMedicationItem(prscSn, actorId);
    }

    /**
     * 복용 종료일 경과 처방약 종료
     */
    @Transactional
    public int updateExpired() {
        String takingCd = PrescriptionStatus.TAKING.getCode();
        String endedCd = PrescriptionStatus.ENDED.getCode();
        List<Prescription> expired = prescriptionMapper.selectExpired(LocalDate.now(ZONE), takingCd);
        for (Prescription prescription : expired) {
            prescription.setPrscSttsCd(endedCd);
            prescriptionMapper.updatePrescription(prescription, SYSTEM_ID);
            prescriptionMapper.insertStatusHistory(prescription.getPrscSn(), null, takingCd, endedCd);
            careItemService.deleteMedicationItem(prescription.getPrscSn(), SYSTEM_ID);
        }
        return expired.size();
    }

    /**
     * 투약 케어 항목 추가·삭제
     */
    private void syncMedicationItem(Long mbrSn, Long petSn, Prescription prescription, String actorId) {
        boolean isLinked = PrescriptionStatus.TAKING.is(prescription.getPrscSttsCd())
                && "Y".equals(prescription.getCareRfltYn());
        if (isLinked) {
            careItemService.createMedicationItem(mbrSn, petSn, prescription);
        } else {
            careItemService.deleteMedicationItem(prescription.getPrscSn(), actorId);
        }
    }

    /**
     * 경로 반려동물의 처방약 잠금 조회
     */
    private Prescription getOwn(Long petSn, Long prscSn) {
        Prescription prescription = prescriptionMapper.selectPrescriptionForUpdate(prscSn);
        if (prescription == null || !checkupService.existsCheckup(petSn, prescription.getCkupRcdSn())) {
            throw new CustomException(ErrorCode.PRESCRIPTION_NOT_FOUND);
        }
        return prescription;
    }

    private void checkPeriod(Prescription prescription) {
        if (prescription.getMdcnEndYmd().isBefore(prescription.getMdcnBgngYmd())) {
            throw invalid();
        }
    }

    private PrescriptionStatus statusOf(String prscSttsCd) {
        return PrescriptionStatus.of(prscSttsCd).orElseThrow(this::invalid);
    }

    private String requiredText(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        return value.trim();
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
