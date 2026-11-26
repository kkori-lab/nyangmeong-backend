package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.dto.PostLikeResponse;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.global.validation.ProfanityFilter;

/**
 * [검사 - 찾았어요·게시 종료된 실종 글 좋아요 차단]
 */
class PostLikeLostClosedTest {

    private static final Long MBR = 1L;
    private static final Long PST = 7L;
    private static final String NORMAL = "CMUC0003";
    private static final String MISSING = "CMUC0006";

    private final PostMapper postMapper = mock(PostMapper.class);
    private final PostService service = new PostService(postMapper, mock(FileStore.class), new OwnerAuthorizer(),
            mock(SystemSettingService.class), mock(PetStoryMissionReflector.class), mock(ProfanityFilter.class));

    private void stubPost() {
        Post post = new Post();
        post.setPstSn(PST);
        post.setMbrSn(2L);
        post.setBbsCd("CMUC0024");
        post.setPstSttsCd(NORMAL);
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post);
    }

    @Test
    void 종료된_실종_글에_좋아요를_누르면_400이고_수는_그대로다() {
        stubPost();
        when(postMapper.existsClosedLostReport(PST, MISSING)).thenReturn(true);

        assertThatThrownBy(() -> service.like(MBR, PST)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOST_REPORT_LIKE_CLOSED));

        verify(postMapper, never()).updateLikeOn(anyLong(), anyLong());
        verify(postMapper, never()).insertLikeIgnore(anyLong(), anyLong());
        verify(postMapper, never()).updateRcmdtnCnt(anyLong(), anyInt());
    }

    @Test
    void 종료된_실종_글이라도_좋아요_취소는_된다() {
        stubPost();
        when(postMapper.existsClosedLostReport(PST, MISSING)).thenReturn(true);
        when(postMapper.updateLikeOff(PST, MBR)).thenReturn(1);

        service.unlike(MBR, PST);

        verify(postMapper).updateRcmdtnCnt(PST, -1);
    }

    @Test
    void 실종중이거나_실종_글이_아니면_좋아요가_그대로_된다() {
        stubPost();
        when(postMapper.existsClosedLostReport(PST, MISSING)).thenReturn(false);
        when(postMapper.updateLikeOn(PST, MBR)).thenReturn(0);
        when(postMapper.insertLikeIgnore(PST, MBR)).thenReturn(1);
        when(postMapper.selectRcmdtnCnt(PST)).thenReturn(4);

        PostLikeResponse res = service.like(MBR, PST);

        verify(postMapper).updateRcmdtnCnt(PST, 1);
        assertThat(res.rcmdtnCnt()).isEqualTo(4);
    }
}
