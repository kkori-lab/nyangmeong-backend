package com.nyangmeong.expense.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.ExpenseCreateCommand;
import com.nyangmeong.expense.dto.ReceiptCheckupCreateRequest;
import com.nyangmeong.expense.dto.ReceiptSaveCheckRequest;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.dto.ReceiptSaveDrugRequest;
import com.nyangmeong.expense.dto.ReceiptSavePetRequest;
import com.nyangmeong.expense.dto.ReceiptSaveRequest;
import com.nyangmeong.expense.dto.ReceiptSaveResponse;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.dto.CheckupResponse;
import com.nyangmeong.health.dto.PrescriptionCreateRequest;
import com.nyangmeong.health.dto.ReceiptCheckupCommand;
import com.nyangmeong.health.service.CheckupService;
import com.nyangmeong.health.service.PrescriptionService;

import lombok.RequiredArgsConstructor;

/**
 * [영수증 확인 후 저장]
 * - 반려동물별 검진기록·가계부를 한 트랜잭션으로 저장 (FN-AIM-009-24)
 * - 검진기록·일정·처방약은 health(JAJ) service, 지출은 expense 지출(SJM) service 로 저장
 * - 영수증 인식 실패 뒤 [직접 입력] 저장은 검진기록만 만들고 가계부에는 기록하지 않는다 (FN-HLT-002-31)
 * - 저장 내용은 요청 본문 값만 쓴다. OCR 결과를 회원 확인 없이 저장하지 않는다 (FN-AIM-009-08)
 */
@Service
@RequiredArgsConstructor
public class ReceiptSaveService {

    /** 처방약 케어리스트 반영 안 함 (FN-AIM-009-11) */
    static final Boolean CARE_RFLT_OFF = Boolean.FALSE;

    private final ReceiptMapper receiptMapper;
    private final ReceiptService receiptService;
    private final PetAuthorizer petAuthorizer;
    private final CheckupService checkupService;
    private final PrescriptionService prescriptionService;
    private final ExpenseService expenseService;

    /**
     * 저장 결과 : created 가 false 면 같은 멱등 요청의 재전송
     */
    public record Saved(ReceiptSaveResponse response, boolean created) {
    }

    /**
     * 저장 전 확인 : 가계부 중복, 가까운 예정 일정. 저장하지 않는다
     */
    @Transactional(readOnly = true)
    public ReceiptSaveCheckResponse check(Long mbrSn, Long recptSn, ReceiptSaveCheckRequest req) {
        Receipt receipt = getOwned(mbrSn, recptSn);
        /** 저장완료 영수증은 저장과 같은 409 (다른 상태는 지금처럼 확인 결과를 준다) */
        if (ReceiptStatus.from(receipt.getOcrPrcsSttsCd()) == ReceiptStatus.SAVED) {
            throw new CustomException(ErrorCode.RECEIPT_ALREADY_SAVED);
        }
        checkPets(mbrSn, req.pets().stream().map(ReceiptSaveCheckRequest.Pet::petSn).toList());

        List<ReceiptSaveCheckResponse.DuplicateExpense> duplicates = new ArrayList<>();
        List<ReceiptSaveCheckResponse.NearbySchedule> nearby = new ArrayList<>();
        for (ReceiptSaveCheckRequest.Pet pet : req.pets()) {
            if (wantsExpense(pet.expenseYn(), pet.amt())) {
                duplicates.addAll(expenseService.searchDuplicates(mbrSn, pet.petSn(), req.ckupYmd(), pet.amt()));
            }
            if (!checkupService.existsHospitalSchedule(pet.petSn(), req.ckupYmd())) {
                checkupService.findNearestUnlinkedSchedule(pet.petSn(), req.ckupYmd())
                        .ifPresent(schedule -> nearby.add(new ReceiptSaveCheckResponse.NearbySchedule(pet.petSn(),
                                schedule.getSchdlSn(), schedule.getSchdlDt(), schedule.getHsptlNm())));
            }
        }
        return new ReceiptSaveCheckResponse(duplicates, nearby);
    }

