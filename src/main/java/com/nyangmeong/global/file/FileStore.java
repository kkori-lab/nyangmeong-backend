package com.nyangmeong.global.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileCategory.Owner;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [파일 저장·읽기]
 * - 디스크(FileRoot) + ATCH_FILE 행, 확장자 jpg·jpeg·png·webp(검진 첨부만 pdf 추가) + 실제 형식, 크기 상한 SYS_STNG.FILE_MAX_SZ
 * - 저장 경로와 열람 범위는 FileCategory 가 정한다 (회원 > 도메인 > 용도, 서비스 파일은 assets/)
 * - SHARED 분류는 로그인 회원 누구나 연다(loadShared). OWNER 분류는 각 도메인 API 가 소유 검사 후 준다(load)
 * - 트랜잭션 롤백 시 디스크 파일도 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStore {

    /** 기존 공개 파일의 저장 경로 접두어 (ATCH_FILE.PHYS_STRG_PATH 는 루트를 뺀 상대 경로). 새 파일은 쓰지 않는다 */
    public static final String PUBLIC_PREFIX = "public/";

    private static final int ORGNL_FILE_NM_MAX = 200;
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final AtchFileMapper atchFileMapper;
    private final FileRoot fileRoot;
    private final SystemSettingService systemSettingService;

    /**
     * 업로드 파일 저장
     * - pdf 는 분류가 허용할 때만 (REQ-NFR-003 : 검진 첨부 자료만)
     */
    public Long store(MultipartFile file, FileCategory category, FileOwner owner, String actorId) {
        String dir = dir(category, owner);
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        FileExtension extension = FileExtension.ofFileName(file.getOriginalFilename())
                .filter(value -> category.isPdfAllowed() || value.isImage())
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED));
        if (file.getSize() > systemSettingService.get().getFileMaxSz()) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!extension.matchesContent(content)) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return save(content, extension, file.getOriginalFilename(), actorId, dir);
    }

    /**
     * 서버 생성 파일 저장
     */
    public Long store(byte[] content, FileExtension extension, String orgnlFileNm, FileCategory category,
            FileOwner owner, String actorId) {
        return save(content, extension, orgnlFileNm, actorId, dir(category, owner));
    }

    /**
     * 파일 읽기 (소유 검사는 부르는 도메인 API 가 한다)
     */
    public StoredFile load(Long atchFileSn) {
        return read(atchFileMapper.selectInUse(atchFileSn));
    }

    /**
     * 로그인 회원 누구나 여는 파일 읽기
     * - 경로의 도메인·용도 폴더로 찾은 분류가 SHARED 이거나 기존 공개 파일(public/)일 때만
     * - 그 밖(OWNER 분류·기존 비공개 날짜 폴더·분류 못 찾음)은 없는 파일과 똑같이 FILE_NOT_FOUND
     */
    public StoredFile loadShared(Long atchFileSn) {
        AtchFile atchFile = atchFileMapper.selectInUse(atchFileSn);
        if (atchFile == null || !isShared(atchFile.getPhysStrgPath())) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return read(atchFile);
    }

    /**
     * 저장 경로로 열람 범위 판정
     * - assets/{용도}/{파일}, {회원}/{도메인}/{용도}/{파일}, {회원}/{도메인}/{반려동물}/{용도}/{파일}
     */
    private boolean isShared(String physStrgPath) {
        if (physStrgPath.startsWith(PUBLIC_PREFIX)) {
            return true;
        }
        String[] parts = physStrgPath.split("/");
        Optional<FileCategory> category = Optional.empty();
        if (parts.length == 3 && FileCategory.ASSET_DIR.equals(parts[0])) {
            category = FileCategory.of(Owner.SERVICE, parts[0], parts[1]);
        } else if (parts.length == 4 && isNumber(parts[0])) {
            category = FileCategory.of(Owner.MEMBER, parts[1], parts[2]);
        } else if (parts.length == 5 && isNumber(parts[0]) && isNumber(parts[2])) {
            category = FileCategory.of(Owner.PET, parts[1], parts[3]);
        }
        return category.map(FileCategory::isShared).orElse(false);
    }

    private boolean isNumber(String value) {
        return NUMBER.matcher(value).matches();
    }

    /**
     * 분류·소유자로 저장 폴더 계산
     * - 소유자 종류가 분류와 다르거나 번호가 비면 프로그래머 실수라 IllegalArgumentException
     */
    private String dir(FileCategory category, FileOwner owner) {
        if (category == null || owner == null || owner.type() != category.getOwner()) {
            throw new IllegalArgumentException("파일 소유자 종류가 분류와 다르다 category=" + category + " owner=" + owner);
        }
        return switch (owner.type()) {
            case MEMBER -> number(owner.mbrSn()) + "/" + category.getDomainDir() + "/" + category.getPurposeDir();
            case PET -> number(owner.mbrSn()) + "/" + category.getDomainDir() + "/" + number(owner.petSn()) + "/"
                    + category.getPurposeDir();
            case SERVICE -> FileCategory.ASSET_DIR + "/" + category.getPurposeDir();
        };
    }

    private Long number(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("파일 소유자 번호가 없다");
        }
        return value;
    }

    private StoredFile read(AtchFile atchFile) {
        if (atchFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        Path path = resolve(atchFile.getPhysStrgPath());
        if (!Files.isRegularFile(path)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new StoredFile(Files.readAllBytes(path),
                    FileExtension.from(atchFile.getFileExtnCd()).getContentType(), atchFile.getOrgnlFileNm());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 디스크 저장과 ATCH_FILE 등록
     */
    private Long save(byte[] content, FileExtension extension, String orgnlFileNm, String actorId, String dir) {
        String physStrgPath = dir + "/" + UUID.randomUUID() + "." + extension.getExtension();
        Path path = resolve(physStrgPath);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        deleteOnRollback(path);

        AtchFile atchFile = new AtchFile();
        atchFile.setFileExtnCd(extension.getCode());
        atchFile.setOrgnlFileNm(truncate(orgnlFileNm));
        atchFile.setPhysStrgPath(physStrgPath);
        atchFile.setFileSz((long) content.length);
        atchFile.setRgtrId(actorId);
        atchFileMapper.insert(atchFile);
        return atchFile.getAtchFileSn();
    }

    /**
     * 저장 경로 계산
     * - 루트 밖 경로 차단
     */
    private Path resolve(String physStrgPath) {
        Path root = fileRoot.getPath();
        Path path = root.resolve(physStrgPath).normalize();
        if (!path.startsWith(root)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return path;
    }

    /**
     * 롤백 시 파일 삭제 등록
     */
    private void deleteOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("[FileStore] 롤백 파일 삭제 실패 path={}", path, e);
                    }
                }
            }
        });
    }

    /**
     * 원본 파일명 길이 제한
     */
    private String truncate(String orgnlFileNm) {
        String name = orgnlFileNm == null || orgnlFileNm.isBlank() ? "file" : orgnlFileNm;
        return name.length() > ORGNL_FILE_NM_MAX ? name.substring(name.length() - ORGNL_FILE_NM_MAX) : name;
    }
}
