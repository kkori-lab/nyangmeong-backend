package com.nyangmeong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.place.domain.PlaceBookmark;
import com.nyangmeong.place.dto.PlaceBookmarkCreateRequest;
import com.nyangmeong.place.dto.PlaceBookmarkResponse;
import com.nyangmeong.place.dto.PlaceBookmarkSummary;
import com.nyangmeong.place.mapper.PlaceBookmarkMapper;
import com.nyangmeong.place.mapper.PlaceMapper;

/**
 * [검사 - 장소 즐겨찾기 저장·해제·목록]
 */
class PlaceBookmarkServiceTest {

    private static final Long MBR = 1L;
    private static final Long PLC = 7L;
    private static final Long BKMK = 21L;
    private static final PlaceBookmarkCreateRequest REQ = new PlaceBookmarkCreateRequest(PLC);

    private final PlaceBookmarkMapper placeBookmarkMapper = mock(PlaceBookmarkMapper.class);
    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final PlaceBookmarkService service = new PlaceBookmarkService(placeBookmarkMapper, placeMapper);

    private PlaceBookmark bookmark(String useYn) {
        PlaceBookmark bookmark = new PlaceBookmark();
        bookmark.setPlcBkmkSn(BKMK);
        bookmark.setPlcSn(PLC);
        bookmark.setMbrSn(MBR);
        bookmark.setUseYn(useYn);
        return bookmark;
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 처음_저장하면_새_행을_만든다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<PlaceBookmark>getArgument(0).setPlcBkmkSn(BKMK);
            return 1;
        }).when(placeBookmarkMapper).insert(any(PlaceBookmark.class));

        PlaceBookmarkResponse res = service.create(MBR, REQ);

        assertThat(res.plcBkmkSn()).isEqualTo(BKMK);
        assertThat(res.plcSn()).isEqualTo(PLC);
    }

    @Test
    void 이미_즐겨찾기한_장소는_409다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(true);
        when(placeBookmarkMapper.selectByMemberAndPlace(MBR, PLC)).thenReturn(bookmark("Y"));

        assertError(() -> service.create(MBR, REQ), ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS);
        verify(placeBookmarkMapper, never()).updateUseYn(any(), any(), any(), any());
    }

    @Test
    void 해제한_장소를_다시_저장하면_같은_번호로_되살린다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(true);
        when(placeBookmarkMapper.selectByMemberAndPlace(MBR, PLC)).thenReturn(bookmark("N"));
        when(placeBookmarkMapper.updateUseYn(BKMK, MBR, "N", "Y")).thenReturn(1);

        PlaceBookmarkResponse res = service.create(MBR, REQ);

        assertThat(res.plcBkmkSn()).isEqualTo(BKMK);
        verify(placeBookmarkMapper, never()).insert(any());
    }

    @Test
    void 되살리는_사이_다른_요청이_먼저_되살렸으면_409다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(true);
        when(placeBookmarkMapper.selectByMemberAndPlace(MBR, PLC)).thenReturn(bookmark("N"));
        when(placeBookmarkMapper.updateUseYn(BKMK, MBR, "N", "Y")).thenReturn(0);

        assertError(() -> service.create(MBR, REQ), ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS);
    }

    @Test
    void 없거나_숨긴_장소는_404다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(false);

        assertError(() -> service.create(MBR, REQ), ErrorCode.PLACE_NOT_FOUND);
        verify(placeBookmarkMapper, never()).insert(any());
    }

    @Test
    void 동시_저장으로_중복키가_나면_409다() {
        when(placeMapper.existsUsablePlace(PLC)).thenReturn(true);
        doThrow(new DuplicateKeyException("UK_PLC_BKMK_1")).when(placeBookmarkMapper).insert(any(PlaceBookmark.class));

        assertError(() -> service.create(MBR, REQ), ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS);
    }

    @Test
    void 해제는_내_사용_중인_행만_N으로_바꾼다() {
        when(placeBookmarkMapper.updateUseYn(BKMK, MBR, "Y", "N")).thenReturn(1);

        service.delete(MBR, BKMK);

        verify(placeBookmarkMapper).updateUseYn(BKMK, MBR, "Y", "N");
    }

    @Test
    void 남의_것이나_없는_것이나_이미_해제한_것은_404다() {
        when(placeBookmarkMapper.updateUseYn(eq(BKMK), eq(MBR), eq("Y"), eq("N"))).thenReturn(0);

        assertError(() -> service.delete(MBR, BKMK), ErrorCode.PLACE_BOOKMARK_NOT_FOUND);
    }

    @Test
    void 목록은_그_카테고리로_조회하고_없으면_빈_목록이다() {
        when(placeBookmarkMapper.selectList(MBR, "PLCC0001")).thenReturn(List.of());

        List<PlaceBookmarkSummary> res = service.search(MBR, "PLCC0001");

        assertThat(res).isEmpty();
        verify(placeBookmarkMapper).selectList(MBR, "PLCC0001");
    }

    @Test
    void 목록의_없는_카테고리는_400이다() {
        assertError(() -> service.search(MBR, "XXXX"), ErrorCode.INVALID_INPUT_VALUE);
        verify(placeBookmarkMapper, never()).selectList(any(), any());
    }
}
