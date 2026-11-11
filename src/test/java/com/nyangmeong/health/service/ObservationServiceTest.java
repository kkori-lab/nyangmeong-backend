package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.health.domain.ObservationCycle;
import com.nyangmeong.health.domain.ObservationItem;
import com.nyangmeong.health.dto.ObservationCycleCreateRequest;
import com.nyangmeong.health.dto.ObservationCycleSearchCond;
import com.nyangmeong.health.dto.ObservationCycleSummary;
import com.nyangmeong.health.dto.ObservationCycleUpdateRequest;
import com.nyangmeong.health.dto.ObservationItemCreateRequest;
import com.nyangmeong.health.dto.ObservationItemResponse;
import com.nyangmeong.health.dto.ObservationItemSearchCond;
import com.nyangmeong.health.dto.ObservationItemSummary;
import com.nyangmeong.health.mapper.ObservationMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

/**
 * [검사 - 관찰 항목·회차]
 * - 목록 증감·정렬, 등록 검증, 종료, 종료 항목 회차 추가 불가, 마지막 회차 삭제 (FN-HLT-002-15~25·32)
 */
class ObservationServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long ITEM = 500L;
    private static final Long CYCLE = 900L;
    private static final String ONGOING = "HLTC0055";
    private static final String ENDED = "HLTC0056";
    private static final LocalDateTime PAST = LocalDateTime.of(2026, 10, 1, 9, 0);

    private final ObservationMapper mapper = mock(ObservationMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final CareMissionService careMissionService = mock(CareMissionService.class);
    private final ObservationService service = new ObservationService(mapper, petAuthorizer, fileStore,
            careMissionService);

    private final MultipartFile photo = new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1 });

    @BeforeEach
    void setUp() {
        when(mapper.existsChildCode("HLTG16", "HLTC0043")).thenReturn(true);
        when(mapper.existsChildCode("HLTG17", "HLTC0054")).thenReturn(true);
        when(fileStore.store(any(MultipartFile.class), anyString())).thenReturn(77L);
        when(mapper.insertItem(any())).thenAnswer(call -> {
            ObservationItem item = call.getArgument(0);
            item.setObsrvArtclSn(ITEM);
            return 1;
        });
        when(mapper.insertCycle(any())).thenAnswer(call -> {
            ObservationCycle cycle = call.getArgument(0);
            cycle.setObsrvCyclSn(CYCLE);
            return 1;
        });
        ObservationCycleSummary summary = cycleSummary(CYCLE, ITEM, 1, "3.0");
        when(mapper.selectCycles(any())).thenReturn(List.of(summary));
    }

    private static ObservationItem item(String status) {
        ObservationItem item = new ObservationItem();
        item.setObsrvArtclSn(ITEM);
        item.setPetSn(PET);
        item.setObsrvSttsCd(status);
        return item;
    }

    private static ObservationCycleSummary cycleSummary(Long sn, Long itemSn, int no, String size) {
        ObservationCycleSummary cycle = new ObservationCycleSummary();
        cycle.setObsrvCyclSn(sn);
        cycle.setObsrvArtclSn(itemSn);
        cycle.setCyclNo(no);
        cycle.setObsrvDt(PAST.plusDays(no));
        cycle.setObsrvSz(size == null ? null : new BigDecimal(size));
        return cycle;
    }

    private static ObservationItemSummary itemSummary(Long sn, String status) {
        ObservationItemSummary summary = new ObservationItemSummary();
        summary.setObsrvArtclSn(sn);
        summary.setObsrvSttsCd(status);
        return summary;
    }

    private ObservationItemCreateRequest itemRequest(String regn, LocalDateTime at, BigDecimal size) {
        return new ObservationItemCreateRequest("피부 점", regn, null, photo, at, size, null);
    }

    @Test
    void 항목_목록은_마지막_회차_증감을_크기_있는_이전_회차와_비교하고_크기_없는_회차는_건너뛴다() {
        when(mapper.selectItems(any())).thenReturn(new ArrayList<>(List.of(itemSummary(ITEM, ONGOING))));
        when(mapper.selectPetCycles(PET)).thenReturn(List.of(cycleSummary(1L, ITEM, 1, "3.0"),
                cycleSummary(2L, ITEM, 2, null), cycleSummary(3L, ITEM, 3, "3.4")));

        ObservationItemSummary result = service.searchItems(MBR, PET, new ObservationItemSearchCond()).get(0);

        assertThat(result.getCyclCnt()).isEqualTo(3);
        assertThat(result.getLastSzDiff()).isEqualByComparingTo("0.4");
        assertThat(result.getFirstCycle().getCyclNo()).isEqualTo(1);
        assertThat(result.getLastCycle().getCyclNo()).isEqualTo(3);
    }

    @Test
    void 마지막_회차에_크기가_없거나_이전_크기가_없으면_증감은_null이다() {
        when(mapper.selectItems(any())).thenReturn(new ArrayList<>(List.of(itemSummary(ITEM, ONGOING),
                itemSummary(501L, ONGOING))));
        when(mapper.selectPetCycles(PET)).thenReturn(List.of(cycleSummary(1L, ITEM, 1, "3.0"),
                cycleSummary(2L, ITEM, 2, null), cycleSummary(3L, 501L, 1, "2.0")));

        List<ObservationItemSummary> result = service.searchItems(MBR, PET, new ObservationItemSearchCond());

        assertThat(result).extracting(ObservationItemSummary::getLastSzDiff).containsOnlyNulls();
    }

    @Test
    void 항목_목록은_관찰중이_먼저_같은_상태_안에서는_최근_관찰일_순이다() {
        when(mapper.selectItems(any())).thenReturn(new ArrayList<>(List.of(itemSummary(1L, ENDED),
                itemSummary(2L, ONGOING), itemSummary(3L, ONGOING))));
        ObservationCycleSummary older = cycleSummary(11L, 2L, 1, null);
        ObservationCycleSummary newer = cycleSummary(12L, 3L, 5, null);
        ObservationCycleSummary ended = cycleSummary(13L, 1L, 9, null);
        when(mapper.selectPetCycles(PET)).thenReturn(List.of(ended, older, newer));

        List<ObservationItemSummary> result = service.searchItems(MBR, PET, new ObservationItemSearchCond());

        assertThat(result).extracting(ObservationItemSummary::getObsrvArtclSn).containsExactly(3L, 2L, 1L);
    }

    @Test
    void 항목_목록_상태_조건이_공통코드에_없으면_400이다() {
        ObservationItemSearchCond cond = new ObservationItemSearchCond();
        cond.setObsrvSttsCd("HLTC9999");

        assertThatThrownBy(() -> service.searchItems(MBR, PET, cond))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 항목_등록은_항목_이력_1회차를_저장하고_케어미션에_반영한다() {
        ObservationItemResponse response = service.createItem(MBR, PET,
                itemRequest("HLTC0043", PAST, new BigDecimal("3.0")));

        assertThat(response.obsrvArtclSn()).isEqualTo(ITEM);
        assertThat(response.obsrvCyclSn()).isEqualTo(CYCLE);
        assertThat(response.obsrvSttsCd()).isEqualTo(ONGOING);
        verify(mapper).insertItemStatusHistory(ITEM, MBR, null, ONGOING);
        verify(careMissionService).reflect(PET, CareMission.OBSERVATION_RECORD, MBR);
    }

    @Test
    void 항목_등록은_미래_관찰일_없는_부위_파일_없음을_400으로_막는다() {
        LocalDateTime future = LocalDateTime.now().plusDays(2);

        assertThatThrownBy(() -> service.createItem(MBR, PET, itemRequest("HLTC0043", future, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        assertThatThrownBy(() -> service.createItem(MBR, PET, itemRequest("HLTC9999", PAST, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        MultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.createItem(MBR, PET,
                new ObservationItemCreateRequest("피부 점", "HLTC0043", null, empty, PAST, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(mapper, never()).insertCycle(any());
        verify(careMissionService, never()).reflect(anyLong(), any(), anyLong());
    }

    @Test
    void 종료된_항목에는_회차를_추가할_수_없다() {
        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ENDED));

        assertThatThrownBy(() -> service.createCycle(MBR, PET,
                new ObservationCycleCreateRequest(ITEM, photo, PAST, null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OBSERVATION_ITEM_ENDED));
        verify(mapper, never()).insertCycle(any());
    }

    @Test
    void 다른_반려동물의_항목에는_회차를_추가할_수_없다() {
        when(mapper.selectItem(PET, ITEM)).thenReturn(null);

        assertThatThrownBy(() -> service.createCycle(MBR, PET,
                new ObservationCycleCreateRequest(ITEM, photo, PAST, null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OBSERVATION_ITEM_NOT_FOUND));
    }

    @Test
    void 회차_추가는_변화_코드를_검사하고_케어미션에_반영한다() {
        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ONGOING));

        var response = service.createCycle(MBR, PET,
                new ObservationCycleCreateRequest(ITEM, photo, PAST, new BigDecimal("3.2"), "HLTC0054", "메모"));

        assertThat(response.obsrvCyclSn()).isEqualTo(CYCLE);
        verify(careMissionService).reflect(PET, CareMission.OBSERVATION_RECORD, MBR);
        assertThatThrownBy(() -> service.createCycle(MBR, PET,
                new ObservationCycleCreateRequest(ITEM, photo, PAST, null, "HLTC9999", null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 관찰_종료는_상태를_바꾸고_이력을_남기며_이미_종료면_아무것도_바꾸지_않는다() {
        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ONGOING));

        service.endItem(MBR, PET, ITEM);

        verify(mapper).updateItemEnd(ITEM, ENDED, String.valueOf(MBR));
        verify(mapper).insertItemStatusHistory(ITEM, MBR, ONGOING, ENDED);

        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ENDED));
        service.endItem(MBR, PET, ITEM);

        verify(mapper).updateItemEnd(eq(ITEM), eq(ENDED), anyString());
    }

    @Test
    void 회차_수정은_보낸_값만_바꾸고_사진을_보내면_교체한다() {
        ObservationCycle cycle = new ObservationCycle();
        cycle.setObsrvCyclSn(CYCLE);
        cycle.setObsrvArtclSn(ITEM);
        cycle.setAtchFileSn(5L);
        cycle.setObsrvSz(new BigDecimal("3.0"));
        cycle.setMemoCn("기존");
        when(mapper.selectCycle(PET, CYCLE)).thenReturn(cycle);

        service.updateCycle(MBR, PET, CYCLE,
                new ObservationCycleUpdateRequest(photo, null, new BigDecimal("3.5"), null, null, null));

        assertThat(cycle.getAtchFileSn()).isEqualTo(77L);
        assertThat(cycle.getObsrvSz()).isEqualByComparingTo("3.5");
        assertThat(cycle.getMemoCn()).isEqualTo("기존");
        verify(mapper).updateCycle(cycle, String.valueOf(MBR));
    }

    @Test
    void 회차_수정에서_clearFields의_크기_변화_메모는_비우고_나머지는_그대로다() {
        LocalDateTime obsrvDt = LocalDateTime.of(2026, 10, 1, 0, 0);
        ObservationCycle cycle = new ObservationCycle();
        cycle.setObsrvCyclSn(CYCLE);
        cycle.setObsrvArtclSn(ITEM);
        cycle.setAtchFileSn(5L);
        cycle.setObsrvDt(obsrvDt);
        cycle.setObsrvSz(new BigDecimal("3.0"));
        cycle.setObsrvChgCd("HLTG1702");
        cycle.setMemoCn("기존");
        when(mapper.selectCycle(PET, CYCLE)).thenReturn(cycle);

        service.updateCycle(MBR, PET, CYCLE, new ObservationCycleUpdateRequest(null, null, null, null, null,
                List.of("obsrvSz", "obsrvChgCd", "memoCn")));

        assertThat(cycle.getObsrvSz()).isNull();
        assertThat(cycle.getObsrvChgCd()).isNull();
        assertThat(cycle.getMemoCn()).isNull();
        assertThat(cycle.getAtchFileSn()).isEqualTo(5L);
        assertThat(cycle.getObsrvDt()).isEqualTo(obsrvDt);
        verify(mapper).updateCycle(cycle, String.valueOf(MBR));
    }

    @Test
    void 회차_수정_요청은_허용_밖_이름과_값_비우기_겹침을_막는다() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var unknown = validator.validate(
                    new ObservationCycleUpdateRequest(null, null, null, null, null, List.of("file")));
            var conflict = validator.validate(new ObservationCycleUpdateRequest(null, null, new BigDecimal("3"),
                    null, null, List.of("obsrvSz")));
            var blankMemo = validator.validate(
                    new ObservationCycleUpdateRequest(null, null, null, null, "", List.of("memoCn")));
            var ok = validator.validate(new ObservationCycleUpdateRequest(null, null, null, null, "메모",
                    List.of("obsrvSz", "obsrvChgCd")));

            assertThat(unknown).extracting(v -> v.getPropertyPath().toString())
                    .anyMatch(p -> p.startsWith("clearFields[0]"));
            assertThat(conflict).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("clearFieldsDistinct");
            assertThat(blankMemo).isEmpty();
            assertThat(ok).isEmpty();
        }
    }

    @Test
    void 없는_회차는_404이고_다른_회원은_권한_검사에서_막힌다() {
        when(mapper.selectCycle(PET, CYCLE)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteCycle(MBR, PET, CYCLE))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OBSERVATION_CYCLE_NOT_FOUND));

        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, 99L);
        assertThatThrownBy(() -> service.searchCycles(MBR, 99L, new ObservationCycleSearchCond()))
                .isInstanceOf(CustomException.class);
        verify(mapper, never()).selectCycles(any());
    }

    @Test
    void 회차_삭제는_남은_회차가_있으면_항목을_두고_마지막_회차면_항목도_삭제한다() {
        ObservationCycle cycle = new ObservationCycle();
        cycle.setObsrvCyclSn(CYCLE);
        cycle.setObsrvArtclSn(ITEM);
        when(mapper.selectCycle(PET, CYCLE)).thenReturn(cycle);
        when(mapper.countCycles(ITEM)).thenReturn(2);

        service.deleteCycle(MBR, PET, CYCLE);

        verify(mapper).deleteCycle(CYCLE, String.valueOf(MBR));
        verify(mapper, never()).deleteItem(anyLong(), anyString());

        when(mapper.countCycles(ITEM)).thenReturn(0);
        service.deleteCycle(MBR, PET, CYCLE);

        verify(mapper).deleteItem(ITEM, String.valueOf(MBR));
    }

    @Test
    void 관찰_중_반려동물_번호를_모아_케어미션_출제에_넘긴다() {
        when(mapper.selectObservingPetSns(ONGOING)).thenReturn(List.of(10L, 20L, 10L));

        assertThat(service.searchObservingPetSns()).isEqualTo(Set.of(10L, 20L));
    }

    @Test
    void 회차_추가_서비스_검사_400은_틀린_칸_이름을_담는다() {
        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ONGOING));

        assertFields(() -> service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM, photo,
                LocalDateTime.now().plusDays(3), null, null, null)), "obsrvDt");
        assertFields(() -> service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM, photo, PAST,
                new BigDecimal("-1"), null, null)), "obsrvSz");
        assertFields(() -> service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM, photo, PAST, null,
                "HLTC9999", null)), "obsrvChgCd");
        assertFields(() -> service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM,
                new MockMultipartFile("file", new byte[0]), PAST, null, null, null)), "file");
    }

    @Test
    void 촬영일은_오늘까지_저장하고_내일은_등록_추가_수정_모두_400이다() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        when(mapper.selectItem(PET, ITEM)).thenReturn(item(ONGOING));
        ObservationCycle cycle = new ObservationCycle();
        cycle.setObsrvCyclSn(CYCLE);
        cycle.setObsrvArtclSn(ITEM);
        cycle.setObsrvDt(PAST);
        when(mapper.selectCycle(PET, CYCLE)).thenReturn(cycle);

        service.createItem(MBR, PET, itemRequest("HLTC0043", now, null));
        service.createCycle(MBR, PET, new ObservationCycleCreateRequest(ITEM, photo, now, null, null, null));
        service.updateCycle(MBR, PET, CYCLE,
                new ObservationCycleUpdateRequest(null, now, null, null, null, null));
        assertThat(cycle.getObsrvDt()).isEqualTo(now);

        assertFields(() -> service.createItem(MBR, PET, itemRequest("HLTC0043", tomorrow, null)), "obsrvDt");
        assertFields(() -> service.createCycle(MBR, PET,
                new ObservationCycleCreateRequest(ITEM, photo, tomorrow, null, null, null)), "obsrvDt");
        assertFields(() -> service.updateCycle(MBR, PET, CYCLE,
                new ObservationCycleUpdateRequest(null, tomorrow, null, null, null, null)), "obsrvDt");
        assertThat(cycle.getObsrvDt()).isEqualTo(now);
    }

    private static void assertFields(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String... fields) {
        assertThatThrownBy(call).isInstanceOfSatisfying(CustomException.class, e -> {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
            assertThat(e.getFields()).containsExactly(fields);
        });
    }
}
