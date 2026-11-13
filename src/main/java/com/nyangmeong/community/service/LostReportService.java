package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.BoardType;
import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.LostReport;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.domain.LostReportStatus;
import com.nyangmeong.community.domain.LostReportStatusHistory;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostImage;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.LostReportCreateRequest;
import com.nyangmeong.community.dto.LostReportInput;
import com.nyangmeong.community.dto.LostReportResponse;
import com.nyangmeong.community.dto.LostReportSearchCond;
import com.nyangmeong.community.dto.LostReportSummary;
import com.nyangmeong.community.dto.LostReportUpdateRequest;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.code.RegionService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.pet.service.BreedService;

import lombok.RequiredArgsConstructor;

/**
 * [실종 제보 서비스]
 * - SCR-CMU-002 목록·상세·등록·수정·삭제·찾았어요·게시 기간 연장
 * - 게시글(PST, 실종센터 게시판)과 제보(LOST_TPOF)가 1:1, 좋아요·댓글·신고는 /api/posts 와 같이 쓴다
 * - 실종지역은 목격 장소 좌표로 서버가 계산하고 전북 밖이면 거절한다
 * - 실종 상태는 등록 후 30일이 지나면 목록에서 빠지고 작성자만 상세를 본다(배치 없이 조회 조건으로 처리)
 * - 종료 7일 전·1일 전 작성자 알림은 LostReportExpiryAlarmJob 이 글마다 sendExpiryAlarm 을 부른다
 */
@Service
@RequiredArgsConstructor
public class LostReportService {

    static final int PAGE_SIZE = 12;
    static final Set<String> PERIODS = Set.of("TODAY", "WEEK", "MONTH", "OVER_MONTH");
    static final String FOUND_COMMENT = "🎉 여러분의 제보와 관심 덕분에 %s가 무사히 가족의 품으로 돌아갔어요.";
    /** 만료 사전 알림을 보내는 남은 일수 (화면설계서 SCR-CMU-002 : 종료 7일 전·1일 전) */
    static final Set<Integer> EXPIRY_ALARM_DAYS = Set.of(7, 1);
    static final String EXPIRY_ALARM_TTL = "실종 제보 게시 종료 안내";
    static final String EXPIRY_ALARM_PATH = "/community/missing/%d";
    private static final DateTimeFormatter EXPIRY_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final LostReportMapper lostReportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final FileStore fileStore;
    private final OwnerAuthorizer ownerAuthorizer;
    private final SystemSettingService systemSettingService;
    private final FieldCryptoService fieldCryptoService;
    private final RegionService regionService;
    private final BreedService breedService;
    private final EventPublisher eventPublisher;

