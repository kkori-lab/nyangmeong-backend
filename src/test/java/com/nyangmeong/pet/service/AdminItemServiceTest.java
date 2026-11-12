package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.pet.domain.Item;
import com.nyangmeong.pet.dto.AdminItemCreateRequest;
import com.nyangmeong.pet.dto.AdminItemOrderRequest;
import com.nyangmeong.pet.dto.AdminItemUpdateRequest;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 관리자 아이템 등록·수정·순서]
 */
class AdminItemServiceTest {

    private final ItemMapper itemMapper = mock(ItemMapper.class);
    private final PixelCharacterMapper pixelCharacterMapper = mock(PixelCharacterMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminItemService service = new AdminItemService(itemMapper, pixelCharacterMapper, fileStore,
            adminTaskLogService);

    private final MockMultipartFile image = new MockMultipartFile("image", "hat.png", "image/png", new byte[] {1});

    private Item stored(String regnCd, String bakeHint) {
        Item item = new Item();
        item.setItemSn(7L);
        item.setItemRegnCd(regnCd);
        item.setItemNm("밀짚모자");
        item.setNeedPnt(500L);
        item.setSortSeq(3L);
        item.setNtslYn("Y");
        item.setBakeHint(bakeHint);
        item.setAtchFileSn(11L);
        return item;
    }

    private AdminItemUpdateRequest update(String regnCd, String bakeHint, Boolean ntslYn) {
        return new AdminItemUpdateRequest(regnCd, null, null, ntslYn, bakeHint, null, null);
    }

    @Test
    void 등록하면_부위_마지막_다음_순서로_넣는다() {
        when(fileStore.store(any(), anyString())).thenReturn(11L);
        when(itemMapper.selectMaxSortSeq("MYPC0001")).thenReturn(5L);

        service.create(1L, new AdminItemCreateRequest("MYPC0001", " 새 모자 ", 300L, true, " a hat ", null, image));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        Item item = captor.getValue();
        assertThat(item.getSortSeq()).isEqualTo(6L);
        assertThat(item.getItemNm()).isEqualTo("새 모자");
        assertThat(item.getNtslYn()).isEqualTo("Y");
        assertThat(item.getBakeHint()).isEqualTo("a hat");
        assertThat(item.getAtchFileSn()).isEqualTo(11L);
    }

    @Test
    void 같은_이름이면_409() {
        when(itemMapper.insert(any())).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(
                () -> service.create(1L, new AdminItemCreateRequest("MYPC0001", "밀짚모자", 300L, true, null, null, image)))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NAME_DUPLICATED);
    }

