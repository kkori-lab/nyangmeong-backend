package com.nyangmeong.health.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.health.domain.AnalysisResult;
import com.nyangmeong.health.domain.AttachmentType;
import com.nyangmeong.health.domain.CheckupAttachment;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.dto.CheckupAttachmentCreateRequest;
import com.nyangmeong.health.dto.CheckupAttachmentResponse;
import com.nyangmeong.health.dto.CheckupAttachmentSearchCond;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;
import com.nyangmeong.health.dto.CheckupAttachmentUpdateRequest;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.dto.InspectionAnalysisResponse;
import com.nyangmeong.health.dto.InspectionAnalysisResponse.InspectionAnalysisItem;
import com.nyangmeong.health.dto.InspectionValueSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupAttachmentMapper;
import com.nyangmeong.health.mapper.InspectionValueMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [검진기록 첨부 서비스]
 * - 첨부 자료 목록·등록·교체·삭제, 검사결과지 PDF 분석 (SCR-HLT-005, FN-HLT-005-01·04)
 * - 분석 결과는 저장하지 않음 : 보호자가 SCR-HLT-007에서 확인 후 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupAttachmentService {

    private static final int PAGE_SIZE = 20;

    /** 분석 중인 첨부 (같은 첨부 중복 요청 차단) */
    private final Set<Long> analyzing = ConcurrentHashMap.newKeySet();

    private final CheckupAttachmentMapper checkupAttachmentMapper;
    private final InspectionValueMapper inspectionValueMapper;
    private final CareScheduleMapper careScheduleMapper;
    private final CheckupService checkupService;
    private final PetAuthorizer petAuthorizer;
    private final FileStore fileStore;
    private final InspectionAnalyzer inspectionAnalyzer;

    /**
     * 검진기록 첨부 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CheckupAttachmentSummary> search(Long mbrSn, Long petSn, CheckupAttachmentSearchCond cond,
            PageQuery pq) {
        if (cond.getCkupRcdSn() == null) {
            List<Long> ckupRcdSns = checkupService.searchAllCompleted(mbrSn, petSn).stream()
                    .map(CheckupSummary::getCkupRcdSn).toList();
            if (ckupRcdSns.isEmpty()) {
                return PageResponse.of(new PageInfo<>(List.of()));
            }
            PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
            return PageResponse.of(new PageInfo<>(checkupAttachmentMapper.selectListByCheckups(ckupRcdSns, cond)));
        }
        checkupService.getCheckup(mbrSn, petSn, cond.getCkupRcdSn());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<CheckupAttachmentSummary> rows = checkupAttachmentMapper.selectList(cond);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 첨부 파일 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getFile(Long mbrSn, Long petSn, Long ckupRcdAtchSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return fileStore.load(getOwn(petSn, ckupRcdAtchSn).getAtchFileSn());
    }

    /**
     * 검진기록 첨부 등록
     */
    @Transactional
    public CheckupAttachmentResponse create(Long mbrSn, Long petSn, CheckupAttachmentCreateRequest req) {
        checkupService.getCheckup(mbrSn, petSn, req.ckupRcdSn());
        String actorId = String.valueOf(mbrSn);

        CheckupAttachment attachment = new CheckupAttachment();
        attachment.setCkupRcdSn(req.ckupRcdSn());
        applyType(attachment, req.atchTypeCd(), req.imgKndCd(), req.shtRegnCd());
        attachment.setAtchFileSn(fileStore.storeDocument(req.file(), actorId));
        attachment.setRgtrId(actorId);
        attachment.setMdfrId(actorId);
        checkupAttachmentMapper.insertAttachment(attachment);
        return CheckupAttachmentResponse.from(checkupAttachmentMapper.selectSummary(attachment.getCkupRcdAtchSn()));
    }

    /**
     * 검진기록 첨부 수정 (파일 1건 교체·유형 변경)
     */
    @Transactional
    public CheckupAttachmentResponse update(Long mbrSn, Long petSn, Long ckupRcdAtchSn,
            CheckupAttachmentUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CheckupAttachment attachment = getOwn(petSn, ckupRcdAtchSn);
        String actorId = String.valueOf(mbrSn);

        String atchTypeCd = req.atchTypeCd() != null ? req.atchTypeCd() : attachment.getAtchTypeCd();
        String imgKndCd = req.imgKndCd() != null ? req.imgKndCd() : attachment.getImgKndCd();
        String shtRegnCd = req.shtRegnCd() != null ? req.shtRegnCd() : attachment.getShtRegnCd();
        applyType(attachment, atchTypeCd, imgKndCd, shtRegnCd);
        if (req.file() != null && !req.file().isEmpty()) {
            attachment.setAtchFileSn(fileStore.storeDocument(req.file(), actorId));
        }
        checkupAttachmentMapper.updateAttachment(attachment, actorId);
        return CheckupAttachmentResponse.from(checkupAttachmentMapper.selectSummary(ckupRcdAtchSn));
    }

    /**
     * 검진기록 첨부 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long ckupRcdAtchSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwn(petSn, ckupRcdAtchSn);
        checkupAttachmentMapper.deleteAttachment(ckupRcdAtchSn, String.valueOf(mbrSn));
    }

    /**
     * 검사결과지 PDF 분석
     */
    @Transactional(readOnly = true)
    public InspectionAnalysisResponse analyze(Long mbrSn, Long petSn, Long ckupRcdAtchSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwn(petSn, ckupRcdAtchSn);
        CheckupAttachmentSummary attachment = checkupAttachmentMapper.selectSummary(ckupRcdAtchSn);
        if (!AttachmentType.INSPECTION_RESULT.is(attachment.getAtchTypeCd())
                || !FileExtension.PDF.is(attachment.getFileExtnCd())) {
            throw new CustomException(ErrorCode.CHECKUP_ATTACHMENT_NOT_ANALYZABLE);
        }
        if (!analyzing.add(ckupRcdAtchSn)) {
            throw new CustomException(ErrorCode.CHECKUP_ANALYSIS_IN_PROGRESS);
        }
        try {
            return analyze(attachment);
        } finally {
            analyzing.remove(ckupRcdAtchSn);
        }
    }

    /**
     * 분석 실행과 이미 등록된 항목 표시
     */
    private InspectionAnalysisResponse analyze(CheckupAttachmentSummary attachment) {
        InspectionAnalyzer.Analysis analysis;
        try {
            CheckupAttachment stored = checkupAttachmentMapper.selectAttachment(attachment.getCkupRcdAtchSn());
            analysis = inspectionAnalyzer.analyze(fileStore.load(stored.getAtchFileSn()).content());
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            /** 외부 API 오류·시간 초과는 200 + 일시 오류 */
            log.warn("[CheckupAttachment] PDF 분석 실패 ckupRcdAtchSn={}", attachment.getCkupRcdAtchSn(), e);
            return new InspectionAnalysisResponse(attachment.getCkupRcdAtchSn(), attachment.getCkupRcdSn(),
                    AnalysisResult.TEMPORARY_ERROR.name(), List.of(), List.of());
        }
        if (analysis.items().isEmpty()) {
            return new InspectionAnalysisResponse(attachment.getCkupRcdAtchSn(), attachment.getCkupRcdSn(),
                    AnalysisResult.NOT_RECOGNIZED.name(), List.of(), analysis.excludedArtclNms());
        }

        List<InspectionValueSummary> registered = inspectionValueMapper.selectArticles(attachment.getCkupRcdSn());
        List<InspectionAnalysisItem> items = analysis.items().stream()
                .map(item -> new InspectionAnalysisItem(item.inspArtclCd(), item.inspArtclDrctInptNm(),
                        item.rsltNvl(), item.unitNm(), item.refLlmtNvl(), item.refUplmtNvl(),
                        registered.stream().anyMatch(value -> isSameArticle(value, item))))
                .toList();
        return new InspectionAnalysisResponse(attachment.getCkupRcdAtchSn(), attachment.getCkupRcdSn(),
                AnalysisResult.SUCCESS.name(), items, analysis.excludedArtclNms());
    }

    private boolean isSameArticle(InspectionValueSummary value, InspectionAnalyzer.Recognized item) {
        return Objects.equals(value.getInspArtclCd(), item.inspArtclCd())
                && Objects.equals(value.getInspArtclDrctInptNm(), item.inspArtclDrctInptNm());
    }

    /**
     * 첨부 유형 검증·반영
     * - 영상 사진만 영상 종류·촬영 부위 저장 (CK_CKUP_RCD_ATCH_IMG_PAIR)
     */
    private void applyType(CheckupAttachment attachment, String atchTypeCd, String imgKndCd, String shtRegnCd) {
        AttachmentType type = AttachmentType.of(atchTypeCd).orElseThrow(this::invalid);
        attachment.setAtchTypeCd(type.getCode());
        if (type != AttachmentType.IMAGE) {
            attachment.setImgKndCd(null);
            attachment.setShtRegnCd(null);
            return;
        }
        if (!isChildCode(HealthCodeGroup.IMAGE_KIND, imgKndCd)
                || !isChildCode(HealthCodeGroup.SHOOTING_REGION, shtRegnCd)) {
            throw invalid();
        }
        attachment.setImgKndCd(imgKndCd);
        attachment.setShtRegnCd(shtRegnCd);
    }

    private boolean isChildCode(HealthCodeGroup group, String comCd) {
        return comCd != null && !comCd.isBlank() && careScheduleMapper.existsChildCode(group.getCode(), comCd);
    }

    /**
     * 경로 반려동물의 검진기록 첨부 조회
     */
    private CheckupAttachment getOwn(Long petSn, Long ckupRcdAtchSn) {
        CheckupAttachment attachment = checkupAttachmentMapper.selectAttachment(ckupRcdAtchSn);
        if (attachment == null || !checkupService.existsCheckup(petSn, attachment.getCkupRcdSn())) {
            throw new CustomException(ErrorCode.CHECKUP_ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