    /**
     * 목록 조회 (실종 먼저·찾았어요 뒤, 한 페이지 12건. mineYn 이면 내 글만, 만료 글은 맨 뒤)
     */
    @Transactional(readOnly = true)
    public PageResponse<LostReportSummary> search(Long mbrSn, LostReportSearchCond cond, PageQuery pq) {
        String sttsCd = blankToNull(cond.getLostTpofSttsCd());
        boolean mine = cond.isMineYn();
        boolean allowed = LostReportStatus.MISSING.is(sttsCd) || LostReportStatus.FOUND.is(sttsCd)
                || (mine && LostReportStatus.EXPIRED.is(sttsCd));
        if (sttsCd != null && !allowed) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String petSeCd = blankToNull(cond.getPetSeCd());
        if (petSeCd != null) {
            breedService.checkPetSe(petSeCd);
        }
        String wtnsPrd = blankToNull(cond.getWtnsPrd());
        if (wtnsPrd != null && !PERIODS.contains(wtnsPrd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String keyword = blankToNull(cond.getKeyword());

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<LostReportSummary> rows = lostReportMapper.selectList(mbrSn, sttsCd, cond.getRgnSn(), petSeCd, wtnsPrd,
                keyword == null ? null : escapeLike(keyword.trim()), PostStatus.NORMAL.getCode(),
                LostReportStatus.MISSING.getCode(), LostReportStatus.FOUND.getCode(),
                LostReportStatus.EXPIRED.getCode(), mine);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 상세 조회 (조회수는 같은 회원 하루 한 번)
     */
    @Transactional
    public LostReportResponse getReport(Long mbrSn, Long lostTpofSn) {
        LostReportDetail detail = getDetailOrThrow(lostTpofSn);
        boolean owner = mbrSn.equals(detail.getMbrSn());
        if (!owner) {
            if (LostReportStatus.FOUND.is(detail.getLostTpofSttsCd())) {
                return LostReportResponse.closed(detail);
            }
            if (isExpired(detail)) {
                throw new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND);
            }
        }
        /** 작성자 본인 조회는 조회수에 넣지 않는다. 같은 회원·같은 날 이력이 이미 있으면 0건 */
        if (!owner && postMapper.insertViewIgnore(detail.getPstSn(), mbrSn) > 0) {
            postMapper.updateInqCnt(detail.getPstSn());
            detail.setInqCnt(detail.getInqCnt() + 1);
        }
        return toResponse(mbrSn, detail);
    }

    /**
     * 실종 제보 등록
     */
    @Transactional
    public LostReportResponse create(Long mbrSn, LostReportCreateRequest req) {
        validate(req);
        Long rgnSn = findRegionOrThrow(req);
        List<MultipartFile> images = attached(req.images());
        if (images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        Post post = new Post();
        post.setMbrSn(mbrSn);
        post.setBbsCd(BoardType.LOST.getCode());
        post.setPstSttsCd(PostStatus.NORMAL.getCode());
        post.setPstTtl(req.pstTtl());
        post.setPstCn(req.pstCn());
        post.setMdfrId(actorId(mbrSn));
        postMapper.insertPost(post);
        insertPostHistory(post.getPstSn(), mbrSn, null, PostStatus.NORMAL);

        LostReport report = toReport(req, rgnSn);
        report.setPstSn(post.getPstSn());
        report.setLostTpofSttsCd(LostReportStatus.MISSING.getCode());
        lostReportMapper.insertLostReport(report);
        insertReportHistory(report.getLostTpofSn(), mbrSn, null, LostReportStatus.MISSING);

        int sortSeq = 0;
        for (MultipartFile image : images) {
            insertImage(post.getPstSn(), fileStore.store(image, FileCategory.COMMUNITY_POST, FileOwner.member(mbrSn), actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getDetailOrThrow(report.getLostTpofSn()));
    }

    /**
     * 실종 제보 수정 (작성자, 실종 상태만, 모든 항목)
     */
    @Transactional
    public LostReportResponse update(Long mbrSn, Long lostTpofSn, LostReportUpdateRequest req) {
        LostReportDetail detail = getDetailOrThrow(lostTpofSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (!LostReportStatus.MISSING.is(detail.getLostTpofSttsCd()) || isExpired(detail)) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_EDITABLE);
        }
        validate(req);
        /** 좌표가 그대로면 실종지역 유지, 바뀌면 다시 계산 */
        boolean samePlace = req.wtnsPlcLat().compareTo(detail.getWtnsPlcLat()) == 0
                && req.wtnsPlcLot().compareTo(detail.getWtnsPlcLot()) == 0;
        Long rgnSn = samePlace ? detail.getRgnSn() : findRegionOrThrow(req);

        List<MultipartFile> images = attached(req.images());
        List<PostImage> remaining = new ArrayList<>(postMapper.selectImages(detail.getPstSn()));
        List<Long> removed = new ArrayList<>();
        if (req.keepAtchFileSns() != null) {
            for (PostImage image : remaining) {
                if (!req.keepAtchFileSns().contains(image.getAtchFileSn())) {
                    removed.add(image.getAtchFileSn());
                }
            }
            remaining.removeIf(image -> removed.contains(image.getAtchFileSn()));
        }
        if (remaining.size() + images.size() > maxImages()) {
            throw new CustomException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        /** 게시판은 바꾸지 않음(bbsCd null) */
        if (postMapper.updatePost(detail.getPstSn(), null, req.pstTtl(), req.pstCn(), actorId(mbrSn),
                PostStatus.NORMAL.getCode()) == 0) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND);
        }
        LostReport report = toReport(req, rgnSn);
        report.setLostTpofSn(lostTpofSn);
        if (lostReportMapper.updateLostReport(report, LostReportStatus.MISSING.getCode()) == 0) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_EDITABLE);
        }
        if (!removed.isEmpty()) {
            postMapper.updateImagesDeleted(detail.getPstSn(), removed, actorId(mbrSn));
        }
        int sortSeq = remaining.stream().mapToInt(PostImage::getSortSeq).max().orElse(-1) + 1;
        for (MultipartFile image : images) {
            insertImage(detail.getPstSn(), fileStore.store(image, FileCategory.COMMUNITY_POST, FileOwner.member(mbrSn), actorId(mbrSn)), sortSeq++, mbrSn);
        }
        return toResponse(mbrSn, getDetailOrThrow(lostTpofSn));
    }

    /**
     * 실종 제보 삭제 (작성자, 찾았어요·만료 글도 가능)
     */
    @Transactional
    public void delete(Long mbrSn, Long lostTpofSn) {
        LostReportDetail detail = getDetailOrThrow(lostTpofSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (postMapper.updateStatus(detail.getPstSn(), PostStatus.NORMAL.getCode(), PostStatus.DELETED.getCode(),
                actorId(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND);
        }
        insertPostHistory(detail.getPstSn(), mbrSn, PostStatus.NORMAL, PostStatus.DELETED);
    }

    /**
     * 찾았어요 전환 (작성자, 취소 불가, 완료 안내 댓글 자동 생성)
     */
    @Transactional
    public void markFound(Long mbrSn, Long lostTpofSn) {
        LostReportDetail detail = getDetailOrThrow(lostTpofSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (lostReportMapper.updateFound(lostTpofSn, LostReportStatus.MISSING.getCode(),
                LostReportStatus.FOUND.getCode()) == 0) {
            throw new CustomException(ErrorCode.LOST_REPORT_ALREADY_FOUND);
        }
        insertReportHistory(lostTpofSn, mbrSn, LostReportStatus.MISSING, LostReportStatus.FOUND);

        /** 작성자는 게시글 작성자, 등록 일시는 확인을 누른 시각 */
        Comment comment = new Comment();
        comment.setPstSn(detail.getPstSn());
        comment.setMbrSn(detail.getMbrSn());
        comment.setCmntCn(FOUND_COMMENT.formatted(detail.getPetNm()));
        commentMapper.insertAutoComment(comment);
    }

    /**
     * 게시 기간 30일 연장 (작성자, 종료 7일 전부터 종료 전까지 한 번)
     */
    @Transactional
    public void renew(Long mbrSn, Long lostTpofSn) {
        LostReportDetail detail = getDetailOrThrow(lostTpofSn);
        ownerAuthorizer.checkOwner(mbrSn, detail.getMbrSn());
        if (lostReportMapper.updateRenewed(lostTpofSn, LostReportStatus.MISSING.getCode()) == 0) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_RENEWABLE);
        }
    }

    /**
     * 만료 사전 알림 대상 제보 번호 (종료일이 오늘부터 days 일 뒤인 실종 제보)
     */
    @Transactional(readOnly = true)
    public List<Long> findExpiryAlarmTargets(int days) {
        checkExpiryAlarmDays(days);
        return lostReportMapper.selectExpiryAlarmTargets(LostReportStatus.MISSING.getCode(),
                PostStatus.NORMAL.getCode(), days);
    }

    /**
     * 만료 사전 알림 한 건 (작성자에게 인앱 알림, 수신 설정은 알림 저장 단계에서 거름)
     * - 보내기 직전 다시 확인 : 그 사이 찾았어요·연장·삭제된 글이면 보내지 않고 false
     */
    @Transactional
    public boolean sendExpiryAlarm(Long lostTpofSn, int days) {
        checkExpiryAlarmDays(days);
        if (!lostReportMapper.existsExpiryAlarmTarget(lostTpofSn, LostReportStatus.MISSING.getCode(),
                PostStatus.NORMAL.getCode(), days)) {
            return false;
        }
        LostReportDetail detail = lostReportMapper.selectDetail(lostTpofSn, PostStatus.NORMAL.getCode());
        if (detail == null) {
            return false;
        }
        String when = days == 1 ? "내일" : days + "일 뒤";
        String content = String.format("%s 제보가 %s(%s) 게시 종료돼요. 아직 찾고 계시면 30일 더 게시할 수 있어요.",
                detail.getPetNm(), when, detail.getExpryCrtrDt().format(EXPIRY_DATE));
        eventPublisher.publish(new NotificationEvent(detail.getMbrSn(), NotificationType.LOST_EXPIRY,
                EXPIRY_ALARM_TTL, content, String.format(EXPIRY_ALARM_PATH, lostTpofSn)));
        return true;
    }

    private void checkExpiryAlarmDays(int days) {
        if (!EXPIRY_ALARM_DAYS.contains(days)) {
            throw new IllegalArgumentException("만료 사전 알림 일수가 아님 : " + days);
        }
    }

    /**
     * 정상 게시글의 제보 조회
     */
    private LostReportDetail getDetailOrThrow(Long lostTpofSn) {
        LostReportDetail detail = lostReportMapper.selectDetail(lostTpofSn, PostStatus.NORMAL.getCode());
        if (detail == null) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 게시 기간이 끝났는지 (실종 상태의 만료 기준 일시가 지났거나 만료 상태)
     */
    private boolean isExpired(LostReport report) {
        if (LostReportStatus.EXPIRED.is(report.getLostTpofSttsCd())) {
            return true;
        }
        return LostReportStatus.MISSING.is(report.getLostTpofSttsCd())
                && report.getExpryCrtrDt().isBefore(LocalDateTime.now());
    }

    /**
     * 입력값 검사 (동의, 목격 일시, 품종, 반려동물 구분)
     */
    private void validate(LostReportInput in) {
        if (!Boolean.TRUE.equals(in.prvcRlsAgreYn())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (in.lastWtnsDt().isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        /** 품종은 선택 항목 : 없거나(빈 값 포함) 마스터 선택·직접 입력 중 하나만 (사람 결정 2026-11-06) */
        if (in.vrtySn() != null && emptyToNull(in.vrtyDrctInptNm()) != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        breedService.checkPetSe(in.petSeCd());
        if (in.vrtySn() != null) {
            breedService.getBreedOrThrow(in.vrtySn(), in.petSeCd());
        }
    }

    /**
     * 목격 장소 좌표의 전북 지역 번호
     */
    private Long findRegionOrThrow(LostReportInput in) {
        Long rgnSn = regionService.findJeonbukRgnSn(in.wtnsPlcLat(), in.wtnsPlcLot());
        if (rgnSn == null) {
            throw new CustomException(ErrorCode.LOST_REPORT_REGION_NOT_ALLOWED);
        }
        return rgnSn;
    }

    private LostReport toReport(LostReportInput in, Long rgnSn) {
        LostReport report = new LostReport();
        report.setRgnSn(rgnSn);
        report.setPetSeCd(in.petSeCd());
        report.setPetNm(in.petNm());
        report.setColrNm(in.colrNm());
        report.setChrcCn(blankToNull(in.chrcCn()));
        report.setCnplEnc(fieldCryptoService.encrypt(in.cnpl()));
        report.setWtnsPlcLat(in.wtnsPlcLat());
        report.setWtnsPlcLot(in.wtnsPlcLot());
        report.setLastWtnsDt(in.lastWtnsDt());
        report.setWtnsPlcNm(blankToNull(in.wtnsPlcNm()));
        report.setPrvcRlsAgreYn("Y");
        report.setVrtySn(in.vrtySn());
        report.setVrtyDrctInptNm(emptyToNull(in.vrtyDrctInptNm()));
        return report;
    }

    private LostReportResponse toResponse(Long mbrSn, LostReportDetail detail) {
        /** 만료 배치가 없어 DB에는 실종으로 남으므로, 목록과 같이 만료 상태로 내린다 */
        if (isExpired(detail)) {
            detail.setLostTpofSttsCd(LostReportStatus.EXPIRED.getCode());
        }
        List<PostImageSummary> imgs = postMapper.selectImages(detail.getPstSn()).stream()
                .map(PostImageSummary::from).toList();
        return LostReportResponse.of(detail, fieldCryptoService.decrypt(detail.getCnplEnc()), imgs,
                postMapper.existsLike(detail.getPstSn(), mbrSn), mbrSn.equals(detail.getMbrSn()));
    }

    private void insertPostHistory(Long pstSn, Long mbrSn, PostStatus before, PostStatus after) {
        PostStatusHistory history = new PostStatusHistory();
        history.setPstSn(pstSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        postMapper.insertStatusHistory(history);
    }

    private void insertReportHistory(Long lostTpofSn, Long mbrSn, LostReportStatus before, LostReportStatus after) {
        LostReportStatusHistory history = new LostReportStatusHistory();
        history.setLostTpofSn(lostTpofSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(before == null ? null : before.getCode());
        history.setAftrSttsCd(after.getCode());
        lostReportMapper.insertStatusHistory(history);
    }

    private void insertImage(Long pstSn, Long atchFileSn, int sortSeq, Long mbrSn) {
        PostImage image = new PostImage();
        image.setPstSn(pstSn);
        image.setAtchFileSn(atchFileSn);
        image.setSortSeq(sortSeq);
        image.setRgtrId(actorId(mbrSn));
        image.setMdfrId(actorId(mbrSn));
        postMapper.insertImage(image);
    }

    /**
     * 선택하지 않은 빈 파일 파트를 뺀 이미지 목록
     */
    private List<MultipartFile> attached(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    }

    private String actorId(Long mbrSn) {
        return String.valueOf(mbrSn);
    }

    private int maxImages() {
        return systemSettingService.get().getPstImgMaxCnt().intValue();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 빈 문자열만 없음으로 본다 (품종 직접 입력 : 공백만 있는 값은 요청 검증에서 400) */
    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
