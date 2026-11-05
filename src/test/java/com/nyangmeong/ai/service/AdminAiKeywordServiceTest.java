package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.ai.domain.KwrdDict;
import com.nyangmeong.ai.dto.AdminAiKeywordSaveRequest;
import com.nyangmeong.ai.dto.AdminAiKeywordSummary;
import com.nyangmeong.ai.mapper.AiKeywordMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;

/**
 * [검사 - 관리자 키워드 사전 조회·등록·수정·삭제, AI 서버용 조회]
 */
class AdminAiKeywordServiceTest {

    private static final String SCHEDULE = "AIMC0037";
    private static final String EXPENSE = "AIMC0038";
    private static final String NEWS = "AIMC0039";

    private final AiKeywordMapper aiKeywordMapper = mock(AiKeywordMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminAiKeywordService service = new AdminAiKeywordService(aiKeywordMapper, adminTaskLogService);
    private final AiKeywordService internalService = new AiKeywordService(aiKeywordMapper);

    private KwrdDict stored(long sn, String dictUsgCd, String kwrdClsfNm, String mpngCd) {
        KwrdDict keyword = new KwrdDict();
        keyword.setKwrdDictSn(sn);
        keyword.setDictUsgCd(dictUsgCd);
        keyword.setKwrdClsfNm(kwrdClsfNm);
        keyword.setArtclNm("사료");
        keyword.setKwrdCn("사료, 건사료");
        keyword.setMpngCd(mpngCd);
        keyword.setSortSeq(10);
        keyword.setUseYn("Y");
        return keyword;
    }

    private AdminAiKeywordSaveRequest create(String dictUsgCd, String kwrdClsfNm, String mpngCd) {
        return new AdminAiKeywordSaveRequest(dictUsgCd, kwrdClsfNm, "간식", "간식, 츄르", null, mpngCd, null, null);
    }

    private AdminAiKeywordSaveRequest patch(String kwrdCn, String mpngCd, Boolean useYn) {
        return new AdminAiKeywordSaveRequest(null, null, null, kwrdCn, null, mpngCd, null, useYn);
    }

    private void givenInsertSn(long sn) {
        doAnswer(invocation -> {
            invocation.<KwrdDict>getArgument(0).setKwrdDictSn(sn);
            return 1;
        }).when(aiKeywordMapper).insertKeyword(any(), anyString());
    }

    @Test
    void 뉴스_용도와_모르는_용도는_조회가_400이다() {
        assertThatThrownBy(() -> service.search(NEWS, null)).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.search(null, null)).isInstanceOf(CustomException.class);
        verify(aiKeywordMapper, never()).selectAdminKeywords(any(), any());
    }

    @Test
    void 조회는_판정_규칙과_시간_행을_뺀다() {
        when(aiKeywordMapper.selectAdminKeywords(SCHEDULE, null)).thenReturn(List.of(
                stored(1L, SCHEDULE, "판정 규칙", null), stored(2L, SCHEDULE, "등록 동사", null),
                stored(3L, SCHEDULE, "시간", null)));

        List<AdminAiKeywordSummary> result = service.search(SCHEDULE, " ");

        assertThat(result).extracting(AdminAiKeywordSummary::kwrdDictSn).containsExactly(2L);
        assertThat(result.get(0).useYn()).isTrue();
    }

