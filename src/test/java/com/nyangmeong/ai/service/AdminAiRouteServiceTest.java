package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.ai.domain.AiRoutMpng;
import com.nyangmeong.ai.dto.AdminAiRouteSaveRequest;
import com.nyangmeong.ai.dto.AdminAiRouteSummary;
import com.nyangmeong.ai.dto.AiRouteResponse;
import com.nyangmeong.ai.mapper.AiRouteMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;

/**
 * [검사 - 관리자 라우팅 매핑표 조회·등록·수정·삭제·우선순위]
 */
class AdminAiRouteServiceTest {

    private static final String PAGE = "AIMC0032";
    private static final String EXCLUDED = "AIMC0035";

    private final AiRouteMapper aiRouteMapper = mock(AiRouteMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminAiRouteService service = new AdminAiRouteService(aiRouteMapper, adminTaskLogService);

    private AiRoutMpng order(long sn, int prrtSeq) {
        AiRoutMpng route = new AiRoutMpng();
        route.setAiRoutMpngSn(sn);
        route.setPrrtSeq(prrtSeq);
        return route;
    }

    private AiRoutMpng stored(long sn, int prrtSeq) {
        AiRoutMpng route = order(sn, prrtSeq);
        route.setDomnNm("HLT");
        route.setTrgtScrnNm("건강 일지");
        route.setScrnId("SCR-HLT-002");
        route.setInstKwrdCn("건강 일지, 오늘 기록");
        route.setMvmnMthdCd(PAGE);
        route.setRprsChipYn("N");
        route.setUseYn("Y");
        return route;
    }

    private AdminAiRouteSaveRequest create(String scrnId, String instKwrdCn, String mvmnMthdCd, Integer prrtSeq) {
        return new AdminAiRouteSaveRequest("영수증 등록", scrnId, "checkup=receipt", instKwrdCn, mvmnMthdCd, null,
                prrtSeq, null, null);
    }

    private AdminAiRouteSaveRequest move(Integer prrtSeq) {
        return new AdminAiRouteSaveRequest(null, null, null, null, null, null, prrtSeq, null, null);
    }

    /** 순서 다시 매기기에서 고친 [번호, 순위] */
    private List<long[]> capturePrrtSeqUpdates() {
        List<long[]> updates = new ArrayList<>();
        doAnswer(invocation -> {
            updates.add(new long[] {invocation.getArgument(0), invocation.<Integer>getArgument(1)});
            return 1;
        }).when(aiRouteMapper).updatePrrtSeq(anyLong(), anyInt(), anyString());
        return updates;
    }

    @Test
    void 검색어는_공백을_빼고_LIKE_특수문자를_막아_넘긴다() {
        service.search(" 병원 찾아_% ");

        verify(aiRouteMapper).selectAdminRoutes("병원찾아\\_\\%");
    }

    @Test
    void 검색어가_공백뿐이면_전체를_조회한다() {
        service.search("   ");

        verify(aiRouteMapper).selectAdminRoutes(isNull());
    }

    @Test
    void 등록은_기본값이_대표칩_아님_사용이고_도메인을_화면ID에서_정해_맨_아래에_둔다() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of(order(1, 1), order(2, 2)));
        doAnswer(invocation -> {
            invocation.<AiRoutMpng>getArgument(0).setAiRoutMpngSn(50L);
            return 1;
        }).when(aiRouteMapper).insertRoute(any(), eq("9"));
        when(aiRouteMapper.selectRoute(50L)).thenReturn(stored(50, 3));
        List<long[]> updates = capturePrrtSeqUpdates();

        assertThat(service.create(9L, create("SCR-HLT-002", "영수증 등록", PAGE, null)).aiRoutMpngSn())
                .isEqualTo(50L);

