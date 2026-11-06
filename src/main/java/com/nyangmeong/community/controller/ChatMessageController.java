package com.nyangmeong.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.ChatMessageCreateRequest;
import com.nyangmeong.community.dto.ChatMessageResponse;
import com.nyangmeong.community.dto.ChatMessageSliceResponse;
import com.nyangmeong.community.service.ChatMessageService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅 메시지 컨트롤러]
 * - SCR-CMU-005 메시지 조회(커서)·전송(multipart), 처음 전송은 201 같은 idmptDmndId 재전송은 200
 * - 새 메시지는 저장 뒤 STOMP /user/queue/chat 으로 참여자에게 전송
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{otoChatRoomSn}/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @Operation(summary = "대화 메시지 조회(커서, 최신순)")
    @GetMapping
    public ResponseEntity<ApiResponse<ChatMessageSliceResponse>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn, @RequestParam(required = false) Long beforeChatMsgSn,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity
                .ok(ApiResponse.success(chatMessageService.search(me.id(), otoChatRoomSn, beforeChatMsgSn, size)));
    }

    @Operation(summary = "메시지 전송(텍스트·사진·주소 공유)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn, @Valid @ModelAttribute ChatMessageCreateRequest req) {
        ChatMessageResponse res = chatMessageService.send(me.id(), otoChatRoomSn, req);
        if (res.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(res));
        }
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
