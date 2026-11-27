package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.Prescription;
import com.nyangmeong.health.domain.PrescriptionStatus;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.PrescriptionBulkSaveRequest;
import com.nyangmeong.health.dto.PrescriptionBulkSaveRequest.PrescriptionItemRequest;
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
        prescription.setMdcnNm(requiredText(req.mdcnNm(), "mdcnNm"));
        prescription.setMdcnBgngYmd(req.mdcnBgngYmd());
        prescription.setMdcnEndYmd(req.mdcnEndYmd());
        prescription.setDayMdcnNmtm(req.dayMdcnNmtm());
        prescription.setPrscSttsCd(statusOf(req.prscSttsCd(), "prscSttsCd").getCode());
        prescription.setCareRfltYn(Boolean.TRUE.equals(req.careRfltYn()) ? "Y" : "N");
        prescription.setRgtrId(actorId);
        prescription.setMdfrId(actorId);
        checkPeriod(prescription, "mdcnEndYmd");

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
            prescription.setMdcnNm(requiredText(req.mdcnNm(), "mdcnNm"));
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
            prescription.setPrscSttsCd(statusOf(req.prscSttsCd(), "prscSttsCd").getCode());
        }
        if (req.careRfltYn() != null) {
            prescription.setCareRfltYn(req.careRfltYn() ? "Y" : "N");
        }
        checkPeriod(prescription, "mdcnEndYmd");

        prescriptionMapper.updatePrescription(prescription, actorId);
        if (!bfrSttsCd.equals(prescription.getPrscSttsCd())) {
            prescriptionMapper.insertStatusHistory(prscSn, mbrSn, bfrSttsCd, prescription.getPrscSttsCd());
        }
        syncMedicationItem(mbrSn, petSn, prescription, actorId);
        return PrescriptionResponse.from(prescription);
    }

    /**
     * 처방약 일괄 저장
     * - 전부 검증·잠금 조회한 뒤 저장 : 하나라도 실패하면 전부 되돌림
     */
    @Transactional
    public List<PrescriptionResponse> saveAll(Long mbrSn, Long petSn, PrescriptionBulkSaveRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        String actorId = String.valueOf(mbrSn);

        Set<Long> prscSns = new HashSet<>();
        List<PendingSave> pendings = new ArrayList<>();
        for (PrescriptionItemRequest item : req.values()) {
            Prescription prescription;
            String bfrSttsCd = null;
            if (item.prscSn() == null) {
                prescription = new Prescription();
                prescription.setCkupRcdSn(req.ckupRcdSn());
                prescription.setRgtrId(actorId);
            } else {
                if (!prscSns.add(item.prscSn())) {
                    throw invalid();
                }
                prescription = getOwn(petSn, item.prscSn());
                if (!req.ckupRcdSn().equals(prescription.getCkupRcdSn())) {
                    throw new CustomException(ErrorCode.PRESCRIPTION_NOT_FOUND);
                }
                bfrSttsCd = prescription.getPrscSttsCd();
            }
            prescription.setMdcnNm(requiredText(item.mdcnNm()));
            prescription.setMdcnBgngYmd(item.mdcnBgngYmd());
            prescription.setMdcnEndYmd(item.mdcnEndYmd());
            prescription.setDayMdcnNmtm(item.dayMdcnNmtm());
            prescription.setPrscSttsCd(statusOf(item.prscSttsCd()).getCode());
            prescription.setCareRfltYn(Boolean.TRUE.equals(item.careRfltYn()) ? "Y" : "N");
            prescription.setMdfrId(actorId);
            checkPeriod(prescription);
            pendings.add(new PendingSave(prescription, item.prscSn() == null, bfrSttsCd));
        }

        List<PrescriptionResponse> responses = new ArrayList<>();
        for (PendingSave pending : pendings) {
            Prescription prescription = pending.prescription();
            if (pending.isNew()) {
                prescriptionMapper.insertPrescription(prescription);
                prescriptionMapper.insertStatusHistory(prescription.getPrscSn(), mbrSn, null,
                        prescription.getPrscSttsCd());
            } else {
                prescriptionMapper.updatePrescription(prescription, actorId);
                if (!pending.bfrSttsCd().equals(prescription.getPrscSttsCd())) {
                    prescriptionMapper.insertStatusHistory(prescription.getPrscSn(), mbrSn, pending.bfrSttsCd(),
                            prescription.getPrscSttsCd());
                }
            }
            syncMedicationItem(mbrSn, petSn, prescription, actorId);
            responses.add(PrescriptionResponse.from(prescription));
        }
        return responses;
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

    /**
     * 복용 기간 검사 : fields는 400 data에 담을 칸 이름(일괄 저장 행은 그대로)
     */
    private void checkPeriod(Prescription prescription, String... fields) {
        if (prescription.getMdcnEndYmd().isBefore(prescription.getMdcnBgngYmd())) {
            throw invalid(fields);
        }
    }

    private PrescriptionStatus statusOf(String prscSttsCd, String... fields) {
        return PrescriptionStatus.of(prscSttsCd).orElseThrow(() -> invalid(fields));
    }

    private String requiredText(String value, String... fields) {
        if (value == null || value.isBlank()) {
            throw invalid(fields);
        }
        return value.trim();
    }

    private CustomException invalid(String... fields) {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE, fields);
    }

    /**
     * 일괄 저장 대기 1건 : 추가 여부, 수정 전 상태
     */
    private record PendingSave(Prescription prescription, boolean isNew, String bfrSttsCd) {
    }
}
