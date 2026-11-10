package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.CommunityAiCommentary;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.TodayPhrase;
import com.nyangmeong.community.dto.AdminCommentaryUseRequest;
import com.nyangmeong.community.dto.AdminHealthTipAssignRequest;
import com.nyangmeong.community.dto.AdminHealthTipResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseCreateRequest;
import com.nyangmeong.community.dto.AdminTodayPhraseListResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseUpdateRequest;
import com.nyangmeong.community.dto.HealthTipDesignation;
import com.nyangmeong.community.dto.PostSummary;
import com.nyangmeong.community.mapper.CommunityAiCommentaryMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.TodayPhraseMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 관리자 노출 콘텐츠 (SCR-ADM-023)]
 */
class AdminExposureServiceTest {

    private static final Long ACTOR = 1L;
    private static final String PET_INFO = "CMUC0001";
    private static final String PET_DAILY = "CMUC0002";
    private static final String NORMAL = "CMUC0003";
    private static final String BLIND = "CMUC0004";

    private final PostMapper postMapper = mock(PostMapper.class);
    private final TodayPhraseMapper todayPhraseMapper = mock(TodayPhraseMapper.class);
    private final CommunityAiCommentaryMapper commentaryMapper = mock(CommunityAiCommentaryMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final PostService postService = new PostService(postMapper, mock(FileStore.class), new OwnerAuthorizer(),
            mock(SystemSettingService.class), mock(PetStoryMissionReflector.class));
    private final AdminExposureService service = new AdminExposureService(postService, postMapper,
            todayPhraseMapper, commentaryMapper, adminTaskLogService);

    private PostSummary summary(Long pstSn) {
        PostSummary summary = new PostSummary();
        summary.setPstSn(pstSn);
        summary.setPstTtl("글" + pstSn);
        return summary;
    }

    private HealthTipDesignation designation(Long pstSn, int seq) {
        HealthTipDesignation designation = new HealthTipDesignation();
        designation.setPstSn(pstSn);
        designation.setHlthInfoExpsrSeq(seq);
        return designation;
    }

    private Post post(String bbsCd, String sttsCd, Integer seq) {
        Post post = new Post();
        post.setPstSn(7L);
        post.setBbsCd(bbsCd);
        post.setPstSttsCd(sttsCd);
        post.setHlthInfoExpsrSeq(seq);
        return post;
    }

    private TodayPhrase phrase(Long sn, String cn, String useYn) {
        TodayPhrase phrase = new TodayPhrase();
        phrase.setTodayPhrsSn(sn);
        phrase.setPhrsCn(cn);
        phrase.setUseYn(useYn);
        return phrase;
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    // ---------- 건강팁 현황 (01·03) ----------

    @Test
    void 지정_2번_자리_뒤로_빈_자리를_자동_선정_글로_채운다() {
        when(postMapper.selectHealthTips(PET_INFO, NORMAL)).thenReturn(List.of(summary(10L)));
        when(postMapper.selectHealthTipDesignations(PET_INFO, NORMAL)).thenReturn(List.of(designation(10L, 2)));
        when(postMapper.selectHealthKeywords()).thenReturn(List.of("건강, 질병,,병원"));
        when(postMapper.selectAutoHealthTips(eq(PET_INFO), eq(NORMAL), any(), eq(2)))
                .thenReturn(List.of(summary(20L), summary(30L)));

        List<AdminHealthTipResponse> rows = service.getHealthTips();

        assertThat(rows).extracting(AdminHealthTipResponse::pstSn).containsExactly(20L, 10L, 30L);
        assertThat(rows).extracting(AdminHealthTipResponse::hlthInfoExpsrSeq).containsExactly(null, 2, null);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keywords = ArgumentCaptor.forClass(List.class);
        verify(postMapper).selectAutoHealthTips(eq(PET_INFO), eq(NORMAL), keywords.capture(), eq(2));
        assertThat(keywords.getValue()).containsExactly("건강", "질병", "병원");
    }

    @Test
    void 지정이_없으면_자동_선정_3건() {
        when(postMapper.selectHealthKeywords()).thenReturn(List.of("건강"));
        when(postMapper.selectAutoHealthTips(eq(PET_INFO), eq(NORMAL), any(), eq(3)))
                .thenReturn(List.of(summary(1L), summary(2L), summary(3L)));

        assertThat(service.getHealthTips()).extracting(AdminHealthTipResponse::pstSn).containsExactly(1L, 2L, 3L);
    }

    @Test
    void 건강_키워드가_없으면_지정_글만() {
        when(postMapper.selectHealthTips(PET_INFO, NORMAL)).thenReturn(List.of(summary(10L)));
        when(postMapper.selectHealthTipDesignations(PET_INFO, NORMAL)).thenReturn(List.of(designation(10L, 1)));

        assertThat(service.getHealthTips()).extracting(AdminHealthTipResponse::pstSn).containsExactly(10L);
        verify(postMapper, never()).selectAutoHealthTips(any(), any(), any(), anyInt());
    }

    @Test
    void 세_자리가_모두_지정이면_자동_선정을_부르지_않는다() {
        when(postMapper.selectHealthTips(PET_INFO, NORMAL))
                .thenReturn(List.of(summary(1L), summary(2L), summary(3L)));
        when(postMapper.selectHealthTipDesignations(PET_INFO, NORMAL))
                .thenReturn(List.of(designation(1L, 1), designation(2L, 2), designation(3L, 3)));
        when(postMapper.selectHealthKeywords()).thenReturn(List.of("건강"));

        assertThat(service.getHealthTips()).extracting(AdminHealthTipResponse::hlthInfoExpsrSeq)
                .containsExactly(1, 2, 3);
        verify(postMapper, never()).selectAutoHealthTips(any(), any(), any(), anyInt());
    }

    @Test
    void 사용자_건강팁도_같은_자리_규칙() {
        when(postMapper.selectHealthTips(PET_INFO, NORMAL)).thenReturn(List.of(summary(10L)));
        when(postMapper.selectHealthTipDesignations(PET_INFO, NORMAL)).thenReturn(List.of(designation(10L, 1)));
        when(postMapper.selectHealthKeywords()).thenReturn(List.of("건강"));
        when(postMapper.selectAutoHealthTips(eq(PET_INFO), eq(NORMAL), any(), eq(2)))
                .thenReturn(List.of(summary(20L)));

        assertThat(postService.getHealthTips()).extracting(PostSummary::getPstSn).containsExactly(10L, 20L);
    }

    // ---------- 지정·해제 (02) ----------

    @Test
    void 지정은_같은_자리의_기존_지정을_풀고_감사_로그를_남긴다() {
        when(postMapper.selectAdminPost(7L)).thenReturn(post(PET_INFO, NORMAL, 3));
        when(postMapper.updateHealthTipSeq(7L, 1, PET_INFO, NORMAL)).thenReturn(1);

        service.assignHealthTip(ACTOR, 7L, new AdminHealthTipAssignRequest(1));

        verify(postMapper).clearHealthTipSeqBySeq(1, 7L);
        verify(postMapper).updateHealthTipSeq(7L, 1, PET_INFO, NORMAL);
        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.UPDATE), eq("PST"), eq(7L), anyString(),
                any(), any());
    }

