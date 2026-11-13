package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.ItemPurchaseRequest;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.ShopItemListResponse;
import com.nyangmeong.pet.dto.ShopItemSummary;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 아이템 상점 목록·구매]
 */
class ItemServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CHAR = 3L;

    private final ItemMapper itemMapper = mock(ItemMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final PixelCharacterMapper pixelCharacterMapper = mock(PixelCharacterMapper.class);
    private final PixelCharacterService pixelCharacterService = mock(PixelCharacterService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final PointService pointService = mock(PointService.class);
    private final ItemService service = new ItemService(itemMapper, fileStore, pixelCharacterMapper,
            pixelCharacterService, petAuthorizer, pointService);

    private PixelCharacter character(String bscCharSbstYn) {
        PixelCharacter character = new PixelCharacter();
        character.setPixelCharSn(CHAR);
        character.setPetSn(PET);
        character.setMbrSn(MBR);
        character.setBscCharSbstYn(bscCharSbstYn);
        return character;
    }

    private ShopItemSummary item(Long itemSn, long needPnt, boolean hldYn) {
        ShopItemSummary item = new ShopItemSummary();
        item.setItemSn(itemSn);
        item.setItemNm("아이템" + itemSn);
        item.setItemRegnCd("MYPC0001");
        item.setNeedPnt(needPnt);
        item.setHldYn(hldYn);
        return item;
    }

    @Test
    void 상점_목록은_보유_포인트와_캐릭터_기준_보유_여부를_준다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        List<ShopItemSummary> rows = List.of(item(1L, 600, true), item(2L, 500, false));
        when(itemMapper.selectShopItems(CHAR, "MYPC0001")).thenReturn(rows);
        when(pointService.getHldPnt(MBR)).thenReturn(8_450);

        ShopItemListResponse res = service.searchShopItems(MBR, CHAR, "MYPC0001");

        assertThat(res.hldPnt()).isEqualTo(8_450L);
        assertThat(res.items()).isEqualTo(rows);
        verify(petAuthorizer).checkPrimary(MBR, PET);
    }

    @Test
    void 공동양육자는_상점_목록을_볼_수_없다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(MBR, PET);

        assertThatThrownBy(() -> service.searchShopItems(MBR, CHAR, null))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
        verify(itemMapper, never()).selectShopItems(anyLong(), any());
    }

    @Test
    void 없는_캐릭터는_PIXEL_CHARACTER_NOT_FOUND() {
        assertThatThrownBy(() -> service.searchShopItems(MBR, 99L, null))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
    }

    @Test
    void 담은_아이템을_한_번에_구매해_보관함에_넣는다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        List<ShopItemSummary> items = List.of(item(1L, 600, false), item(2L, 500, false));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(1L, 2L))).thenReturn(items);
        PixelCharacterResponse res = mock(PixelCharacterResponse.class);
        when(pixelCharacterService.getPixelCharacter(MBR, CHAR)).thenReturn(res);

        PixelCharacterResponse result = service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L, 2L, 1L)));

        assertThat(result).isSameAs(res);
        verify(petAuthorizer).checkPrimary(MBR, PET);
        verify(itemMapper).insertHeldItems(CHAR, items);
        verify(pointService).use(MBR, 1_100L, "req-1", "아이템 구매 · 아이템1, 아이템2");
    }

    @Test
    void 총액이_보유_포인트를_넘으면_POINT_NOT_ENOUGH_이고_저장하지_않는다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(1L, 2L)))
                .thenReturn(List.of(item(1L, 8_000, false), item(2L, 500, false)));
        doThrow(new CustomException(ErrorCode.POINT_NOT_ENOUGH)).when(pointService)
                .use(MBR, 8_500L, "req-1", "아이템 구매 · 아이템1, 아이템2");

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L, 2L))))
                .extracting("errorCode").isEqualTo(ErrorCode.POINT_NOT_ENOUGH);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 쓴_idmptDmndId로_다른_아이템을_사면_IDEMPOTENCY_KEY_REUSED_이고_저장하지_않는다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(2L))).thenReturn(List.of(item(2L, 500, false)));
        doThrow(new CustomException(ErrorCode.IDEMPOTENCY_KEY_REUSED)).when(pointService)
                .use(MBR, 500L, "req-1", "아이템 구매 · 아이템2");

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(2L))))
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 이미_보유한_아이템이_있으면_ITEM_ALREADY_HELD() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(1L, 2L)))
                .thenReturn(List.of(item(1L, 600, true), item(2L, 500, false)));

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L, 2L))))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_ALREADY_HELD);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 동시에_같은_아이템을_사서_유일_키가_충돌하면_ITEM_ALREADY_HELD() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(1L))).thenReturn(List.of(item(1L, 600, false)));
        when(itemMapper.insertHeldItems(anyLong(), any())).thenThrow(new DuplicateKeyException("UK_HLD_ITEM_1"));

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L))))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_ALREADY_HELD);
    }

    @Test
    void 없거나_판매_종료된_아이템이_있으면_ITEM_NOT_FOUND() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(itemMapper.selectShopItemsBySn(CHAR, List.of(1L, 2L))).thenReturn(List.of(item(1L, 600, false)));

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L, 2L))))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 오류_확인_중인_캐릭터는_구매할_수_없다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L))))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_UNDER_REVIEW);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 공동양육자는_구매할_수_없다() {
        when(pixelCharacterMapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(MBR, PET);

        assertThatThrownBy(() -> service.purchase(MBR, CHAR, new ItemPurchaseRequest("req-1", List.of(1L))))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
        verify(itemMapper, never()).insertHeldItems(anyLong(), any());
    }

    @Test
    void 구매_원장_메모는_1000자에서_자른다() {
        ShopItemSummary longItem = item(1L, 100, false);
        longItem.setItemNm("가".repeat(1_200));

        String memo = ItemService.purchaseMemo(List.of(longItem));

        assertThat(memo).hasSize(ItemService.MEMO_MAX_LEN).startsWith(ItemService.PURCHASE_MEMO_PREFIX);
    }
}
