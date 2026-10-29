package com.nyangmeong.expense.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.expense.domain.Expense;
import com.nyangmeong.expense.domain.ExpenseCategory;
import com.nyangmeong.expense.domain.ExpenseInputMethod;
import com.nyangmeong.expense.dto.ExpenseBulkDeleteRequest;
import com.nyangmeong.expense.dto.ExpenseCreateCommand;
import com.nyangmeong.expense.dto.ExpenseCreateRequest;
import com.nyangmeong.expense.dto.ExpenseLineRequest;
import com.nyangmeong.expense.dto.ExpenseResponse;
import com.nyangmeong.expense.dto.ExpenseSearchCond;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.dto.ExpenseUpdateRequest;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.PetService;

import lombok.RequiredArgsConstructor;

/**
 * [가계부 지출 등록·중복 확인 (SCR-MYP-031)·수정·삭제 (SCR-MYP-030)]
 * - 지출은 회원별 : 중복 확인·한도 합계·수정·삭제 모두 내가 등록한 지출만
 * - 등록 뒤 이번 달 합계가 한도 90%·100% 를 처음 넘으면 한도 알림 (FN-MYP-030-08), 수정·삭제는 알리지 않음
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    static final String LIMIT_PATH = "/mypage/expenses";

    private final ExpenseMapper expenseMapper;
    private final ReceiptService receiptService;
    private final PetAuthorizer petAuthorizer;
    private final PetService petService;
    private final CareMissionService careMissionService;
    private final EventPublisher eventPublisher;

    /**
     * 등록 결과 : created 가 false 면 같은 멱등 요청의 재전송
     */
    public record Created(List<ExpenseResponse> responses, boolean created) {
    }

    /**
     * 지출 등록
     */
    @Transactional
    public Created create(Long mbrSn, ExpenseCreateRequest req) {
        List<String> idmptDmndIds = req.lines().stream().map(ExpenseLineRequest::idmptDmndId).toList();
        List<ExpenseSummary> existing = expenseMapper.selectByIdmptDmndIds(mbrSn, idmptDmndIds);
        if (existing.size() == idmptDmndIds.size()) {
            return new Created(existing.stream().map(ExpenseResponse::from).toList(), false);
        }
        if (!existing.isEmpty()) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        validate(mbrSn, req);

        if (req.recptSn() != null) {
            receiptService.markSavedByExpense(mbrSn, req.recptSn(), idmptDmndIds.get(0));
        }
        List<Expense> expenses = new ArrayList<>();
        for (ExpenseLineRequest line : req.lines()) {
            Expense expense = new Expense();
            expense.setPetSn(line.petSn());
            expense.setMbrSn(mbrSn);
            expense.setRecptSn(req.recptSn());
            expense.setExpndClsfCd(req.expndClsfCd());
            expense.setExpndInptMthCd(req.expndInptMthCd());
            expense.setExpndAmt(line.expndAmt());
            expense.setExpndPlcusNm(req.expndPlcusNmOrNull());
            expense.setDtlMttr(req.dtlMttrOrNull());
            expense.setExpndYmd(req.expndYmd());
            expense.setIdmptDmndId(line.idmptDmndId());
            expenses.add(expense);
        }
        insertAll(mbrSn, expenses);
        return new Created(expenseMapper.selectByIdmptDmndIds(mbrSn, idmptDmndIds).stream()
                .map(ExpenseResponse::from).toList(), true);
    }

    /**
     * 영수증 저장의 병원 지출 등록 (FN-AIM-009-10)
     * - 영수증 상태는 영수증 저장이 바꿈
     */
    @Transactional
    public void create(ExpenseCreateCommand command) {
        petAuthorizer.checkCaregiver(command.mbrSn(), command.petSn());
        Expense expense = new Expense();
        expense.setPetSn(command.petSn());
        expense.setMbrSn(command.mbrSn());
        expense.setRecptSn(command.recptSn());
        expense.setExpndClsfCd(ExpenseCategory.HOSPITAL.getCode());
        expense.setExpndInptMthCd(ExpenseInputMethod.OCR.getCode());
        expense.setExpndAmt(command.expndAmt());
        expense.setExpndPlcusNm(command.expndPlcusNm());
        expense.setDtlMttr(command.dtlMttr());
        expense.setExpndYmd(command.expndYmd());
        insertAll(command.mbrSn(), List.of(expense));
    }

    /**
     * 저장 전 중복 지출 조회
     */
    @Transactional(readOnly = true)
    public List<ExpenseSummary> searchDuplicates(Long mbrSn, ExpenseCreateRequest req) {
        List<ExpenseSummary> rows = expenseMapper.selectDuplicates(mbrSn, req.expndYmd(), req.expndClsfCd(),
                req.lines());
        List<ExpenseSummary> ordered = new ArrayList<>();
        for (ExpenseLineRequest line : req.lines()) {
            rows.stream()
                    .filter(row -> Objects.equals(row.getPetSn(), line.petSn())
                            && row.getExpndAmt().equals(line.expndAmt()))
                    .forEach(ordered::add);
        }
        return ordered;
    }

    /**
     * 영수증 저장 전 병원 지출 중복 조회 (FN-AIM-009-23)
     */
    @Transactional(readOnly = true)
    public List<ReceiptSaveCheckResponse.DuplicateExpense> searchDuplicates(Long mbrSn, Long petSn, LocalDate ymd,
            Long amt) {
        return expenseMapper.selectDuplicates(mbrSn, ymd, ExpenseCategory.HOSPITAL.getCode(),
                List.of(new ExpenseLineRequest(petSn, amt, null))).stream()
                .map(row -> new ReceiptSaveCheckResponse.DuplicateExpense(row.getPetSn(), row.getExpndSn(),
                        row.getExpndYmd(), row.getExpndAmt(), row.getDtlMttr(), row.getExpndPlcusNm()))
                .toList();
    }

    /**
     * 지출 수정 (FN-MYP-030-03·12)
     * - 반려동물을 바꿀 때만 연동 검사 : 그대로 두면 연동 해제된 반려동물이어도 수정 가능
     */
    @Transactional
    public ExpenseResponse update(Long mbrSn, Long expndSn, ExpenseUpdateRequest req) {
        Expense expense = expenseMapper.selectMine(mbrSn, expndSn);
        if (expense == null) {
            throw new CustomException(ErrorCode.EXPENSE_NOT_FOUND);
        }
        boolean common = Boolean.TRUE.equals(req.common());
        if (common && req.petSn() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (req.expndClsfCd() != null && !ExpenseCategory.exists(req.expndClsfCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long petSn = common ? null : req.petSn() != null ? req.petSn() : expense.getPetSn();
        String clsfCd = req.expndClsfCd() != null ? req.expndClsfCd() : expense.getExpndClsfCd();
        if (petSn == null && ExpenseCategory.HOSPITAL.is(clsfCd)) {
            throw new CustomException(ErrorCode.EXPENSE_PET_REQUIRED);
        }
        if (petSn != null && !petSn.equals(expense.getPetSn())) {
            petAuthorizer.checkCaregiver(mbrSn, petSn);
        }
        expense.setPetSn(petSn);
        expense.setExpndClsfCd(clsfCd);
        if (req.expndYmd() != null) {
            expense.setExpndYmd(req.expndYmd());
        }
        if (req.expndAmt() != null) {
            expense.setExpndAmt(req.expndAmt());
        }
        if (req.dtlMttr() != null) {
            expense.setDtlMttr(req.dtlMttr().isBlank() ? null : req.dtlMttr());
        }
        if (req.expndPlcusNm() != null) {
            expense.setExpndPlcusNm(req.expndPlcusNm().isBlank() ? null : req.expndPlcusNm());
        }
        expenseMapper.update(expense);
        return ExpenseResponse.from(expenseMapper.selectSummary(mbrSn, expndSn));
    }

    /**
     * 지출 삭제 (FN-MYP-030-03·15·19·21)
     * - 하나라도 없거나 남의 지출이면 모두 취소
     */
    @Transactional
    public void delete(Long mbrSn, List<Long> expndSns) {
        List<Long> distinct = expndSns == null ? List.of()
                : expndSns.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (expenseMapper.deleteMine(mbrSn, distinct) != distinct.size()) {
            throw new CustomException(ErrorCode.EXPENSE_NOT_FOUND);
        }
    }

    /**
     * 조건 전체 삭제 (FN-MYP-030-18·19·21)
     * - 화면이 보여 준 건수와 다르면 그 사이 바뀐 것이므로 지우지 않음
     */
    @Transactional
    public void bulkDelete(Long mbrSn, ExpenseBulkDeleteRequest req) {
        ExpenseSearchCond cond = ExpenseQueryService.normalize(req.toCond());
        if (expenseMapper.selectCount(mbrSn, cond) != req.expectedCnt()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        expenseMapper.deleteByCond(mbrSn, cond);
    }

    /**
     * 등록 요청 검사
     */
    private void validate(Long mbrSn, ExpenseCreateRequest req) {
        if (!ExpenseCategory.exists(req.expndClsfCd()) || !ExpenseInputMethod.exists(req.expndInptMthCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Set<Long> petSns = new HashSet<>();
        for (ExpenseLineRequest line : req.lines()) {
            if (line.petSn() == null && ExpenseCategory.HOSPITAL.is(req.expndClsfCd())) {
                throw new CustomException(ErrorCode.EXPENSE_PET_REQUIRED);
            }
            if (!petSns.add(line.petSn())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        /** 반려동물이 없어도 공용 1줄은 등록 가능 */
        if (req.lines().size() > Math.max(petService.searchMyPets(mbrSn).size(), 1)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        for (Long petSn : petSns) {
            if (petSn != null) {
                petAuthorizer.checkCaregiver(mbrSn, petSn);
            }
        }
    }

    /**
     * 지출 저장과 케어미션 반영·한도 알림
     */
    private void insertAll(Long mbrSn, List<Expense> expenses) {
        try {
            expenses.forEach(expenseMapper::insert);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        for (Expense expense : expenses) {
            if (expense.getPetSn() != null) {
                careMissionService.reflect(expense.getPetSn(), CareMission.EXPENSE_CREATE, mbrSn);
            }
        }
        notifyLimit(mbrSn, expenses);
    }

    /**
     * 이번 달 한도 알림
     * - 한 번에 90%·100% 를 모두 넘으면 초과 알림만
     */
    private void notifyLimit(Long mbrSn, List<Expense> expenses) {
        YearMonth thisMonth = YearMonth.now(ZONE);
        long addedAmt = expenses.stream()
                .filter(expense -> YearMonth.from(expense.getExpndYmd()).equals(thisMonth))
                .mapToLong(Expense::getExpndAmt)
                .sum();
        if (addedAmt == 0) {
            return;
        }
        Long limitAmt = expenseMapper.selectNotifiedLimitAmt(mbrSn);
        if (limitAmt == null || limitAmt <= 0) {
            return;
        }
        long afterAmt = expenseMapper.selectTotalAmt(mbrSn, thisMonth.atDay(1), thisMonth.atEndOfMonth());
        long beforeAmt = afterAmt - addedAmt;
        String notiCn = String.format("%,d원 / %,d원", afterAmt, limitAmt);
        if (beforeAmt <= limitAmt && afterAmt > limitAmt) {
            eventPublisher.publish(new NotificationEvent(mbrSn, NotificationType.EXPENSE,
                    "이번 달 지출이 한도를 넘었어요", notiCn, LIMIT_PATH));
        } else if (beforeAmt * 10 < limitAmt * 9 && afterAmt * 10 >= limitAmt * 9) {
            eventPublisher.publish(new NotificationEvent(mbrSn, NotificationType.EXPENSE,
                    "이번 달 지출이 한도의 90%에 가까워졌어요", notiCn, LIMIT_PATH));
        }
    }
}
