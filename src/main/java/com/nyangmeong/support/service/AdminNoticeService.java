package com.nyangmeong.support.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NoticeNotificationEvent;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.support.domain.Notice;
import com.nyangmeong.support.dto.AdminNoticeCreateRequest;
import com.nyangmeong.support.dto.AdminNoticeImageResponse;
import com.nyangmeong.support.dto.AdminNoticeResponse;
import com.nyangmeong.support.dto.AdminNoticeSearchCond;
import com.nyangmeong.support.dto.AdminNoticeSummary;
import com.nyangmeong.support.dto.AdminNoticeTargetCountResponse;
import com.nyangmeong.support.dto.AdminNoticeUpdateRequest;
import com.nyangmeong.support.mapper.NoticeMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 공지사항 서비스]
 * - SCR-ADM-040 (FN-ADM-040-01~05)
 * - 삭제 없음 : USE_YN='N' 비노출
 * - 알림 발송 : 등록 때 체크한 것만, 정상 회원 전체에 1회 (이벤트 1건). 수정 때는 다시 보내지 않음 (결정 15)
 * - 상단 고정 개수 제한 없음 (결정 16)
 */
@Service
@RequiredArgsConstructor
public class AdminNoticeService {

    private static final String TARGET_TABLE = "NTC_MTTR";
    private static final int PAGE_SIZE = 20;
    private static final String NOTI_TTL = "새 공지사항이 있어요";
    private static final String NOTICE_PATH = "/notices/";

    private final NoticeMapper noticeMapper;
    private final MemberService memberService;
    private final EventPublisher eventPublisher;
    private final FileStore fileStore;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record NoticeState(String ntcMttrTtl, String upendFixYn, String useYn) {
    }

    /**
     * 관리자 공지 목록 조회 (비노출 포함, 고정 먼저 그다음 최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminNoticeSummary> search(AdminNoticeSearchCond cond, PageQuery pq) {
        String keyword = hasText(cond.getKeyword()) ? escapeLike(cond.getKeyword().strip()) : null;
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(PAGE_SIZE));
        List<AdminNoticeSummary> rows = noticeMapper.selectAdminList(keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 공지 상세 조회 (사용 여부 무관)
     */
    @Transactional(readOnly = true)
    public AdminNoticeResponse get(Long ntcMttrSn) {
        return AdminNoticeResponse.from(getOrThrow(ntcMttrSn));
    }

    /**
     * 공지 알림 대상 수 조회 (정상 회원 수)
     */
    @Transactional(readOnly = true)
    public AdminNoticeTargetCountResponse getNotificationTargetCount() {
        return new AdminNoticeTargetCountResponse(memberService.countNormalMembers());
    }

    /**
     * 공지 등록 (알림 발송 체크면 정상 회원 전체에 인앱 알림)
     */
    @Transactional
    public AdminNoticeResponse create(Long actorSn, AdminNoticeCreateRequest req) {
        if (req.notiSndngYn() && !req.useYn()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Notice notice = new Notice();
        notice.setNtcMttrTtl(req.ntcMttrTtl().strip());
        notice.setNtcMttrCn(req.ntcMttrCn());
        notice.setUpendFixYn(toYn(req.upendFixYn()));
        notice.setUseYn(toYn(req.useYn()));
        notice.setRgtrId(String.valueOf(actorSn));
        notice.setMdfrId(String.valueOf(actorSn));
        noticeMapper.insert(notice);

        if (req.notiSndngYn()) {
            sendNotification(notice);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, notice.getNtcMttrSn(), "공지 등록",
                null, new NoticeState(notice.getNtcMttrTtl(), notice.getUpendFixYn(), notice.getUseYn()));
        return AdminNoticeResponse.from(getOrThrow(notice.getNtcMttrSn()));
    }

    /**
     * 공지 수정·비노출 (알림은 다시 보내지 않음)
     */
    @Transactional
    public AdminNoticeResponse update(Long actorSn, Long ntcMttrSn, AdminNoticeUpdateRequest req) {
        Notice before = getOrThrow(ntcMttrSn);
        Notice notice = new Notice();
        notice.setNtcMttrSn(ntcMttrSn);
        notice.setNtcMttrTtl(req.ntcMttrTtl() == null ? null : req.ntcMttrTtl().strip());
        notice.setNtcMttrCn(req.ntcMttrCn());
        notice.setUpendFixYn(req.upendFixYn() == null ? null : toYn(req.upendFixYn()));
        notice.setUseYn(req.useYn() == null ? null : toYn(req.useYn()));
        notice.setMdfrId(String.valueOf(actorSn));
        noticeMapper.update(notice);

        Notice after = getOrThrow(ntcMttrSn);
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, ntcMttrSn, "공지 수정",
                new NoticeState(before.getNtcMttrTtl(), before.getUpendFixYn(), before.getUseYn()),
                new NoticeState(after.getNtcMttrTtl(), after.getUpendFixYn(), after.getUseYn()));
        return AdminNoticeResponse.from(after);
    }

    /**
     * 공지 본문 이미지 업로드 (공개 파일)
     */
    @Transactional
    public AdminNoticeImageResponse uploadImage(Long actorSn, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new AdminNoticeImageResponse(fileStore.storePublic(image, String.valueOf(actorSn)));
    }

    /**
     * 공지 전체 알림 이벤트 1건 발행, 발송 시각·대상 수(정상 회원 수) 기록
     * - notification 이 커밋 뒤 SQL 한 번으로 저장, 앱 알림 채널을 끈 회원은 거기서 걸러진다 (대상 수에는 포함)
     */
    private void sendNotification(Notice notice) {
        eventPublisher.publish(new NoticeNotificationEvent(NOTI_TTL, notice.getNtcMttrTtl(),
                NOTICE_PATH + notice.getNtcMttrSn()));
        noticeMapper.updateNotificationSent(notice.getNtcMttrSn(), memberService.countNormalMembers());
    }

    private Notice getOrThrow(Long ntcMttrSn) {
        Notice notice = noticeMapper.selectAdminNotice(ntcMttrSn);
        if (notice == null) {
            throw new CustomException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
