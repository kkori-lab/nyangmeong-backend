package com.nyangmeong.health.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.health.domain.AttachmentType;
import com.nyangmeong.health.domain.CheckupAttachment;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.dto.CheckupAttachmentCreateRequest;
import com.nyangmeong.health.dto.CheckupAttachmentResponse;
import com.nyangmeong.health.dto.CheckupAttachmentSearchCond;
import com.nyangmeong.health.dto.CheckupAttachmentSummary;
import com.nyangmeong.health.dto.CheckupAttachmentUpdateRequest;
import com.nyangmeong.health.dto.CheckupSummary;
import com.nyangmeong.health.mapper.CareScheduleMapper;
import com.nyangmeong.health.mapper.CheckupAttachmentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [검진기록 첨부 서비스]
 * - 첨부 자료 목록·등록·교체·삭제 (SCR-HLT-005, FN-HLT-005-01)
 * - PDF 분석은 화면이 Python(POST /ai/checkups/lab-results)으로 직접 요청
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupAttachmentService {

    private static final int PAGE_SIZE = 20;

    private final CheckupAttachmentMapper checkupAttachmentMapper;
    private final CareScheduleMapper careScheduleMapper;
    private final CheckupService checkupService;
    private final PetAuthorizer petAuthorizer;
    private final FileStore fileStore;

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