    @Test
    void 정상이_아닌_글_지정은_404() {
        when(postMapper.selectAdminPost(7L)).thenReturn(post(PET_INFO, BLIND, null));

        assertError(() -> service.assignHealthTip(ACTOR, 7L, new AdminHealthTipAssignRequest(1)),
                ErrorCode.POST_NOT_FOUND);
        verify(postMapper, never()).clearHealthTipSeqBySeq(any(), any());
    }

    @Test
    void 없는_글_지정은_404() {
        assertError(() -> service.assignHealthTip(ACTOR, 7L, new AdminHealthTipAssignRequest(1)),
                ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 펫정보가_아닌_글_지정은_400() {
        when(postMapper.selectAdminPost(7L)).thenReturn(post(PET_DAILY, NORMAL, null));

        assertError(() -> service.assignHealthTip(ACTOR, 7L, new AdminHealthTipAssignRequest(1)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 지정_안_된_글_해제는_409() {
        when(postMapper.selectAdminPost(7L)).thenReturn(post(PET_INFO, NORMAL, null));

        assertError(() -> service.releaseHealthTip(ACTOR, 7L), ErrorCode.ALREADY_PROCESSED);
        verify(postMapper, never()).clearHealthTipSeq(any());
    }

    @Test
    void 지정_해제() {
        when(postMapper.selectAdminPost(7L)).thenReturn(post(PET_INFO, NORMAL, 2));
        when(postMapper.clearHealthTipSeq(7L)).thenReturn(1);

        service.releaseHealthTip(ACTOR, 7L);

        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.UPDATE), eq("PST"), eq(7L), anyString(),
                any(), any());
    }

    // ---------- 오늘의 한마디 (04·05) ----------

    @Test
    void 목록의_오늘_문구는_같은_문구의_후보_번호를_붙인다() {
        when(todayPhraseMapper.selectList())
                .thenReturn(List.of(phrase(1L, "산책 가자", "Y"), phrase(2L, "물 마셔요", "N")));
        CommunityAiCommentary today = new CommunityAiCommentary();
        today.setCmtryCn("물 마셔요");
        when(commentaryMapper.selectByYmd(any())).thenReturn(today);

        AdminTodayPhraseListResponse res = service.getPhrases();

        assertThat(res.todayPhrs().todayPhrsSn()).isEqualTo(2L);
        assertThat(res.todayPhrs().phrsCn()).isEqualTo("물 마셔요");
        assertThat(res.content()).extracting(row -> row.useYn()).containsExactly(true, false);
    }

    @Test
    void 오늘_문구가_없으면_todayPhrs_null() {
        when(todayPhraseMapper.selectList()).thenReturn(List.of(phrase(1L, "산책 가자", "Y")));

        assertThat(service.getPhrases().todayPhrs()).isNull();
    }

    @Test
    void 문구_등록은_앞뒤_공백을_지우고_Y_N으로_저장한다() {
        service.createPhrase(ACTOR, new AdminTodayPhraseCreateRequest("  산책 가자  ", false));

        ArgumentCaptor<TodayPhrase> captor = ArgumentCaptor.forClass(TodayPhrase.class);
        verify(todayPhraseMapper).insert(captor.capture());
        assertThat(captor.getValue().getPhrsCn()).isEqualTo("산책 가자");
        assertThat(captor.getValue().getUseYn()).isEqualTo("N");
        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.CREATE), eq("TODAY_PHRS"), any(),
                anyString(), any(), any());
    }

    @Test
    void 문구_201자는_400() {
        assertError(() -> service.createPhrase(ACTOR, new AdminTodayPhraseCreateRequest("가".repeat(201), true)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(todayPhraseMapper, never()).insert(any());
    }

    @Test
    void 문구_200자는_등록된다() {
        service.createPhrase(ACTOR, new AdminTodayPhraseCreateRequest("가".repeat(200), true));

        verify(todayPhraseMapper).insert(any());
    }

    @Test
    void 공백_문구_수정은_400() {
        when(todayPhraseMapper.selectById(1L)).thenReturn(phrase(1L, "산책 가자", "Y"));

        assertError(() -> service.updatePhrase(ACTOR, 1L, new AdminTodayPhraseUpdateRequest("   ", null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 빈_수정_요청은_400() {
        assertError(() -> service.updatePhrase(ACTOR, 1L, new AdminTodayPhraseUpdateRequest(null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_문구_수정은_404() {
        assertError(() -> service.updatePhrase(ACTOR, 9L, new AdminTodayPhraseUpdateRequest(null, false)),
                ErrorCode.TODAY_PHRASE_NOT_FOUND);
    }

    @Test
    void 문구_수정은_보낸_칸만() {
        when(todayPhraseMapper.selectById(1L)).thenReturn(phrase(1L, "산책 가자", "Y"));
        when(todayPhraseMapper.update(any())).thenReturn(1);

        service.updatePhrase(ACTOR, 1L, new AdminTodayPhraseUpdateRequest(null, false));

        ArgumentCaptor<TodayPhrase> captor = ArgumentCaptor.forClass(TodayPhrase.class);
        verify(todayPhraseMapper).update(captor.capture());
        assertThat(captor.getValue().getPhrsCn()).isNull();
        assertThat(captor.getValue().getUseYn()).isEqualTo("N");
    }

    @Test
    void 오늘의_한마디_선택은_관리자_문구_출처로_저장한다() {
        when(todayPhraseMapper.selectRandomInUse()).thenReturn(phrase(1L, "산책 가자", "Y"));
        when(commentaryMapper.insertIgnore(any())).thenReturn(1);
        LocalDate ymd = LocalDate.of(2026, 11, 4);

        assertThat(service.selectTodayPhrase(ymd)).isTrue();

        ArgumentCaptor<CommunityAiCommentary> captor = ArgumentCaptor.forClass(CommunityAiCommentary.class);
        verify(commentaryMapper).insertIgnore(captor.capture());
        assertThat(captor.getValue().getCmtrySrcCd()).isEqualTo("AIMC0029");
        assertThat(captor.getValue().getCmtryCrtrYmd()).isEqualTo(ymd);
        assertThat(captor.getValue().getCmtryCn()).isEqualTo("산책 가자");
    }

    @Test
    void 사용_중_후보가_없으면_저장하지_않는다() {
        assertThat(service.selectTodayPhrase(LocalDate.of(2026, 11, 4))).isFalse();
        verify(commentaryMapper, never()).insertIgnore(any());
    }

    @Test
    void 그날_행이_이미_있으면_0건() {
        when(todayPhraseMapper.selectRandomInUse()).thenReturn(phrase(1L, "산책 가자", "Y"));
        when(commentaryMapper.insertIgnore(any())).thenReturn(0);

        assertThat(service.selectTodayPhrase(LocalDate.of(2026, 11, 4))).isFalse();
    }

    // ---------- AI 코멘트 (06) ----------

    @Test
    void 없는_코멘트_사용_여부_변경은_404() {
        assertError(() -> service.changeCommentaryUse(ACTOR, 9L, new AdminCommentaryUseRequest(false)),
                ErrorCode.AI_COMMENTARY_NOT_FOUND);
    }

    @Test
    void 코멘트_사용_여부_변경() {
        CommunityAiCommentary before = new CommunityAiCommentary();
        before.setUseYn("Y");
        when(commentaryMapper.selectById(3L)).thenReturn(before);
        when(commentaryMapper.updateUseYn(3L, "N")).thenReturn(1);

        service.changeCommentaryUse(ACTOR, 3L, new AdminCommentaryUseRequest(false));

        verify(commentaryMapper).updateUseYn(3L, "N");
        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.UPDATE), eq("CMNTY_AI_CMTRY"), eq(3L),
                anyString(), any(), any());
    }
}
