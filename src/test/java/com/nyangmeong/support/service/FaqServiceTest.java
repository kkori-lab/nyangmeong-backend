package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.support.dto.FaqSearchCond;
import com.nyangmeong.support.mapper.FaqMapper;

/**
 * [검사 - FAQ 목록]
 */
class FaqServiceTest {

    private final FaqMapper faqMapper = mock(FaqMapper.class);
    private final FaqService service = new FaqService(faqMapper);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    void 조건이_없으면_전체를_조회한다() {
        service.search(new FaqSearchCond(), new PageQuery(null, null, null));

        verify(faqMapper).selectList(null, null);
    }

    @Test
    void 검색어는_다듬어_넘기고_빈_값은_조건_없음() {
        FaqSearchCond cond = new FaqSearchCond();
        cond.setKeyword(" 비밀번호 ");
        service.search(cond, new PageQuery(null, null, null));
        verify(faqMapper).selectList(null, "비밀번호");

        cond.setKeyword("   ");
        service.search(cond, new PageQuery(null, null, null));
        verify(faqMapper).selectList(null, null);
    }

    @Test
    void 카테고리와_검색어를_함께_넘긴다() {
        FaqSearchCond cond = new FaqSearchCond();
        cond.setFaqClsfCd("CSCC0002");
        cond.setKeyword("비밀번호");

        service.search(cond, new PageQuery(null, null, null));

        verify(faqMapper).selectList("CSCC0002", "비밀번호");
    }

    @Test
    void 공백_카테고리는_조건_없음() {
        FaqSearchCond cond = new FaqSearchCond();
        cond.setFaqClsfCd("  ");

        service.search(cond, new PageQuery(null, null, null));

        verify(faqMapper).selectList(null, null);
    }

    @Test
    void 없는_카테고리_코드는_400() {
        FaqSearchCond cond = new FaqSearchCond();
        cond.setFaqClsfCd("XXXX");

        assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verifyNoInteractions(faqMapper);
    }
}
