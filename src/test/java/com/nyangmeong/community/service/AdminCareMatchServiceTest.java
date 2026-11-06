package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.dto.AdminCareMatchResponse;
import com.nyangmeong.community.dto.AdminCareMatchSearchCond;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 펫돌봄 거래 목록·매칭 상세]
 */
class AdminCareMatchServiceTest {

    private static final String NORMAL = "CMUC0003";
    private static final String DELETED = "CMUC0005";
    private static final Long RCRT = 7L;

    private final CareRecruitmentMapper careRecruitmentMapper = mock(CareRecruitmentMapper.class);
    private final CareMatchMapper careMatchMapper = mock(CareMatchMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final AdminCareMatchService service = new AdminCareMatchService(careRecruitmentMapper, careMatchMapper,
            fileStore);

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    private AdminCareMatchResponse detail() {
        AdminCareMatchResponse detail = new AdminCareMatchResponse();
        detail.setPetPrtcRcrtSn(RCRT);
        detail.setPstTtl("주말 산책 부탁드려요");
        return detail;
    }

    private AdminCareMatchResponse.Match match(Long sn) {
        AdminCareMatchResponse.Match match = new AdminCareMatchResponse.Match();
        match.setPetPrtcMtchSn(sn);
        return match;
    }

    private AdminCareMatchResponse.ConditionVersion version(Long mtchSn, Long verNo) {
        AdminCareMatchResponse.ConditionVersion version = new AdminCareMatchResponse.ConditionVersion();
        version.setPetPrtcMtchSn(mtchSn);
        version.setCndVerNo(verNo);
        return version;
    }

    private AdminCareMatchResponse.StatusHistory history(Long mtchSn, String aftrSttsCd) {
        AdminCareMatchResponse.StatusHistory history = new AdminCareMatchResponse.StatusHistory();
        history.setPetPrtcMtchSn(mtchSn);
        history.setAftrSttsCd(aftrSttsCd);
        return history;
    }

    private AdminCareMatchResponse.CompletionRequest request(Long mtchSn, Long cyclSn, Long cycl) {
        AdminCareMatchResponse.CompletionRequest request = new AdminCareMatchResponse.CompletionRequest();
        request.setPetPrtcMtchSn(mtchSn);
        request.setPrtcCmptnDmndCyclSn(cyclSn);
        request.setDmndCycl(cycl);
        return request;
    }

    private AdminCareMatchResponse.Photo photo(Long cyclSn, Long atchFileSn) {
        AdminCareMatchResponse.Photo photo = new AdminCareMatchResponse.Photo();
        photo.setPrtcCmptnDmndCyclSn(cyclSn);
        photo.setAtchFileSn(atchFileSn);
        return photo;
    }

    @Test
    void 상태_필터를_정상_글_조건과_함께_넘긴다() {
        AdminCareMatchSearchCond cond = new AdminCareMatchSearchCond();
        cond.setRcrtSttsCd("PCAC0002");

        service.search(cond, new PageQuery(null, null, null));

        verify(careRecruitmentMapper).selectAdminList("PCAC0002", NORMAL);
    }

    @Test
    void 빈_상태는_전체() {
        AdminCareMatchSearchCond cond = new AdminCareMatchSearchCond();
        cond.setRcrtSttsCd(" ");

        service.search(cond, new PageQuery(null, null, null));

        verify(careRecruitmentMapper).selectAdminList(null, NORMAL);
    }

    @Test
    void 모르는_상태_코드는_400() {
        AdminCareMatchSearchCond cond = new AdminCareMatchSearchCond();
        cond.setRcrtSttsCd("PCAC0012");

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(careRecruitmentMapper, never()).selectAdminList(any(), any());
    }

    @Test
    void 없거나_삭제된_모집글은_404() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(null);

        assertError(() -> service.get(RCRT), ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        verify(careMatchMapper, never()).selectAdminMatches(anyLong());
    }

    @Test
    void 매칭이_없으면_빈_목록이고_하위_조회를_하지_않는다() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(detail());
        when(careMatchMapper.selectAdminMatches(RCRT)).thenReturn(List.of());

        AdminCareMatchResponse result = service.get(RCRT);

        assertThat(result.getMtchs()).isEmpty();
        verify(careMatchMapper, never()).selectAdminConditionVersions(anyLong());
        verify(careMatchMapper, never()).selectAdminCompletionRequests(anyLong());
    }

