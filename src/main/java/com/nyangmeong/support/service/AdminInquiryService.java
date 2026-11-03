package com.nyangmeong.support.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.service.PixelCharacterService;
import com.nyangmeong.support.domain.CharacterErrorResult;
import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryCategory;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatus;
import com.nyangmeong.support.domain.InquiryType;
import com.nyangmeong.support.dto.AdminCharacterErrorRequest;
import com.nyangmeong.support.dto.AdminInquiryAnswerRequest;
import com.nyangmeong.support.dto.AdminInquiryResponse;
import com.nyangmeong.support.dto.AdminInquirySearchCond;
import com.nyangmeong.support.dto.AdminInquirySummary;
import com.nyangmeong.support.dto.InquiryImageSummary;
import com.nyangmeong.support.mapper.InquiryMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 1:1 문의 서비스]
 * - SCR-ADM-042 (FN-ADM-042-01~08), 답변 등록은 InquiryService.answer() 재사용
 * - 회원이 삭제한 문의 : 조회만, 첨부는 함께 삭제되어 빈 목록
 * - 캐릭터 오류 처리 : 결과 기록 뒤 pet 의 finishErrorCheck, 회원 알림은 pet 이 발행
 */
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private static final String TARGET_TABLE = "OTO_INQRY";
    private static final int PAGE_SIZE = 20;
    private static final int CHARACTER_IMAGE_SORT_SEQ = 0;

    private final InquiryMapper inquiryMapper;
    private final InquiryService inquiryService;
    private final PixelCharacterService pixelCharacterService;
    private final FileStore fileStore;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record InquiryState(String inqryPrcsSttsCd, String ansCn, String charErrPrcsRsltCd) {
        static InquiryState of(Inquiry inquiry) {
            return new InquiryState(inquiry.getInqryPrcsSttsCd(), inquiry.getAnsCn(),
                    inquiry.getCharErrPrcsRsltCd());
        }
    }

    /**
     * 관리자 문의 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminInquirySummary> search(AdminInquirySearchCond cond, PageQuery pq) {
        cond.setInqryTypeCd(blankToNull(cond.getInqryTypeCd()));
        cond.setInqryPrcsSttsCd(blankToNull(cond.getInqryPrcsSttsCd()));
        String inqryCtgr = blankToNull(cond.getInqryCtgr());
        if (cond.getInqryTypeCd() != null && !InquiryType.exists(cond.getInqryTypeCd())
                || cond.getInqryPrcsSttsCd() != null && !InquiryStatus.exists(cond.getInqryPrcsSttsCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<String> inqryTypeCds = null;
        if (inqryCtgr != null) {
            InquiryCategory category = InquiryCategory.from(inqryCtgr);
            if (category == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            inqryTypeCds = category.typeCodes();
        }
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<AdminInquirySummary> rows = inquiryMapper.selectAdminList(cond, inqryTypeCds,
                Boolean.TRUE.equals(cond.getInclDelYn()));
        rows.forEach(row -> row.setInqryCtgr(InquiryCategory.of(row.getInqryTypeCd()).name()));
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 문의 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminInquiryResponse get(Long otoInqrySn) {
        return toResponse(getOrThrow(otoInqrySn));
    }

    /**
     * 관리자 문의 첨부 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long otoInqrySn, Long atchFileSn) {
        Inquiry inquiry = getOrThrow(otoInqrySn);
        boolean attached = !isDeleted(inquiry) && (inquiryMapper.selectPhotos(otoInqrySn).stream()
                .anyMatch(photo -> photo.getAtchFileSn().equals(atchFileSn))
                || atchFileSn.equals(characterImage(inquiry)));
        if (!attached) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    /**
     * 답변 등록
     */
    @Transactional
    public AdminInquiryResponse answer(Long actorSn, Long otoInqrySn, AdminInquiryAnswerRequest req) {
        Inquiry before = getActiveOrThrow(otoInqrySn);
        inquiryService.answer(otoInqrySn, actorSn, req.ansCn());

        Inquiry after = getOrThrow(otoInqrySn);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, otoInqrySn, "1:1 문의 답변 등록",
                InquiryState.of(before), InquiryState.of(after));
        return toResponse(after);
    }

    /**
     * 답변 수정
     */
    @Transactional
    public AdminInquiryResponse updateAnswer(Long actorSn, Long otoInqrySn, AdminInquiryAnswerRequest req) {
        Inquiry before = getActiveOrThrow(otoInqrySn);
        if (!InquiryStatus.ANSWERED.is(before.getInqryPrcsSttsCd())) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_ANSWERED);
        }
        if (inquiryMapper.updateAnswerContent(otoInqrySn, req.ansCn(), InquiryStatus.ANSWERED.getCode()) == 0) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_ANSWERED);
        }

        Inquiry after = getOrThrow(otoInqrySn);
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, otoInqrySn, "1:1 문의 답변 수정",
                InquiryState.of(before), InquiryState.of(after));
        return toResponse(after);
    }

    /**
     * 캐릭터 오류 처리
     */
    @Transactional
    public AdminInquiryResponse processCharacterError(Long actorSn, Long otoInqrySn,
            AdminCharacterErrorRequest req) {
        Inquiry before = getActiveOrThrow(otoInqrySn);
        CharacterErrorResult result = CharacterErrorResult.from(req.charErrPrcsRsltCd().strip());
        if (result == null || before.getPixelCharSn() == null
                || !InquiryType.PIXEL_CHARACTER.is(before.getInqryTypeCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (before.getCharErrPrcsRsltCd() != null
                || inquiryMapper.updateCharErrResult(otoInqrySn, result.getCode()) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        pixelCharacterService.finishErrorCheck(before.getPixelCharSn(), result == CharacterErrorResult.REGENERATE);

        Inquiry after = getOrThrow(otoInqrySn);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, otoInqrySn, "캐릭터 오류 처리",
                InquiryState.of(before), InquiryState.of(after));
        return toResponse(after);
    }

    private Inquiry getOrThrow(Long otoInqrySn) {
        Inquiry inquiry = inquiryMapper.selectAdminInquiry(otoInqrySn);
        if (inquiry == null) {
            throw new CustomException(ErrorCode.INQUIRY_NOT_FOUND);
        }
        return inquiry;
    }

    /**
     * 삭제되지 않은 문의 조회
     */
    private Inquiry getActiveOrThrow(Long otoInqrySn) {
        Inquiry inquiry = getOrThrow(otoInqrySn);
        if (isDeleted(inquiry)) {
            throw new CustomException(ErrorCode.INQUIRY_DELETED);
        }
        return inquiry;
    }

    private boolean isDeleted(Inquiry inquiry) {
        return "Y".equals(inquiry.getDelYn());
    }

    /**
     * 응답 변환 (코드 이름·닉네임·첨부·이력 포함)
     */
    private AdminInquiryResponse toResponse(Inquiry inquiry) {
        Long otoInqrySn = inquiry.getOtoInqrySn();
        boolean deleted = isDeleted(inquiry);
        List<InquiryImageSummary> imgs = deleted ? List.of() : inquiryMapper.selectImageSummaries(otoInqrySn);
        Long charImgAtchFileSn = deleted ? null : characterImage(inquiry);
        AdminInquiryResponse.Extra extra = new AdminInquiryResponse.Extra(
                inquiryMapper.selectCodeName(inquiry.getInqryTypeCd()),
                InquiryCategory.of(inquiry.getInqryTypeCd()).name(),
                inquiryMapper.selectCodeName(inquiry.getInqryPrcsSttsCd()),
                inquiryMapper.selectNickname(inquiry.getMbrSn()),
                inquiry.getPrcsMbrSn() == null ? null : inquiryMapper.selectNickname(inquiry.getPrcsMbrSn()),
                inquiry.getCharErrPrcsRsltCd() == null ? null
                        : inquiryMapper.selectCodeName(inquiry.getCharErrPrcsRsltCd()),
                charImgAtchFileSn, imgs, inquiryMapper.selectStatusHistories(otoInqrySn));
        return AdminInquiryResponse.from(inquiry, extra);
    }

    /**
     * 접수 시점 캐릭터 이미지 : 문의의 캐릭터 사진(CHAR_PHOTO_SN), 없으면 정렬 순서 0 첨부 (InquiryService.create)
     */
    private Long characterImage(Inquiry inquiry) {
        if (inquiry.getPixelCharSn() == null) {
            return null;
        }
        if (inquiry.getCharPhotoSn() != null) {
            Long photoAtchFileSn = pixelCharacterService.getPhotoAtchFileSn(inquiry.getPixelCharSn(),
                    inquiry.getCharPhotoSn());
            if (photoAtchFileSn != null) {
                return photoAtchFileSn;
            }
        }
        return inquiryMapper.selectPhotos(inquiry.getOtoInqrySn()).stream()
                .filter(photo -> photo.getSortSeq() != null && photo.getSortSeq() == CHARACTER_IMAGE_SORT_SEQ)
                .map(InquiryPhoto::getAtchFileSn)
                .findFirst()
                .orElse(null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
