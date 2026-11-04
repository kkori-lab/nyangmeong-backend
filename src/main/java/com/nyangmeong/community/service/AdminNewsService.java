package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsCategory;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.domain.NewsSource;
import com.nyangmeong.community.dto.AdminNewsCreateRequest;
import com.nyangmeong.community.dto.AdminNewsCreateResponse;
import com.nyangmeong.community.dto.AdminNewsImageResponse;
import com.nyangmeong.community.dto.AdminNewsKeywordCreateRequest;
import com.nyangmeong.community.dto.AdminNewsKeywordCreateResponse;
import com.nyangmeong.community.dto.AdminNewsKeywordResponse;
import com.nyangmeong.community.dto.AdminNewsSearchCond;
import com.nyangmeong.community.dto.AdminNewsSummary;
import com.nyangmeong.community.dto.AdminNewsUpdateRequest;
import com.nyangmeong.community.mapper.NewsMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 펫뉴스 서비스]
 * - SCR-ADM-050 뉴스 목록, 관리자 뉴스 등록·수정, 수집 기사 숨김·카테고리 변경, 분류 키워드 등록·삭제
 * - 뉴스 수집 실행(FN-ADM-050-07)은 배치 SYSC0028 수동 실행
 */
@Service
@RequiredArgsConstructor
public class AdminNewsService {

    static final int PAGE_SIZE = 20;
    static final int TITLE_MAX_LENGTH = 200;
    static final int SUMMARY_MAX_LENGTH = 1000;
    static final int KEYWORD_MAX_LENGTH = 50;

    private final NewsMapper newsMapper;
    private final FileStore fileStore;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record NewsState(String newsTtl, String newsCn, String newsClsfCd, Long atchFileSn, String useYn) {

        static NewsState of(News news) {
            return new NewsState(news.getNewsTtl(), news.getNewsCn(), news.getNewsClsfCd(), news.getAtchFileSn(),
                    news.getUseYn());
        }
    }

    private record KeywordState(String mpngCd, String kwrdCn) {
    }

