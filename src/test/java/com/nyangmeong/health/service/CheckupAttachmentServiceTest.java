package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.health.domain.AnalysisResult;
import com.nyangmeong.health.domain.AttachmentType;
import com.nyangmeong.health.domain.CheckupAttachment;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.InspectionArticle;
import com.nyangmeong.health.dto.CheckupAttachmentCreateRequest;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;
import com.nyangmeong.health.dto.CheckupAttachmentUpdateRequest;
import com.nyangmeong.health.dto.InspectionAnalysisResponse;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupAttachmentMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;

/**
 * [검사 - 검진기록 첨부·PDF 분석]
 * - 영상 사진 필수 입력, 검사결과지 PDF만 분석 (FN-HLT-005-01·04)
 */
class CheckupAttachmentServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CKUP = 500L;
    private static final Long ATCH = 900L;
    private static final Long FILE = 3000L;
    private static final String ULTRASOUND = "HLTC0069";
    private static final String ABDOMEN = "HLTC0074";

    private final CheckupAttachmentMapper mapper = mock(CheckupAttachmentMapper.class);
    private final InspectionValueMapper inspectionValueMapper = mock(InspectionValueMapper.class);
    private final CareScheduleMapper careScheduleMapper = mock(CareScheduleMapper.class);
    private final CheckupService checkupService = mock(CheckupService.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final InspectionAnalyzer analyzer = new InspectionAnalyzer();
    private final CheckupAttachmentService service = new CheckupAttachmentService(mapper, inspectionValueMapper,
            careScheduleMapper, checkupService, petAuthorizer, fileStore, analyzer);

    private final MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1 });

    @BeforeEach
    void setUp() {
        when(careScheduleMapper.existsChildCode(HealthCodeGroup.IMAGE_KIND.getCode(), ULTRASOUND)).thenReturn(true);
        when(careScheduleMapper.existsChildCode(HealthCodeGroup.SHOOTING_REGION.getCode(), ABDOMEN)).thenReturn(true);
        when(fileStore.storeDocument(any(), anyString())).thenReturn(FILE);
        doAnswer(call -> {
            CheckupAttachment attachment = call.getArgument(0);
            attachment.setCkupRcdAtchSn(ATCH);
            return 1;
        }).when(mapper).insertAttachment(any());
        when(mapper.selectSummary(ATCH)).thenReturn(summary(AttachmentType.INSPECTION_RESULT, FileExtension.PDF));
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(true);
    }

    private CheckupAttachmentSummary summary(AttachmentType type, FileExtension extension) {
        CheckupAttachmentSummary summary = new CheckupAttachmentSummary();
        summary.setCkupRcdAtchSn(ATCH);
        summary.setCkupRcdSn(CKUP);
        summary.setAtchTypeCd(type.getCode());
        summary.setFileExtnCd(extension.getCode());
        return summary;
    }

    private CheckupAttachment stored(AttachmentType type) {
        CheckupAttachment attachment = new CheckupAttachment();
        attachment.setCkupRcdAtchSn(ATCH);
        attachment.setCkupRcdSn(CKUP);
        attachment.setAtchFileSn(FILE);
        attachment.setAtchTypeCd(type.getCode());
        when(mapper.selectAttachment(ATCH)).thenReturn(attachment);
        return attachment;
    }

    @Test
    void 영상_사진은_영상_종류와_촬영_부위를_함께_저장한다() {
        service.create(MBR, PET, new CheckupAttachmentCreateRequest(CKUP, file, AttachmentType.IMAGE.getCode(),
                ULTRASOUND, ABDOMEN));

        verify(checkupService).getCheckup(MBR, PET, CKUP);
        verify(fileStore).storeDocument(file, "1");
        verify(mapper).insertAttachment(argThat(attachment -> ULTRASOUND.equals(attachment.getImgKndCd())
                && ABDOMEN.equals(attachment.getShtRegnCd()) && FILE.equals(attachment.getAtchFileSn())));
    }

    @Test
    void 영상_사진에_영상_종류가_없으면_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new CheckupAttachmentCreateRequest(CKUP, file, AttachmentType.IMAGE.getCode(), null, ABDOMEN)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(fileStore, never()).storeDocument(any(), anyString());
    }

    @Test
    void 영상_사진이_아니면_영상_종류와_촬영_부위를_비운다() {
        service.create(MBR, PET, new CheckupAttachmentCreateRequest(CKUP, file,
                AttachmentType.VACCINATION_CERTIFICATE.getCode(), ULTRASOUND, ABDOMEN));

        verify(mapper).insertAttachment(argThat(attachment -> attachment.getImgKndCd() == null
                && attachment.getShtRegnCd() == null));
    }

    @Test
    void 알_수_없는_첨부_유형은_400() {
        assertThatThrownBy(() -> service.create(MBR, PET,
                new CheckupAttachmentCreateRequest(CKUP, file, "HLTC9999", null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 교체는_같은_첨부의_파일만_바꾼다() {
        stored(AttachmentType.INSPECTION_RESULT);
        when(fileStore.storeDocument(any(), anyString())).thenReturn(4000L);

        service.update(MBR, PET, ATCH, new CheckupAttachmentUpdateRequest(file, null, null, null));

        verify(mapper).updateAttachment(argThat(attachment -> ATCH.equals(attachment.getCkupRcdAtchSn())
                && Long.valueOf(4000L).equals(attachment.getAtchFileSn())
                && AttachmentType.INSPECTION_RESULT.is(attachment.getAtchTypeCd())), eq("1"));
    }

    @Test
    void 영상_사진으로_바꾸면서_영상_종류가_없으면_400() {
        stored(AttachmentType.INSPECTION_RESULT);

        assertThatThrownBy(() -> service.update(MBR, PET, ATCH,
                new CheckupAttachmentUpdateRequest(null, AttachmentType.IMAGE.getCode(), null, ABDOMEN)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 다른_반려동물의_첨부는_404() {
        stored(AttachmentType.INSPECTION_RESULT);
        when(checkupService.existsCheckup(PET, CKUP)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(MBR, PET, ATCH))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_ATTACHMENT_NOT_FOUND);
        verify(mapper, never()).deleteAttachment(any(), anyString());
    }

    @Test
    void 검사결과지_PDF를_분석하면_이미_있는_항목을_표시한다() {
        stored(AttachmentType.INSPECTION_RESULT);
        when(fileStore.load(FILE)).thenReturn(new StoredFile(new byte[] { 1 }, "application/pdf", "a.pdf"));
        InspectionValueSummary registered = new InspectionValueSummary();
        registered.setInspArtclCd(InspectionArticle.WBC.getCode());
        when(inspectionValueMapper.selectArticles(CKUP)).thenReturn(List.of(registered));

        InspectionAnalysisResponse response = service.analyze(MBR, PET, ATCH);

        assertThat(response.anlysRslt()).isEqualTo(AnalysisResult.SUCCESS.name());
        assertThat(response.ckupRcdSn()).isEqualTo(CKUP);
        assertThat(response.items()).extracting("inspArtclCd", "registered").containsExactly(
                tuple(InspectionArticle.WBC.getCode(), true),
                tuple(InspectionArticle.CREA.getCode(), false));
    }

    @Test
    void 접종_증명서는_분석할_수_없다() {
        stored(AttachmentType.VACCINATION_CERTIFICATE);
        when(mapper.selectSummary(ATCH)).thenReturn(summary(AttachmentType.VACCINATION_CERTIFICATE, FileExtension.PDF));

        assertThatThrownBy(() -> service.analyze(MBR, PET, ATCH))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_ATTACHMENT_NOT_ANALYZABLE);
    }

    @Test
    void 검사결과지라도_이미지면_분석할_수_없다() {
        stored(AttachmentType.INSPECTION_RESULT);
        when(mapper.selectSummary(ATCH)).thenReturn(summary(AttachmentType.INSPECTION_RESULT, FileExtension.PNG));

        assertThatThrownBy(() -> service.analyze(MBR, PET, ATCH))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKUP_ATTACHMENT_NOT_ANALYZABLE);
    }

    @Test
    void 분석_중_외부_오류는_일시_오류로_돌려준다() {
        stored(AttachmentType.INSPECTION_RESULT);
        when(fileStore.load(FILE)).thenThrow(new IllegalStateException("디스크 오류"));

        InspectionAnalysisResponse response = service.analyze(MBR, PET, ATCH);

        assertThat(response.anlysRslt()).isEqualTo(AnalysisResult.TEMPORARY_ERROR.name());
        assertThat(response.items()).isEmpty();
    }
}