    /**
     * 확인 후 저장
     */
    @Transactional
    public Saved save(Long mbrSn, Long recptSn, ReceiptSaveRequest req) {
        Receipt receipt = getOwned(mbrSn, recptSn);
        ReceiptStatus before = ReceiptStatus.from(receipt.getOcrPrcsSttsCd());

        if (before == ReceiptStatus.SAVED && req.idmptDmndId().equals(receipt.getIdmptDmndId())) {
            return new Saved(new ReceiptSaveResponse(recptSn, findCheckupRecords(recptSn)), false);
        }
        Receipt usedBy = receiptMapper.selectByIdmptDmndId(req.idmptDmndId());
        if (usedBy != null && !recptSn.equals(usedBy.getRecptSn())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        checkSavable(before);
        validate(mbrSn, req);

        // 상태를 먼저 바꿔 같은 영수증의 동시 저장을 막는다. 아래에서 실패하면 함께 되돌아간다
        receiptService.markSaved(mbrSn, recptSn, before, req.idmptDmndId());

        List<ReceiptSaveResponse.CheckupRecord> records = new ArrayList<>();
        for (ReceiptSavePetRequest pet : req.pets()) {
            CheckupResponse checkup = checkupService.createFromReceipt(mbrSn, pet.petSn(),
                    new ReceiptCheckupCommand(recptSn, req.ckupYmd(), req.hsptlNm(), pet.ckupTypeCd(), pet.ckupCn(),
                            pet.ckupMemoCn(), pet.schdlSn()));
            for (ReceiptSaveDrugRequest drug : pet.drugsOrEmpty()) {
                prescriptionService.create(mbrSn, pet.petSn(), new PrescriptionCreateRequest(checkup.getCkupRcdSn(),
                        drug.mdcnNm(), drug.mdcnBgngYmd(), drug.mdcnEndYmd(), drug.dayMdcnNmtm(), drug.prscSttsCd(),
                        CARE_RFLT_OFF));
            }
            records.add(new ReceiptSaveResponse.CheckupRecord(pet.petSn(), checkup.getCkupRcdSn()));
            if (wantsExpense(pet.expenseYn(), pet.amt())) {
                expenseService.create(new ExpenseCreateCommand(mbrSn, pet.petSn(), recptSn, req.ckupYmd(), pet.amt(),
                        req.expndPlcusNmOrNull(), null));
            }
        }
        return new Saved(new ReceiptSaveResponse(recptSn, records), true);
    }

    /**
     * 영수증 인식 실패·일시 오류 뒤 [직접 입력] 저장 (FN-HLT-002-31)
     * - 영수증을 저장완료로 바꾸고 검진기록에 이 영수증을 연결한다. 가계부에는 기록하지 않는다
     */
    @Transactional
    public CheckupResponse createDirectCheckup(Long mbrSn, Long recptSn, Long petSn,
            ReceiptCheckupCreateRequest req) {
        Receipt usedBy = receiptMapper.selectByIdmptDmndId(req.idmptDmndId());
        if (usedBy != null && !recptSn.equals(usedBy.getRecptSn())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        receiptService.markSavedByExpense(mbrSn, recptSn, req.idmptDmndId());
        return checkupService.create(mbrSn, petSn, req.toCheckupCreateRequest(), recptSn);
    }

    /**
     * 영수증으로 만든 검진기록 (같은 요청 재전송 응답)
     */
    private List<ReceiptSaveResponse.CheckupRecord> findCheckupRecords(Long recptSn) {
        return checkupService.findReceiptCheckups(recptSn).stream()
                .map(row -> new ReceiptSaveResponse.CheckupRecord(row.getPetSn(), row.getCkupRcdSn()))
                .toList();
    }

    /**
     * 회원 영수증 조회
     */
    private Receipt getOwned(Long mbrSn, Long recptSn) {
        Receipt receipt = receiptMapper.selectOwned(recptSn, mbrSn);
        if (receipt == null) {
            throw new CustomException(ErrorCode.RECEIPT_NOT_FOUND);
        }
        return receipt;
    }

    /**
     * 저장할 수 있는 상태 확인 : 인식성공·부분인식만
     */
    private void checkSavable(ReceiptStatus status) {
        switch (status) {
            case RECOGNIZED, PARTIAL -> { }
            case ANALYZING -> throw new CustomException(ErrorCode.RECEIPT_ANALYZING);
            case CANCELED -> throw new CustomException(ErrorCode.RECEIPT_ALREADY_CANCELED);
            case SAVED -> throw new CustomException(ErrorCode.RECEIPT_ALREADY_SAVED);
            case NOT_RECOGNIZED, TEMPORARY_ERROR -> throw new CustomException(ErrorCode.RECEIPT_NOT_SAVABLE);
        }
    }

    /**
     * 탭 검증 : 반려동물 중복·권한, 약품 복용 기간 (FN-AIM-009-07·018)
     */
    private void validate(Long mbrSn, ReceiptSaveRequest req) {
        checkPets(mbrSn, req.pets().stream().map(ReceiptSavePetRequest::petSn).toList());
        for (ReceiptSavePetRequest pet : req.pets()) {
            for (ReceiptSaveDrugRequest drug : pet.drugsOrEmpty()) {
                if (drug.mdcnEndYmd().isBefore(drug.mdcnBgngYmd())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }
            }
        }
    }

    /**
     * 같은 반려동물이 두 탭에 있으면 안 되고, 탭마다 양육자여야 한다
     */
    private void checkPets(Long mbrSn, List<Long> petSns) {
        Set<Long> seen = new HashSet<>();
        for (Long petSn : petSns) {
            if (!seen.add(petSn)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        petSns.forEach(petSn -> petAuthorizer.checkCaregiver(mbrSn, petSn));
    }

    /**
     * 가계부에 기록할 탭인지 : 체크했고 금액이 1원 이상 (FN-AIM-009-22)
     */
    private boolean wantsExpense(Boolean expenseYn, Long amt) {
        return Boolean.TRUE.equals(expenseYn) && amt != null && amt >= 1;
    }
}
