package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.batch.BatchType;

/**
 * [검사 - 탈퇴 회원 파기 배치]
 */
class MemberPurgeJobTest {

    private MemberPurgeService memberPurgeService;
    private MemberPurgeJob job;

    @BeforeEach
    void setUp() {
        memberPurgeService = mock(MemberPurgeService.class);
        job = new MemberPurgeJob(memberPurgeService);
    }

    @Test
    void 배치_구분과_주기() {
        assertThat(job.type()).isEqualTo(BatchType.MEMBER_PURGE);
        assertThat(job.cron()).isEqualTo("0 30 3 * * *");
    }

    @Test
    void 실제로_파기한_회원만_센다() {
        when(memberPurgeService.searchTargets()).thenReturn(List.of(1L, 2L, 3L));
        when(memberPurgeService.purge(1L)).thenReturn(true);
        when(memberPurgeService.purge(2L)).thenReturn(false);
        when(memberPurgeService.purge(3L)).thenReturn(true);

        assertThat(job.execute()).isEqualTo(2);
    }

    @Test
    void 대상이_없으면_0건이다() {
        when(memberPurgeService.searchTargets()).thenReturn(List.of());

        assertThat(job.execute()).isZero();
    }

    @Test
    void 한_회원이_실패해도_다음_회원을_파기하고_끝에_실패로_남긴다() {
        when(memberPurgeService.searchTargets()).thenReturn(List.of(1L, 2L, 3L));
        when(memberPurgeService.purge(1L)).thenThrow(new IllegalStateException("실패"));
        when(memberPurgeService.purge(2L)).thenReturn(true);
        when(memberPurgeService.purge(3L)).thenThrow(new IllegalStateException("실패"));

        assertThatThrownBy(job::execute).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("성공 1건").hasMessageContaining("실패 2건")
                .hasMessageContaining("1, 3");

        verify(memberPurgeService).purge(2L);
    }

    @Test
    void 실패한_회원이_많으면_앞_20개만_적고_나머지는_건수로_적는다() {
        List<Long> targets = java.util.stream.LongStream.rangeClosed(1, 25).boxed().toList();
        when(memberPurgeService.searchTargets()).thenReturn(targets);
        targets.forEach(mbrSn -> when(memberPurgeService.purge(mbrSn)).thenThrow(new IllegalStateException("실패")));

        assertThatThrownBy(job::execute).hasMessageContaining("실패 25건").hasMessageContaining("외 5건");
    }
}
