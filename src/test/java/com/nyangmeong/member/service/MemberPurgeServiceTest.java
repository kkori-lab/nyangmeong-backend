package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.mapper.MemberMapper;

/**
 * [검사 - 탈퇴 회원 파기 서비스]
 */
class MemberPurgeServiceTest {

    private static final Long MBR_SN = 7L;
    private static final String WITHDRAWN = MemberStatus.WITHDRAWN.getCode();

    private MemberMapper memberMapper;
    private FieldCryptoService fieldCryptoService;
    private MemberPurgeService service;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        fieldCryptoService = mock(FieldCryptoService.class);
        SystemSettingService systemSettingService = mock(SystemSettingService.class);
        SystemSetting setting = mock(SystemSetting.class);
        when(systemSettingService.get()).thenReturn(setting);
        when(setting.getWhdwlMbrKpngDayCnt()).thenReturn(30L);
        when(fieldCryptoService.encrypt(any())).thenReturn("암호문");
        service = new MemberPurgeService(memberMapper, systemSettingService, fieldCryptoService);
    }

    @Test
    void 대상은_보관일수로_조회하고_이미_파기한_회원을_뺀다() {
        when(memberMapper.selectPurgeTargetMbrSns(WITHDRAWN, 30, "PURGED%")).thenReturn(List.of(MBR_SN));

        assertThat(service.searchTargets()).containsExactly(MBR_SN);
    }

    @Test
    void 파기하면_식별값을_바꾸고_SNS_연동을_지운다() {
        when(memberMapper.updatePurge(anyLong(), anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(1);

        assertThat(service.purge(MBR_SN)).isTrue();

        String hmac = "PURGED" + "0".repeat(57) + "7";
        assertThat(hmac).hasSize(64);
        verify(memberMapper).updatePurge(MBR_SN, WITHDRAWN, 30, "암호문", hmac, "탈퇴회원#7", "SYSTEM");
        verify(memberMapper).deleteSnsLinks(MBR_SN);
    }

    @Test
    void 복구돼_바뀐_행이_없으면_건너뛴다() {
        when(memberMapper.updatePurge(anyLong(), anyString(), anyInt(), anyString(), anyString(), anyString(),
                anyString())).thenReturn(0);

        assertThat(service.purge(MBR_SN)).isFalse();

        verify(memberMapper, never()).deleteSnsLinks(eq(MBR_SN));
    }
}
