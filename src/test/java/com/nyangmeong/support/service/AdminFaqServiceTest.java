package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.support.domain.Faq;
import com.nyangmeong.support.dto.AdminFaqCreateRequest;
import com.nyangmeong.support.dto.AdminFaqResponse;
import com.nyangmeong.support.dto.AdminFaqSearchCond;
import com.nyangmeong.support.dto.AdminFaqUpdateRequest;
import com.nyangmeong.support.mapper.FaqMapper;

/**
 * [검사 - 관리자 FAQ 목록·등록·수정·비노출]
 */
class AdminFaqServiceTest {

    private final FaqMapper faqMapper = mock(FaqMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminFaqService service = new AdminFaqService(faqMapper, adminTaskLogService);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Faq stored(Long sn, String useYn) {
        Faq faq = new Faq();
        faq.setFaqSn(sn);
        faq.setFaqClsfCd("CSCC0002");
        faq.setQstnTtl("비밀번호를 잊었어요");
        faq.setAnsCn("답변");
        faq.setSortSeq(1);
        faq.setUseYn(useYn);
        return faq;
    }

    private AdminFaqSearchCond cond(String faqClsfCd, String keyword) {
        AdminFaqSearchCond cond = new AdminFaqSearchCond();
        cond.setFaqClsfCd(faqClsfCd);
        cond.setKeyword(keyword);
        return cond;
    }

    @Test
    void 목록은_카테고리와_검색어를_다듬어_넘긴다() {
        service.search(cond(" CSCC0002 ", " 50%_할인 "), new PageQuery(null, null, null));

        verify(faqMapper).selectAdminList("CSCC0002", "50\\%\\_할인");
    }

    @Test
    void 목록에서_공백_조건은_전체다() {
        service.search(cond("  ", ""), new PageQuery(null, null, null));

        verify(faqMapper).selectAdminList(isNull(), isNull());
    }

    @Test
    void 목록에서_없는_카테고리는_400() {
        assertThatThrownBy(() -> service.search(cond("CSCC9999", null), new PageQuery(null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(faqMapper, never()).selectAdminList(any(), any());
    }

    @Test
    void 없는_FAQ_상세는_404() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FAQ_NOT_FOUND);
    }

    @Test
    void 등록은_관리자_번호를_남기고_감사_로그를_쓴다() {
        doAnswer(invocation -> {
            ((Faq) invocation.getArgument(0)).setFaqSn(4L);
            return 1;
        }).when(faqMapper).insert(any());
        when(faqMapper.selectAdminFaq(4L)).thenReturn(stored(4L, "N"));

        AdminFaqResponse res = service.create(9L,
                new AdminFaqCreateRequest("CSCC0002", " 비밀번호를 잊었어요 ", "답변", 1, false));

        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);
        verify(faqMapper).insert(captor.capture());
        Faq saved = captor.getValue();
        assertThat(saved.getQstnTtl()).isEqualTo("비밀번호를 잊었어요");
        assertThat(saved.getUseYn()).isEqualTo("N");
        assertThat(saved.getRgtrId()).isEqualTo("9");
        assertThat(saved.getMdfrId()).isEqualTo("9");
        assertThat(res.faqSn()).isEqualTo(4L);
        assertThat(res.useYn()).isFalse();
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.CREATE), eq("FAQ"), eq(4L), any(), isNull(),
                any());
    }

    @Test
    void 등록에서_없는_카테고리는_400이고_저장하지_않는다() {
        assertThatThrownBy(() -> service.create(9L, new AdminFaqCreateRequest("PETC0001", "질문", "답변", 0, true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(faqMapper, never()).insert(any());
    }

    @Test
    void 비노출은_사용_여부만_바꾼다() {
        when(faqMapper.selectAdminFaq(4L)).thenReturn(stored(4L, "Y"), stored(4L, "N"));

        AdminFaqResponse res = service.update(9L, 4L, new AdminFaqUpdateRequest(null, null, null, null, false));

        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);
        verify(faqMapper).update(captor.capture());
        Faq param = captor.getValue();
        assertThat(param.getUseYn()).isEqualTo("N");
        assertThat(param.getQstnTtl()).isNull();
        assertThat(param.getFaqClsfCd()).isNull();
        assertThat(param.getSortSeq()).isNull();
        assertThat(param.getMdfrId()).isEqualTo("9");
        assertThat(res.useYn()).isFalse();
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.UPDATE), eq("FAQ"), eq(4L), any(), any(), any());
    }

    @Test
    void 없는_FAQ_수정은_404() {
        assertThatThrownBy(() -> service.update(9L, 99L, new AdminFaqUpdateRequest(null, null, null, null, false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FAQ_NOT_FOUND);
        verify(faqMapper, never()).update(any());
    }

    @Test
    void 수정에서_없는_카테고리는_400() {
        when(faqMapper.selectAdminFaq(anyLong())).thenReturn(stored(4L, "Y"));

        assertThatThrownBy(() -> service.update(9L, 4L, new AdminFaqUpdateRequest("CSCC9999", null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(faqMapper, never()).update(any());
    }
}
