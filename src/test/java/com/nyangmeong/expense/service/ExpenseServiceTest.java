package com.nyangmeong.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.expense.domain.Expense;
import com.nyangmeong.expense.domain.ExpenseCategory;
import com.nyangmeong.expense.domain.ExpenseInputMethod;
import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptStatus;
import com.nyangmeong.expense.dto.ExpenseCreateCommand;
import com.nyangmeong.expense.dto.ExpenseCreateRequest;
import com.nyangmeong.expense.dto.ExpenseLineRequest;
import com.nyangmeong.expense.dto.ExpenseSummary;
import com.nyangmeong.expense.mapper.ExpenseMapper;
import com.nyangmeong.expense.mapper.ReceiptMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.CareMissionService;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 가계부 지출 등록·중복 확인·한도 알림]
 * - 영수증 상태 변경은 실제 ReceiptService 로 확인
 */
class ExpenseServiceTest {

    private static final Long MBR = 1L;
    private static final String HOSPITAL = ExpenseCategory.HOSPITAL.getCode();
    private static final String FOOD = ExpenseCategory.FOOD.getCode();
    private static final String DIRECT = ExpenseInputMethod.DIRECT.getCode();
    private static final String OCR = ExpenseInputMethod.OCR.getCode();

    private final ExpenseMapper expenseMapper = mock(ExpenseMapper.class);
    private final ReceiptMapper receiptMapper = mock(ReceiptMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final PetService petService = mock(PetService.class);
    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final ReceiptService receiptService = new ReceiptService(receiptMapper, mock(FileStore.class),
            petAuthorizer);
    private final ExpenseService service = new ExpenseService(expenseMapper, receiptService, petAuthorizer,
            petService, careMissionService, eventPublisher);

    private final LocalDate today = LocalDate.now(ExpenseService.ZONE);

    @BeforeEach
    void setUp() {
        linkedPets(3);
        when(expenseMapper.selectByIdmptDmndIds(eq(MBR), anyList())).thenReturn(List.of());
    }

    private void linkedPets(int count) {
        List<PetSummary> pets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PetSummary pet = new PetSummary();
            pet.setPetSn(10L + i);
            pets.add(pet);
        }
        when(petService.searchMyPets(MBR)).thenReturn(pets);
    }

    private ExpenseCreateRequest request(String clsfCd, String inptMthCd, Long recptSn, LocalDate ymd,
            ExpenseLineRequest... lines) {
        return new ExpenseCreateRequest(ymd, clsfCd, " ", "행복동물병원", recptSn, inptMthCd, List.of(lines));
    }

    private ExpenseLineRequest line(Long petSn, long amt, String idmptDmndId) {
        return new ExpenseLineRequest(petSn, amt, idmptDmndId);
    }

    private ExpenseSummary summary(Long expndSn, Long petSn, long amt) {
        ExpenseSummary row = new ExpenseSummary();
        row.setExpndSn(expndSn);
        row.setPetSn(petSn);
        row.setExpndAmt(amt);
        row.setExpndYmd(today);
        row.setExpndClsfCd(HOSPITAL);
        return row;
    }

    private void receipt(ReceiptStatus status) {
        Receipt receipt = new Receipt();
        receipt.setRecptSn(3L);
        receipt.setMbrSn(MBR);
        receipt.setOcrPrcsSttsCd(status.getCode());
        when(receiptMapper.selectOwned(3L, MBR)).thenReturn(receipt);
        when(receiptMapper.updateSaved(eq(3L), anyList(), eq(ReceiptStatus.SAVED.getCode()), anyString()))
                .thenReturn(1);
    }

