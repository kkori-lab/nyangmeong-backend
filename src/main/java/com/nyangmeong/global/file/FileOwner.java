package com.nyangmeong.global.file;

import com.nyangmeong.global.file.FileCategory.Owner;

/**
 * [파일 소유자]
 * - member : 올린 회원의 폴더
 * - pet : 주양육자 회원의 폴더 아래 반려동물 폴더 (공동양육자가 올려도 같은 폴더, 올린 사람은 ATCH_FILE.RGTR_ID)
 * - service : 회원 폴더 없음 (assets/)
 */
public record FileOwner(Owner type, Long mbrSn, Long petSn) {

    public static FileOwner member(Long mbrSn) {
        return new FileOwner(Owner.MEMBER, mbrSn, null);
    }

    public static FileOwner pet(Long mbrSn, Long petSn) {
        return new FileOwner(Owner.PET, mbrSn, petSn);
    }

    public static FileOwner service() {
        return new FileOwner(Owner.SERVICE, null, null);
    }
}
