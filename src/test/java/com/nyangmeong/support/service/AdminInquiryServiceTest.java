package com.nyangmeong.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.pet.service.PixelCharacterService;
import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatus;
import com.nyangmeong.support.domain.InquiryType;
import com.nyangmeong.support.dto.AdminCharacterErrorRequest;
import com.nyangmeong.support.dto.AdminInquiryAnswerRequest;
import com.nyangmeong.support.dto.AdminInquiryResponse;
import com.nyangmeong.support.dto.AdminInquirySearchCond;
import com.nyangmeong.support.dto.AdminInquirySummary;
import com.nyangmeong.support.mapper.InquiryMapper;

/**
 * [검사 - 관리자 1:1 문의 목록·상세·답변·캐릭터 오류 처리]
 */
class AdminInquiryServiceTest {

    private static final Long ADMIN = 5L;
    private static final Long WRITER = 1L;
    private static final Long INQ = 7L;
    private static final Long PIXEL_CHAR = 30L;

    private final InquiryMapper inquiryMapper = mock(InquiryMapper.class);
    private final InquiryService inquiryService = mock(InquiryService.class);
    private final PixelCharacterService pixelCharacterService = mock(PixelCharacterService.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminInquiryService service = new AdminInquiryService(inquiryMapper, inquiryService,
            pixelCharacterService, fileStore, adminTaskLogService);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private Inquiry inquiry(InquiryType type, InquiryStatus status, String delYn) {
        Inquiry inquiry = new Inquiry();
        inquiry.setOtoInqrySn(INQ);
        inquiry.setMbrSn(WRITER);
        inquiry.setInqryTypeCd(type.getCode());
        inquiry.setInqryPrcsSttsCd(status.getCode());
        inquiry.setInqryTtl("제목");
        inquiry.setInqryCn("내용");
        inquiry.setDelYn(delYn);
        return inquiry;
    }

    private Inquiry characterInquiry(String charErrPrcsRsltCd) {
        Inquiry inquiry = inquiry(InquiryType.PIXEL_CHARACTER, InquiryStatus.RECEIVED, "N");
        inquiry.setPixelCharSn(PIXEL_CHAR);
        inquiry.setCharErrPrcsRsltCd(charErrPrcsRsltCd);
        return inquiry;
    }

    private InquiryPhoto photo(Long atchFileSn, int sortSeq) {
        InquiryPhoto photo = new InquiryPhoto();
        photo.setAtchFileSn(atchFileSn);
        photo.setSortSeq(sortSeq);
        return photo;
    }

    private AdminInquirySummary row(InquiryType type) {
        AdminInquirySummary row = new AdminInquirySummary();
        row.setInqryTypeCd(type.getCode());
        return row;
    }

    @Test
    void 내부_분류로_거르면_그_분류의_유형_코드로_조회하고_행에_분류를_채운다() {
        AdminInquirySearchCond cond = new AdminInquirySearchCond();
        cond.setInqryCtgr("DISPUTE");
        when(inquiryMapper.selectAdminList(eq(cond), any(), eq(false)))
                .thenReturn(new ArrayList<>(List.of(row(InquiryType.COMMUNITY))));

        List<AdminInquirySummary> rows = service.search(cond, new PageQuery(null, null, null)).content();

        verify(inquiryMapper).selectAdminList(cond, List.of("CSCC0008", "CSCC0009"), false);
        assertThat(rows).extracting(AdminInquirySummary::getInqryCtgr).containsExactly("DISPUTE");
    }

    @Test
    void 분류_없이_조회하면_유형_목록_조건이_없고_삭제됨_포함이_넘어간다() {
        AdminInquirySearchCond cond = new AdminInquirySearchCond();
        cond.setInclDelYn(true);
        when(inquiryMapper.selectAdminList(eq(cond), isNull(), eq(true)))
                .thenReturn(new ArrayList<>(List.of(row(InquiryType.HEALTH_RECORD), row(InquiryType.PLACE))));

        List<AdminInquirySummary> rows = service.search(cond, new PageQuery(null, null, null)).content();

        assertThat(rows).extracting(AdminInquirySummary::getInqryCtgr).containsExactly("ERROR", "ETC");
    }

    @Test
    void 없는_유형_상태_분류_코드나_뒤집힌_기간이면_INVALID_INPUT_VALUE() {
        AdminInquirySearchCond badType = new AdminInquirySearchCond();
        badType.setInqryTypeCd("XXXX0000");
        AdminInquirySearchCond badStatus = new AdminInquirySearchCond();
        badStatus.setInqryPrcsSttsCd("XXXX0000");
        AdminInquirySearchCond badCategory = new AdminInquirySearchCond();
        badCategory.setInqryCtgr("WRONG");
        AdminInquirySearchCond badPeriod = new AdminInquirySearchCond();
        badPeriod.setBgngYmd(LocalDate.of(2026, 11, 3));
        badPeriod.setEndYmd(LocalDate.of(2026, 11, 1));

        for (AdminInquirySearchCond cond : List.of(badType, badStatus, badCategory, badPeriod)) {
            assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null))).isInstanceOfSatisfying(
                    CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }
        verify(inquiryMapper, never()).selectAdminList(any(), any(), anyBoolean());
    }

    @Test
    void 없는_문의_상세는_INQUIRY_NOT_FOUND() {
        assertThatThrownBy(() -> service.get(INQ)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));
    }

    @Test
    void 삭제된_문의_상세는_조회되고_첨부는_비어_있다() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "Y"));

        AdminInquiryResponse res = service.get(INQ);

        assertThat(res.delYn()).isTrue();
        assertThat(res.imgs()).isEmpty();
        verify(inquiryMapper, never()).selectImageSummaries(anyLong());
    }

    @Test
    void 캐릭터_오류_문의_상세는_정렬_순서_0_첨부를_접수_시점_캐릭터_이미지로_준다() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry(null));
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(100L, 0), photo(101L, 1)));

        AdminInquiryResponse res = service.get(INQ);

        assertThat(res.charImgAtchFileSn()).isEqualTo(100L);
        assertThat(res.inqryCtgr()).isEqualTo("ERROR");
    }

    @Test
    void 캐릭터_사진_번호가_있으면_그_사진을_접수_시점_캐릭터_이미지로_준다() {
        Inquiry inquiry = characterInquiry(null);
        inquiry.setCharPhotoSn(1018L);
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry);
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(4118L, 10)));
        when(pixelCharacterService.getPhotoAtchFileSn(PIXEL_CHAR, 1018L)).thenReturn(2499L);

        AdminInquiryResponse res = service.get(INQ);

        assertThat(res.charImgAtchFileSn()).isEqualTo(2499L);
    }

    @Test
    void 접수_시점_캐릭터_이미지는_문의_첨부가_아니어도_열린다() {
        Inquiry inquiry = characterInquiry(null);
        inquiry.setCharPhotoSn(1018L);
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry);
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(4118L, 10)));
        when(pixelCharacterService.getPhotoAtchFileSn(PIXEL_CHAR, 1018L)).thenReturn(2499L);

        service.getImage(INQ, 2499L);

        verify(fileStore).load(2499L);
    }

    @Test
    void 문의_첨부도_캐릭터_이미지도_아닌_파일은_FILE_NOT_FOUND() {
        Inquiry inquiry = characterInquiry(null);
        inquiry.setCharPhotoSn(1018L);
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry);
        when(inquiryMapper.selectPhotos(INQ)).thenReturn(List.of(photo(4118L, 10)));
        when(pixelCharacterService.getPhotoAtchFileSn(PIXEL_CHAR, 1018L)).thenReturn(2499L);

        assertThatThrownBy(() -> service.getImage(INQ, 5000L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 삭제된_문의의_첨부_이미지는_FILE_NOT_FOUND() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "Y"));

        assertThatThrownBy(() -> service.getImage(INQ, 100L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 답변을_등록하면_InquiryService_answer를_부르고_감사_로그를_남긴다() {
        Inquiry answered = inquiry(InquiryType.ETC, InquiryStatus.ANSWERED, "N");
        answered.setAnsCn("답변");
        when(inquiryMapper.selectAdminInquiry(INQ))
                .thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "N"), answered);

        service.answer(ADMIN, INQ, new AdminInquiryAnswerRequest("답변"));

        verify(inquiryService).answer(INQ, ADMIN, "답변");
        verify(adminTaskLogService).record(eq(ADMIN), eq(AdminTaskType.PROCESS), eq("OTO_INQRY"), eq(INQ),
                anyString(), any(), any());
    }

    @Test
    void 삭제된_문의에_답변하면_INQUIRY_DELETED() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "Y"));

        assertThatThrownBy(() -> service.answer(ADMIN, INQ, new AdminInquiryAnswerRequest("답변")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_DELETED));
        verify(inquiryService, never()).answer(anyLong(), anyLong(), anyString());
    }

    @Test
    void 답변을_수정하면_내용만_바꾸고_알림_없이_감사_로그를_남긴다() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.ANSWERED, "N"));
        when(inquiryMapper.updateAnswerContent(INQ, "고친 답변", "CSCC0013")).thenReturn(1);

        service.updateAnswer(ADMIN, INQ, new AdminInquiryAnswerRequest("고친 답변"));

        verify(inquiryMapper).updateAnswerContent(INQ, "고친 답변", "CSCC0013");
        verify(inquiryService, never()).answer(anyLong(), anyLong(), anyString());
        verify(adminTaskLogService).record(eq(ADMIN), eq(AdminTaskType.UPDATE), eq("OTO_INQRY"), eq(INQ),
                anyString(), any(), any());
    }

    @Test
    void 답변_전_문의의_답변을_수정하면_INQUIRY_NOT_ANSWERED() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "N"));

        assertThatThrownBy(() -> service.updateAnswer(ADMIN, INQ, new AdminInquiryAnswerRequest("답변")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_ANSWERED));
        verify(inquiryMapper, never()).updateAnswerContent(anyLong(), anyString(), anyString());
    }

    @Test
    void 무료_재생성은_결과를_기록하고_재생성으로_오류_확인을_끝낸다() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry(null));
        when(inquiryMapper.updateCharErrResult(INQ, "CHRC0008")).thenReturn(1);

        service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0008"));

        verify(pixelCharacterService).finishErrorCheck(PIXEL_CHAR, true);
        verify(adminTaskLogService).record(eq(ADMIN), eq(AdminTaskType.PROCESS), eq("OTO_INQRY"), eq(INQ),
                anyString(), any(), any());
    }

    @Test
    void 원래_캐릭터_복원은_복원으로_오류_확인을_끝낸다() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry(null));
        when(inquiryMapper.updateCharErrResult(INQ, "CHRC0009")).thenReturn(1);

        service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0009"));

        verify(pixelCharacterService).finishErrorCheck(PIXEL_CHAR, false);
    }

    @Test
    void 이미_처리한_캐릭터_오류를_다시_처리하면_ALREADY_PROCESSED() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry("CHRC0009"));

        assertThatThrownBy(() -> service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0008")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED));
        verify(pixelCharacterService, never()).finishErrorCheck(anyLong(), anyBoolean());
    }

    @Test
    void 동시에_처리돼_결과_기록이_0건이면_ALREADY_PROCESSED() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry(null));
        when(inquiryMapper.updateCharErrResult(INQ, "CHRC0008")).thenReturn(0);

        assertThatThrownBy(() -> service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0008")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_PROCESSED));
        verify(pixelCharacterService, never()).finishErrorCheck(anyLong(), anyBoolean());
    }

    @Test
    void 캐릭터_문의가_아니거나_없는_결과_코드면_INVALID_INPUT_VALUE() {
        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(inquiry(InquiryType.ETC, InquiryStatus.RECEIVED, "N"));
        assertThatThrownBy(() -> service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0008")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        when(inquiryMapper.selectAdminInquiry(INQ)).thenReturn(characterInquiry(null));
        assertThatThrownBy(() -> service.processCharacterError(ADMIN, INQ, new AdminCharacterErrorRequest("CHRC0001")))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(inquiryMapper, never()).updateCharErrResult(anyLong(), anyString());
    }
}
