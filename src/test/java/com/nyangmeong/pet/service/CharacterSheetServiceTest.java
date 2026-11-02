package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.domain.CharacterItemSheet;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.CharacterSheetItem;
import com.nyangmeong.pet.dto.CharacterSheetsResponse;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 캐릭터 시트 저장·조회]
 */
class CharacterSheetServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CHAR = 3L;

    private final PixelCharacterMapper mapper = mock(PixelCharacterMapper.class);
    private final ItemMapper itemMapper = mock(ItemMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final CharacterSheetService service = new CharacterSheetService(mapper, itemMapper, petAuthorizer,
            fileStore);

    private MockMultipartFile sheet(int size) {
        return new MockMultipartFile("sheet",
                CharacterSpec.toPng(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)));
    }

    private PixelCharacter character(Long bodySheetFileSn) {
        PixelCharacter character = new PixelCharacter();
        character.setPixelCharSn(CHAR);
        character.setPetSn(PET);
        character.setBodySheetFileSn(bodySheetFileSn);
        character.setSheetScale(1.5);
        character.setSheetOriginX(2.0);
        character.setSheetOriginY(3.0);
        return character;
    }

    private CharacterItemSheet itemSheet(Long itemSn, Long atchFileSn, String bakeFailYn) {
        CharacterItemSheet sheet = new CharacterItemSheet();
        sheet.setItemSn(itemSn);
        sheet.setAtchFileSn(atchFileSn);
        sheet.setBakeFailYn(bakeFailYn);
        return sheet;
    }

    private void givenCharacter(Long bodySheetFileSn) {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character(bodySheetFileSn));
        PetProfileSummary pet = new PetProfileSummary();
        pet.setPetSeCd("PETC0001");
        when(mapper.selectPetProfile(PET)).thenReturn(pet);
    }

    @Test
    void 착용_시트를_비공개로_저장한다() {
        givenCharacter(50L);
        when(itemMapper.countBakeItem(7L)).thenReturn(1);
        when(fileStore.store(any(byte[].class), eq(FileExtension.PNG), anyString(), eq("1"))).thenReturn(60L);

        service.saveItemSheet(MBR, CHAR, 7L, sheet(1024));

        verify(petAuthorizer).checkPrimary(MBR, PET);
        verify(mapper).upsertItemSheet(CHAR, 7L, 60L);
    }

    @Test
    void 시트가_1024_PNG가_아니면_FILE_TYPE_NOT_ALLOWED() {
        givenCharacter(50L);
        when(itemMapper.countBakeItem(7L)).thenReturn(1);

        assertThatThrownBy(() -> service.saveItemSheet(MBR, CHAR, 7L, sheet(512)))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        verify(mapper, never()).upsertItemSheet(anyLong(), anyLong(), anyLong());
    }

    @Test
    void 굽기_대상이_아닌_아이템은_ITEM_NOT_FOUND() {
        givenCharacter(50L);

        assertThatThrownBy(() -> service.saveItemSheet(MBR, CHAR, 7L, sheet(1024)))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        assertThatThrownBy(() -> service.failItemSheet(MBR, CHAR, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void 없는_캐릭터는_PIXEL_CHARACTER_NOT_FOUND() {
        assertThatThrownBy(() -> service.failItemSheet(MBR, CHAR, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
    }

    @Test
    void 굽기_실패를_표시한다() {
        givenCharacter(50L);
        when(itemMapper.countBakeItem(7L)).thenReturn(1);

        service.failItemSheet(MBR, CHAR, 7L);

        verify(mapper).upsertItemSheetFailure(CHAR, 7L);
    }

    @Test
    void 공동양육자는_시트를_저장할_수_없다() {
        givenCharacter(50L);
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(2L, PET);

        assertThatThrownBy(() -> service.saveItemSheet(2L, CHAR, 7L, sheet(1024)))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
    }

    @Test
    void 시트_정보는_구운_것_실패_안_구운_것을_요청_순서로_준다() {
        givenCharacter(50L);
        when(mapper.selectItemSheets(CHAR, List.of(7L, 8L, 9L)))
                .thenReturn(List.of(itemSheet(8L, null, "Y"), itemSheet(7L, 60L, "N")));

        CharacterSheetsResponse res = service.getSheets(MBR, CHAR, List.of(7L, 8L, 9L, 7L));

        assertThat(res.petSeCd()).isEqualTo("PETC0001");
        assertThat(res.sheetFileSn()).isEqualTo(50L);
        assertThat(res.scale()).isEqualTo(1.5);
        assertThat(res.items()).containsExactly(new CharacterSheetItem(7L, 60L, false),
                new CharacterSheetItem(8L, null, true), new CharacterSheetItem(9L, null, false));
    }

    @Test
    void 아이템을_안_고르면_맨몸_정보만_준다() {
        givenCharacter(50L);

        CharacterSheetsResponse res = service.getSheets(MBR, CHAR, null);

        assertThat(res.items()).isEmpty();
        verify(mapper, never()).selectItemSheets(any(), any());
    }

    @Test
    void 맨몸_시트가_없으면_PIXEL_CHARACTER_NOT_FOUND() {
        givenCharacter(null);

        assertThatThrownBy(() -> service.getSheets(MBR, CHAR, List.of()))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
    }

    @Test
    void 맨몸_시트_파일을_내려받는다() {
        givenCharacter(50L);
        StoredFile file = new StoredFile(new byte[] {1}, "image/png", "sheet.png");
        when(fileStore.load(50L)).thenReturn(file);

        assertThat(service.getSheetFile(MBR, CHAR, 50L)).isSameAs(file);
    }

    @Test
    void 착용_시트_파일을_내려받는다() {
        givenCharacter(50L);
        StoredFile file = new StoredFile(new byte[] {1}, "image/png", "sheet.png");
        when(mapper.countItemSheetFile(CHAR, 60L)).thenReturn(1);
        when(fileStore.load(60L)).thenReturn(file);

        assertThat(service.getSheetFile(MBR, CHAR, 60L)).isSameAs(file);
    }

    @Test
    void 그_캐릭터의_시트가_아니면_FILE_NOT_FOUND() {
        givenCharacter(50L);

        assertThatThrownBy(() -> service.getSheetFile(MBR, CHAR, 99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 남의_캐릭터_시트는_볼_수_없다() {
        givenCharacter(50L);
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(2L, PET);

        assertThatThrownBy(() -> service.getSheets(2L, CHAR, List.of()))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
        assertThatThrownBy(() -> service.getSheetFile(2L, CHAR, 50L))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
    }
}
