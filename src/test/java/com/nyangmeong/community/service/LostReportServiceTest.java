package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.LostReport;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.domain.LostReportStatusHistory;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.dto.LostReportCreateRequest;
import com.nyangmeong.community.dto.LostReportResponse;
import com.nyangmeong.community.dto.LostReportSearchCond;
import com.nyangmeong.community.dto.LostReportUpdateRequest;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.code.RegionService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.pet.service.BreedService;

/**
 * [검사 - 실종센터 목록·상세·등록·수정·삭제·찾았어요·연장]
 */
class LostReportServiceTest {

    private static final Long MBR = 1L;
    private static final Long OTHER = 2L;
    private static final Long PST = 7L;
    private static final Long LOST = 5L;
    private static final String NORMAL = "CMUC0003";
    private static final String MISSING = "CMUC0006";
    private static final String FOUND = "CMUC0007";
    private static final BigDecimal LAT = new BigDecimal("35.8242000000");
    private static final BigDecimal LOT = new BigDecimal("127.1480000000");

    private final LostReportMapper lostReportMapper = mock(LostReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final RegionService regionService = mock(RegionService.class);
    private final BreedService breedService = mock(BreedService.class);
    private final LostReportService service = new LostReportService(lostReportMapper, postMapper, commentMapper,
            fileStore, new OwnerAuthorizer(), systemSettingService, fieldCryptoService, regionService,
            breedService);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private LostReportDetail detail(String sttsCd, LocalDateTime expryCrtrDt) {
        LostReportDetail detail = new LostReportDetail();
        detail.setLostTpofSn(LOST);
        detail.setPstSn(PST);
        detail.setMbrSn(MBR);
        detail.setMbrNcnm("토리맘");
        detail.setPstSttsCd(NORMAL);
        detail.setLostTpofSttsCd(sttsCd);
        detail.setPstTtl("보리를 찾습니다");
        detail.setPstCn("서곡동 인근");
        detail.setPetSeCd("PETC0001");
        detail.setPetNm("보리");
        detail.setVrtySn(1L);
        detail.setVrtyNm("말티즈");
        detail.setColrNm("흰색");
        detail.setRgnSn(3L);
        detail.setRgnNm("전북특별자치도 전주시");
        detail.setWtnsPlcLat(LAT);
        detail.setWtnsPlcLot(LOT);
        detail.setCnplEnc("enc");
        detail.setLastWtnsDt(LocalDateTime.now().minusDays(1));
        detail.setExpryCrtrDt(expryCrtrDt);
        detail.setInqCnt(10);
        detail.setRcmdtnCnt(3);
        return detail;
    }

    private LostReportDetail missing() {
        return detail(MISSING, LocalDateTime.now().plusDays(20));
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[] {1});
    }

    private LostReportCreateRequest create(Long vrtySn, String vrtyDrctInptNm, Boolean agree,
            List<MultipartFile> images) {
        return new LostReportCreateRequest("PETC0001", "보리", vrtySn, vrtyDrctInptNm, "흰색", "빨간 목줄",
                LocalDateTime.now().minusDays(1), "서곡초 인근", LAT, LOT, "010-1234-5678", "보리를 찾습니다", "서곡동 인근",
                images, agree);
    }

    private LostReportUpdateRequest update(BigDecimal lat, BigDecimal lot, List<Long> keep,
            List<MultipartFile> images) {
        return new LostReportUpdateRequest("PETC0001", "보리", 1L, null, "흰색", "빨간 목줄",
                LocalDateTime.now().minusDays(1), "서곡초 인근", lat, lot, "010-1234-5678", "새 제목", "새 내용", keep,
                images, true);
    }

    private PostImage image(Long atchFileSn, int sortSeq) {
        PostImage image = new PostImage();
        image.setPstSn(PST);
        image.setAtchFileSn(atchFileSn);
        image.setSortSeq(sortSeq);
        return image;
    }

