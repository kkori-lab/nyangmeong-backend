package com.nyangmeong.notification.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.notification.domain.NotificationChannel;
import com.nyangmeong.notification.domain.NotificationTemplate;
import com.nyangmeong.notification.domain.NotificationTemplateType;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateRequest;
import com.nyangmeong.notification.dto.AdminNotificationTemplateCreateResponse;
import com.nyangmeong.notification.dto.AdminNotificationTemplateSummary;
import com.nyangmeong.notification.dto.AdminNotificationTemplateUpdateRequest;
import com.nyangmeong.notification.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 알림 템플릿 서비스]
 * - SCR-ADM-090 (FN-ADM-090-01~03)
 * - 등록은 발송 코드가 템플릿을 읽는 유형만 : 안 쓰이는 템플릿 방지
 * - 발송마다 템플릿을 다시 읽음 -> 저장 즉시 다음 발송부터 적용
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationTemplateService {

    private static final String TARGET_TABLE = "NOTI_TMPLT";

    private final NotificationMapper notificationMapper;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record TemplateState(String notiTypeCd, String notiChnlCd, String notiTtl, String notiCn, String useYn) {
        static TemplateState from(NotificationTemplate template) {
            return new TemplateState(template.getNotiTypeCd(), template.getNotiChnlCd(), template.getNotiTtl(),
                    template.getNotiCn(), template.getUseYn());
        }
    }

    /**
     * 알림 템플릿 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AdminNotificationTemplateSummary> search(String notiChnlCd) {
        boolean hasChannel = notiChnlCd != null && !notiChnlCd.isBlank();
        if (hasChannel) {
            checkChannel(notiChnlCd);
        }
        return notificationMapper.selectTemplates(hasChannel ? notiChnlCd : null);
    }

    /**
     * 알림 템플릿 등록
     */
    @Transactional
    public AdminNotificationTemplateCreateResponse create(Long actorSn, AdminNotificationTemplateCreateRequest req) {
        checkChannel(req.notiChnlCd());
        NotificationTemplateType type = NotificationTemplateType.from(req.notiTypeCd())
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_SUPPORTED));
        String notiTtl = normalizeTitle(req.notiTtl());
        checkVariables(type, notiTtl, req.notiCn());

        String actorId = String.valueOf(actorSn);
        NotificationTemplate template = new NotificationTemplate();
        template.setNotiTypeCd(req.notiTypeCd());
        template.setNotiChnlCd(req.notiChnlCd());
        template.setNotiTtl(notiTtl);
        template.setNotiCn(req.notiCn());
        template.setUseYn(toYn(req.useYn()));
        template.setRgtrId(actorId);
        template.setMdfrId(actorId);
        try {
            notificationMapper.insertTemplate(template);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_DUPLICATED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, template.getNotiTmpltSn(),
                "알림 템플릿 등록", null, TemplateState.from(template));
        return new AdminNotificationTemplateCreateResponse(template.getNotiTmpltSn());
    }

    /**
     * 알림 템플릿 수정
     */
    @Transactional
    public void update(Long actorSn, Long notiTmpltSn, AdminNotificationTemplateUpdateRequest req) {
        NotificationTemplate template = notificationMapper.selectTemplateBySn(notiTmpltSn);
        if (template == null) {
            throw new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
        }
        NotificationTemplateType type = NotificationTemplateType.from(template.getNotiTypeCd())
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_SUPPORTED));
        String notiTtl = normalizeTitle(req.notiTtl());
        checkVariables(type, notiTtl, req.notiCn());

        TemplateState before = TemplateState.from(template);
        template.setNotiTtl(notiTtl);
        template.setNotiCn(req.notiCn());
        template.setUseYn(toYn(req.useYn()));
        template.setMdfrId(String.valueOf(actorSn));
        notificationMapper.updateTemplate(template);
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, notiTmpltSn, "알림 템플릿 수정",
                before, TemplateState.from(template));
    }

    private void checkChannel(String notiChnlCd) {
        boolean isKnown = Arrays.stream(NotificationChannel.values()).anyMatch(channel -> channel.is(notiChnlCd));
        if (!isKnown) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void checkVariables(NotificationTemplateType type, String notiTtl, String notiCn) {
        if (!type.unknownVariables(notiTtl).isEmpty() || !type.unknownVariables(notiCn).isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 빈 제목 정리
     */
    private String normalizeTitle(String notiTtl) {
        return notiTtl == null || notiTtl.isBlank() ? null : notiTtl.strip();
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }
}