    private List<Expense> inserted(int count) {
        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseMapper, times(count)).insert(captor.capture());
        return captor.getAllValues();
    }

    private void limit(Long limitAmt, long afterAmt) {
        when(expenseMapper.selectNotifiedLimitAmt(MBR)).thenReturn(limitAmt);
        when(expenseMapper.selectTotalAmt(eq(MBR), any(), any())).thenReturn(afterAmt);
    }

    private NotificationEvent publishedOnce() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void 마리별_2줄이면_같은_영수증에_연결된_2건을_만들고_영수증을_저장완료로_바꾼다() {
        receipt(ReceiptStatus.RECOGNIZED);
        when(expenseMapper.selectByIdmptDmndIds(MBR, List.of("a", "b")))
                .thenReturn(List.of(), List.of(summary(100L, 10L, 447_000), summary(101L, 11L, 15_000)));

        ExpenseService.Created created = service.create(MBR,
                request(HOSPITAL, OCR, 3L, today, line(10L, 447_000, "a"), line(11L, 15_000, "b")));

        assertThat(created.created()).isTrue();
        assertThat(created.responses()).extracting("expndSn").containsExactly(100L, 101L);
        List<Expense> expenses = inserted(2);
        assertThat(expenses).extracting(Expense::getPetSn).containsExactly(10L, 11L);
        assertThat(expenses).extracting(Expense::getRecptSn).containsOnly(3L);
        assertThat(expenses).extracting(Expense::getMbrSn).containsOnly(MBR);
        assertThat(expenses).extracting(Expense::getExpndPlcusNm).containsOnly("행복동물병원");
        assertThat(expenses).extracting(Expense::getDtlMttr).containsOnlyNulls();
        verify(receiptMapper).updateSaved(eq(3L), anyList(), eq(ReceiptStatus.SAVED.getCode()), eq("a"));
    }

    @Test
    void 인식하지_못한_영수증도_직접_입력으로_저장하면_연결하고_저장완료로_바꾼다() {
        receipt(ReceiptStatus.NOT_RECOGNIZED);

        service.create(MBR, request(FOOD, DIRECT, 3L, today, line(10L, 30_000, "a")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> codes = ArgumentCaptor.forClass(List.class);
        verify(receiptMapper).updateSaved(eq(3L), codes.capture(), eq(ReceiptStatus.SAVED.getCode()), eq("a"));
        assertThat(codes.getValue()).contains(ReceiptStatus.NOT_RECOGNIZED.getCode(),
                ReceiptStatus.TEMPORARY_ERROR.getCode());
        assertThat(inserted(1).get(0).getExpndInptMthCd()).isEqualTo(DIRECT);
    }

    @Test
    void 이미_저장한_영수증이면_지출을_만들지_않는다() {
        receipt(ReceiptStatus.SAVED);

        assertThatThrownBy(() -> service.create(MBR, request(FOOD, DIRECT, 3L, today, line(10L, 30_000, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.RECEIPT_ALREADY_SAVED);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    void 병원을_공용으로_등록하면_EXPENSE_PET_REQUIRED() {
        assertThatThrownBy(() -> service.create(MBR, request(HOSPITAL, DIRECT, null, today, line(null, 1, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.EXPENSE_PET_REQUIRED);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    void 같은_반려동물을_두_줄에_고르면_거절한다() {
        assertThatThrownBy(() -> service.create(MBR,
                request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"), line(10L, 2_000, "b"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    void 줄_수가_연동된_반려동물_수를_넘으면_거절한다() {
        linkedPets(1);

        assertThatThrownBy(() -> service.create(MBR,
                request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"), line(null, 2_000, "b"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 반려동물이_없어도_공용_1줄은_등록한다() {
        linkedPets(0);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(null, 2_000, "a")));

        assertThat(inserted(1).get(0).getPetSn()).isNull();
    }

    @Test
    void 연동되지_않은_반려동물은_거절한다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, 99L);

        assertThatThrownBy(() -> service.create(MBR, request(FOOD, DIRECT, null, today, line(99L, 1_000, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_ACCESS_DENIED);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    void 없는_카테고리나_입력방식은_거절한다() {
        assertThatThrownBy(() -> service.create(MBR, request("MYPC0099", DIRECT, null, today, line(10L, 1, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.create(MBR, request(FOOD, "MYPC0099", null, today, line(10L, 1, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 같은_멱등_요청을_다시_보내면_새로_만들지_않고_기존_지출을_돌려준다() {
        when(expenseMapper.selectByIdmptDmndIds(MBR, List.of("a", "b")))
                .thenReturn(List.of(summary(100L, 10L, 1_000), summary(101L, 11L, 2_000)));

        ExpenseService.Created created = service.create(MBR,
                request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"), line(11L, 2_000, "b")));

        assertThat(created.created()).isFalse();
        assertThat(created.responses()).extracting("expndSn").containsExactly(100L, 101L);
        verify(expenseMapper, never()).insert(any());
        verify(careMissionService, never()).reflect(any(), any(), any());
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 일부_줄만_이미_있으면_ALREADY_PROCESSED() {
        when(expenseMapper.selectByIdmptDmndIds(MBR, List.of("a", "b")))
                .thenReturn(List.of(summary(100L, 10L, 1_000)));

        assertThatThrownBy(() -> service.create(MBR,
                request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"), line(11L, 2_000, "b"))))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    void 동시에_같은_멱등_요청이_들어와_유일_키에_걸리면_ALREADY_PROCESSED() {
        doThrow(new DuplicateKeyException("UK_EXPND_1")).when(expenseMapper).insert(any());

        assertThatThrownBy(() -> service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"))))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 반려동물이_있는_줄만_가계부_케어미션에_반영한다() {
        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 1_000, "a"), line(11L, 2_000, "b"),
                line(null, 3_000, "c")));

        verify(careMissionService).reflect(10L, CareMission.EXPENSE_CREATE, MBR);
        verify(careMissionService).reflect(11L, CareMission.EXPENSE_CREATE, MBR);
        verify(careMissionService, times(2)).reflect(any(), eq(CareMission.EXPENSE_CREATE), eq(MBR));
    }

    @Test
    void 한도_90퍼센트에_처음_닿으면_사전_경고를_한_번_보낸다() {
        limit(100_000L, 90_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 10_000, "a")));

        NotificationEvent event = publishedOnce();
        assertThat(event.type()).isEqualTo(NotificationType.EXPENSE);
        assertThat(event.notiTtl()).contains("90%");
        assertThat(event.notiCn()).isEqualTo("90,000원 / 100,000원");
        assertThat(event.mvmnPath()).isEqualTo("/mypage/expenses");
    }

    @Test
    void 한도를_처음_넘으면_초과_알림을_한_번_보낸다() {
        limit(100_000L, 105_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 10_000, "a")));

        assertThat(publishedOnce().notiTtl()).isEqualTo("이번 달 지출이 한도를 넘었어요");
    }

    @Test
    void 한_번에_90퍼센트와_한도를_모두_넘으면_초과_알림만_보낸다() {
        limit(100_000L, 110_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 30_000, "a")));

        assertThat(publishedOnce().notiTtl()).isEqualTo("이번 달 지출이 한도를 넘었어요");
    }

    @Test
    void 이미_한도를_넘은_뒤에_더_저장하면_알림을_보내지_않는다() {
        limit(100_000L, 120_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 10_000, "a")));

        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 이미_90퍼센트를_넘었고_한도_안이면_다시_경고하지_않는다() {
        limit(100_000L, 98_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 3_000, "a")));

        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 한도가_없거나_알림을_끄면_보내지_않는다() {
        limit(null, 200_000);

        service.create(MBR, request(FOOD, DIRECT, null, today, line(10L, 200_000, "a")));

        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 지난_달_지출은_한도_알림을_보내지_않는다() {
        limit(100_000L, 200_000);

        service.create(MBR, request(FOOD, DIRECT, null, today.minusMonths(1), line(10L, 200_000, "a")));

        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
        verify(expenseMapper, never()).selectTotalAmt(any(), any(), any());
    }

    @Test
    void 영수증_저장의_병원_지출은_OCR로_기록하고_케어미션과_한도도_처리한다() {
        limit(100_000L, 101_000);

        service.create(new ExpenseCreateCommand(MBR, 10L, 3L, today, 50_000L, "행복동물병원", null));

        Expense expense = inserted(1).get(0);
        assertThat(expense.getExpndClsfCd()).isEqualTo(HOSPITAL);
        assertThat(expense.getExpndInptMthCd()).isEqualTo(OCR);
        assertThat(expense.getRecptSn()).isEqualTo(3L);
        assertThat(expense.getIdmptDmndId()).isNull();
        verify(petAuthorizer).checkCaregiver(MBR, 10L);
        verify(careMissionService).reflect(10L, CareMission.EXPENSE_CREATE, MBR);
        assertThat(publishedOnce().notiTtl()).isEqualTo("이번 달 지출이 한도를 넘었어요");
    }

    @Test
    void 중복_확인은_겹치는_기존_지출을_줄_순서대로_돌려준다() {
        ExpenseCreateRequest req = request(HOSPITAL, OCR, null, today, line(10L, 447_000, "a"),
                line(11L, 15_000, "b"));
        when(expenseMapper.selectDuplicates(MBR, today, HOSPITAL, req.lines()))
                .thenReturn(List.of(summary(5L, 11L, 15_000), summary(7L, 10L, 447_000)));

        assertThat(service.searchDuplicates(MBR, req)).extracting(ExpenseSummary::getExpndSn)
                .containsExactly(7L, 5L);
    }

    @Test
    void 영수증_저장의_중복_확인은_병원_카테고리_한_줄로_찾는다() {
        when(expenseMapper.selectDuplicates(eq(MBR), eq(today), eq(HOSPITAL), anyList()))
                .thenReturn(List.of(summary(5L, 10L, 15_000)));

        assertThat(service.searchDuplicates(MBR, 10L, today, 15_000L)).singleElement()
                .satisfies(duplicate -> {
                    assertThat(duplicate.expndSn()).isEqualTo(5L);
                    assertThat(duplicate.petSn()).isEqualTo(10L);
                });
    }

    @Test
    void 중복_조회_SQL은_내_지출만_삭제_제외_공용끼리_사용처는_보지_않는다() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/expense/ExpenseMapper.xml"));
        Matcher select = Pattern.compile("<select id=\"selectDuplicates\".*?</select>", Pattern.DOTALL)
                .matcher(xml);
        assertThat(select.find()).isTrue();
        String where = select.group().substring(select.group().indexOf("WHERE"));

        assertThat(where).contains("E.MBR_SN = #{mbrSn}", "E.DEL_YN = 'N'", "E.EXPND_YMD = #{expndYmd}",
                "E.EXPND_CLSF_CD = #{expndClsfCd}", "E.PET_SN &lt;=&gt; #{line.petSn}",
                "E.EXPND_AMT = #{line.expndAmt}");
        assertThat(where).doesNotContain("EXPND_PLCUS_NM", "DTL_MTTR");
    }
}
