package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.FakeSnsOAuthClient;
import com.nyangmeong.global.external.SnsOAuthClient;
import com.nyangmeong.global.external.SnsProvider;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberSnsLinkRequest;

/**
 * [검사 - SNS 계정 연동 서비스]
 * - 제공자 프로필의 계정 ID로 연동을 부르고, 모르는 제공자나 제공자 실패는 연동을 부르지 않는다
 * - 제공자 호출은 가짜 클라이언트(fake-이름 코드)로 대신한다
 */
class MemberSnsLinkServiceTest {

    private static final String NAVER = SnsProvider.NAVER.getCode();

    private final SnsOAuthClient snsOAuthClient = new FakeSnsOAuthClient();
    private final MemberService memberService = mock(MemberService.class);
    private final MemberSnsLinkService service = new MemberSnsLinkService(snsOAuthClient, memberService);

    private static void assertCode(Throwable thrown, ErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 제공자_프로필의_계정_ID로_로그인_회원에_연동한다() {
        MemberResponse response = new MemberResponse("a@example.com", "냥멍", "01012345678", null, "USRC0006", false,
                List.of(), null, null);
        /** 가짜 제공자의 계정 ID는 "제공자코드:인가코드" */
        when(memberService.linkSns(7L, NAVER, NAVER + ":fake-a")).thenReturn(response);

        assertThat(service.link(7L, new MemberSnsLinkRequest(NAVER, "fake-a", "s"))).isSameAs(response);
    }

    @Test
    void 모르는_제공자면_400이고_연동을_부르지_않는다() {
        assertCode(catchThrowable(() -> service.link(7L, new MemberSnsLinkRequest("USRC0006", "fake-a", null))),
                ErrorCode.BAD_REQUEST);
        verify(memberService, never()).linkSns(anyLong(), anyString(), anyString());
    }

    @Test
    void 제공자_호출이_실패하면_연동용_SNS_LINK_FAILED고_연동을_부르지_않는다() {
        assertCode(catchThrowable(() -> service.link(7L, new MemberSnsLinkRequest(NAVER, "wrong-code", null))),
                ErrorCode.SNS_LINK_FAILED);
        verify(memberService, never()).linkSns(anyLong(), anyString(), anyString());
    }
}
