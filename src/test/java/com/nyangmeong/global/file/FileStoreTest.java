package com.nyangmeong.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 파일 저장·읽기]
 */
class FileStoreTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @TempDir
    Path root;

    private final AtchFileMapper atchFileMapper = mock(AtchFileMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private FileStore fileStore;

    @BeforeEach
    void setUp() {
        SystemSetting setting = new SystemSetting();
        setting.setFileMaxSz(10L);
        when(systemSettingService.get()).thenReturn(setting);
        doAnswer(invocation -> {
            invocation.<AtchFile>getArgument(0).setAtchFileSn(7L);
            return null;
        }).when(atchFileMapper).insert(any());
        fileStore = new FileStore(atchFileMapper, new FileRoot(root), systemSettingService);
    }

    @Test
    void 이미지를_디스크와_ATCH_FILE에_저장한다() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("photo", "kongi.PNG", "image/png", png);

        Long atchFileSn = fileStore.store(file, FileCategory.COMMUNITY_CHAT, FileOwner.member(1L), "1");

        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        AtchFile saved = captor.getValue();
        assertThat(atchFileSn).isEqualTo(7L);
        assertThat(saved.getFileExtnCd()).isEqualTo(FileExtension.PNG.getCode());
        assertThat(saved.getOrgnlFileNm()).isEqualTo("kongi.PNG");
        assertThat(saved.getFileSz()).isEqualTo((long) png.length);
        assertThat(saved.getRgtrId()).isEqualTo("1");
        assertThat(Files.readAllBytes(root.resolve(saved.getPhysStrgPath()))).containsExactly(png);
    }

    @Test
    void 확장자와_실제_형식이_다르면_FILE_TYPE_NOT_ALLOWED() {
        MockMultipartFile pdfAsPng = new MockMultipartFile("photo", "a.png", "image/png",
                new byte[] {'%', 'P', 'D', 'F', '-', '1'});
        MockMultipartFile textAsJpg = new MockMultipartFile("photo", "a.jpg", "image/jpeg", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> fileStore.store(pdfAsPng, FileCategory.COMMUNITY_POST, FileOwner.member(1L), "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> fileStore.store(textAsJpg, FileCategory.COMMUNITY_POST, FileOwner.member(1L), "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        verify(atchFileMapper, never()).insert(any());
    }

    @Test
    void 허용하지_않는_확장자는_FILE_TYPE_NOT_ALLOWED() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[] {1});
        MockMultipartFile gif = new MockMultipartFile("photo", "a.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> fileStore.store(pdf, FileCategory.COMMUNITY_POST, FileOwner.member(1L), "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        assertThatThrownBy(() -> fileStore.store(gif, FileCategory.COMMUNITY_POST, FileOwner.member(1L), "1"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        verify(atchFileMapper, never()).insert(any());
    }

    @Test
    void 설정_상한을_넘으면_FILE_TOO_LARGE() {
        MockMultipartFile big = new MockMultipartFile("photo", "a.jpg", "image/jpeg", new byte[11]);

        assertThatThrownBy(() -> fileStore.store(big, FileCategory.COMMUNITY_POST, FileOwner.member(1L), "1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 저장한_파일을_읽는다() {
        Long atchFileSn = fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "character-1.png",
                FileCategory.PET_CHARACTER, FileOwner.pet(1L, 2L), "SYSTEM");
        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper).insert(captor.capture());
        AtchFile saved = captor.getValue();
        saved.setUseYn("Y");
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);

        StoredFile loaded = fileStore.load(atchFileSn);

        assertThat(loaded.content()).containsExactly(9, 8);
        assertThat(loaded.contentType()).isEqualTo("image/png");
    }

    @Test
    void 없는_파일은_FILE_NOT_FOUND() {
        assertThatThrownBy(() -> fileStore.load(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 루트_밖_경로는_읽지_않는다() {
        AtchFile escaped = new AtchFile();
        escaped.setFileExtnCd(FileExtension.PNG.getCode());
        escaped.setPhysStrgPath("../outside.png");
        when(atchFileMapper.selectInUse(5L)).thenReturn(escaped);

        assertThatThrownBy(() -> fileStore.load(5L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 회원_파일은_회원번호_도메인_용도_폴더에_저장한다() {
        MockMultipartFile file = new MockMultipartFile("photo", "post.png", "image/png", PNG);

        fileStore.store(file, FileCategory.COMMUNITY_POST, FileOwner.member(5L), "5");

        assertThat(savedFile().getPhysStrgPath()).matches("5/community/post/[0-9a-f-]{36}\\.png");
    }

    @Test
    void 반려동물_파일은_주양육자번호_도메인_반려동물번호_용도_폴더에_저장한다() {
        fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "sheet-1.png", FileCategory.PET_SHEET,
                FileOwner.pet(5L, 12L), "9");

        assertThat(savedFile().getPhysStrgPath()).matches("5/pet/12/sheet/[0-9a-f-]{36}\\.png");
    }

    @Test
    void 서비스_파일은_assets_용도_폴더에_저장한다() {
        MockMultipartFile file = new MockMultipartFile("image", "notice.png", "image/png", PNG);

        fileStore.store(file, FileCategory.ASSET_NOTICE, FileOwner.service(), "3");

        assertThat(savedFile().getPhysStrgPath()).matches("assets/notice/[0-9a-f-]{36}\\.png");
    }

    @Test
    void pdf는_검진_첨부만_허용한다() {
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1'};
        MockMultipartFile checkup = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf);
        MockMultipartFile observation = new MockMultipartFile("file", "a.pdf", "application/pdf", pdf);

        fileStore.store(checkup, FileCategory.HEALTH_CHECKUP, FileOwner.pet(5L, 12L), "5");

        assertThat(savedFile().getPhysStrgPath()).matches("5/health/12/checkup/[0-9a-f-]{36}\\.pdf");
        assertThatThrownBy(
                () -> fileStore.store(observation, FileCategory.HEALTH_OBSERVATION, FileOwner.pet(5L, 12L), "5"))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void 소유자_종류가_분류와_다르거나_번호가_없으면_IllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("photo", "a.png", "image/png", PNG);

        assertThatThrownBy(() -> fileStore.store(file, FileCategory.PET_WEAR, FileOwner.member(5L), "5"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fileStore.store(file, FileCategory.ASSET_ITEM, FileOwner.member(5L), "5"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fileStore.store(file, FileCategory.HEALTH_CHECKUP, FileOwner.pet(5L, null), "5"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(atchFileMapper, never()).insert(any());
    }

    @Test
    void 로그인_회원_누구나_여는_분류는_loadShared로_읽는다() {
        MockMultipartFile post = new MockMultipartFile("photo", "post.png", "image/png", PNG);
        Long atchFileSn = fileStore.store(post, FileCategory.COMMUNITY_POST, FileOwner.member(5L), "5");
        AtchFile saved = savedFile();
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);

        assertThat(fileStore.loadShared(atchFileSn).content()).containsExactly(PNG);
    }

    @Test
    void 반려동물_파일과_assets_파일도_분류대로_loadShared_여부가_정해진다() {
        assertThat(sharedAfterStore(FileCategory.PET_WEAR, FileOwner.pet(5L, 12L))).isTrue();
        assertThat(sharedAfterStore(FileCategory.ASSET_NEWS, FileOwner.service())).isTrue();
        assertThat(sharedAfterStore(FileCategory.PET_SHEET, FileOwner.pet(5L, 12L))).isFalse();
        assertThat(sharedAfterStore(FileCategory.COMMUNITY_CHAT, FileOwner.member(5L))).isFalse();
    }

    @Test
    void 도메인_API_전용_분류는_loadShared로_읽으면_FILE_NOT_FOUND() {
        Long atchFileSn = fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "receipt.png",
                FileCategory.EXPENSE_RECEIPT, FileOwner.member(1L), "1");
        AtchFile saved = savedFile();
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);

        assertThatThrownBy(() -> fileStore.loadShared(atchFileSn))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void 기존_공개_파일은_loadShared로_읽는다() throws Exception {
        when(atchFileMapper.selectInUse(3L)).thenReturn(legacyFile("public/2026/10/old.png"));

        assertThat(fileStore.loadShared(3L).content()).containsExactly(PNG);
    }

    @Test
    void 기존_비공개_날짜_폴더_파일은_loadShared로_읽으면_FILE_NOT_FOUND() throws Exception {
        when(atchFileMapper.selectInUse(4L)).thenReturn(legacyFile("2026/10/old.png"));

        assertThatThrownBy(() -> fileStore.loadShared(4L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
        assertThat(fileStore.load(4L).content()).containsExactly(PNG);
    }

    @Test
    void 없는_번호는_loadShared로_읽어도_FILE_NOT_FOUND() {
        assertThatThrownBy(() -> fileStore.loadShared(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    private AtchFile savedFile() {
        ArgumentCaptor<AtchFile> captor = ArgumentCaptor.forClass(AtchFile.class);
        verify(atchFileMapper, atLeastOnce()).insert(captor.capture());
        return captor.getValue();
    }

    private boolean sharedAfterStore(FileCategory category, FileOwner owner) {
        Long atchFileSn = fileStore.store(new byte[] {9, 8}, FileExtension.PNG, "a.png", category, owner, "5");
        AtchFile saved = savedFile();
        when(atchFileMapper.selectInUse(atchFileSn)).thenReturn(saved);
        try {
            fileStore.loadShared(atchFileSn);
            return true;
        } catch (CustomException e) {
            return false;
        }
    }

    private AtchFile legacyFile(String physStrgPath) throws Exception {
        Path path = root.resolve(physStrgPath);
        Files.createDirectories(path.getParent());
        Files.write(path, PNG);
        AtchFile atchFile = new AtchFile();
        atchFile.setFileExtnCd(FileExtension.PNG.getCode());
        atchFile.setPhysStrgPath(physStrgPath);
        return atchFile;
    }
}
