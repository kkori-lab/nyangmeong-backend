package com.nyangmeong.notification.dto;

import lombok.Data;

/**
 * [관리자 알림 템플릿 목록 행]
 * - FN-ADM-090-01 : 등록된 템플릿만, 없는 유형·채널 조합은 화면이 "기본 문구"로 표시
 */
@Data
public class AdminNotificationTemplateSummary {
    private Long notiTmpltSn;
    private String notiTypeCd;
    private String notiChnlCd;
    private String notiTtl;
    private String notiCn;
    private boolean useYn;
}
