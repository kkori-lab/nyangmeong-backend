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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.aitool.AiTool;
import com.nyangmeong.global.aitool.AiToolKind;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.session.LoginSessionManager;
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
import com.nyangmeong.member.dto.MemberSnsLinkRequest;
import com.nyangmeong.member.dto.MemberTutorialResponse;
import com.nyangmeong.member.dto.MemberUpdateRequest;
import com.nyangmeong.member.dto.MemberWithdrawRequest;
import com.nyangmeong.member.dto.PasswordChangeRequest;
import com.nyangmeong.member.dto.PasswordVerifyRequest;
import com.nyangmeong.member.service.MemberJoinService;
import com.nyangmeong.member.service.MemberPasswordService;
import com.nyangmeong.member.service.MemberRecoveryService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSnsLinkService;
import com.nyangmeong.member.service.MemberWithdrawService;
import com.nyangmeong.member.service.NicknameSetupTokenManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [회원 컨트롤러]
 * - 가입(POST /api/members)·휴대전화번호/닉네임 중복확인·닉네임 설정(PATCH /api/members/me/nickname)
 * - 탈퇴 회원 기존 정보 복구 안내·실행(POST /api/members/recovery-info·recovery), 복구 확인 권한 토큰이 본인확인이다
 * - 위는 모두 로그인 없이 열려 있고, 닉네임 설정과 닉네임 중복확인의 본인은 로그인 회원 또는 가입 직후 쿠키(ncnm_setup)로 가린다
 * - 회원정보 수정 조회·저장(GET·PATCH /api/members/me, SCR-MYP-040)은 로그인 회원만
 * - 현재 비밀번호 확인·변경(POST /api/members/me/password-verification, PATCH /api/members/me/password)도 로그인 회원만
 * - 회원탈퇴(POST /api/members/me/withdraw)도 로그인 회원만, 성공하면 요청한 기기의 쿠키도 지운다
 * - SNS 계정 연동(POST /api/members/me/sns-links)도 로그인 회원만
 * - 서비스 튜토리얼 봤음 조회·저장(GET·PUT /api/members/me/tutorial, SCR-MAN-001)도 로그인 회원만
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberJoinService memberJoinService;
    private final MemberRecoveryService memberRecoveryService;
    private final MemberPasswordService memberPasswordService;
    private final MemberWithdrawService memberWithdrawService;
    private final MemberSnsLinkService memberSnsLinkService;
    private final LoginSessionManager loginSessionManager;
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
        Long actorSn = nicknameTargetMbrSn(me, request);
        return ResponseEntity.ok(ApiResponse.success(memberService.getNicknameAvailability(query.mbrNcnm(), actorSn)));
    }

    @Operation(summary = "회원정보 수정 화면 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(me.id())));
    }

    @Operation(summary = "SNS 계정 연동(로그인 상태)")
    @PostMapping("/me/sns-links")
    public ResponseEntity<ApiResponse<MemberResponse>> linkSns(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody MemberSnsLinkRequest req,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        return ResponseEntity.ok(ApiResponse.success(memberSnsLinkService.link(me.id(), req, origin)));
    }

    @Operation(summary = "현재 비밀번호 확인(회원정보 수정 본인 확인)")
    @PostMapping("/me/password-verification")
    public ResponseEntity<ApiResponse<Void>> verifyPassword(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody PasswordVerifyRequest req) {
        memberPasswordService.verify(me.id(), req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 변경하면 모든 기기의 세션이 끊기므로, 요청한 기기에만 새 세션을 다시 준다
     */
    @Operation(summary = "비밀번호 변경(로그인 상태)")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal LoginMember me,
            HttpServletResponse response, @Valid @RequestBody PasswordChangeRequest req) {
        memberPasswordService.change(me.id(), req);
        loginSessionManager.issue(me.id(), response);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원탈퇴")
    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal LoginMember me,
            HttpServletRequest request, HttpServletResponse response,
            @Valid @RequestBody MemberWithdrawRequest req) {
        memberWithdrawService.withdraw(me.id(), req);
        loginSessionManager.revoke(request, response);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원정보 수정(휴대전화번호·프로필 사진)")
    @AiTool(name = "draft_member_profile_update", domain = "member", kind = AiToolKind.CARD)
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberResponse>> updateMe(@AuthenticationPrincipal LoginMember me,
            @ModelAttribute MemberUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(me.id(), req)));
    }

    @Operation(summary = "서비스 튜토리얼 봤음 여부 조회")
    @AiTool(name = "get_service_tutorial", domain = "member", kind = AiToolKind.READ)
    @GetMapping("/me/tutorial")
    public ResponseEntity<ApiResponse<MemberTutorialResponse>> getTutorial(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getTutorial(me.id())));
    }

    @Operation(summary = "서비스 튜토리얼 봤음 저장")
    @PutMapping("/me/tutorial")
    public ResponseEntity<ApiResponse<MemberTutorialResponse>> markTutorialViewed(
            @AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(memberService.markTutorialViewed(me.id())));
    }

    @Operation(summary = "닉네임 설정 완료")
    @AiTool(name = "draft_member_nickname_update", domain = "member", kind = AiToolKind.CARD)
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<MemberResponse>> updateNickname(@AuthenticationPrincipal LoginMember me,
            HttpServletRequest request, @Valid @RequestBody MemberNicknameUpdateRequest req) {
        Long mbrSn = nicknameTargetMbrSn(me, request);
        if (mbrSn == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        MemberResponse response = memberService.updateNickname(mbrSn, req);
        /** 다 쓴 가입 쿠키는 지운다 : 남겨 두면 30분 안에 마이페이지 닉네임 수정이 이 회원에게 적용된다 */
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, nicknameSetupTokenManager.delete().toString())
                .body(ApiResponse.success(response));
    }

    /**
     * 닉네임을 바꿀 회원 : 가입 직후 쿠키가 먼저, 없으면 로그인 회원
     * - 다른 계정으로 로그인한 브라우저에서 가입해도 새 회원의 닉네임이 저장되어야 한다
     */
    private Long nicknameTargetMbrSn(LoginMember me, HttpServletRequest request) {
        Long setupMbrSn = nicknameSetupTokenManager.resolve(request);
        return setupMbrSn != null ? setupMbrSn : me != null ? me.id() : null;
    }
}