    @Test
    void 매칭마다_버전_이력_완료_회차와_사진을_나눠_담는다() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(detail());
        when(careMatchMapper.selectAdminMatches(RCRT)).thenReturn(List.of(match(2L), match(1L)));
        when(careMatchMapper.selectAdminConditionVersions(RCRT))
                .thenReturn(List.of(version(1L, 1L), version(2L, 2L), version(2L, 1L)));
        when(careMatchMapper.selectAdminStatusHistories(RCRT))
                .thenReturn(List.of(history(1L, "PCAC0012"), history(1L, "PCAC0017"), history(2L, "PCAC0012")));
        when(careMatchMapper.selectAdminCompletionRequests(RCRT))
                .thenReturn(List.of(request(2L, 30L, 1L), request(2L, 31L, 2L)));
        when(careMatchMapper.selectAdminCompletionPhotos(RCRT))
                .thenReturn(List.of(photo(30L, 900L), photo(31L, 901L), photo(31L, 902L)));

        AdminCareMatchResponse result = service.get(RCRT);

        AdminCareMatchResponse.Match second = result.getMtchs().get(0);
        AdminCareMatchResponse.Match first = result.getMtchs().get(1);
        assertThat(second.getCndVers()).extracting(AdminCareMatchResponse.ConditionVersion::getCndVerNo)
                .containsExactly(2L, 1L);
        assertThat(second.getSttsHstry()).extracting(AdminCareMatchResponse.StatusHistory::getAftrSttsCd)
                .containsExactly("PCAC0012");
        assertThat(second.getCmptnDmnds()).extracting(AdminCareMatchResponse.CompletionRequest::getDmndCycl)
                .containsExactly(1L, 2L);
        assertThat(second.getCmptnDmnds().get(1).getImgs())
                .extracting(AdminCareMatchResponse.Photo::getAtchFileSn).containsExactly(901L, 902L);
        assertThat(first.getSttsHstry()).extracting(AdminCareMatchResponse.StatusHistory::getAftrSttsCd)
                .containsExactly("PCAC0012", "PCAC0017");
        assertThat(first.getCmptnDmnds()).isEmpty();
    }

    @Test
    void 응답에_채팅_필드가_없다() {
        for (Class<?> type : new Class<?>[] { AdminCareMatchResponse.class, AdminCareMatchResponse.Match.class }) {
            assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getName))
                    .noneMatch(name -> name.toLowerCase().contains("chat") || name.toLowerCase().contains("msg"));
        }
    }

    @Test
    void 이_모집글_완료_사진이면_파일을_읽어_준다() {
        StoredFile stored = new StoredFile(new byte[] { 1, 2 }, "image/jpeg", "care_3555.jpg");
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(detail());
        when(careMatchMapper.selectAdminCompletionPhotos(RCRT)).thenReturn(List.of(photo(1L, 3555L), photo(1L, 3556L)));
        when(fileStore.load(3556L)).thenReturn(stored);

        assertThat(service.getImage(RCRT, 3556L)).isSameAs(stored);
    }

    @Test
    void 이_모집글_완료_사진이_아니면_파일을_읽지_않고_404() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(detail());
        when(careMatchMapper.selectAdminCompletionPhotos(RCRT)).thenReturn(List.of(photo(1L, 3555L)));

        assertError(() -> service.getImage(RCRT, 9999L), ErrorCode.FILE_NOT_FOUND);
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 완료_사진이_하나도_없는_모집글이면_파일을_읽지_않고_404() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(detail());
        when(careMatchMapper.selectAdminCompletionPhotos(RCRT)).thenReturn(List.of());

        assertError(() -> service.getImage(RCRT, 3555L), ErrorCode.FILE_NOT_FOUND);
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 없거나_삭제된_모집글의_사진은_조회하지_않고_404() {
        when(careRecruitmentMapper.selectAdminDetail(RCRT, DELETED)).thenReturn(null);

        assertError(() -> service.getImage(RCRT, 3555L), ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        verify(careMatchMapper, never()).selectAdminCompletionPhotos(anyLong());
        verify(fileStore, never()).load(anyLong());
    }
}