        ArgumentCaptor<AiRoutMpng> captor = ArgumentCaptor.forClass(AiRoutMpng.class);
        verify(aiRouteMapper).insertRoute(captor.capture(), eq("9"));
        AiRoutMpng inserted = captor.getValue();
        assertThat(inserted.getDomnNm()).isEqualTo("HLT");
        assertThat(inserted.getRprsChipYn()).isEqualTo("N");
        assertThat(inserted.getUseYn()).isEqualTo("Y");
        assertThat(inserted.getPrrtSeq()).isEqualTo(3);
        assertThat(updates).isEmpty();
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.CREATE), eq("AI_ROUT_MPNG"), eq(50L), any(),
                isNull(), any());
    }

    @Test
    void 등록에_prrtSeq를_주면_그_자리에_넣고_나머지를_민다() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of(order(1, 1), order(2, 2)));
        doAnswer(invocation -> {
            invocation.<AiRoutMpng>getArgument(0).setAiRoutMpngSn(50L);
            return 1;
        }).when(aiRouteMapper).insertRoute(any(), any());
        when(aiRouteMapper.selectRoute(50L)).thenReturn(stored(50, 1));
        List<long[]> updates = capturePrrtSeqUpdates();

        service.create(9L, create("SCR-HLT-002", "영수증 등록", PAGE, 1));

        assertThat(updates).containsExactly(new long[] {50, 1}, new long[] {1, 2}, new long[] {2, 3});
    }

    @Test
    void 화면_이동_행에_지시어가_없으면_400() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(9L, create("SCR-HLT-002", "  ", PAGE, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(aiRouteMapper, never()).insertRoute(any(), any());
    }

    @Test
    void 모르는_이동_방식이면_400() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(9L, create("SCR-HLT-002", "영수증", "AIMC9999", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 제외_행은_화면ID와_지시어가_없어도_되고_도메인은_ETC다() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of());
        doAnswer(invocation -> {
            invocation.<AiRoutMpng>getArgument(0).setAiRoutMpngSn(51L);
            return 1;
        }).when(aiRouteMapper).insertRoute(any(), any());
        when(aiRouteMapper.selectRoute(51L)).thenReturn(stored(51, 1));

        service.create(9L, create(null, null, EXCLUDED, null));

        ArgumentCaptor<AiRoutMpng> captor = ArgumentCaptor.forClass(AiRoutMpng.class);
        verify(aiRouteMapper).insertRoute(captor.capture(), any());
        assertThat(captor.getValue().getDomnNm()).isEqualTo("ETC");
    }

    @Test
    void 도메인은_SCR_뒤_세_글자() {
        assertThat(AdminAiRouteService.domainOf("SCR-CMU-001-01")).isEqualTo("CMU");
        assertThat(AdminAiRouteService.domainOf("SCR-AIM-0xx")).isEqualTo("AIM");
        assertThat(AdminAiRouteService.domainOf("관리자 전체")).isEqualTo("ETC");
    }

    @Test
    void 마지막_행을_1위로_옮기면_나머지가_한_칸씩_밀리고_빈틈이_없다() {
        when(aiRouteMapper.selectRouteOrderForUpdate())
                .thenReturn(List.of(order(1, 10), order(2, 20), order(3, 30)));
        when(aiRouteMapper.selectRoute(3L)).thenReturn(stored(3, 30));
        when(aiRouteMapper.updateRoute(any(), any())).thenReturn(1);
        List<long[]> updates = capturePrrtSeqUpdates();

        service.update(9L, 3L, move(1));

        assertThat(updates).containsExactly(new long[] {3, 1}, new long[] {1, 2}, new long[] {2, 3});
    }

    @Test
    void 범위_밖_순위는_맨_아래로_맞춘다() {
        assertThat(AdminAiRouteService.placeAt(List.of(1L, 2L), 3L, 99)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void 수정은_보낸_칸만_바꾸고_빈_문자열은_비운다() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of(order(1, 1)));
        AiRoutMpng before = stored(1, 1);
        before.setScrnParamCn("cat=hospital");
        when(aiRouteMapper.selectRoute(1L)).thenReturn(before);
        when(aiRouteMapper.updateRoute(any(), any())).thenReturn(1);

        service.update(9L, 1L,
                new AdminAiRouteSaveRequest(null, null, "", null, null, true, null, null, false));

        ArgumentCaptor<AiRoutMpng> captor = ArgumentCaptor.forClass(AiRoutMpng.class);
        verify(aiRouteMapper).updateRoute(captor.capture(), eq("9"));
        AiRoutMpng updated = captor.getValue();
        assertThat(updated.getTrgtScrnNm()).isEqualTo("건강 일지");
        assertThat(updated.getScrnParamCn()).isNull();
        assertThat(updated.getRprsChipYn()).isEqualTo("Y");
        assertThat(updated.getUseYn()).isEqualTo("N");
    }

    @Test
    void 아무_칸도_없는_수정은_400() {
        assertThatThrownBy(() -> service.update(9L, 1L, move(null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_매핑_수정은_404() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of());

        assertThatThrownBy(() -> service.update(9L, 99L, move(1)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_ROUTE_NOT_FOUND);
    }

    @Test
    void 삭제하면_뒤_순위를_당기고_감사_로그를_남긴다() {
        when(aiRouteMapper.selectRouteOrderForUpdate())
                .thenReturn(List.of(order(1, 1), order(2, 2), order(3, 3)));
        when(aiRouteMapper.selectRoute(2L)).thenReturn(stored(2, 2));
        when(aiRouteMapper.deleteRoute(2L, "9")).thenReturn(1);
        List<long[]> updates = capturePrrtSeqUpdates();

        service.delete(9L, 2L);

        assertThat(updates).containsExactly(new long[] {3, 2});
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.DELETE), eq("AI_ROUT_MPNG"), eq(2L), any(),
                any(AdminAiRouteSummary.class), isNull());
    }

    @Test
    void 삭제된_매핑을_다시_삭제하면_404() {
        when(aiRouteMapper.selectRouteOrderForUpdate()).thenReturn(List.of());

        assertThatThrownBy(() -> service.delete(9L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_ROUTE_NOT_FOUND);
    }

    @Test
    void AI_서버용_지시어는_쉼표로_나눠_공백을_뺀다() {
        AiRoutMpng route = stored(1, 1);
        route.setInstKwrdCn(" 병원, 동물병원 ,, 진료 ");

        assertThat(AiRouteResponse.from(route).instKwrds()).containsExactly("병원", "동물병원", "진료");
    }
}