    @Test
    void 등록은_그_탭_맨_아래에_붙고_감사_로그를_남긴다() {
        when(aiKeywordMapper.countCodeInGroup("MYPC0014", "MYPG05")).thenReturn(1);
        when(aiKeywordMapper.selectMaxSortSeq(EXPENSE, "가계부 카테고리")).thenReturn(60);
        givenInsertSn(99L);
        when(aiKeywordMapper.selectKeyword(99L)).thenReturn(stored(99L, EXPENSE, "가계부 카테고리", "MYPC0014"));

        assertThat(service.create(9L, create(EXPENSE, "가계부 카테고리", "MYPC0014")).kwrdDictSn()).isEqualTo(99L);

        ArgumentCaptor<KwrdDict> captor = ArgumentCaptor.forClass(KwrdDict.class);
        verify(aiKeywordMapper).insertKeyword(captor.capture(), eq("9"));
        assertThat(captor.getValue().getSortSeq()).isEqualTo(61);
        assertThat(captor.getValue().getUseYn()).isEqualTo("Y");
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.CREATE), eq("KWRD_DICT"), eq(99L), anyString(),
                isNull(), any());
    }

    @Test
    void 빈_탭에_등록하면_정렬순서는_10이다() {
        when(aiKeywordMapper.selectMaxSortSeq(SCHEDULE, "등록 동사")).thenReturn(null);
        givenInsertSn(5L);
        when(aiKeywordMapper.selectKeyword(5L)).thenReturn(stored(5L, SCHEDULE, "등록 동사", null));

        service.create(9L, create(SCHEDULE, "등록 동사", null));

        ArgumentCaptor<KwrdDict> captor = ArgumentCaptor.forClass(KwrdDict.class);
        verify(aiKeywordMapper).insertKeyword(captor.capture(), anyString());
        assertThat(captor.getValue().getSortSeq()).isEqualTo(10);
    }

    @Test
    void 등록_검사를_어기면_400이고_저장하지_않는다() {
        when(aiKeywordMapper.countCodeInGroup("HLTC0001", "MYPG05")).thenReturn(0);

        List<AdminAiKeywordSaveRequest> bad = List.of(
                create(NEWS, "뉴스 카테고리", null),
                create(SCHEDULE, "판정 규칙", null),
                create(SCHEDULE, "시간", null),
                create(EXPENSE, "가계부 카테고리", null),
                create(EXPENSE, "가계부 카테고리", "HLTC0001"),
                create(SCHEDULE, "날짜", "HLTC0001"),
                new AdminAiKeywordSaveRequest(SCHEDULE, "날짜", " ", "오늘", null, null, null, null),
                new AdminAiKeywordSaveRequest(SCHEDULE, "날짜", "상대 날짜", "", null, null, null, null));
        for (AdminAiKeywordSaveRequest req : bad) {
            assertThatThrownBy(() -> service.create(9L, req)).isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
        verify(aiKeywordMapper, never()).insertKeyword(any(), anyString());
    }

    @Test
    void 수정은_보낸_칸만_바꾸고_빈_문자열은_비운다() {
        KwrdDict before = stored(7L, SCHEDULE, "일정 종류", "HLTC0057");
        before.setRmrkCn("비고");
        when(aiKeywordMapper.selectKeyword(7L)).thenReturn(before);
        when(aiKeywordMapper.countCodeInGroup("HLTC0058", "HLTG19")).thenReturn(1);
        when(aiKeywordMapper.updateKeyword(any(), anyString())).thenReturn(1);

        service.update(9L, 7L, new AdminAiKeywordSaveRequest(null, null, null, null, null, "HLTC0058", "", false));

        ArgumentCaptor<KwrdDict> captor = ArgumentCaptor.forClass(KwrdDict.class);
        verify(aiKeywordMapper).updateKeyword(captor.capture(), eq("9"));
        KwrdDict saved = captor.getValue();
        assertThat(saved.getMpngCd()).isEqualTo("HLTC0058");
        assertThat(saved.getRmrkCn()).isNull();
        assertThat(saved.getUseYn()).isEqualTo("N");
        assertThat(saved.getKwrdCn()).isEqualTo("사료, 건사료");
        assertThat(saved.getKwrdClsfNm()).isEqualTo("일정 종류");
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.UPDATE), eq("KWRD_DICT"), eq(7L), anyString(),
                any(), any());
    }

    @Test
    void 수정에서_필수_칸을_비우거나_연결_코드를_지우면_400이다() {
        when(aiKeywordMapper.selectKeyword(7L)).thenReturn(stored(7L, EXPENSE, "가계부 카테고리", "MYPC0013"));

        assertThatThrownBy(() -> service.update(9L, 7L, patch(" ", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.update(9L, 7L, patch(null, "", null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> service.update(9L, 7L, patch(null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(aiKeywordMapper, never()).updateKeyword(any(), anyString());
    }

    @Test
    void 없거나_다른_화면의_행은_수정_삭제가_404다() {
        when(aiKeywordMapper.selectKeyword(1L)).thenReturn(null);
        when(aiKeywordMapper.selectKeyword(2L)).thenReturn(stored(2L, NEWS, "뉴스 카테고리", "CMUC0018"));
        when(aiKeywordMapper.selectKeyword(3L)).thenReturn(stored(3L, SCHEDULE, "판정 규칙", null));

        for (long sn : new long[] {1L, 2L, 3L}) {
            assertThatThrownBy(() -> service.update(9L, sn, patch("x", null, null)))
                    .extracting("errorCode").isEqualTo(ErrorCode.AI_KEYWORD_NOT_FOUND);
            assertThatThrownBy(() -> service.delete(9L, sn))
                    .extracting("errorCode").isEqualTo(ErrorCode.AI_KEYWORD_NOT_FOUND);
        }
        verify(aiKeywordMapper, never()).deleteKeyword(any(), anyString());
    }

    @Test
    void 삭제는_DEL_YN을_바꾸고_감사_로그를_남긴다() {
        when(aiKeywordMapper.selectKeyword(7L)).thenReturn(stored(7L, SCHEDULE, "주기", null));
        when(aiKeywordMapper.deleteKeyword(7L, "9")).thenReturn(1);

        service.delete(9L, 7L);

        verify(aiKeywordMapper).deleteKeyword(7L, "9");
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.DELETE), eq("KWRD_DICT"), eq(7L), anyString(),
                any(), isNull());
    }

    @Test
    void AI_서버용_조회는_두_용도만_받는다() {
        when(aiKeywordMapper.selectActiveKeywords(EXPENSE)).thenReturn(List.of(
                stored(1L, EXPENSE, "가계부 카테고리", "MYPC0013")));

        assertThat(internalService.getActiveKeywords(EXPENSE)).singleElement()
                .satisfies(row -> assertThat(row.mpngCd()).isEqualTo("MYPC0013"));
        assertThatThrownBy(() -> internalService.getActiveKeywords(NEWS))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
