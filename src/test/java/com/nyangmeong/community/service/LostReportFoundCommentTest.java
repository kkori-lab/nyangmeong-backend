package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.dto.LostReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.code.RegionService;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.global.validation.ProfanityFilter;
import com.nyangmeong.pet.service.BreedService;

/**
 * [검사 - 찾았어요 자동 댓글이 운영 설정 문구를 쓰는지, 상세 응답의 미리보기 문구]
 */
class LostReportFoundCommentTest {

    private static final Long MBR = 1L;
    private static final Long OTHER = 2L;
    private static final Long PST = 7L;
    private static final Long LOST = 5L;
    private static final String NORMAL = "CMUC0003";
    private static final String MISSING = "CMUC0006";
    private static final String FOUND = "CMUC0007";

    private final LostReportMapper lostReportMapper = mock(LostReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final LostReportService service = new LostReportService(lostReportMapper, postMapper, commentMapper,
            mock(FileStore.class), new OwnerAuthorizer(), systemSettingService, mock(FieldCryptoService.class),
            mock(RegionService.class), mock(BreedService.class), mock(EventPublisher.class),
            mock(ProfanityFilter.class));

    @BeforeEach
    void stubSetting() {
        SystemSetting setting = new SystemSetting();
        setting.setDscvAutoCmntCn("[펫이름]가 무사히 가족 품으로 돌아왔어요!");
        when(systemSettingService.get()).thenReturn(setting);
    }

    private LostReportDetail detail(String sttsCd, String petNm, LocalDateTime expryCrtrDt) {
        LostReportDetail detail = new LostReportDetail();
        detail.setLostTpofSn(LOST);
        detail.setPstSn(PST);
        detail.setMbrSn(MBR);
        detail.setPstSttsCd(NORMAL);
        detail.setLostTpofSttsCd(sttsCd);
        detail.setPstTtl("밤을 찾습니다");
        detail.setPetNm(petNm);
        detail.setExpryCrtrDt(expryCrtrDt);
        return detail;
    }

    @Test
    void 찾았어요_자동_댓글은_운영_설정_문구에_이름과_조사를_맞춘다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, "밤", LocalDateTime.now().plusDays(10)));
        when(lostReportMapper.updateFound(LOST, MISSING, FOUND)).thenReturn(1);

        service.markFound(MBR, LOST);

        ArgumentCaptor<Comment> comment = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertAutoComment(comment.capture());
        assertThat(comment.getValue().getCmntCn()).isEqualTo("밤이 무사히 가족 품으로 돌아왔어요!");
        assertThat(comment.getValue().getMbrSn()).isEqualTo(MBR);
        assertThat(comment.getValue().getPstSn()).isEqualTo(PST);
    }

    @Test
    void 관리자가_문구를_바꾸면_바뀐_문구로_달린다() {
        SystemSetting changed = new SystemSetting();
        changed.setDscvAutoCmntCn("제보 고마워요, [펫이름]를 찾았어요");
        when(systemSettingService.get()).thenReturn(changed);
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, "초코", LocalDateTime.now().plusDays(10)));
        when(lostReportMapper.updateFound(LOST, MISSING, FOUND)).thenReturn(1);

        service.markFound(MBR, LOST);

        ArgumentCaptor<Comment> comment = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insertAutoComment(comment.capture());
        assertThat(comment.getValue().getCmntCn()).isEqualTo("제보 고마워요, 초코를 찾았어요");
    }

    @Test
    void 작성자이고_실종이면_상세에_같은_미리보기_문구가_온다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, "밤", LocalDateTime.now().plusDays(10)));

        LostReportResponse res = service.getReport(MBR, LOST);

        assertThat(res.dscvAutoCmntCn()).isEqualTo("밤이 무사히 가족 품으로 돌아왔어요!");
    }

    @Test
    void 다른_회원_찾았어요_만료_글에는_미리보기_문구가_없다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, "밤", LocalDateTime.now().plusDays(10)));
        assertThat(service.getReport(OTHER, LOST).dscvAutoCmntCn()).isNull();

        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(FOUND, "밤", LocalDateTime.now().plusDays(10)));
        assertThat(service.getReport(MBR, LOST).dscvAutoCmntCn()).isNull();
        assertThat(service.getReport(OTHER, LOST).dscvAutoCmntCn()).isNull();

        when(lostReportMapper.selectDetail(LOST, NORMAL))
                .thenReturn(detail(MISSING, "밤", LocalDateTime.now().minusDays(1)));
        assertThat(service.getReport(MBR, LOST).dscvAutoCmntCn()).isNull();
    }

    @Test
    void 문구를_넣지_않는_응답_생성은_미리보기가_null이다() {
        LostReportResponse res = LostReportResponse.of(detail(MISSING, "밤", null), null,
                java.util.List.of(), false, true);
        assertThat(res.dscvAutoCmntCn()).isNull();
        verify(commentMapper, org.mockito.Mockito.never()).insertAutoComment(any());
    }
}