    private void stubSystemSetting() {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    /** 등록 성공에 필요한 준비 */
    private void stubCreate() {
        stubSystemSetting();
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(3L);
        when(fieldCryptoService.encrypt(anyString())).thenReturn("enc");
        when(fieldCryptoService.decrypt("enc")).thenReturn("010-1234-5678");
        doAnswer(inv -> {
            inv.<Post>getArgument(0).setPstSn(PST);
            return 1;
        }).when(postMapper).insertPost(any(Post.class));
        doAnswer(inv -> {
            inv.<LostReport>getArgument(0).setLostTpofSn(LOST);
            return 1;
        }).when(lostReportMapper).insertLostReport(any(LostReport.class));
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
    }

    // ===== 목록 =====

    @Test
    void 목록은_정상_글만_기본_12건으로_조회한다() {
        service.search(MBR, new LostReportSearchCond(), new PageQuery(null, null, null));

        verify(lostReportMapper).selectList(MBR, null, null, null, null, null, NORMAL, MISSING, FOUND, "CMUC0008",
                false);
    }

    @Test
    void 조건과_키워드를_넘기고_키워드의_와일드카드는_이스케이프한다() {
        LostReportSearchCond cond = new LostReportSearchCond();
        cond.setLostTpofSttsCd(FOUND);
        cond.setRgnSn(10L);
        cond.setPetSeCd("PETC0002");
        cond.setWtnsPrd("WEEK");
        cond.setKeyword(" 50%_말티즈 ");

        service.search(MBR, cond, new PageQuery(null, null, null));

        verify(breedService).checkPetSe("PETC0002");
        verify(lostReportMapper).selectList(MBR, FOUND, 10L, "PETC0002", "WEEK", "50\\%\\_말티즈", NORMAL, MISSING,
                FOUND, "CMUC0008", false);
    }

    @Test
    void 알_수_없는_상태나_기간은_400() {
        LostReportSearchCond status = new LostReportSearchCond();
        status.setLostTpofSttsCd("CMUC0008");
        assertError(() -> service.search(MBR, status, new PageQuery(null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);

        LostReportSearchCond period = new LostReportSearchCond();
        period.setWtnsPrd("YEAR");
        assertError(() -> service.search(MBR, period, new PageQuery(null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 내_글만_보기는_mineYn을_넘기고_만료_상태_필터도_받는다() {
        LostReportSearchCond cond = new LostReportSearchCond();
        cond.setMineYn(true);
        cond.setLostTpofSttsCd("CMUC0008");

        service.search(MBR, cond, new PageQuery(null, null, null));

        verify(lostReportMapper).selectList(MBR, "CMUC0008", null, null, null, null, NORMAL, MISSING, FOUND,
                "CMUC0008", true);
    }

    // ===== 상세 =====

    @Test
    void 작성자는_연락처와_만료일을_본다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(fieldCryptoService.decrypt("enc")).thenReturn("010-1234-5678");

        LostReportResponse res = service.getReport(MBR, LOST);

        assertThat(res.cnpl()).isEqualTo("010-1234-5678");
        assertThat(res.expryCrtrDt()).isNotNull();
        assertThat(res.owner()).isTrue();
        assertThat(res.vrtyNm()).isEqualTo("말티즈");
    }

    @Test
    void 만료된_글의_작성자_상세는_만료_상태로_내린다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, LocalDateTime.now().minusDays(1)));
        when(fieldCryptoService.decrypt("enc")).thenReturn("010-1234-5678");

        LostReportResponse res = service.getReport(MBR, LOST);

        assertThat(res.lostTpofSttsCd()).isEqualTo("CMUC0008");
    }

    @Test
    void 다른_회원은_연락처는_보지만_만료일은_못_본다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(fieldCryptoService.decrypt("enc")).thenReturn("010-1234-5678");

        LostReportResponse res = service.getReport(OTHER, LOST);

        assertThat(res.cnpl()).isEqualTo("010-1234-5678");
        assertThat(res.expryCrtrDt()).isNull();
        assertThat(res.owner()).isFalse();
    }

    @Test
    void 조회수는_같은_회원_하루_한_번만_올린다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(postMapper.insertViewIgnore(PST, OTHER)).thenReturn(1);

        LostReportResponse res = service.getReport(OTHER, LOST);

        verify(postMapper).updateInqCnt(PST);
        assertThat(res.inqCnt()).isEqualTo(11);
    }

    @Test
    void 이미_본_회원이면_조회수를_올리지_않는다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(postMapper.insertViewIgnore(PST, OTHER)).thenReturn(0);

        service.getReport(OTHER, LOST);

        verify(postMapper, never()).updateInqCnt(anyLong());
    }

    @Test
    void 찾았어요_글은_다른_회원에게_제목_상태_시각만_준다() {
        LostReportDetail found = detail(FOUND, LocalDateTime.now().plusDays(20));
        found.setDscvSwtcDt(LocalDateTime.of(2026, 10, 29, 10, 0));
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(found);

        LostReportResponse res = service.getReport(OTHER, LOST);

        assertThat(res.pstTtl()).isEqualTo("보리를 찾습니다");
        assertThat(res.lostTpofSttsCd()).isEqualTo(FOUND);
        assertThat(res.dscvSwtcDt()).isNotNull();
        assertThat(res.cnpl()).isNull();
        assertThat(res.pstCn()).isNull();
        assertThat(res.petNm()).isNull();
        assertThat(res.wtnsPlcLat()).isNull();
        assertThat(res.imgs()).isNull();
        verify(postMapper, never()).insertViewIgnore(anyLong(), anyLong());
    }

