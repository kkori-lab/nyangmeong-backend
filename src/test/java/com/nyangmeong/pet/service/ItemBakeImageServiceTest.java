package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 굽기 참고용 아이템 그림]
 * - FN-MYP-011-01, FN-ADM-070-02
 */
class ItemBakeImageServiceTest {

    private final ItemMapper itemMapper = mock(ItemMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final ItemService service = new ItemService(itemMapper, fileStore, mock(PixelCharacterMapper.class),
            mock(PixelCharacterService.class), mock(PetAuthorizer.class), mock(PointService.class));

    @Test
    void 굽기_대상이면_아이템_그림을_준다() {
        StoredFile file = new StoredFile(new byte[] {1, 2, 3}, "image/png", "hat.png");
        when(itemMapper.countBakeItem(7L)).thenReturn(1);
        when(itemMapper.selectAtchFileSn(7L)).thenReturn(50L);
        when(fileStore.load(50L)).thenReturn(file);

        assertThat(service.getBakeImage(7L)).isSameAs(file);
    }

    @Test
    void 굽기_대상이_아니면_404이고_파일을_읽지_않는다() {
        when(itemMapper.countBakeItem(7L)).thenReturn(0);

        assertThatThrownBy(() -> service.getBakeImage(7L))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        verify(fileStore, never()).load(anyLong());
    }
}
