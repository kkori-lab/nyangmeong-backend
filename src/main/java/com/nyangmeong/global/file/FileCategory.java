package com.nyangmeong.global.file;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [파일 분류]
 * - 저장 경로의 폴더와 열람 범위를 정한다
 * - 회원 파일 : {회원번호}/{도메인}/{용도}/
 * - 반려동물 파일 : {주양육자 회원번호}/{도메인}/{반려동물번호}/{용도}/
 * - 서비스 파일 : assets/{용도}/
 * - SHARED 는 로그인 회원 누구나 GET /api/files/{atchFileSn} 으로 연다. OWNER 는 각 도메인 API 가 소유 검사 후 준다
 */
@Getter
@RequiredArgsConstructor
public enum FileCategory {

    MEMBER_PROFILE("member", "profile", Scope.SHARED, Owner.MEMBER, false),
    COMMUNITY_POST("community", "post", Scope.SHARED, Owner.MEMBER, false),
    COMMUNITY_CHAT("community", "chat", Scope.OWNER, Owner.MEMBER, false),
    SUPPORT_INQUIRY("support", "inquiry", Scope.OWNER, Owner.MEMBER, false),
    EXPENSE_RECEIPT("expense", "receipt", Scope.OWNER, Owner.MEMBER, false),
    HEALTH_CHECKUP("health", "checkup", Scope.OWNER, Owner.PET, true),
    HEALTH_OBSERVATION("health", "observation", Scope.OWNER, Owner.PET, false),
    PET_CHARACTER("pet", "character", Scope.SHARED, Owner.PET, false),
    PET_WEAR("pet", "wear", Scope.SHARED, Owner.PET, false),
    PET_SHEET("pet", "sheet", Scope.OWNER, Owner.PET, false),
    ASSET_ITEM(FileCategory.ASSET_DIR, "item", Scope.SHARED, Owner.SERVICE, false),
    ASSET_NOTICE(FileCategory.ASSET_DIR, "notice", Scope.SHARED, Owner.SERVICE, false),
    ASSET_NEWS(FileCategory.ASSET_DIR, "news", Scope.SHARED, Owner.SERVICE, false);

    /** 서비스 파일 최상위 폴더 */
    public static final String ASSET_DIR = "assets";

    private final String domainDir;
    private final String purposeDir;
    private final Scope scope;
    private final Owner owner;
    private final boolean pdfAllowed;

    /** 열람 범위 */
    public enum Scope {
        /** 로그인 회원 누구나 */
        SHARED,
        /** 도메인 API 로만 (소유 검사) */
        OWNER
    }

    /** 소유자 종류 */
    public enum Owner {
        MEMBER, PET, SERVICE
    }

    public boolean isShared() {
        return scope == Scope.SHARED;
    }

    /**
     * 도메인·용도 폴더로 분류 찾기
     */
    public static Optional<FileCategory> of(Owner owner, String domainDir, String purposeDir) {
        return Arrays.stream(values())
                .filter(value -> value.owner == owner && value.domainDir.equals(domainDir)
                        && value.purposeDir.equals(purposeDir))
                .findFirst();
    }
}
