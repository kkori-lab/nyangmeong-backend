package com.nyangmeong.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.service.ChatRoomService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅방 컨트롤러]
 * - SCR-CMU-004 채팅하기, 새 방은 201 기존 방은 200
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅하기(채팅방 생성 또는 기존 방 반환)")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ChatRoomCreateRequest req) {
        ChatRoomResponse res = chatRoomService.create(me.id(), req);
        if (res.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(res));
        }
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
