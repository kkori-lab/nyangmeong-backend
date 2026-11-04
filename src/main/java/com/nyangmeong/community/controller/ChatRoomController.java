package com.nyangmeong.community.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomReadRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.dto.ChatRoomSearchCond;
import com.nyangmeong.community.dto.ChatRoomSummary;
import com.nyangmeong.community.dto.ChatUnreadCountResponse;
import com.nyangmeong.community.service.ChatRoomService;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅방 컨트롤러]
 * - SCR-CMU-004 채팅하기, 새 방은 201 기존 방은 200
 * - SCR-CMU-005 채팅 목록·방 조회·읽음·나가기·사진 보기
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "내 채팅 목록(탭·안 읽음 필터)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomSummary>>> search(@AuthenticationPrincipal LoginMember me,
            ChatRoomSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(chatRoomService.search(me.id(), cond)));
    }

    @Operation(summary = "읽지 않은 메시지 총수")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<ChatUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(chatRoomService.getUnreadCount(me.id())));
    }

    @Operation(summary = "채팅방 조회(참여자만)")
    @GetMapping("/{otoChatRoomSn}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn) {
        return ResponseEntity.ok(ApiResponse.success(chatRoomService.getRoom(me.id(), otoChatRoomSn)));
    }

    @Operation(summary = "읽음 처리")
    @PostMapping("/{otoChatRoomSn}/read")
    public ResponseEntity<ApiResponse<Void>> read(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn, @Valid @RequestBody ChatRoomReadRequest req) {
        chatRoomService.read(me.id(), otoChatRoomSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "채팅방 나가기")
    @PostMapping("/{otoChatRoomSn}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn) {
        chatRoomService.leave(me.id(), otoChatRoomSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "채팅 사진(참여자만)")
    @GetMapping("/{otoChatRoomSn}/images/{atchFileSn}")
    public ResponseEntity<byte[]> getImage(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoChatRoomSn, @PathVariable Long atchFileSn) {
        StoredFile image = chatRoomService.getImage(me.id(), otoChatRoomSn, atchFileSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

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
