package com.nyangmeong.community.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.BoardType;
import com.nyangmeong.community.domain.CommunityAiCommentary;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.TodayPhrase;
import com.nyangmeong.community.dto.AdminCommentarySummary;
import com.nyangmeong.community.dto.AdminCommentaryUseRequest;
import com.nyangmeong.community.dto.AdminHealthTipAssignRequest;
import com.nyangmeong.community.dto.AdminHealthTipResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseCreateRequest;
import com.nyangmeong.community.dto.AdminTodayPhraseCreateResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseListResponse;
import com.nyangmeong.community.dto.AdminTodayPhraseSummary;
import com.nyangmeong.community.dto.AdminTodayPhraseUpdateRequest;
import com.nyangmeong.community.mapper.CommunityAiCommentaryMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.TodayPhraseMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 노출 콘텐츠 서비스]
 * - SCR-ADM-023 오늘의 건강팁 지정·해제, 오늘의 한마디 후보 문구·매일 선택, AI 코멘트 사용 여부
 */
@Service
@RequiredArgsConstructor
public class AdminExposureService {

    static final int PHRASE_MAX_LENGTH = 200;
    /** 관리자 등록 문구 출처 (AIMG08 기본 문구) */
    static final String COMMENTARY_SRC_ADMIN_PHRASE = "AIMC0029";

    private final PostService postService;
    private final PostMapper postMapper;
    private final TodayPhraseMapper todayPhraseMapper;
    private final CommunityAiCommentaryMapper commentaryMapper;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record HealthTipState(Integer hlthInfoExpsrSeq) {
    }

    private record PhraseState(String phrsCn, String useYn) {
    }

    private record UseState(String useYn) {
    }

    /**
     * 오늘의 건강팁 현황 (FN-ADM-023-01)
     */
    @Transactional(readOnly = true)
    public List<AdminHealthTipResponse> getHealthTips() {
        return postService.getHealthTipSlots().stream().map(AdminHealthTipResponse::from).toList();
    }