    @Test
    void 신발에_굽기_지시를_주면_400() {
        assertThatThrownBy(
                () -> service.create(1L, new AdminItemCreateRequest("MYPC0006", "장화", 300L, true, "boots", null, image)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(itemMapper, never()).insert(any());
    }

    @Test
    void 신발은_사용하지_않는_부위라_등록하면_400() {
        assertThatThrownBy(
                () -> service.create(1L, new AdminItemCreateRequest("MYPC0006", "장화", 300L, true, null, null, image)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(itemMapper, never()).insert(any());
    }

    @Test
    void 신발로_부위를_바꾸면_400() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", null));

        assertThatThrownBy(() -> service.update(1L, 7L, update("MYPC0006", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(itemMapper, never()).update(any(), anyBoolean());
    }

    @Test
    void 신발_부위로_목록을_조회하면_400() {
        assertThatThrownBy(() -> service.search("MYPC0006"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(itemMapper, never()).selectAdminList(any());
    }

    @Test
    void 없는_부위면_400() {
        assertThatThrownBy(
                () -> service.create(1L, new AdminItemCreateRequest("MYPC9999", "모자", 300L, true, null, null, image)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 보유_회원이_있으면_부위를_바꿀_수_없다() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", null));
        when(itemMapper.countHolders(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service.update(1L, 7L, update("MYPC0002", null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_REGION_LOCKED);
        verify(itemMapper, never()).update(any(), anyBoolean());
    }

    @Test
    void 부위를_바꾸면_새_부위_마지막_다음_순서() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", null));
        when(itemMapper.selectMaxSortSeq("MYPC0002")).thenReturn(9L);

        service.update(1L, 7L, update("MYPC0002", null, null));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).update(captor.capture(), eq(false));
        assertThat(captor.getValue().getItemRegnCd()).isEqualTo("MYPC0002");
        assertThat(captor.getValue().getSortSeq()).isEqualTo(10L);
    }

    @Test
    void 판매_중지는_보유를_건드리지_않는다() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", "a hat"));

        service.update(1L, 7L, update(null, null, false));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).update(captor.capture(), eq(false));
        assertThat(captor.getValue().getNtslYn()).isEqualTo("N");
        verify(pixelCharacterMapper, never()).updateItemSheetDeleteByItemSn(anyLong());
    }

    @Test
    void 굽기_지시가_바뀌면_착용_시트를_지운다() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", "a hat"));

        service.update(1L, 7L, update(null, "a new hat", null));

        verify(itemMapper).update(any(), eq(true));
        verify(pixelCharacterMapper).updateItemSheetDeleteByItemSn(7L);
    }

    @Test
    void 굽기_지시를_빈_문자열로_보내면_지운다() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", "a hat"));

        service.update(1L, 7L, update(null, "", null));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).update(captor.capture(), eq(true));
        assertThat(captor.getValue().getBakeHint()).isNull();
        verify(pixelCharacterMapper).updateItemSheetDeleteByItemSn(7L);
    }

    @Test
    void 같은_굽기_지시면_시트를_지우지_않는다() {
        when(itemMapper.selectItem(7L)).thenReturn(stored("MYPC0001", "a hat"));

        service.update(1L, 7L, update(null, "a hat", null));

        verify(itemMapper).update(any(), eq(false));
        verify(pixelCharacterMapper, never()).updateItemSheetDeleteByItemSn(anyLong());
    }

    @Test
    void 없는_아이템_수정은_404() {
        assertThatThrownBy(() -> service.update(1L, 7L, update(null, null, false)))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void 순서는_부위_아이템_전부를_받아_1부터_매긴다() {
        when(itemMapper.selectItemSnsByRegion("MYPC0001")).thenReturn(List.of(1L, 2L, 3L));

        service.saveOrder(1L, new AdminItemOrderRequest("MYPC0001", List.of(3L, 1L, 2L)));

        verify(itemMapper).updateSortSeq(3L, 1L, "1");
        verify(itemMapper).updateSortSeq(1L, 2L, "1");
        verify(itemMapper).updateSortSeq(2L, 3L, "1");
    }

    @Test
    void 순서에_빠짐_중복_남의_부위가_있으면_400() {
        when(itemMapper.selectItemSnsByRegion("MYPC0001")).thenReturn(List.of(1L, 2L, 3L));

        for (List<Long> itemSns : List.of(List.of(1L, 2L), List.of(1L, 1L, 2L), List.of(1L, 2L, 9L))) {
            assertThatThrownBy(() -> service.saveOrder(1L, new AdminItemOrderRequest("MYPC0001", itemSns)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
        verify(itemMapper, never()).updateSortSeq(anyLong(), anyLong(), anyString());
    }

    @Test
    void 그림_참고_굽기로_등록하면_굽기_지시_없이_Y로_넣는다() {
        when(fileStore.store(any(), anyString())).thenReturn(11L);

        service.create(1L, new AdminItemCreateRequest("MYPC0001", "거북이 가방", 300L, true, null, true, image));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        assertThat(captor.getValue().getBakeImgRefYn()).isEqualTo("Y");
        assertThat(captor.getValue().getBakeHint()).isNull();
    }

    @Test
    void 그림_참고_굽기를_안_보내면_N으로_등록한다() {
        when(fileStore.store(any(), anyString())).thenReturn(11L);

        service.create(1L, new AdminItemCreateRequest("MYPC0001", "털모자", 300L, true, "a fur hat", null, image));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        assertThat(captor.getValue().getBakeImgRefYn()).isEqualTo("N");
    }
}
