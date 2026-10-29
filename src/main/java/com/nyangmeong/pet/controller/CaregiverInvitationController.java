package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.CaregiverInvitationAcceptRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateRequest;
import com.nyangmeong.pet.dto.CaregiverInvitationCreateResult;
import com.nyangmeong.pet.dto.CaregiverInvitationResponse;
import com.nyangmeong.pet.dto.CaregiverInvitationSummary;
import com.nyangmeong.pet.service.CaregiverInvitationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [공동양육 초대 컨트롤러]
 * - 이메일 초대 발송·보낸 초대 (SCR-MYP-005 5-1·5-2)
 * - 받은 초대 상세·수락·거절 (SCR-MYP-005 5-3~5-6)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/caregiver-invitations")
public class CaregiverInvitationController {

    private final CaregiverInvitationService caregiverInvitationService;

    @Operation(summary = "공동양육 초대 발송")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createInvitation(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody CaregiverInvitationCreateRequest req, HttpServletRequest request) {
        CaregiverInvitationCreateResult result = caregiverInvitationService.createInvitation(me.id(), req);
        if (!result.succeeded()) {
            return ResponseEntity.status(result.error().status())
                    .<ApiResponse<?>>body(ApiResponse.errorWithData(result.error(), request.getRequestURI(),
                            result.conflict()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result.response()));
    }

    @Operation(summary = "보낸 공동양육 초대 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaregiverInvitationSummary>>> searchSentInvitations(
            @AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(caregiverInvitationService.searchSentInvitations(me.id())));
    }

    @Operation(summary = "받은 공동양육 초대 상세")
    @GetMapping("/{jntBrupInvtnSn}")
    public ResponseEntity<ApiResponse<CaregiverInvitationResponse>> getInvitation(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long jntBrupInvtnSn) {
        return ResponseEntity.ok(ApiResponse.success(caregiverInvitationService.getInvitation(me.id(), jntBrupInvtnSn)));
    }

    @Operation(summary = "공동양육 초대 수락")
    @PostMapping("/{jntBrupInvtnSn}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long jntBrupInvtnSn, @Valid @RequestBody CaregiverInvitationAcceptRequest req) {
        caregiverInvitationService.acceptInvitation(me.id(), jntBrupInvtnSn, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "공동양육 초대 거절")
    @PostMapping("/{jntBrupInvtnSn}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long jntBrupInvtnSn) {
        caregiverInvitationService.rejectInvitation(me.id(), jntBrupInvtnSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