    /**
     * 건강팁 지정 : 같은 자리의 기존 지정을 풀고 이 글을 그 자리로 (FN-ADM-023-02)
     */
    @Transactional
    public void assignHealthTip(Long actorSn, Long pstSn, AdminHealthTipAssignRequest req) {
        Post post = postMapper.selectAdminPost(pstSn);
        if (post == null || !PostStatus.NORMAL.is(post.getPstSttsCd())) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        if (!BoardType.PET_INFO.is(post.getBbsCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Integer seq = req.hlthInfoExpsrSeq();
        postMapper.clearHealthTipSeqBySeq(seq, pstSn);
        if (postMapper.updateHealthTipSeq(pstSn, seq, BoardType.PET_INFO.getCode(),
                PostStatus.NORMAL.getCode()) != 1) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, "PST", pstSn, "건강팁 지정",
                new HealthTipState(post.getHlthInfoExpsrSeq()), new HealthTipState(seq));
    }

    /**
     * 건강팁 지정 해제 (FN-ADM-023-02)
     */
    @Transactional
    public void releaseHealthTip(Long actorSn, Long pstSn) {
        Post post = postMapper.selectAdminPost(pstSn);
        if (post == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        if (post.getHlthInfoExpsrSeq() == null || postMapper.clearHealthTipSeq(pstSn) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, "PST", pstSn, "건강팁 지정 해제",
                new HealthTipState(post.getHlthInfoExpsrSeq()), new HealthTipState(null));
    }

    /**
     * 오늘의 한마디 후보 문구 목록과 오늘 노출 문구 (FN-ADM-023-04·05)
     */
    @Transactional(readOnly = true)
    public AdminTodayPhraseListResponse getPhrases() {
        List<TodayPhrase> phrases = todayPhraseMapper.selectList();
        CommunityAiCommentary today = commentaryMapper.selectByYmd(LocalDate.now());
        AdminTodayPhraseListResponse.TodayPhrs todayPhrs = null;
        if (today != null) {
            Long todayPhrsSn = phrases.stream()
                    .filter(phrase -> phrase.getPhrsCn().equals(today.getCmtryCn()))
                    .map(TodayPhrase::getTodayPhrsSn)
                    .findFirst()
                    .orElse(null);
            todayPhrs = new AdminTodayPhraseListResponse.TodayPhrs(todayPhrsSn, today.getCmtryCn());
        }
        return new AdminTodayPhraseListResponse(todayPhrs,
                phrases.stream().map(AdminTodayPhraseSummary::from).toList());
    }

    /**
     * 오늘의 한마디 후보 문구 등록 (FN-ADM-023-04)
     */
    @Transactional
    public AdminTodayPhraseCreateResponse createPhrase(Long actorSn, AdminTodayPhraseCreateRequest req) {
        TodayPhrase phrase = new TodayPhrase();
        phrase.setPhrsCn(normalizePhrase(req.phrsCn()));
        phrase.setUseYn(toYn(req.useYn()));
        phrase.setRgtrId(String.valueOf(actorSn));
        phrase.setMdfrId(String.valueOf(actorSn));
        todayPhraseMapper.insert(phrase);
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, "TODAY_PHRS", phrase.getTodayPhrsSn(),
                "오늘의 한마디 문구 등록", null, new PhraseState(phrase.getPhrsCn(), phrase.getUseYn()));
        return new AdminTodayPhraseCreateResponse(phrase.getTodayPhrsSn());
    }

    /**
     * 오늘의 한마디 후보 문구 수정 : 보낸 칸만 (FN-ADM-023-04)
     */
    @Transactional
    public void updatePhrase(Long actorSn, Long todayPhrsSn, AdminTodayPhraseUpdateRequest req) {
        if (req.phrsCn() == null && req.useYn() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        TodayPhrase before = todayPhraseMapper.selectById(todayPhrsSn);
        if (before == null) {
            throw new CustomException(ErrorCode.TODAY_PHRASE_NOT_FOUND);
        }
        TodayPhrase change = new TodayPhrase();
        change.setTodayPhrsSn(todayPhrsSn);
        change.setPhrsCn(req.phrsCn() == null ? null : normalizePhrase(req.phrsCn()));
        change.setUseYn(req.useYn() == null ? null : toYn(req.useYn()));
        change.setMdfrId(String.valueOf(actorSn));
        if (todayPhraseMapper.update(change) != 1) {
            throw new CustomException(ErrorCode.TODAY_PHRASE_NOT_FOUND);
        }
        String afterCn = change.getPhrsCn() != null ? change.getPhrsCn() : before.getPhrsCn();
        String afterYn = change.getUseYn() != null ? change.getUseYn() : before.getUseYn();
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, "TODAY_PHRS", todayPhrsSn, "오늘의 한마디 문구 수정",
                new PhraseState(before.getPhrsCn(), before.getUseYn()), new PhraseState(afterCn, afterYn));
    }

    /**
     * 오늘의 한마디 선택 (FN-ADM-023-05, 매일 00:00 배치)
     * - 사용 중 후보 1건을 무작위로 골라 그 일자 CMNTY_AI_CMTRY 에 저장
     * - 그 일자 행이 이미 있거나 후보가 없으면 저장하지 않음 (사용자 화면은 운영 설정 기본 문구)
     *
     * @return 저장했으면 true
     */
    @Transactional
    public boolean selectTodayPhrase(LocalDate ymd) {
        TodayPhrase phrase = todayPhraseMapper.selectRandomInUse();
        if (phrase == null) {
            return false;
        }
        CommunityAiCommentary commentary = new CommunityAiCommentary();
        commentary.setCmtrySrcCd(COMMENTARY_SRC_ADMIN_PHRASE);
        commentary.setCmtryCrtrYmd(ymd);
        commentary.setCmtryCn(phrase.getPhrsCn());
        return commentaryMapper.insertIgnore(commentary) == 1;
    }

    /**
     * AI 코멘트 목록 (FN-ADM-023-06, 사용 안 함 포함 최근 일자순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCommentarySummary> searchCommentaries(PageQuery pq) {
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        PageInfo<CommunityAiCommentary> page = new PageInfo<>(commentaryMapper.selectAdminList());
        return new PageResponse<>(page.getList().stream().map(AdminCommentarySummary::from).toList(),
                page.getPageNum(), page.getPageSize(), page.getTotal(), page.getPages(), page.isHasNextPage());
    }

    /**
     * AI 코멘트 사용 여부 변경 (FN-ADM-023-06)
     */
    @Transactional
    public void changeCommentaryUse(Long actorSn, Long cmntyAiCmtrySn, AdminCommentaryUseRequest req) {
        CommunityAiCommentary before = commentaryMapper.selectById(cmntyAiCmtrySn);
        if (before == null) {
            throw new CustomException(ErrorCode.AI_COMMENTARY_NOT_FOUND);
        }
        String useYn = toYn(req.useYn());
        if (commentaryMapper.updateUseYn(cmntyAiCmtrySn, useYn) != 1) {
            throw new CustomException(ErrorCode.AI_COMMENTARY_NOT_FOUND);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, "CMNTY_AI_CMTRY", cmntyAiCmtrySn,
                "AI 코멘트 사용 여부 변경", new UseState(before.getUseYn()), new UseState(useYn));
    }

    /**
     * 문구 앞뒤 공백 제거 후 1~200자 검사
     */
    private String normalizePhrase(String phrsCn) {
        String stripped = phrsCn == null ? "" : phrsCn.strip();
        if (stripped.isEmpty() || stripped.length() > PHRASE_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return stripped;
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }
}
