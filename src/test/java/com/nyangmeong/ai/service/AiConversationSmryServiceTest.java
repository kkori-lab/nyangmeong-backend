package com.nyangmeong.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.nyangmeong.ai.domain.AiConversationRoom;
import com.nyangmeong.ai.domain.AiConversationSmry;
import com.nyangmeong.ai.dto.ConversationResponse;
import com.nyangmeong.ai.dto.ConversationSmrySaveRequest;
import com.nyangmeong.ai.mapper.AiConversationMapper;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

/**
 * [검사 - AI 대화 요약 저장·상세 조회]
 * - 방마다 한 행, 경계선이 커질 때만 바뀌는 것은 SQL이 하고 여기서는 호출과 검사만 본다
 */
class AiConversationSmryServiceTest {

    private static final Long MBR = 1L;
    private static final Long ROOM = 7L;

    private final AiConversationMapper aiConversationMapper = mock(AiConversationMapper.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final AiConversationService service = new AiConversationService(aiConversationMapper,
            mock(PetAuthorizer.class), fieldCryptoService);

    AiConversationSmryServiceTest() {
        when(fieldCryptoService.encrypt(any())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
        when(fieldCryptoService.decrypt(any())).thenAnswer(inv -> {
            String envelope = inv.getArgument(0);
            return envelope.substring(4, envelope.length() - 1);
        });
    }

    private static AiConversationRoom room() {
        AiConversationRoom room = new AiConversationRoom();
        room.setAiCnvrsRoomSn(ROOM);
        room.setMbrSn(MBR);
        room.setCnvrsTtlEnc("ENC(제목)");
        return room;
    }

    @Test
    void 요약은_암호문으로_등록을_먼저_시도하고_이어서_경계선_비교_수정을_부른다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, ROOM)).thenReturn(room());
        when(aiConversationMapper.selectLastMsgSeq(ROOM)).thenReturn(12);

        service.saveSummary(MBR, ROOM, new ConversationSmrySaveRequest("앞부분 요약", 12));

        InOrder order = inOrder(aiConversationMapper);
        ArgumentCaptor<AiConversationSmry> insert = ArgumentCaptor.forClass(AiConversationSmry.class);
        ArgumentCaptor<AiConversationSmry> update = ArgumentCaptor.forClass(AiConversationSmry.class);
        order.verify(aiConversationMapper).insertSmryIfAbsent(insert.capture());
        order.verify(aiConversationMapper).updateSmryIfAfter(update.capture());
        assertThat(insert.getValue().getAiCnvrsRoomSn()).isEqualTo(ROOM);
        assertThat(insert.getValue().getSmryCnEnc()).isEqualTo("ENC(앞부분 요약)");
        assertThat(insert.getValue().getSmryEndMsgSeq()).isEqualTo(12);
        assertThat(update.getValue().getSmryCnEnc()).isEqualTo("ENC(앞부분 요약)");
        assertThat(update.getValue().getSmryEndMsgSeq()).isEqualTo(12);
    }

    @Test
    void 남의_대화나_없는_번호의_요약은_NOT_FOUND이고_저장하지_않는다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, 99L)).thenReturn(null);

        assertThatThrownBy(() -> service.saveSummary(MBR, 99L, new ConversationSmrySaveRequest("요약", 1)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(aiConversationMapper, never()).insertSmryIfAbsent(any());
        verify(aiConversationMapper, never()).updateSmryIfAfter(any());
    }

    @Test
    void 경계선이_마지막_메시지_순서보다_크면_INVALID_INPUT_VALUE이고_저장하지_않는다() {
        when(aiConversationMapper.selectRoomForUpdate(MBR, ROOM)).thenReturn(room());
        when(aiConversationMapper.selectLastMsgSeq(ROOM)).thenReturn(12);

        assertThatThrownBy(() -> service.saveSummary(MBR, ROOM, new ConversationSmrySaveRequest("요약", 13)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(aiConversationMapper, never()).insertSmryIfAbsent(any());
        verify(aiConversationMapper, never()).updateSmryIfAfter(any());
    }

    @Test
    void 상세는_요약을_복호화해_경계선과_함께_준다() {
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(room());
        when(aiConversationMapper.selectMessageList(ROOM)).thenReturn(List.of());
        AiConversationSmry smry = new AiConversationSmry();
        smry.setAiCnvrsRoomSn(ROOM);
        smry.setSmryCnEnc("ENC(앞부분 요약)");
        smry.setSmryEndMsgSeq(12);
        when(aiConversationMapper.selectSmry(ROOM)).thenReturn(smry);

        ConversationResponse result = service.getConversation(MBR, ROOM);

        assertThat(result.smryCn()).isEqualTo("앞부분 요약");
        assertThat(result.smryEndMsgSeq()).isEqualTo(12);
    }

    @Test
    void 요약이_없으면_상세의_요약과_경계선은_null이다() {
        when(aiConversationMapper.selectRoom(MBR, ROOM)).thenReturn(room());
        when(aiConversationMapper.selectMessageList(ROOM)).thenReturn(List.of());
        when(aiConversationMapper.selectSmry(ROOM)).thenReturn(null);

        ConversationResponse result = service.getConversation(MBR, ROOM);

        assertThat(result.smryCn()).isNull();
        assertThat(result.smryEndMsgSeq()).isNull();
    }
}
