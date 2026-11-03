package com.nyangmeong.support.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.support.domain.Faq;
import com.nyangmeong.support.domain.FaqCategory;
import com.nyangmeong.support.dto.AdminFaqCreateRequest;
import com.nyangmeong.support.dto.AdminFaqResponse;
import com.nyangmeong.support.dto.AdminFaqSearchCond;
import com.nyangmeong.support.dto.AdminFaqSummary;
import com.nyangmeong.support.dto.AdminFaqUpdateRequest;
import com.nyangmeong.support.mapper.FaqMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 FAQ 서비스]
 * - SCR-ADM-041 (FN-ADM-041-01~04)
 * - 삭제 없음 : USE_YN='N' 비노출
 * - 정렬 순서는 중복 허용, 다른 FAQ 를 자동으로 밀지 않음 (같으면 번호순)
 */
@Service
@RequiredArgsConstructor
public class AdminFaqService {

    private static final String TARGET_TABLE = "FAQ";
    private static final int PAGE_SIZE = 20;

    private final FaqMapper faqMapper;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record FaqState(String faqClsfCd, String qstnTtl, Integer sortSeq, String useYn) {
        static FaqState of(Faq faq) {
            return new FaqState(faq.getFaqClsfCd(), faq.getQstnTtl(), faq.getSortSeq(), faq.getUseYn());
        }
    }

    /**
     * 관리자 FAQ 목록 조회 (비노출 포함, 카테고리·키워드는 AND)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminFaqSummary> search(AdminFaqSearchCond cond, PageQuery pq) {
        String faqClsfCd = hasText(cond.getFaqClsfCd()) ? cond.getFaqClsfCd().strip() : null;
        if (faqClsfCd != null) {
            validateCategory(faqClsfCd);
        }
        String keyword = hasText(cond.getKeyword()) ? escapeLike(cond.getKeyword().strip()) : null;
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<AdminFaqSummary> rows = faqMapper.selectAdminList(faqClsfCd, keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 FAQ 상세 조회 (사용 여부 무관)
     */
    @Transactional(readOnly = true)
    public AdminFaqResponse get(Long faqSn) {
        return AdminFaqResponse.from(getOrThrow(faqSn));
    }

    /**
     * FAQ 등록
     */
    @Transactional
    public AdminFaqResponse create(Long actorSn, AdminFaqCreateRequest req) {
        String faqClsfCd = req.faqClsfCd().strip();
        validateCategory(faqClsfCd);
        Faq faq = new Faq();
        faq.setFaqClsfCd(faqClsfCd);
        faq.setQstnTtl(req.qstnTtl().strip());
        faq.setAnsCn(req.ansCn());
        faq.setSortSeq(req.sortSeq());
        faq.setUseYn(toYn(req.useYn()));
        faq.setRgtrId(String.valueOf(actorSn));
        faq.setMdfrId(String.valueOf(actorSn));
        faqMapper.insert(faq);

        Faq saved = getOrThrow(faq.getFaqSn());
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, saved.getFaqSn(), "FAQ 등록", null,
                FaqState.of(saved));
        return AdminFaqResponse.from(saved);
    }

    /**
     * FAQ 수정·비노출 (보낸 값만 바꾼다)
     */
    @Transactional
    public AdminFaqResponse update(Long actorSn, Long faqSn, AdminFaqUpdateRequest req) {
        Faq before = getOrThrow(faqSn);
        String faqClsfCd = req.faqClsfCd() == null ? null : req.faqClsfCd().strip();
        if (faqClsfCd != null) {
            validateCategory(faqClsfCd);
        }
        Faq faq = new Faq();
        faq.setFaqSn(faqSn);
        faq.setFaqClsfCd(faqClsfCd);
        faq.setQstnTtl(req.qstnTtl() == null ? null : req.qstnTtl().strip());
        faq.setAnsCn(req.ansCn());
        faq.setSortSeq(req.sortSeq());
        faq.setUseYn(req.useYn() == null ? null : toYn(req.useYn()));
        faq.setMdfrId(String.valueOf(actorSn));
        faqMapper.update(faq);

        Faq after = getOrThrow(faqSn);
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, faqSn, "FAQ 수정", FaqState.of(before),
                FaqState.of(after));
        return AdminFaqResponse.from(after);
    }

    private void validateCategory(String faqClsfCd) {
        if (!FaqCategory.exists(faqClsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Faq getOrThrow(Long faqSn) {
        Faq faq = faqMapper.selectAdminFaq(faqSn);
        if (faq == null) {
            throw new CustomException(ErrorCode.FAQ_NOT_FOUND);
        }
        return faq;
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
