package com.nyangmeong.expense.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptSource;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;

import lombok.RequiredArgsConstructor;

/**
 * [영수증 상태 처리]
 * - 업로드·다시 분석·분석 종료·취소 각각 별도 트랜잭션 : OCR 호출은 트랜잭션 밖 (ReceiptAnalyzer)
 * - 출처 : 건강기록(기본값)·가계부·AI 대화창 (FN-AIM-008-02·013-04, FN-MYP-031-03)
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    /** 가계부 지출에 연결할 수 있는 상태 */
    private static final List<ReceiptStatus> EXPENSE_SAVABLE = List.of(ReceiptStatus.RECOGNIZED,
            ReceiptStatus.PARTIAL, ReceiptStatus.NOT_RECOGNIZED, ReceiptStatus.TEMPORARY_ERROR);

    private final ReceiptMapper receiptMapper;
    private final FileStore fileStore;
    private final PetAuthorizer petAuthorizer;

    /**
     * 영수증 업로드
     */
    @Transactional
    public Long create(Long mbrSn, Long petSn, MultipartFile file, String ocrDmndSrcCd) {
        ReceiptSource source = ReceiptSource.from(ocrDmndSrcCd);
        // 가계부 출처 : RECPT에 반려동물 칸이 없고 반려동물은 확인 모달에서 고른다 (FN-MYP-031-03)
        if (source != ReceiptSource.EXPENSE) {
            if (petSn == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            petAuthorizer.checkCaregiver(mbrSn, petSn);
        }
        Long atchFileSn = fileStore.store(file, FileCategory.EXPENSE_RECEIPT, FileOwner.member(mbrSn), String.valueOf(mbrSn));

        Receipt receipt = new Receipt();
        receipt.setMbrSn(mbrSn);
        receipt.setAtchFileSn(atchFileSn);
        receipt.setOcrPrcsSttsCd(ReceiptStatus.ANALYZING.getCode());
        receipt.setOcrDmndSrcCd(source.getCode());
        receiptMapper.insert(receipt);
        insertHistory(receipt.getRecptSn(), mbrSn, null, ReceiptStatus.ANALYZING, null);
        return receipt.getRecptSn();
    }

    /**
     * 회원 영수증 조회 : 결과 다시 열기 (FN-AIM-013-06)
     * - 가계부 출처만 반려동물 생략 가능 (업로드와 같은 규칙, FN-MYP-031-03)
     */
    @Transactional(readOnly = true)
    public Receipt get(Long mbrSn, Long recptSn, Long petSn) {
        Receipt receipt = getOwned(mbrSn, recptSn);
        if (petSn != null) {
            petAuthorizer.checkCaregiver(mbrSn, petSn);
        } else if (ReceiptSource.from(receipt.getOcrDmndSrcCd()) != ReceiptSource.EXPENSE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "petSn");
        }
        return receipt;
    }

    /**
     * 본인 영수증의 요청 출처 : 인식 판정 기준 (FN-AIM-010-01)
     */
    @Transactional(readOnly = true)
    public ReceiptSource getSource(Long mbrSn, Long recptSn) {
        return ReceiptSource.from(getOwned(mbrSn, recptSn).getOcrDmndSrcCd());
    }

    /**
     * 마지막 상태 변경 사유 : 일시 오류 사유
     */
    @Transactional(readOnly = true)
    public String getLastChgRsn(Long recptSn) {
        return receiptMapper.selectLastChgRsn(recptSn);
    }

    /**
     * 영수증 원본 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long mbrSn, Long recptSn) {
        return fileStore.load(getOwned(mbrSn, recptSn).getAtchFileSn());
    }

    /**
     * 분석 종료
     */
    @Transactional
    public boolean finish(Long mbrSn, Long recptSn, ReceiptStatus after, String chgRsn, String ocrRspnsJson) {
        if (receiptMapper.updateAnalyzed(recptSn, ReceiptStatus.ANALYZING.getCode(), after.getCode(),
                ocrRspnsJson) == 0) {
            return false;
        }
        insertHistory(recptSn, mbrSn, ReceiptStatus.ANALYZING, after, chgRsn);
        return true;
    }

    /**
     * Python AI 서버의 분석 결과·상태 반영 : 분석중인 본인 영수증만 (FN-AIM-010-01·013-04)
     */
    @Transactional
    public void report(Long mbrSn, Long recptSn, ReceiptStatus after, String chgRsn, String ocrRspnsJson) {
        ReceiptStatus before = ReceiptStatus.from(getOwned(mbrSn, recptSn).getOcrPrcsSttsCd());
        if (before != ReceiptStatus.ANALYZING) {
            throw conflict(before);
        }
        if (!finish(mbrSn, recptSn, after, chgRsn, ocrRspnsJson)) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
    }

    /**
     * 저장 완료 처리 : 인식성공·부분인식에서만
     */
    @Transactional
    public void markSaved(Long mbrSn, Long recptSn, ReceiptStatus before, String idmptDmndId) {
        if (receiptMapper.updateSaved(recptSn, codes(List.of(ReceiptStatus.RECOGNIZED, ReceiptStatus.PARTIAL)),
                ReceiptStatus.SAVED.getCode(), idmptDmndId) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        insertHistory(recptSn, mbrSn, before, ReceiptStatus.SAVED, null);
    }

    /**
     * 가계부 지출 저장의 영수증 저장 완료 처리
     * - 인식 실패 뒤 직접 입력(FN-MYP-031-05)도 연결 : 미인식·일시오류도 저장완료로
     */
    @Transactional
    public void markSavedByExpense(Long mbrSn, Long recptSn, String idmptDmndId) {
        ReceiptStatus before = ReceiptStatus.from(getOwned(mbrSn, recptSn).getOcrPrcsSttsCd());
        if (!EXPENSE_SAVABLE.contains(before)) {
            throw conflict(before);
        }
        if (receiptMapper.updateSaved(recptSn, codes(EXPENSE_SAVABLE), ReceiptStatus.SAVED.getCode(),
                idmptDmndId) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        insertHistory(recptSn, mbrSn, before, ReceiptStatus.SAVED, null);
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
     * 현재 상태별 충돌 예외
     */
    private CustomException conflict(ReceiptStatus status) {
        return switch (status) {
            case ANALYZING -> new CustomException(ErrorCode.RECEIPT_ANALYZING);
            case CANCELED -> new CustomException(ErrorCode.RECEIPT_ALREADY_CANCELED);
            case SAVED -> new CustomException(ErrorCode.RECEIPT_ALREADY_SAVED);
            default -> new CustomException(ErrorCode.ALREADY_PROCESSED);
        };
    }

    /**
     * 상태 코드 목록
     */
    private static List<String> codes(List<ReceiptStatus> statuses) {
        return statuses.stream().map(ReceiptStatus::getCode).toList();
    }

    /**
     * 상태 이력 등록
     */
    private void insertHistory(Long recptSn, Long mbrSn, ReceiptStatus before, ReceiptStatus after, String chgRsn) {
        ReceiptStatusHistory history = new ReceiptStatusHistory();
        history.setRecptSn(recptSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(chgRsn);
        receiptMapper.insertStatusHistory(history);
    }
}
