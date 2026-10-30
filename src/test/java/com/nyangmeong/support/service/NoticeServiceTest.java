package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.support.domain.Notice;
import com.nyangmeong.support.dto.NoticeResponse;
import com.nyangmeong.support.dto.NoticeSearchCond;
import com.nyangmeong.support.mapper.NoticeMapper;

/**
 * [검사 - 공지사항 목록·상세]
 */
class NoticeServiceTest {

    private final NoticeMapper noticeMapper = mock(NoticeMapper.class);
    private final NoticeService service = new NoticeService(noticeMapper);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    void 목록은_검색어를_다듬어_넘기고_빈_값은_전체로_본다() {
        NoticeSearchCond cond = new NoticeSearchCond();
        cond.setKeyword(" 점검 ");
        service.search(cond, new PageQuery(null, null, null));
        verify(noticeMapper).selectList("점검", false);

        cond.setKeyword("   ");
        service.search(cond, new PageQuery(null, null, null));
        verify(noticeMapper).selectList(null, false);
    }

    @Test
    void 검색어가_없으면_조건_없이_조회한다() {
        service.search(new NoticeSearchCond(), new PageQuery(null, null, null));

        verify(noticeMapper).selectList(null, false);
    }

    @Test
    void latestYn이_true면_최신순으로_조회한다() {
        NoticeSearchCond cond = new NoticeSearchCond();
        cond.setLatestYn(true);

        service.search(cond, new PageQuery(null, null, null));

        verify(noticeMapper).selectList(null, true);
    }

    @Test
    void 상세는_제목_본문_작성일을_돌려준다() {
        Notice notice = new Notice();
        notice.setNtcMttrSn(5L);
        notice.setNtcMttrTtl("점검 안내");
        notice.setNtcMttrCn("본문");
        notice.setInqCnt(12);
        notice.setRegDt(LocalDateTime.of(2026, 10, 1, 9, 0));
        when(noticeMapper.selectNotice(5L)).thenReturn(notice);

        NoticeResponse res = service.getNotice(5L);

        assertThat(res.ntcMttrSn()).isEqualTo(5L);
        assertThat(res.ntcMttrTtl()).isEqualTo("점검 안내");
        assertThat(res.ntcMttrCn()).isEqualTo("본문");
        assertThat(res.regDt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    @Test
    void 없는_공지는_404() {
        when(noticeMapper.selectNotice(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.getNotice(9L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND));
    }
}
