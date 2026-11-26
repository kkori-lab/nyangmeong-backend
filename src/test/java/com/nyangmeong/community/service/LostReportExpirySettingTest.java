package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.LostReport;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.dto.LostReportCreateRequest;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.code.RegionService;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.global.validation.ProfanityFilter;
import com.nyangmeong.pet.service.BreedService;

/**
 * [검사 - 실종 게시 기간이 운영 설정 일수를 따름]
 * - 등록 종료일, 연장 일수, 종료 사전 알림 문구
 */
class LostReportExpirySettingTest {

    private static final Long MBR = 1L;
    private static final Long PST = 7L;
    private static final Long LOST = 5L;
    private static final String NORMAL = "CMUC0003";
    private static final String MISSING = "CMUC0006";
    private static final long DAYS = 45L;

    private final LostReportMapper lostReportMapper = mock(LostReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final RegionService regionService = mock(RegionService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final LostReportService service = new LostReportService(lostReportMapper, postMapper,
            mock(CommentMapper.class), mock(FileStore.class), new OwnerAuthorizer(), systemSettingService,
            fieldCryptoService, regionService, mock(BreedService.class), eventPublisher, mock(ProfanityFilter.class));

    @BeforeEach
    void stubSetting() {
        SystemSetting setting = new SystemSetting();
        setting.setLostTpofExpryCrtrDayCnt(DAYS);
        when(systemSettingService.get()).thenReturn(setting);
    }

    private LostReportDetail detail() {
        LostReportDetail detail = new LostReportDetail();
        detail.setLostTpofSn(LOST);
        detail.setPstSn(PST);
        detail.setMbrSn(MBR);
        detail.setPstSttsCd(NORMAL);
        detail.setLostTpofSttsCd(MISSING);
        detail.setPetSeCd("PETC0001");
        detail.setPetNm("보리");
        detail.setCnplEnc("enc");
        detail.setLastWtnsDt(LocalDateTime.now().minusDays(1));
        detail.setExpryCrtrDt(LocalDateTime.of(2026, 11, 13, 10, 30));
        return detail;
    }

    @Test
    void 등록하면_종료일은_지금부터_설정_일수_뒤다() {
        when(regionService.findJeonbukRgnSn(any(), any())).thenReturn(3L);
        when(fieldCryptoService.encrypt(anyString())).thenReturn("enc");
        doAnswer(inv -> {
            inv.<Post>getArgument(0).setPstSn(PST);
            return 1;
        }).when(postMapper).insertPost(any(Post.class));
        doAnswer(inv -> {
            inv.<LostReport>getArgument(0).setLostTpofSn(LOST);
            return 1;
        }).when(lostReportMapper).insertLostReport(any(LostReport.class));
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail());
        LostReportCreateRequest req = new LostReportCreateRequest("PETC0001", "보리", null, null, "흰색", "빨간 목줄",
                LocalDateTime.now().minusDays(1), "서곡초 인근", new BigDecimal("35.8242"), new BigDecimal("127.148"),
                "010-1234-5678", "보리를 찾습니다", "서곡동 인근", null, true);

        LocalDateTime before = LocalDateTime.now();
        service.create(MBR, req);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LostReport> report = ArgumentCaptor.forClass(LostReport.class);
        verify(lostReportMapper).insertLostReport(report.capture());
        assertThat(report.getValue().getExpryCrtrDt()).isBetween(before.plusDays(DAYS), after.plusDays(DAYS));
    }

    @Test
    void 연장은_설정_일수만큼_늘린다() {
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail());
        when(lostReportMapper.updateRenewed(LOST, MISSING, 45)).thenReturn(1);

        service.renew(MBR, LOST);

        verify(lostReportMapper).updateRenewed(LOST, MISSING, 45);
    }

    @Test
    void 종료_알림은_설정_일수로_연장을_안내한다() {
        when(lostReportMapper.existsExpiryAlarmTarget(LOST, MISSING, NORMAL, 7)).thenReturn(true);
        when(lostReportMapper.selectDetail(LOST, NORMAL)).thenReturn(detail());

        service.sendExpiryAlarm(LOST, 7);

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().notiCn())
                .isEqualTo("보리 제보가 7일 뒤(2026.11.13) 게시 종료돼요. 아직 찾고 계시면 45일 더 게시할 수 있어요.");
    }
}
