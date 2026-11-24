package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.InspectionArticle;
import com.nyangmeong.health.dto.InspectionValueCreateRequest;
import com.nyangmeong.health.dto.InspectionValueCreateRequest.InspectionValueItemRequest;
import com.nyangmeong.health.mapper.InspectionValueMapper;

/**
 * [검사 - 검사 수치 저장 서비스 검사의 칸 이름]
 * - AI 도구 draft_inspection_values : 거절한 행을 values[i].<칸>으로 알림
 */
class InspectionValueFieldErrorTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final String WBC = InspectionArticle.WBC.getCode();
    private static final String DIRECT = InspectionArticle.DIRECT.getCode();

    private final InspectionValueMapper mapper = mock(InspectionValueMapper.class);
    private final InspectionValueService service = new InspectionValueService(mapper, mock(CheckupService.class),
            mock(PetAuthorizer.class));

    @Test
    void 둘째_행_코드가_없으면_values_1_inspArtclCd() {
        assertFields(List.of("values[1].inspArtclCd"), ok(), item(null, null, "1", "u", null, null));
    }

    @Test
    void 코드가_12종이_아니거나_직접입력명과_다른_코드면_inspArtclCd() {
        assertFields(List.of("values[0].inspArtclCd"), item("HLTC0036", null, "1", "u", null, null));
        assertFields(List.of("values[0].inspArtclCd"), item(DIRECT, null, "1", "u", null, null));
        assertFields(List.of("values[0].inspArtclCd"), item(WBC, "SDMA", "1", "u", null, null));
    }

    @Test
    void 참고범위가_한쪽만_있으면_빈_쪽_칸() {
        assertFields(List.of("values[1].refUplmtNvl"), ok(), item(WBC, null, "1", "u", "10", null));
        assertFields(List.of("values[0].refLlmtNvl"), item(WBC, null, "1", "u", null, "10"));
    }

    @Test
    void 하한이_상한보다_크면_두_칸() {
        assertFields(List.of("values[0].refLlmtNvl", "values[0].refUplmtNvl"),
                item(WBC, null, "1", "u", "50", "10"));
    }

    @Test
    void 단위가_공백만이면_unitNm() {
        assertFields(List.of("values[2].unitNm"), ok(), ok(), item(WBC, null, "1", "  ", null, null));
    }

    private void assertFields(List<String> fields, InspectionValueItemRequest... items) {
        CustomException e = catchThrowableOfType(CustomException.class,
                () -> service.create(MBR, PET, new InspectionValueCreateRequest(CKUP, List.of(items))));
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(e.getFields()).containsExactlyElementsOf(fields);
        verify(mapper, never()).insertValue(any());
    }

    private InspectionValueItemRequest ok() {
        return item(null, "SDMA", "14", "ug/dL", null, null);
    }

    private InspectionValueItemRequest item(String code, String name, String rslt, String unit, String llmt,
            String uplmt) {
        return new InspectionValueItemRequest(code, name, new BigDecimal(rslt), unit,
                llmt == null ? null : new BigDecimal(llmt), uplmt == null ? null : new BigDecimal(uplmt), null);
    }
}
