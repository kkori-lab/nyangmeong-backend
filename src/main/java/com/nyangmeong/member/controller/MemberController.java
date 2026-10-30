package com.nyangmeong.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.member.dto.MemberCreateRequest;
import com.nyangmeong.member.dto.MemberJoinResult;
import com.nyangmeong.member.dto.MemberNicknameAvailabilityResponse;
import com.nyangmeong.member.dto.MemberNicknameQuery;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberPhoneAvailabilityResponse;
import com.nyangmeong.member.dto.MemberPhoneQuery;
import com.nyangmeong.member.dto.MemberRecoverRequest;
import com.nyangmeong.member.dto.MemberRecoveryInfoRequest;
import com.nyangmeong.member.dto.MemberRecoveryResponse;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberUpdateRequest;
import com.nyangmeong.member.service.MemberJoinService;
import com.nyangmeong.member.service.MemberRecoveryService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.NicknameSetupTokenManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [회원 컨트롤러]
 * - 가입(POST /api/members)·휴대전화번호/닉네임 중복확인·닉네임 설정(PATCH /api/members/me/nickname)
 * - 탈퇴 회원 기존 정보 복구 안내·실행(POST /api/members/recovery-info·recovery), 복구 확인 권한 토큰이 본인확인이다
 * - 위는 모두 로그인 없이 열려 있고, 닉네임 설정과 닉네임 중복확인의 본인은 로그인 회원 또는 가입 직후 쿠키(ncnm_setup)로 가린다
 * - 회원정보 수정 조회·저장(GET·PATCH /api/members/me, SCR-MYP-040)은 로그인 회원만
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberJoinService memberJoinService;
    private final MemberRecoveryService memberRecoveryService;
    private final NicknameSetupTokenManager nicknameSetupTokenManager;

    @Operation(summary = "회원가입(이메일)")
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> create(@Valid @RequestBody MemberCreateRequest req) {
        MemberJoinResult result = memberJoinService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, nicknameSetupTokenManager.issue(result.mbrSn()).toString())
                .body(ApiResponse.created(result.response()));
    }

    @Operation(summary = "탈퇴 회원 기존 정보 복구 안내(복구 확인 권한 토큰)")
    @PostMapping("/recovery-info")
    public ResponseEntity<ApiResponse<MemberRecoveryResponse>> getRecovery(
            @Valid @RequestBody MemberRecoveryInfoRequest req) {
        return ResponseEntity.ok(ApiResponse.success(memberRecoveryService.getRecovery(req)));
    }

    @Operation(summary = "탈퇴 회원 기존 정보 복구 실행(복구 확인 권한 토큰)")
    @PostMapping("/recovery")
    public ResponseEntity<ApiResponse<Void>> recover(@Valid @RequestBody MemberRecoverRequest req) {
        memberRecoveryService.recover(req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "휴대전화번호 정규화·중복확인")
    @GetMapping("/phone-availability")
    public ResponseEntity<ApiResponse<MemberPhoneAvailabilityResponse>> getPhoneAvailability(
            @AuthenticationPrincipal LoginMember me, @Valid MemberPhoneQuery query) {
        return ResponseEntity.ok(ApiResponse
                .success(memberService.getPhoneAvailability(query.mblTelno(), me == null ? null : me.id())));
    }

    @Operation(summary = "닉네임 중복확인")
    @GetMapping("/nickname-availability")
    public ResponseEntity<ApiResponse<MemberNicknameAvailabilityResponse>> getNicknameAvailability(
            @AuthenticationPrincipal LoginMember me, HttpServletRequest request, @Valid MemberNicknameQuery query) {
        Long actorSn = me != null ? me.id() : nicknameSetupTokenManager.resolve(request);
        return ResponseEntity.ok(ApiResponse.success(memberService.getNicknameAvailability(query.mbrNcnm(), actorSn)));
    }

    @Operation(summary = "회원정보 수정 화면 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(me.id())));
    }

    @Operation(summary = "회원정보 수정(휴대전화번호·프로필 사진)")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberResponse>> updateMe(@AuthenticationPrincipal LoginMember me,
            @ModelAttribute MemberUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(me.id(), req)));
    }

    @Operation(summary = "닉네임 설정 완료")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<MemberResponse>> updateNickname(@AuthenticationPrincipal LoginMember me,
            HttpServletRequest request, @Valid @RequestBody MemberNicknameUpdateRequest req) {
        Long mbrSn = me != null ? me.id() : nicknameSetupTokenManager.resolve(request);
        if (mbrSn == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.success(memberService.updateNickname(mbrSn, req)));
    }
}