    /**
     * 관리자 뉴스 목록 (FN-ADM-050-01, 숨긴 기사 포함 등록 최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminNewsSummary> search(AdminNewsSearchCond cond, PageQuery pq) {
        String newsSrcCd = blankToNull(cond.getNewsSrcCd());
        String newsClsfCd = blankToNull(cond.getNewsClsfCd());
        if (newsSrcCd != null && !NewsSource.PRESS.is(newsSrcCd) && !NewsSource.ADMIN.is(newsSrcCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (newsClsfCd != null && !NewsCategory.exists(newsClsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<AdminNewsSummary> rows = newsMapper.selectAdminList(newsSrcCd, newsClsfCd);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 뉴스 등록 (FN-ADM-050-02) : 출처 관리자 등록, 발행일시 지금, 원문 경로 없음
     */
    @Transactional
    public AdminNewsCreateResponse create(Long actorSn, AdminNewsCreateRequest req) {
        News news = new News();
        news.setNewsTtl(normalize(req.newsTtl(), TITLE_MAX_LENGTH));
        news.setNewsCn(normalize(req.newsCn(), SUMMARY_MAX_LENGTH));
        news.setNewsClsfCd(requireCategory(req.newsClsfCd()));
        news.setNewsSrcCd(NewsSource.ADMIN.getCode());
        news.setAtchFileSn(req.atchFileSn());
        news.setPblcnDt(LocalDateTime.now());
        news.setUseYn("Y");
        newsMapper.insertAdmin(news);
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, "NEWS", news.getNewsSn(), "관리자 뉴스 등록", null,
                NewsState.of(news));
        return new AdminNewsCreateResponse(news.getNewsSn());
    }

    /**
     * 뉴스 수정 : 보낸 칸만 (FN-ADM-050-02·03)
     * - 제목·요약·썸네일은 관리자 등록 기사만, 카테고리·사용 여부는 모든 기사
     */
    @Transactional
    public void update(Long actorSn, Long newsSn, AdminNewsUpdateRequest req) {
        boolean editsContent = req.newsTtl() != null || req.newsCn() != null || req.atchFileSn() != null;
        if (!editsContent && req.newsClsfCd() == null && req.useYn() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        News before = newsMapper.selectById(newsSn);
        if (before == null) {
            throw new CustomException(ErrorCode.NEWS_NOT_FOUND);
        }
        if (editsContent && !NewsSource.ADMIN.is(before.getNewsSrcCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        News change = new News();
        change.setNewsSn(newsSn);
        change.setNewsTtl(req.newsTtl() == null ? null : normalize(req.newsTtl(), TITLE_MAX_LENGTH));
        change.setNewsCn(req.newsCn() == null ? null : normalize(req.newsCn(), SUMMARY_MAX_LENGTH));
        change.setNewsClsfCd(req.newsClsfCd() == null ? null : requireCategory(req.newsClsfCd()));
        change.setAtchFileSn(req.atchFileSn());
        change.setUseYn(req.useYn() == null ? null : (req.useYn() ? "Y" : "N"));
        if (newsMapper.updateAdmin(change) != 1) {
            throw new CustomException(ErrorCode.NEWS_NOT_FOUND);
        }
        News after = new News();
        after.setNewsTtl(change.getNewsTtl() != null ? change.getNewsTtl() : before.getNewsTtl());
        after.setNewsCn(change.getNewsCn() != null ? change.getNewsCn() : before.getNewsCn());
        after.setNewsClsfCd(change.getNewsClsfCd() != null ? change.getNewsClsfCd() : before.getNewsClsfCd());
        after.setAtchFileSn(change.getAtchFileSn() != null ? change.getAtchFileSn() : before.getAtchFileSn());
        after.setUseYn(change.getUseYn() != null ? change.getUseYn() : before.getUseYn());
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, "NEWS", newsSn, "뉴스 수정", NewsState.of(before),
                NewsState.of(after));
    }

    /**
     * 관리자 뉴스 썸네일 업로드 (FN-ADM-050-02, 공개 파일)
     */
    @Transactional
    public AdminNewsImageResponse uploadImage(Long actorSn, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new AdminNewsImageResponse(fileStore.storePublic(image, String.valueOf(actorSn)));
    }

    /**
     * 분류 키워드 목록 (FN-ADM-050-05, 정렬순)
     */
    @Transactional(readOnly = true)
    public List<AdminNewsKeywordResponse> getKeywords() {
        return newsMapper.selectAdminKeywords().stream().map(AdminNewsKeywordResponse::from).toList();
    }

    /**
     * 분류 키워드 등록 (FN-ADM-050-05)
     * - 정렬 순서는 그 카테고리 기존 행과 같게 해 카테고리 우선순위를 유지한다
     */
    @Transactional
    public AdminNewsKeywordCreateResponse createKeyword(Long actorSn, AdminNewsKeywordCreateRequest req) {
        String mpngCd = requireCategory(req.mpngCd());
        String kwrdCn = normalize(req.kwrdCn(), KEYWORD_MAX_LENGTH);
        if (kwrdCn.contains(",")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (newsMapper.existsKeyword(mpngCd, kwrdCn)) {
            throw new CustomException(ErrorCode.DUPLICATE_NEWS_KEYWORD);
        }
        NewsKeyword keyword = new NewsKeyword();
        keyword.setMpngCd(mpngCd);
        keyword.setKwrdCn(kwrdCn);
        keyword.setSortSeq(newsMapper.selectKeywordSortSeq(mpngCd));
        if (newsMapper.insertKeyword(keyword, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, "KWRD_DICT", keyword.getKwrdDictSn(),
                "뉴스 분류 키워드 등록", null, new KeywordState(mpngCd, kwrdCn));
        return new AdminNewsKeywordCreateResponse(keyword.getKwrdDictSn());
    }

    /**
     * 분류 키워드 삭제 (FN-ADM-050-05, DEL_YN = 'Y')
     */
    @Transactional
    public void deleteKeyword(Long actorSn, Long kwrdDictSn) {
        NewsKeyword before = newsMapper.selectKeywordById(kwrdDictSn);
        if (before == null || newsMapper.deleteKeyword(kwrdDictSn, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.NEWS_KEYWORD_NOT_FOUND);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.DELETE, "KWRD_DICT", kwrdDictSn, "뉴스 분류 키워드 삭제",
                new KeywordState(before.getMpngCd(), before.getKwrdCn()), null);
    }

    /**
     * 앞뒤 공백 제거 후 1~max 자 검사
     */
    private String normalize(String value, int maxLength) {
        String stripped = value == null ? "" : value.strip();
        if (stripped.isEmpty() || stripped.length() > maxLength) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return stripped;
    }

    private String requireCategory(String newsClsfCd) {
        if (newsClsfCd == null || !NewsCategory.exists(newsClsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return newsClsfCd;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
