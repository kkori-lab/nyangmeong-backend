package com.nyangmeong.pet.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.domain.CharacterItemSheet;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.BakeItemSummary;
import com.nyangmeong.pet.dto.CharacterSheetItem;
import com.nyangmeong.pet.dto.CharacterSheetsResponse;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [캐릭터 시트 서비스]
 * - 합성용 맨몸·착용 시트 조회와 굽기 결과 저장 (FN-MYP-011-01, FN-MYP-010-13)
 * - 시트는 안 산 아이템도 대상 : 보유(HLD_ITEM)와 무관
 */
@Service
@RequiredArgsConstructor
public class CharacterSheetService {

    private final PixelCharacterMapper pixelCharacterMapper;
    private final ItemMapper itemMapper;
    private final PetAuthorizer petAuthorizer;
    private final FileStore fileStore;

    /**
     * 굽기 대상 아이템 조회
     */
    @Transactional(readOnly = true)
    public List<BakeItemSummary> searchBakeItems() {
        return itemMapper.selectBakeItems();
    }

    /**
     * 착용 시트 저장
     */
    @Transactional
    public void saveItemSheet(Long mbrSn, boolean admin, Long pixelCharSn, Long itemSn, MultipartFile sheet) {
        PixelCharacter character = checkBakeTarget(mbrSn, admin, pixelCharSn, itemSn);
        byte[] content = readSheet(sheet);
        Long atchFileSn = fileStore.store(content, FileExtension.PNG,
                "sheet-" + pixelCharSn + "-" + itemSn + ".png", FileCategory.PET_SHEET,
                FileOwner.pet(character.getMbrSn(), character.getPetSn()), String.valueOf(mbrSn));
        pixelCharacterMapper.upsertItemSheet(pixelCharSn, itemSn, atchFileSn);
    }

    /**
     * 굽기 실패 표시
     */
    @Transactional
    public void failItemSheet(Long mbrSn, boolean admin, Long pixelCharSn, Long itemSn) {
        checkBakeTarget(mbrSn, admin, pixelCharSn, itemSn);
        pixelCharacterMapper.upsertItemSheetFailure(pixelCharSn, itemSn);
    }

    /**
     * 합성용 시트 정보 조회
     */
    @Transactional(readOnly = true)
    public CharacterSheetsResponse getSheets(Long mbrSn, boolean admin, Long pixelCharSn, List<Long> itemSns) {
        PixelCharacter character = getPrimaryCharacter(mbrSn, admin, pixelCharSn);
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(character.getPetSn());
        if (character.getBodySheetFileSn() == null || pet == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        List<Long> wanted = itemSns == null ? List.of() : itemSns.stream().distinct().toList();
        return CharacterSheetsResponse.of(pet.getPetSeCd(), character, toItems(pixelCharSn, wanted));
    }

    /**
     * 시트 파일 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getSheetFile(Long mbrSn, boolean admin, Long pixelCharSn, Long atchFileSn) {
        PixelCharacter character = getPrimaryCharacter(mbrSn, admin, pixelCharSn);
        if (!atchFileSn.equals(character.getBodySheetFileSn())
                && pixelCharacterMapper.countItemSheetFile(pixelCharSn, atchFileSn) == 0) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    /**
     * 시트 PNG 내용 (규격 밖 -> FILE_TYPE_NOT_ALLOWED)
     */
    static byte[] readSheet(MultipartFile sheet) {
        byte[] content = readBytes(sheet);
        if (!CharacterSpec.matchesSheet(content)) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return content;
    }

    /**
     * 업로드 파일 내용
     */
    static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 요청 아이템별 시트 : 안 구운 아이템은 빈 값
     */
    private List<CharacterSheetItem> toItems(Long pixelCharSn, List<Long> itemSns) {
        if (itemSns.isEmpty()) {
            return List.of();
        }
        Map<Long, CharacterItemSheet> sheets = pixelCharacterMapper.selectItemSheets(pixelCharSn, itemSns).stream()
                .collect(Collectors.toMap(CharacterItemSheet::getItemSn, Function.identity()));
        return itemSns.stream()
                .map(itemSn -> sheets.containsKey(itemSn)
                        ? CharacterSheetItem.from(sheets.get(itemSn))
                        : CharacterSheetItem.notBaked(itemSn))
                .toList();
    }

    /**
     * 굽기 결과를 받을 수 있는 캐릭터·아이템인지 검사
     */
    private PixelCharacter checkBakeTarget(Long mbrSn, boolean admin, Long pixelCharSn, Long itemSn) {
        PixelCharacter character = getPrimaryCharacter(mbrSn, admin, pixelCharSn);
        if (itemMapper.countBakeItem(itemSn) == 0) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        return character;
    }

    /**
     * 주양육자 캐릭터 조회
     * - 관리자는 전체 굽기(FN-ADM-070-02)로 남의 캐릭터 시트도 읽고 쓴다
     */
    private PixelCharacter getPrimaryCharacter(Long mbrSn, boolean admin, Long pixelCharSn) {
        PixelCharacter character = pixelCharacterMapper.selectPixelCharacter(pixelCharSn);
        if (character == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        if (!admin) {
            petAuthorizer.checkPrimary(mbrSn, character.getPetSn());
        }
        return character;
    }
}
