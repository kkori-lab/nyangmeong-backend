package com.nyangmeong.global.security.stomp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import com.nyangmeong.global.security.domain.LoginMember;

import static org.mockito.Mockito.when;

/**
 * [검사 - 채팅 WebSocket 연결·프레임 검사]
 * - 인증된 사용자만 연결하고, 구독은 내 개인 큐만, 보내기는 보는 방 알림만
 */
class StompAuthChannelInterceptorTest {

    private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);

    private Message<?> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void 인증된_사용자의_연결은_통과한다() {
        Message<?> message = frame(StompCommand.CONNECT, null, StompPrincipal.of(1L));

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void 인증되지_않은_연결은_거부한다() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void 내_개인_큐만_구독할_수_있다() {
        Message<?> mine = frame(StompCommand.SUBSCRIBE, "/user/queue/chat", StompPrincipal.of(1L));

        assertThat(interceptor.preSend(mine, channel)).isSameAs(mine);
        for (String destination : new String[] { "/queue/chat", "/topic/all", "/user/queue/other" }) {
            assertThatThrownBy(() -> interceptor
                    .preSend(frame(StompCommand.SUBSCRIBE, destination, StompPrincipal.of(1L)), channel))
                    .isInstanceOf(MessageDeliveryException.class);
        }
    }

    @Test
    void 보는_방_알림만_보낼_수_있다() {
        Message<?> view = frame(StompCommand.SEND, "/app/chat/view", StompPrincipal.of(1L));

        assertThat(interceptor.preSend(view, channel)).isSameAs(view);
        assertThatThrownBy(() -> interceptor
                .preSend(frame(StompCommand.SEND, "/app/chat/other", StompPrincipal.of(1L)), channel))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor
                .preSend(frame(StompCommand.SEND, "/queue/chat", StompPrincipal.of(1L)), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void 연결_종료_같은_다른_프레임은_통과한다() {
        Message<?> disconnect = frame(StompCommand.DISCONNECT, null, StompPrincipal.of(1L));

        assertThat(interceptor.preSend(disconnect, channel)).isSameAs(disconnect);
    }

    @Test
    void 로그인_회원의_번호가_연결_사용자_이름이_된다() {
        MemberHandshakeHandler handler = new MemberHandshakeHandler();
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getPrincipal()).thenReturn(UsernamePasswordAuthenticationToken
                .authenticated(new LoginMember(7L, "USRC0001"), null, java.util.List.of()));

        Principal user = handler.determineUser(request, mock(WebSocketHandler.class), Map.of());

        assertThat(user.getName()).isEqualTo("7");
    }

    @Test
    void 인증_정보가_없는_연결은_사용자가_없다() {
        MemberHandshakeHandler handler = new MemberHandshakeHandler();
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getPrincipal()).thenReturn(null);

        assertThat(handler.determineUser(request, mock(WebSocketHandler.class), Map.of())).isNull();
    }
}
