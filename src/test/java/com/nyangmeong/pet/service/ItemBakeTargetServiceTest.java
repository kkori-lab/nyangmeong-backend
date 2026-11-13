package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.pet.dto.BakeTargetPageResponse;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 굽기 대상 캐릭터 페이지]
 * - FN-ADM-070-02
 */
class ItemBakeTargetServiceTest {

    private final ItemMapper itemMapper = mock(ItemMapper.class);
    private final PixelCharacterMapper pixelCharacterMapper = mock(PixelCharacterMapper.class);
    private final ItemService service = new ItemService(itemMapper, mock(FileStore.class), pixelCharacterMapper,
            mock(PixelCharacterService.class), mock(PetAuthorizer.class), mock(PointService.class));

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Page<Long> page(int pageNum, long total, List<Long> rows) {
        Page<Long> page = new Page<>(pageNum, 100);
        page.setTotal(total);
        page.addAll(rows);
        return page;
    }

    @Test
    void 관리자가_아니면_403() {
        assertThatThrownBy(() -> service.searchBakeTargets(false, 7L, 0))
                .extracting("errorCode").isEqualTo(ErrorCode.ADMIN_ONLY);
        verify(pixelCharacterMapper, never()).selectBakeTargets(anyLong());
    }

    @Test
    void 굽기_대상_아이템이_아니면_404() {
        when(itemMapper.countBakeItem(7L)).thenReturn(0);

        assertThatThrownBy(() -> service.searchBakeTargets(true, 7L, 0))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void 페이지가_음수면_400() {
        assertThatThrownBy(() -> service.searchBakeTargets(true, 7L, -1))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다음_페이지가_있으면_last_false() {
        when(itemMapper.countBakeItem(7L)).thenReturn(1);
        when(pixelCharacterMapper.selectBakeTargets(7L)).thenReturn(page(1, 150, List.of(1L, 2L)));

        BakeTargetPageResponse res = service.searchBakeTargets(true, 7L, 0);

        assertThat(res.pixelCharSns()).containsExactly(1L, 2L);
        assertThat(res.last()).isFalse();
    }

    @Test
    void 마지막_페이지면_last_true() {
        when(itemMapper.countBakeItem(7L)).thenReturn(1);
        when(pixelCharacterMapper.selectBakeTargets(7L)).thenReturn(page(2, 150, List.of(101L)));

        BakeTargetPageResponse res = service.searchBakeTargets(true, 7L, 1);

        assertThat(res.pixelCharSns()).containsExactly(101L);
        assertThat(res.last()).isTrue();
    }
}
