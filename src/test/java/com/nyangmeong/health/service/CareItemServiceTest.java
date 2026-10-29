package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.CareCompletion;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.CareItem;
import com.nyangmeong.health.domain.CareZone;
import com.nyangmeong.health.dto.CareCompletionCreateRequest;
import com.nyangmeong.health.dto.CareItemCreateRequest;
import com.nyangmeong.health.dto.CareItemExposureSaveRequest;
import com.nyangmeong.health.dto.CareItemSearchCond;
import com.nyangmeong.health.dto.CareItemSummary;
import com.nyangmeong.health.dto.CareItemUpdateRequest;
import com.nyangmeong.health.mapper.CareItemMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 토탈 케어리스트]
 */
class CareItemServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long OTHER_PET = 20L;
    private static final Long ITEM = 100L;
    private static final Long PRSC = 500L;
    private static final Long CMPTN = 900L;

    private final CareItemMapper mapper = mock(CareItemMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final CareItemService service = new CareItemService(mapper, petAuthorizer, careMissionService);

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        when(mapper.selectSummary(any(), any())).thenReturn(new CareItemSummary());
    }

    private CareItem item(String careArtclNm, CareZone zone, CareCycle cycle, boolean isBasic) {
        CareItem item = new CareItem();
        item.setCareArtclSn(ITEM);
        item.setPetSn(PET);
        item.setCareZoneCd(zone.getCode());
        item.setCareCycleCd(cycle == null ? null : cycle.getCode());
        item.setCareArtclNm(careArtclNm);
        item.setRprsIconId("task_alt");
        item.setBscArtclYn(isBasic ? "Y" : "N");
        item.setRegDt(LocalDateTime.of(2026, 10, 1, 9, 0));
        item.setDelYn("N");
        return item;
    }

    private CareCompletion completion(int cmptnCycl, String delYn) {
        CareCompletion completion = new CareCompletion();
        completion.setCareCmptnHstrySn(CMPTN);
        completion.setCareArtclSn(ITEM);
        completion.setCmptnCycl(cmptnCycl);
        completion.setCmptnYmd(today);
        completion.setDelYn(delYn);
        return completion;
    }

    private CareItemCreateRequest createRequest(String zoneCd, String cycleCd, Integer dayCnt, LocalDate lastYmd) {
        return new CareItemCreateRequest("목욕 가기", null, zoneCd, cycleCd, dayCnt, lastYmd);
    }

    private CustomException thrown(Runnable call) {
        return assertThrows(CustomException.class, call::run);
    }

    @Test
    void 기본_항목이_없으면_11개를_만들고_양치_빗질_발톱_관리를_메인에_올린다() {
        when(mapper.countBasicItems(PET)).thenReturn(0L);
        when(mapper.selectPetForUpdate(PET)).thenReturn(PET);
        when(mapper.countBasicItemsForUpdate(PET)).thenReturn(0L);

        service.search(MBR, PET, new CareItemSearchCond());

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper, times(11)).insertItem(captor.capture());
        List<CareItem> items = captor.getAllValues();
        assertThat(items).allMatch(i -> "Y".equals(i.getBscArtclYn()));
        assertThat(items).filteredOn(i -> "Y".equals(i.getMainExpsrYn()))
                .extracting(CareItem::getCareArtclNm, CareItem::getMainSortSeq)
                .containsExactlyInAnyOrder(tuple("양치", 1), tuple("빗질", 2), tuple("발톱 관리", 3));
        assertThat(items).filteredOn(i -> CareZone.REGULAR.is(i.getCareZoneCd())).hasSize(6)
                .allMatch(i -> i.getNxtmPrnmntYmd() != null);
        assertThat(items).filteredOn(i -> CareZone.DAILY.is(i.getCareZoneCd())).hasSize(5)
                .allMatch(i -> i.getNxtmPrnmntYmd() == null);
    }

    @Test
    void 기본_정기_항목의_주기는_화면설계서_값이다() {
        when(mapper.countBasicItems(PET)).thenReturn(0L);
        when(mapper.selectPetForUpdate(PET)).thenReturn(PET);

        service.search(MBR, PET, new CareItemSearchCond());

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper, times(11)).insertItem(captor.capture());
        assertThat(captor.getAllValues()).filteredOn(i -> CareZone.REGULAR.is(i.getCareZoneCd()))
                .extracting(CareItem::getCareArtclNm, CareItem::getCareCycleCd)
                .containsExactly(
                        tuple("귀 청소", CareCycle.WEEK_1.getCode()),
                        tuple("발톱 관리", CareCycle.WEEK_2.getCode()),
                        tuple("목욕", CareCycle.MONTH_1.getCode()),
                        tuple("눈 주변 관리", CareCycle.WEEK_2.getCode()),
                        tuple("발바닥 털 관리", CareCycle.WEEK_2.getCode()),
                        tuple("구강 상태 확인", CareCycle.WEEK_2.getCode()));
    }

    @Test
    void 기본_항목이_이미_있으면_만들지_않는다() {
        when(mapper.countBasicItems(PET)).thenReturn(3L);

        service.search(MBR, PET, new CareItemSearchCond());

        verify(mapper, never()).insertItem(any());
        verify(mapper, never()).selectPetForUpdate(anyLong());
    }

    @Test
    void 잠근_뒤_다른_요청이_이미_만들었으면_만들지_않는다() {
        when(mapper.countBasicItems(PET)).thenReturn(0L);
        when(mapper.selectPetForUpdate(PET)).thenReturn(PET);
        when(mapper.countBasicItemsForUpdate(PET)).thenReturn(11L);

        service.search(MBR, PET, new CareItemSearchCond());

        verify(mapper, never()).insertItem(any());
    }

    @Test
    void 항목_추가는_마지막으로_한_날_더하기_주기로_예정일을_정하고_완료_기록을_함께_저장한다() {
        LocalDate lastYmd = today.minusDays(14);

        service.create(MBR, PET, createRequest(CareZone.REGULAR.getCode(), CareCycle.WEEK_2.getCode(), null, lastYmd));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).insertItem(captor.capture());
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(lastYmd.plusWeeks(2));
        assertThat(captor.getValue().getLastCmptnYmd()).isEqualTo(lastYmd);
        assertThat(captor.getValue().getBscArtclYn()).isEqualTo("N");
        assertThat(captor.getValue().getRprsIconId()).isEqualTo("task_alt");
        verify(mapper).upsertCompletion(captor.getValue().getCareArtclSn(), MBR, lastYmd, 1);
    }

    @Test
    void 마지막으로_한_날이_없으면_오늘_기준으로_예정일을_정하고_완료_기록은_없다() {
        service.create(MBR, PET, createRequest(CareZone.REGULAR.getCode(), CareCycle.MONTH_1.getCode(), null, null));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).insertItem(captor.capture());
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(today.plusMonths(1));
        verify(mapper, never()).upsertCompletion(any(), anyLong(), any(), anyInt());
    }

    @Test
    void 직접입력_주기는_일수만큼_더한다() {
        service.create(MBR, PET, createRequest(CareZone.REGULAR.getCode(), CareCycle.CUSTOM.getCode(), 10, null));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).insertItem(captor.capture());
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(today.plusDays(10));
        assertThat(captor.getValue().getCycleDrctInptDayCnt()).isEqualTo(10);
    }

    @Test
    void 일상관리_항목은_주기와_마지막으로_한_날을_버린다() {
        service.create(MBR, PET,
                createRequest(CareZone.DAILY.getCode(), CareCycle.WEEK_1.getCode(), 3, today.minusDays(1)));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).insertItem(captor.capture());
        assertThat(captor.getValue().getCareCycleCd()).isNull();
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isNull();
        verify(mapper, never()).upsertCompletion(any(), anyLong(), any(), anyInt());
    }

    @Test
    void 정기관리인데_주기가_없으면_400() {
        assertThat(thrown(() -> service.create(MBR, PET, createRequest(CareZone.REGULAR.getCode(), null, null, null)))
                .getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertItem(any());
    }

    @Test
    void 직접입력인데_일수가_없으면_400() {
        assertThat(thrown(() -> service.create(MBR, PET,
                createRequest(CareZone.REGULAR.getCode(), CareCycle.CUSTOM.getCode(), null, null))).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 구역_코드가_목록_밖이면_400() {
        assertThat(thrown(() -> service.create(MBR, PET, createRequest("HLTC9999", null, null, null))).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 마지막으로_한_날이_내일이면_400() {
        assertThat(thrown(() -> service.create(MBR, PET,
                createRequest(CareZone.REGULAR.getCode(), CareCycle.WEEK_1.getCode(), null, today.plusDays(1))))
                .getErrorCode()).isEqualTo(ErrorCode.CARE_COMPLETION_FUTURE_DATE);
        verify(mapper, never()).insertItem(any());
    }

    @Test
    void 기본_항목_삭제는_400() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("양치", CareZone.DAILY, null, true));

        assertThat(thrown(() -> service.delete(MBR, PET, ITEM)).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_DEFAULT_NOT_DELETABLE);
        verify(mapper, never()).deleteItem(anyLong(), anyString());
    }

    @Test
    void 사용자_항목은_삭제한다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("산책 준비", CareZone.DAILY, null, false));

        service.delete(MBR, PET, ITEM);

        verify(mapper).deleteItem(ITEM, "1");
    }

    @Test
    void 처방약_연동_항목은_수정도_삭제도_막는다() {
        CareItem linked = item("약", CareZone.DAILY, null, false);
        linked.setPrscSn(PRSC);
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(linked);

        assertThat(thrown(() -> service.delete(MBR, PET, ITEM)).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_LINKED_NOT_EDITABLE);
        assertThat(thrown(() -> service.update(MBR, PET, ITEM,
                new CareItemUpdateRequest("새 이름", null, null, null, null))).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_LINKED_NOT_EDITABLE);
    }

    @Test
    void 다른_반려동물의_항목은_찾을_수_없다() {
        CareItem other = item("양치", CareZone.DAILY, null, false);
        other.setPetSn(OTHER_PET);
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(other);

        assertThat(thrown(() -> service.delete(MBR, PET, ITEM)).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_NOT_FOUND);
    }

    @Test
    void 기본_정기_항목은_이름을_바꿀_수_없다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("귀 청소", CareZone.REGULAR, CareCycle.WEEK_1, true));

        assertThat(thrown(() -> service.update(MBR, PET, ITEM,
                new CareItemUpdateRequest("새 이름", null, null, null, null))).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_DEFAULT_LIMITED);
        verify(mapper, never()).updateItem(any(), anyString());
    }

    @Test
    void 기본_정기_항목은_주기를_바꿀_수_있고_예정일을_다시_계산한다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("귀 청소", CareZone.REGULAR, CareCycle.WEEK_1, true));
        when(mapper.selectLatestCompletionYmd(ITEM)).thenReturn(LocalDate.of(2026, 10, 20));

        service.update(MBR, PET, ITEM, new CareItemUpdateRequest(null, null, null, CareCycle.MONTH_1.getCode(), null));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).updateItem(captor.capture(), eq("1"));
        assertThat(captor.getValue().getCareCycleCd()).isEqualTo(CareCycle.MONTH_1.getCode());
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void 기본_일상_항목은_주기를_보내도_막는다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("양치", CareZone.DAILY, null, true));

        assertThat(thrown(() -> service.update(MBR, PET, ITEM,
                new CareItemUpdateRequest(null, null, null, CareCycle.WEEK_1.getCode(), null))).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_DEFAULT_LIMITED);
    }

    @Test
    void 사용자_항목은_이름과_구역을_바꾸고_일상으로_옮기면_예정일이_사라진다() {
        CareItem user = item("산책 준비", CareZone.REGULAR, CareCycle.WEEK_1, false);
        user.setNxtmPrnmntYmd(LocalDate.of(2026, 11, 1));
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(user);

        service.update(MBR, PET, ITEM, new CareItemUpdateRequest(" 새 이름 ", null, CareZone.DAILY.getCode(), null, null));

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).updateItem(captor.capture(), eq("1"));
        assertThat(captor.getValue().getCareArtclNm()).isEqualTo("새 이름");
        assertThat(captor.getValue().getCareCycleCd()).isNull();
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isNull();
    }

    @Test
    void 완료_체크는_오늘_기록을_만들고_예정일을_완료일_더하기_주기로_바꾼다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("목욕", CareZone.REGULAR, CareCycle.MONTH_1, false));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(null, completion(1, "N"));
        when(mapper.selectLatestCompletionYmd(ITEM)).thenReturn(today);

        CareItemService.Completed completed = service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        assertThat(completed.created()).isTrue();
        assertThat(completed.response().nxtmPrnmntYmd()).isEqualTo(today.plusMonths(1));
        assertThat(completed.response().lastCmptnYmd()).isEqualTo(today);
        verify(mapper).upsertCompletion(ITEM, MBR, today, 1);
        verify(careMissionService, never()).reflect(anyLong(), any(), anyLong());
    }

    @Test
    void 이미_체크한_항목을_다시_체크하면_기존_기록을_돌려주고_새로_만들지_않는다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("목욕", CareZone.REGULAR, CareCycle.MONTH_1, false));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(completion(1, "N"));

        CareItemService.Completed completed = service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        assertThat(completed.created()).isFalse();
        assertThat(completed.response().careCmptnHstrySn()).isEqualTo(CMPTN);
        verify(mapper, never()).upsertCompletion(any(), anyLong(), any(), anyInt());
        verify(mapper, never()).updateItem(any(), anyString());
    }

    @Test
    void 취소한_날_다시_체크하면_삭제된_기록을_되살린다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("목욕", CareZone.REGULAR, CareCycle.MONTH_1, false));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(completion(1, "Y"), completion(1, "N"));

        CareItemService.Completed completed = service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        assertThat(completed.created()).isTrue();
        verify(mapper).upsertCompletion(ITEM, MBR, today, 1);
    }

    @Test
    void 기본_항목을_그날_처음_완료하면_케어미션에_반영한다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("발톱 관리", CareZone.REGULAR, CareCycle.WEEK_2, true));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(null, completion(1, "N"));

        service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        verify(careMissionService).reflect(PET, CareMission.NAIL_CARE, MBR);
    }

    @Test
    void 취소했다가_같은_날_다시_완료해도_미션에_다시_반영하지_않는다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("양치", CareZone.DAILY, null, true));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(completion(1, "Y"), completion(1, "N"));

        service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        verify(careMissionService, never()).reflect(anyLong(), any(), anyLong());
    }

    @Test
    void 사용자_항목은_완료해도_미션에_반영하지_않는다() {
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("산책 준비", CareZone.DAILY, null, false));
        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(null, completion(1, "N"));

        service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));

        verify(careMissionService, never()).reflect(anyLong(), any(), anyLong());
    }

    @Test
    void 투약_항목은_누를_때마다_회차를_올리고_N회에서_멈춘다() {
        CareItem medication = item("약", CareZone.DAILY, null, false);
        medication.setPrscSn(PRSC);
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(medication);
        when(mapper.selectDayMdcnNmtm(PRSC)).thenReturn(2);

        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(null, completion(1, "N"));
        assertThat(service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM)).created()).isTrue();
        verify(mapper).upsertCompletion(ITEM, MBR, today, 1);

        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(completion(1, "N"), completion(2, "N"));
        CareItemService.Completed second = service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));
        assertThat(second.created()).isFalse();
        assertThat(second.response().cmptnCycl()).isEqualTo(2);
        verify(mapper).updateCompletionCycle(CMPTN, 2, "1");

        when(mapper.selectCompletionByDateForUpdate(ITEM, today)).thenReturn(completion(2, "N"), completion(2, "N"));
        CareItemService.Completed third = service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM));
        assertThat(third.created()).isFalse();
        assertThat(third.response().cmptnCycl()).isEqualTo(2);
        verify(mapper, times(1)).updateCompletionCycle(anyLong(), anyInt(), anyString());
    }

    @Test
    void 다른_반려동물의_항목은_완료할_수_없다() {
        CareItem other = item("양치", CareZone.DAILY, null, true);
        other.setPetSn(OTHER_PET);
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(other);

        assertThat(thrown(() -> service.complete(MBR, PET, new CareCompletionCreateRequest(ITEM))).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_NOT_FOUND);
    }

    @Test
    void 완료_취소는_남은_완료_기록_중_가장_최근_날짜_더하기_주기로_예정일을_다시_계산한다() {
        when(mapper.selectCompletion(CMPTN)).thenReturn(completion(1, "N"));
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("목욕", CareZone.REGULAR, CareCycle.MONTH_1, true));
        when(mapper.selectCompletionForUpdate(CMPTN)).thenReturn(completion(1, "N"));
        when(mapper.selectLatestCompletionYmd(ITEM)).thenReturn(LocalDate.of(2026, 10, 31));

        service.cancel(MBR, PET, CMPTN);

        verify(mapper).deleteCompletion(CMPTN, "1");
        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).updateItem(captor.capture(), eq("1"));
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(captor.getValue().getLastCmptnYmd()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void 남은_완료_기록이_없으면_등록일_더하기_주기로_예정일을_정한다() {
        when(mapper.selectCompletion(CMPTN)).thenReturn(completion(1, "N"));
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(item("목욕", CareZone.REGULAR, CareCycle.WEEK_2, false));
        when(mapper.selectCompletionForUpdate(CMPTN)).thenReturn(completion(1, "N"));
        when(mapper.selectLatestCompletionYmd(ITEM)).thenReturn(null);

        service.cancel(MBR, PET, CMPTN);

        ArgumentCaptor<CareItem> captor = ArgumentCaptor.forClass(CareItem.class);
        verify(mapper).updateItem(captor.capture(), eq("1"));
        assertThat(captor.getValue().getNxtmPrnmntYmd()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(captor.getValue().getLastCmptnYmd()).isNull();
    }

    @Test
    void 투약_항목_취소는_마지막_1회만_줄인다() {
        CareItem medication = item("약", CareZone.DAILY, null, false);
        medication.setPrscSn(PRSC);
        when(mapper.selectCompletion(CMPTN)).thenReturn(completion(2, "N"));
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(medication);
        when(mapper.selectCompletionForUpdate(CMPTN)).thenReturn(completion(2, "N"));

        service.cancel(MBR, PET, CMPTN);

        verify(mapper).updateCompletionCycle(CMPTN, 1, "1");
        verify(mapper, never()).deleteCompletion(anyLong(), anyString());
    }

    @Test
    void 없거나_다른_반려동물의_완료_기록은_취소할_수_없다() {
        when(mapper.selectCompletion(CMPTN)).thenReturn(null);
        assertThat(thrown(() -> service.cancel(MBR, PET, CMPTN)).getErrorCode())
                .isEqualTo(ErrorCode.CARE_COMPLETION_NOT_FOUND);

        CareItem other = item("양치", CareZone.DAILY, null, true);
        other.setPetSn(OTHER_PET);
        when(mapper.selectCompletion(CMPTN)).thenReturn(completion(1, "N"));
        when(mapper.selectItemForUpdate(ITEM)).thenReturn(other);
        assertThat(thrown(() -> service.cancel(MBR, PET, CMPTN)).getErrorCode())
                .isEqualTo(ErrorCode.CARE_COMPLETION_NOT_FOUND);
        verify(mapper, never()).deleteCompletion(anyLong(), anyString());
    }

    @Test
    void 메인_표시는_7개부터_400() {
        List<Long> seven = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);

        assertThat(thrown(() -> service.save(MBR, PET, new CareItemExposureSaveRequest(seven))).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ITEM_MAIN_LIMIT_EXCEEDED);
        verify(mapper, never()).updateMainExposureClear(anyLong(), anyString());
    }

    @Test
    void 메인_표시는_목록_순서대로_정렬_순서를_저장한다() {
        List<Long> sns = List.of(7L, 3L, 5L);
        when(mapper.countBasicItems(PET)).thenReturn(11L);
        when(mapper.countItemsIn(PET, sns)).thenReturn(3L);

        service.save(MBR, PET, new CareItemExposureSaveRequest(sns));

        verify(mapper).updateMainExposureClear(PET, "1");
        verify(mapper).updateMainExposure(7L, 1, "1");
        verify(mapper).updateMainExposure(3L, 2, "1");
        verify(mapper).updateMainExposure(5L, 3, "1");
        ArgumentCaptor<CareItemSearchCond> captor = ArgumentCaptor.forClass(CareItemSearchCond.class);
        verify(mapper).selectList(eq(PET), captor.capture(), any(), eq(CareZone.REGULAR.getCode()));
        assertThat(captor.getValue().getMainExpsrYn()).isTrue();
    }

    @Test
    void 메인_표시_빈_목록은_전부_해제한다() {
        when(mapper.countBasicItems(PET)).thenReturn(11L);

        service.save(MBR, PET, new CareItemExposureSaveRequest(List.of()));

        verify(mapper).updateMainExposureClear(PET, "1");
        verify(mapper, never()).updateMainExposure(anyLong(), anyInt(), anyString());
    }

    @Test
    void 중복이거나_다른_반려동물_항목이_섞인_메인_표시는_400() {
        when(mapper.countBasicItems(PET)).thenReturn(11L);
        List<Long> mixed = List.of(1L, 2L);
        when(mapper.countItemsIn(PET, mixed)).thenReturn(1L);

        assertThat(thrown(() -> service.save(MBR, PET, new CareItemExposureSaveRequest(List.of(1L, 1L))))
                .getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(thrown(() -> service.save(MBR, PET, new CareItemExposureSaveRequest(mixed))).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updateMainExposureClear(anyLong(), anyString());
    }

    private CareItem named(Long petSn, String careArtclNm) {
        CareItem item = new CareItem();
        item.setPetSn(petSn);
        item.setCareArtclNm(careArtclNm);
        return item;
    }

    @Test
    void 반려동물별_항목명은_요청한_반려동물마다_키를_두고_등록순으로_묶는다() {
        when(mapper.selectNamesByPets(List.of(PET, OTHER_PET)))
                .thenReturn(List.of(named(PET, "양치"), named(PET, "산책 준비")));

        Map<Long, List<String>> names = service.searchNamesByPets(List.of(PET, OTHER_PET));

        assertThat(names).containsOnlyKeys(PET, OTHER_PET);
        assertThat(names.get(PET)).containsExactly("양치", "산책 준비");
        assertThat(names.get(OTHER_PET)).isEmpty();
    }

    @Test
    void 반려동물_목록이_비면_DB를_부르지_않는다() {
        assertThat(service.searchNamesByPets(List.of())).isEmpty();
        assertThat(service.searchNamesByPets(null)).isEmpty();
        verify(mapper, never()).selectNamesByPets(any());
    }
}
