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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.InspectionArticle;
import com.nyangmeong.health.domain.InspectionValue;
import com.nyangmeong.health.dto.InspectionValueCreateRequest;
import com.nyangmeong.health.dto.InspectionValueCreateRequest.InspectionValueItemRequest;
import com.nyangmeong.health.dto.InspectionValueResponse;
import com.nyangmeong.health.dto.InspectionValueUpdateRequest;
import com.nyangmeong.health.mapper.InspectionValueMapper;

/**
 * [검사 - 검사 수치 저장·수정]
 * - 매퍼는 메모리 저장소로 흉내 : 살아 있는 행·삭제된 행을 항목(코드|직접입력명)별로 보관
 */
class InspectionValueServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long OTHER_PET = 20L;
    private static final Long CKUP = 500L;
    private static final String WBC = InspectionArticle.WBC.getCode();
    private static final String CREA = InspectionArticle.CREA.getCode();
    private static final String DIRECT = InspectionArticle.DIRECT.getCode();

    private final InspectionValueMapper mapper = mock(InspectionValueMapper.class);
    private final CheckupService checkupService = mock(CheckupService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final InspectionValueService service = new InspectionValueService(mapper, checkupService, petAuthorizer);

    private final Map<String, InspectionValue> active = new HashMap<>();
    private final Map<String, InspectionValue> deleted = new HashMap<>();
    private long nextSn = 1000L;

    @BeforeEach
    void setUp() {
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
        when(mapper.selectValueByArticle(eq(CKUP), anyString(), anyString()))
                .thenAnswer(call -> active.get(key(call.getArgument(1), call.getArgument(2))));
        when(mapper.selectMaxSortSeq(CKUP)).thenAnswer(call -> active.values().stream()
                .mapToInt(InspectionValue::getSortSeq).max().orElse(0));
        when(mapper.insertValue(any())).thenAnswer(call -> {
            InspectionValue value = call.getArgument(0);
            value.setInspNvlSn(nextSn++);
            active.put(key(value), value);
            return 1;
        });
        when(mapper.restoreValue(any(), anyString())).thenAnswer(call -> {
            InspectionValue value = call.getArgument(0);
            InspectionValue old = deleted.remove(key(value));
            if (old == null) {
                return 0;
            }
            value.setInspNvlSn(old.getInspNvlSn());
            active.put(key(value), value);
            return 1;
        });
        when(mapper.selectValue(anyLong())).thenAnswer(call -> active.values().stream()
                .filter(value -> value.getInspNvlSn().equals(call.getArgument(0)))
                .findFirst()
                .map(this::copy)
                .orElse(null));
    }

    @Test
    void 코드와_직접입력_항목을_요청_순서대로_저장한다() {
        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(WBC, null, "12.4", "10^3/uL", null, null),
                item(null, "SDMA", "14", "ug/dL", "0", "14")));

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).inspArtclCd()).isEqualTo(WBC);
        assertThat(saved.get(0).inspArtclDrctInptNm()).isNull();
        assertThat(saved.get(0).sortSeq()).isEqualTo(1);
        assertThat(saved.get(1).inspArtclCd()).isEqualTo(DIRECT);
        assertThat(saved.get(1).inspArtclDrctInptNm()).isEqualTo("SDMA");
        assertThat(saved.get(1).sortSeq()).isEqualTo(2);
        assertThat(active.get(key(WBC, "")).getRgtrId()).isEqualTo(String.valueOf(MBR));
    }

    @Test
    void SDMA는_공통코드_항목으로_저장한다() {
        String sdma = InspectionArticle.SDMA.getCode();

        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(sdma, null, "14", "ug/dL", "0", "14")));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).inspArtclCd()).isEqualTo("HLTC0078");
        assertThat(saved.get(0).inspArtclDrctInptNm()).isNull();
    }

    @Test
    void 같은_검진기록에_이미_있는_항목은_건너뛰고_새_항목만_뒤에_붙인다() {
        InspectionValue wbc = stored(WBC, "");
        wbc.setSortSeq(5);
        active.put(key(wbc), wbc);

        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(WBC, null, "9", "10^3/uL", null, null),
                item(CREA, null, "1.8", "mg/dL", "0.8", "1.5")));

        assertThat(saved).extracting(InspectionValueResponse::inspArtclCd).containsExactly(CREA);
        assertThat(saved.get(0).sortSeq()).isEqualTo(6);
        assertThat(active.get(key(WBC, "")).getRsltNvl()).isEqualByComparingTo("12.4");
    }

    @Test
    void 전부_이미_있으면_빈_목록이고_아무것도_쓰지_않는다() {
        InspectionValue wbc = stored(WBC, "");
        active.put(key(wbc), wbc);

        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(WBC, null, "9", "10^3/uL", null, null)));

        assertThat(saved).isEmpty();
        verify(mapper, never()).insertValue(any());
        verify(mapper, never()).restoreValue(any(), anyString());
    }

    @Test
    void 삭제된_같은_항목은_같은_번호로_새_값을_넣어_복구한다() {
        InspectionValue old = stored(CREA, "");
        old.setInspNvlSn(77L);
        deleted.put(key(old), old);

        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(CREA, null, "2.1", "mg/dL", null, null)));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).inspNvlSn()).isEqualTo(77L);
        assertThat(saved.get(0).rsltNvl()).isEqualByComparingTo("2.1");
        verify(mapper, never()).insertValue(any());
    }

    @Test
    void 요청_안에서_같은_항목이_두_번이면_첫_행만_저장한다() {
        List<InspectionValueResponse> saved = service.create(MBR, PET, request(
                item(WBC, null, "1", "u", null, null),
                item(WBC, null, "2", "u", null, null),
                item(null, "SDMA", "3", "u", null, null),
                item(DIRECT, " SDMA ", "4", "u", null, null)));

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).rsltNvl()).isEqualByComparingTo("1");
        assertThat(saved.get(1).rsltNvl()).isEqualByComparingTo("3");
    }

    @Test
    void 참고범위를_한쪽만_넣으면_거절하고_아무것도_저장하지_않는다() {
        assertThatThrownBy(() -> service.create(MBR, PET, request(
                item(WBC, null, "1", "u", null, null),
                item(CREA, null, "1.8", "mg/dL", "0.8", null))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertValue(any());
    }

    @Test
    void 항목_코드가_없거나_12종이_아니거나_직접입력명과_다른_코드면_거절한다() {
        assertInvalid(item(null, null, "1", "u", null, null));
        assertInvalid(item("HLTC0036", null, "1", "u", null, null));
        assertInvalid(item(DIRECT, null, "1", "u", null, null));
        assertInvalid(item(WBC, "SDMA", "1", "u", null, null));
        assertInvalid(item(WBC, null, "1", "  ", null, null));
    }

    @Test
    void 남의_검진기록이면_404로_거절한다() {
        doThrow(new CustomException(ErrorCode.CHECKUP_NOT_FOUND))
                .when(checkupService).getCheckup(MBR, OTHER_PET, CKUP);

        assertThatThrownBy(() -> service.create(MBR, OTHER_PET, request(item(WBC, null, "1", "u", null, null))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_NOT_FOUND);
        verify(mapper, never()).insertValue(any());
    }

    @Test
    void 수정은_보낸_항목만_바꾸고_검사_항목은_그대로다() {
        InspectionValue crea = stored(CREA, "");
        crea.setRefLlmtNvl(new BigDecimal("0.8"));
        crea.setRefUplmtNvl(new BigDecimal("1.5"));
        crea.setMemoCn("공복");
        active.put(key(crea), crea);
        when(mapper.updateValue(any(), anyString())).thenAnswer(call -> {
            InspectionValue value = call.getArgument(0);
            active.put(key(value), value);
            return 1;
        });

        InspectionValueResponse res = service.update(MBR, PET, crea.getInspNvlSn(),
                new InspectionValueUpdateRequest(new BigDecimal("2.0"), null, null, null, "재검"));

        assertThat(res.inspArtclCd()).isEqualTo(CREA);
        assertThat(res.rsltNvl()).isEqualByComparingTo("2.0");
        assertThat(res.unitNm()).isEqualTo("mg/dL");
        assertThat(res.refLlmtNvl()).isEqualByComparingTo("0.8");
        assertThat(res.memoCn()).isEqualTo("재검");
        verify(petAuthorizer).checkCaregiver(MBR, PET);
    }

    @Test
    void 수정_결과_참고범위가_한쪽만_남거나_단위가_비면_거절한다() {
        InspectionValue wbc = stored(WBC, "");
        active.put(key(wbc), wbc);

        assertThatThrownBy(() -> service.update(MBR, PET, wbc.getInspNvlSn(),
                new InspectionValueUpdateRequest(null, null, new BigDecimal("1"), null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.update(MBR, PET, wbc.getInspNvlSn(),
                new InspectionValueUpdateRequest(null, "", null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).updateValue(any(), anyString());
    }

    @Test
    void 다른_반려동물이나_삭제된_수치는_수정하지_못한다() {
        InspectionValue wbc = stored(WBC, "");
        active.put(key(wbc), wbc);
        InspectionValueUpdateRequest req = new InspectionValueUpdateRequest(BigDecimal.ONE, null, null, null, null);

        assertThatThrownBy(() -> service.update(MBR, OTHER_PET, wbc.getInspNvlSn(), req))
                .extracting("errorCode").isEqualTo(ErrorCode.INSPECTION_VALUE_NOT_FOUND);
        assertThatThrownBy(() -> service.update(MBR, PET, 9999L, req))
                .extracting("errorCode").isEqualTo(ErrorCode.INSPECTION_VALUE_NOT_FOUND);
        verify(mapper, never()).updateValue(any(), anyString());
    }

    private void assertInvalid(InspectionValueItemRequest item) {
        assertThatThrownBy(() -> service.create(MBR, PET, request(item)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private InspectionValueCreateRequest request(InspectionValueItemRequest... items) {
        return new InspectionValueCreateRequest(CKUP, List.of(items));
    }

    private InspectionValueItemRequest item(String code, String name, String rslt, String unit, String llmt,
            String uplmt) {
        return new InspectionValueItemRequest(code, name, new BigDecimal(rslt), unit,
                llmt == null ? null : new BigDecimal(llmt), uplmt == null ? null : new BigDecimal(uplmt), null);
    }

    private InspectionValue stored(String code, String name) {
        InspectionValue value = new InspectionValue();
        value.setInspNvlSn(nextSn++);
        value.setCkupRcdSn(CKUP);
        value.setInspArtclCd(code);
        value.setInspArtclDrctInptNm(name);
        value.setRsltNvl(new BigDecimal("12.4"));
        value.setUnitNm("mg/dL");
        value.setSortSeq(1);
        return value;
    }

    private InspectionValue copy(InspectionValue src) {
        InspectionValue value = new InspectionValue();
        value.setInspNvlSn(src.getInspNvlSn());
        value.setCkupRcdSn(src.getCkupRcdSn());
        value.setInspArtclCd(src.getInspArtclCd());
        value.setInspArtclDrctInptNm(src.getInspArtclDrctInptNm());
        value.setRsltNvl(src.getRsltNvl());
        value.setUnitNm(src.getUnitNm());
        value.setRefLlmtNvl(src.getRefLlmtNvl());
        value.setRefUplmtNvl(src.getRefUplmtNvl());
        value.setMemoCn(src.getMemoCn());
        value.setSortSeq(src.getSortSeq());
        return value;
    }

    private String key(InspectionValue value) {
        return key(value.getInspArtclCd(), value.getInspArtclDrctInptNm());
    }

    private String key(String code, String name) {
        return code + "|" + name;
    }
}