    @Test
    void 찾았어요_글도_작성자는_전체를_본다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(FOUND, LocalDateTime.now().plusDays(20)));
        when(fieldCryptoService.decrypt("enc")).thenReturn("010-1234-5678");

        LostReportResponse res = service.getReport(MBR, LOST);

        assertThat(res.cnpl()).isEqualTo("010-1234-5678");
        assertThat(res.petNm()).isEqualTo("보리");
    }

    @Test
    void 만료된_글은_다른_회원에게_404이고_작성자는_본다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, LocalDateTime.now().minusDays(1)));

        assertError(() -> service.getReport(OTHER, LOST), ErrorCode.LOST_REPORT_NOT_FOUND);
        assertThat(service.getReport(MBR, LOST).owner()).isTrue();
    }

    @Test
    void 없거나_삭제된_제보는_404() {
        assertError(() -> service.getReport(MBR, 99L), ErrorCode.LOST_REPORT_NOT_FOUND);
    }

    // ===== 등록 =====

    @Test
    void 정상_등록은_게시글_제보_이력을_저장하고_연락처를_암호화한다() {
        stubCreate();

        LostReportResponse res = service.create(MBR, create(1L, null, true, null));

        ArgumentCaptor<Post> post = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insertPost(post.capture());
        assertThat(post.getValue().getBbsCd()).isEqualTo("CMUC0024");
        assertThat(post.getValue().getPstSttsCd()).isEqualTo(NORMAL);
        ArgumentCaptor<LostReport> report = ArgumentCaptor.forClass(LostReport.class);
        verify(lostReportMapper).insertLostReport(report.capture());
        assertThat(report.getValue().getLostTpofSttsCd()).isEqualTo(MISSING);
        assertThat(report.getValue().getRgnSn()).isEqualTo(3L);
        assertThat(report.getValue().getCnplEnc()).isEqualTo("enc");
        assertThat(report.getValue().getPstSn()).isEqualTo(PST);
        ArgumentCaptor<LostReportStatusHistory> history = ArgumentCaptor.forClass(LostReportStatusHistory.class);
        verify(lostReportMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isNull();
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(MISSING);
        verify(postMapper).insertStatusHistory(any());
        assertThat(res.lostTpofSttsCd()).isEqualTo(MISSING);
    }

    @Test
    void 사진은_순서대로_저장한다() {
        stubCreate();
        when(fileStore.storePublic(any(MultipartFile.class), anyString())).thenReturn(101L, 102L);

        service.create(MBR, create(null, "믹스", true, List.of(file("a.png"), file("b.png"))));

        ArgumentCaptor<PostImage> saved = ArgumentCaptor.forClass(PostImage.class);
        verify(postMapper, org.mockito.Mockito.times(2)).insertImage(saved.capture());
        assertThat(saved.getAllValues()).extracting(PostImage::getSortSeq).containsExactly(0, 1);
        assertThat(saved.getAllValues()).extracting(PostImage::getAtchFileSn).containsExactly(101L, 102L);
    }

    @Test
    void 품종은_마스터_선택과_직접_입력_중_하나만() {
        assertError(() -> service.create(MBR, create(null, null, true, null)), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(MBR, create(1L, "믹스", true, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void 개인정보_공개에_동의하지_않으면_400() {
        assertError(() -> service.create(MBR, create(1L, null, false, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void 마지막_목격이_미래면_400() {
        LostReportCreateRequest future = new LostReportCreateRequest("PETC0001", "보리", 1L, null, "흰색", null,
                LocalDateTime.now().plusDays(1), null, LAT, LOT, "010-1234-5678", "제목", "내용", null, true);

        assertError(() -> service.create(MBR, future), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다른_구분의_품종이면_품종_확인에서_막힌다() {
        doThrow(new CustomException(ErrorCode.BREED_NOT_FOUND)).when(breedService).getBreedOrThrow(1L, "PETC0001");

        assertError(() -> service.create(MBR, create(1L, null, true, null)), ErrorCode.BREED_NOT_FOUND);
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void 전북_밖_좌표면_400이고_아무것도_저장하지_않는다() {
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(null);

        assertError(() -> service.create(MBR, create(1L, null, true, null)),
                ErrorCode.LOST_REPORT_REGION_NOT_ALLOWED);
        verify(postMapper, never()).insertPost(any());
        verify(lostReportMapper, never()).insertLostReport(any());
    }

    @Test
    void 사진이_최대_장수를_넘으면_400() {
        stubSystemSetting();
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(3L);
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            images.add(file(i + ".png"));
        }

        assertError(() -> service.create(MBR, create(1L, null, true, images)), ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        verify(postMapper, never()).insertPost(any());
    }

    // ===== 수정 =====

    @Test
    void 좌표가_그대로면_실종지역을_다시_계산하지_않는다() {
        stubSystemSetting();
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(postMapper.updatePost(eq(PST), eq(null), anyString(), anyString(), anyString(), eq(NORMAL)))
                .thenReturn(1);
        when(lostReportMapper.updateLostReport(any(LostReport.class), eq(MISSING))).thenReturn(1);
        when(fieldCryptoService.encrypt(anyString())).thenReturn("enc");

        service.update(MBR, LOST, update(new BigDecimal("35.8242"), new BigDecimal("127.148"), null, null));

        verify(regionService, never()).findJeonbukRgnSn(any(), any());
        ArgumentCaptor<LostReport> saved = ArgumentCaptor.forClass(LostReport.class);
        verify(lostReportMapper).updateLostReport(saved.capture(), eq(MISSING));
        assertThat(saved.getValue().getRgnSn()).isEqualTo(3L);
        assertThat(saved.getValue().getLostTpofSn()).isEqualTo(LOST);
        verify(postMapper).updatePost(PST, null, "새 제목", "새 내용", "1", NORMAL);
    }

    @Test
    void 좌표가_바뀌면_실종지역을_다시_계산한다() {
        stubSystemSetting();
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(4L);
        when(postMapper.updatePost(eq(PST), eq(null), anyString(), anyString(), anyString(), eq(NORMAL)))
                .thenReturn(1);
        when(lostReportMapper.updateLostReport(any(LostReport.class), eq(MISSING))).thenReturn(1);

        service.update(MBR, LOST, update(new BigDecimal("35.9"), new BigDecimal("127.2"), null, null));

        ArgumentCaptor<LostReport> saved = ArgumentCaptor.forClass(LostReport.class);
        verify(lostReportMapper).updateLostReport(saved.capture(), eq(MISSING));
        assertThat(saved.getValue().getRgnSn()).isEqualTo(4L);
    }

    @Test
    void 바뀐_좌표가_전북_밖이면_400이고_저장하지_않는다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(null);

        assertError(() -> service.update(MBR, LOST, update(new BigDecimal("37.5"), new BigDecimal("127.0"), null, null)),
                ErrorCode.LOST_REPORT_REGION_NOT_ALLOWED);
        verify(postMapper, never()).updatePost(any(), any(), any(), any(), any(), any());
        verify(lostReportMapper, never()).updateLostReport(any(), any());
    }

    @Test
    void 남의_제보_수정은_403() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());

        assertError(() -> service.update(OTHER, LOST, update(LAT, LOT, null, null)), ErrorCode.NOT_OWNER);
        verify(lostReportMapper, never()).updateLostReport(any(), any());
    }

    @Test
    void 찾았어요나_만료된_제보는_수정할_수_없다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(FOUND, LocalDateTime.now().plusDays(20)));
        assertError(() -> service.update(MBR, LOST, update(LAT, LOT, null, null)),
                ErrorCode.LOST_REPORT_NOT_EDITABLE);

        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, LocalDateTime.now().minusDays(1)));
        assertError(() -> service.update(MBR, LOST, update(LAT, LOT, null, null)),
                ErrorCode.LOST_REPORT_NOT_EDITABLE);
    }

    @Test
    void 동의하지_않으면_수정도_400() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        LostReportUpdateRequest req = new LostReportUpdateRequest("PETC0001", "보리", 1L, null, "흰색", null,
                LocalDateTime.now().minusDays(1), null, LAT, LOT, "010-1234-5678", "제목", "내용", null, null, false);

        assertError(() -> service.update(MBR, LOST, req), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 남길_사진과_새_사진의_합이_최대_장수를_넘으면_400() {
        stubSystemSetting();
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        List<PostImage> existing = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            existing.add(image(100L + i, i));
        }
        when(postMapper.selectImages(PST)).thenReturn(existing);

        assertError(() -> service.update(MBR, LOST, update(LAT, LOT, null, List.of(file("a.png")))),
                ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        verify(lostReportMapper, never()).updateLostReport(any(), any());
    }

    @Test
    void keep_목록에_없는_기존_사진은_삭제하고_새_사진은_뒤에_붙인다() {
        stubSystemSetting();
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(postMapper.selectImages(PST)).thenReturn(List.of(image(101L, 0), image(102L, 1)));
        when(postMapper.updatePost(eq(PST), eq(null), anyString(), anyString(), anyString(), eq(NORMAL)))
                .thenReturn(1);
        when(lostReportMapper.updateLostReport(any(LostReport.class), eq(MISSING))).thenReturn(1);
        when(fileStore.storePublic(any(MultipartFile.class), anyString())).thenReturn(103L);

        service.update(MBR, LOST, update(LAT, LOT, List.of(101L), List.of(file("c.png"))));

        verify(postMapper).updateImagesDeleted(PST, List.of(102L), "1");
        ArgumentCaptor<PostImage> added = ArgumentCaptor.forClass(PostImage.class);
        verify(postMapper).insertImage(added.capture());
        assertThat(added.getValue().getAtchFileSn()).isEqualTo(103L);
        assertThat(added.getValue().getSortSeq()).isEqualTo(1);
    }

    // ===== 삭제 =====

    @Test
    void 작성자_삭제는_게시글_상태를_삭제로_바꾸고_이력을_남긴다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(FOUND, LocalDateTime.now().plusDays(20)));
        when(postMapper.updateStatus(PST, NORMAL, "CMUC0005", "1")).thenReturn(1);

        service.delete(MBR, LOST);

        verify(postMapper).updateStatus(PST, NORMAL, "CMUC0005", "1");
        verify(postMapper).insertStatusHistory(any());
    }

    @Test
    void 남의_제보_삭제는_403() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());

        assertError(() -> service.delete(OTHER, LOST), ErrorCode.NOT_OWNER);
        verify(postMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void 이미_삭제된_제보는_404() {
        assertError(() -> service.delete(MBR, LOST), ErrorCode.LOST_REPORT_NOT_FOUND);
    }

    // ===== 찾았어요 =====

    @Test
    void 찾았어요_전환은_이력과_완료_안내_댓글을_남긴다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(lostReportMapper.updateFound(LOST, MISSING, FOUND)).thenReturn(1);

        service.markFound(MBR, LOST);

        ArgumentCaptor<LostReportStatusHistory> history = ArgumentCaptor.forClass(LostReportStatusHistory.class);
        verify(lostReportMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(MISSING);
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(FOUND);
        ArgumentCaptor<Comment> comment = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertAutoComment(comment.capture());
        assertThat(comment.getValue().getPstSn()).isEqualTo(PST);
        assertThat(comment.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(comment.getValue().getCmntCn())
                .isEqualTo("🎉 여러분의 제보와 관심 덕분에 보리가 무사히 가족의 품으로 돌아갔어요.");
    }

    @Test
    void 이미_찾았어요면_두_번째는_거절하고_댓글을_또_만들지_않는다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail(FOUND, LocalDateTime.now().plusDays(20)));
        when(lostReportMapper.updateFound(LOST, MISSING, FOUND)).thenReturn(0);

        assertError(() -> service.markFound(MBR, LOST), ErrorCode.LOST_REPORT_ALREADY_FOUND);
        verify(commentMapper, never()).insertAutoComment(any());
    }

    @Test
    void 남의_제보는_찾았어요로_바꿀_수_없다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());

        assertError(() -> service.markFound(OTHER, LOST), ErrorCode.NOT_OWNER);
        verify(lostReportMapper, never()).updateFound(anyLong(), anyString(), anyString());
    }

    // ===== 게시 기간 연장 =====

    @Test
    void 연장_창_안이면_연장한다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(lostReportMapper.updateRenewed(LOST, MISSING)).thenReturn(1);

        service.renew(MBR, LOST);

        verify(lostReportMapper).updateRenewed(LOST, MISSING);
    }

    @Test
    void 연장_창_밖이거나_이미_연장했으면_400() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());
        when(lostReportMapper.updateRenewed(LOST, MISSING)).thenReturn(0);

        assertError(() -> service.renew(MBR, LOST), ErrorCode.LOST_REPORT_NOT_RENEWABLE);
    }

    @Test
    void 남의_제보는_연장할_수_없다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(missing());

        assertError(() -> service.renew(OTHER, LOST), ErrorCode.NOT_OWNER);
        verify(lostReportMapper, never()).updateRenewed(anyLong(), anyString());
    }
}
