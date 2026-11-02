package com.nyangmeong.member.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.global.validation.PhoneNumberNormalizer;
import com.nyangmeong.member.domain.JoinPath;
import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.dto.AdminWithdrawnMemberSummary;
import com.nyangmeong.member.dto.MemberNicknameAvailabilityResponse;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberPhoneAvailabilityResponse;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.dto.MemberSnsAccountSummary;
import com.nyangmeong.member.dto.MemberUpdateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.PasswordResetAccount;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원 서비스]
 * - 다른 도메인(auth 등)은 mapper 대신 이 서비스를 부른다
 * - 가입 때 이메일·번호·닉네임 중복 검사와 회원 저장, 닉네임 설정, 회원정보 수정(휴대전화번호·프로필 사진)
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String NICKNAME_PREFIX = "냥멍";
    private static final int NICKNAME_MAX_TRY = 5;
    private static final String SIGNUP_REASON = "회원가입";
    private static final String RECOVERY_REASON = "재가입 복구";
    private static final int MASK_VISIBLE_LENGTH = 3;

    private final MemberMapper memberMapper;
    private final FieldCryptoService fieldCryptoService;
    private final SystemSettingService systemSettingService;
    private final FileStore fileStore;

    /**
     * 관리자 회원 목록 조회
     * - 페이지 정보를 잃지 않도록 목록을 새로 만들지 않고 행에 마스킹 값만 채움
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminMemberSummary> searchForAdmin(AdminMemberSearchCond cond, PageQuery pq) {
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String q = cond.getQ();
        boolean hasKeyword = q != null && !q.isBlank();
        boolean isEmailKeyword = hasKeyword && q.contains("@");
        String acntEmlHmac = isEmailKeyword ? fieldCryptoService.emailHmac(q) : null;
        String mbrNcnm = hasKeyword && !isEmailKeyword ? q.strip() : null;

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminMemberSummary> rows = memberMapper.selectAdminList(cond, acntEmlHmac, mbrNcnm);
        rows.forEach(row -> row.setMaskedAcntEml(maskEmail(fieldCryptoService.decrypt(row.getAcntEmlEnc()))));
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 관리자 탈퇴 회원 목록 조회
     * - 페이지 정보를 잃지 않도록 목록을 새로 만들지 않고 행에 마스킹 값과 파기 예정일만 채움
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminWithdrawnMemberSummary> searchWithdrawnForAdmin(PageQuery pq) {
        int keepDayCnt = systemSettingService.get().getWhdwlMbrKpngDayCnt().intValue();

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminWithdrawnMemberSummary> rows = memberMapper.selectWithdrawnList(MemberStatus.WITHDRAWN.getCode(),
                keepDayCnt);
        rows.forEach(row -> {
            row.setMaskedAcntEml(maskEmail(fieldCryptoService.decrypt(row.getAcntEmlEnc())));
            row.setPurgeYmd(row.getWhdwlDt().plusDays(keepDayCnt).toLocalDate());
        });
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 이메일 마스킹 (cho***@gmail.com)
     */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return "***";
        }
        return email.substring(0, Math.min(MASK_VISIBLE_LENGTH, at)) + "***" + email.substring(at);
    }

    /**
     * 로그인용 회원 조회 : 없으면 null (없는 아이디도 불일치와 같게 다루기 위해)
     */
    @Transactional(readOnly = true)
    public Member findForLogin(String acntEmlHmac) {
        return memberMapper.selectByEmailHmac(acntEmlHmac);
    }

    /**
     * 로그인 성공 기록 (최종 로그인 일시)
     */
    @Transactional
    public void recordLogin(Long mbrSn) {
        memberMapper.updateLastLogin(mbrSn, String.valueOf(mbrSn));
    }

    /**
     * 가입 이메일 사용 가능 검사 : 정상·정지 회원은 중복, 탈퇴 보관 회원은 복구 안내
     * - 실패 횟수를 남기는 호출자의 트랜잭션이 롤백 표시되지 않도록 CustomException 에는 롤백하지 않는다
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public void checkEmailAvailable(String acntEmlHmac) {
        Member member = memberMapper.selectByEmailHmac(acntEmlHmac);
        if (member == null) {
            return;
        }
        if (MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_AVAILABLE);
        }
        throw new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    /**
     * 복구 대상 회원 확인 : 보관 기간 안의 탈퇴 회원만
     * - 정상·정지·미가입은 MEMBER_NOT_FOUND, 보관 기간이 지났으면(탈퇴일시를 몰라 기간을 셀 수 없는 경우 포함) MEMBER_RECOVERY_EXPIRED
     * - 인증 확인처럼 실패 횟수를 남기는 호출자의 트랜잭션이 롤백 표시되지 않도록 CustomException 에는 롤백하지 않는다
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public Member getRecoverableMember(String acntEmlHmac) {
        Member member = memberMapper.selectByEmailHmac(acntEmlHmac);
        if (member == null || !MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (member.getWhdwlDt() == null || !getRecoveryExpiryDt(member).isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_EXPIRED);
        }
        return member;
    }

    /**
     * 복구 보관 만료시각 : 탈퇴일시 + 탈퇴회원보관일수
     */
    @Transactional(readOnly = true)
    public LocalDateTime getRecoveryExpiryDt(Member member) {
        return member.getWhdwlDt().plusDays(systemSettingService.get().getWhdwlMbrKpngDayCnt());
    }

    /**
     * 기존 정보 복구 : 탈퇴 → 정상, 이력 한 번, 보관 번호가 없던 회원은 받은 번호를 저장
     * - 상태 변경은 보관 기간 조건을 함께 걸어서 파기 배치와 겹쳐도 기간이 지난 회원은 되살리지 않는다(0행이면 기간 경과)
     * - 닉네임·비밀번호·최종로그인 등 다른 값은 건드리지 않는다
     */
    @Transactional
    public void recover(Member member, String normalizedMblTelno) {
        Long mbrSn = member.getMbrSn();
        int keepDayCnt = systemSettingService.get().getWhdwlMbrKpngDayCnt().intValue();
        if (memberMapper.updateRecover(mbrSn, MemberStatus.NORMAL.getCode(), MemberStatus.WITHDRAWN.getCode(),
                keepDayCnt, String.valueOf(mbrSn)) != 1) {
            throw new CustomException(ErrorCode.MEMBER_RECOVERY_EXPIRED);
        }
        memberMapper.insertStatusHistory(mbrSn, MemberStatus.WITHDRAWN.getCode(), MemberStatus.NORMAL.getCode(),
                RECOVERY_REASON);
        if (normalizedMblTelno != null) {
            try {
                if (memberMapper.updatePhone(mbrSn, fieldCryptoService.encrypt(normalizedMblTelno),
                        fieldCryptoService.hmac(normalizedMblTelno), String.valueOf(mbrSn)) != 1) {
                    throw new CustomException(ErrorCode.ALREADY_PROCESSED);
                }
            } catch (DuplicateKeyException e) {
                throw duplicateException(e);
            }
        }
    }

    /**
     * 비밀번호 찾기 계정 판단 : 본인확인이 끝난 이메일에만 쓴다
     * - 비밀번호 없는 회원은 SNS 전용, 탈퇴 회원은 보관 기간 안이면 복구 안내 밖이면 미가입과 같다
     * - 정지 회원은 비밀번호를 바꿔도 로그인이 막히므로 재설정 대상으로 둔다
     */
    @Transactional(readOnly = true)
    public PasswordResetAccount resolvePasswordResetAccount(String acntEmlHmac) {
        Member member = memberMapper.selectByEmailHmac(acntEmlHmac);
        if (member == null) {
            return new PasswordResetAccount(EmailCertificationResponse.MEMBER_NOT_FOUND, null);
        }
        if (MemberStatus.WITHDRAWN.is(member.getMbrSttsCd())) {
            int keepDayCnt = systemSettingService.get().getWhdwlMbrKpngDayCnt().intValue();
            boolean keeping = member.getWhdwlDt() == null
                    || member.getWhdwlDt().plusDays(keepDayCnt).isAfter(LocalDateTime.now());
            return new PasswordResetAccount(keeping ? EmailCertificationResponse.MEMBER_WITHDRAWN
                    : EmailCertificationResponse.MEMBER_NOT_FOUND, null);
        }
        if (member.getPswdHash() == null) {
            return new PasswordResetAccount(EmailCertificationResponse.SNS_ONLY,
                    memberMapper.selectSnsOffrCds(member.getMbrSn()));
        }
        return new PasswordResetAccount(EmailCertificationResponse.PASSWORD_RESET_READY, null, member.getMbrSn());
    }

    /**
     * 비밀번호 재설정 저장 : 이미 BCrypt 로 만든 해시를 받는다, 바뀐 행이 없으면(그 사이 탈퇴) 재설정 불가
     */
    @Transactional
    public void changePassword(Long mbrSn, String pswdHash) {
        if (memberMapper.updatePassword(mbrSn, pswdHash, MemberStatus.WITHDRAWN.getCode(),
                String.valueOf(mbrSn)) != 1) {
            throw new CustomException(ErrorCode.PASSWORD_RESET_NOT_AVAILABLE);
        }
    }

    /**
     * 휴대전화번호 정규화와 중복확인 : 로그인 회원 본인의 번호는 사용 가능
     */
    @Transactional(readOnly = true)
    public MemberPhoneAvailabilityResponse getPhoneAvailability(String mblTelno, Long actorSn) {
        String normalized = PhoneNumberNormalizer.normalize(mblTelno);
        Long owner = memberMapper.selectMbrSnByPhoneHmac(fieldCryptoService.hmac(normalized));
        return new MemberPhoneAvailabilityResponse(normalized, owner == null || owner.equals(actorSn));
    }

    /**
     * 가입 휴대전화번호 사용 가능 검사 (정규화된 번호)
     */
    @Transactional(readOnly = true, noRollbackFor = CustomException.class)
    public void checkPhoneAvailable(String normalizedMblTelno) {
        if (memberMapper.selectMbrSnByPhoneHmac(fieldCryptoService.hmac(normalizedMblTelno)) != null) {
            throw new CustomException(ErrorCode.PHONE_ALREADY_USED);
        }
    }

    /**
     * 닉네임 중복확인 : 현재 본인 닉네임은 사용 가능
     */
    @Transactional(readOnly = true)
    public MemberNicknameAvailabilityResponse getNicknameAvailability(String mbrNcnm, Long actorSn) {
        Long owner = memberMapper.selectMbrSnByNickname(mbrNcnm);
        return new MemberNicknameAvailabilityResponse(owner == null || owner.equals(actorSn));
    }

    /**
     * 이메일 회원 등록 : 기본 닉네임, 최초 상태 이력(없음 → 정상)과 함께 저장하고 첫 로그인 전이라 최종 로그인은 비운다
     */
    @Transactional
    public Member create(String acntEml, String normalizedMblTelno, String pswdHash) {
        Member member = new Member();
        member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
        member.setJoinPathCd(JoinPath.EMAIL.getCode());
        member.setAcntEmlEnc(fieldCryptoService.encrypt(acntEml));
        member.setAcntEmlHmac(fieldCryptoService.emailHmac(acntEml));
        member.setMblTelnoEnc(fieldCryptoService.encrypt(normalizedMblTelno));
        member.setMblTelnoHmac(fieldCryptoService.hmac(normalizedMblTelno));
        member.setPswdHash(pswdHash);
        member.setMbrNcnm(createDefaultNickname());
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw duplicateException(e);
        }
        memberMapper.insertStatusHistory(member.getMbrSn(), null, MemberStatus.NORMAL.getCode(), SIGNUP_REASON);
        return member;
    }

    /**
     * 회원 응답 (복호화한 이메일·번호)
     */
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long mbrSn) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return toResponse(member);
    }

    /**
     * 닉네임 설정 완료 : 저장과 설정 완료 표시를 함께, 현재 닉네임 그대로도 확정
     */
    @Transactional
    public MemberResponse updateNickname(Long mbrSn, MemberNicknameUpdateRequest req) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (MemberStatus.isInactive(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        Long owner = memberMapper.selectMbrSnByNickname(req.mbrNcnm());
        if (owner != null && !owner.equals(mbrSn)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_USED);
        }
        try {
            memberMapper.updateNickname(mbrSn, req.mbrNcnm(), String.valueOf(mbrSn));
        } catch (DuplicateKeyException e) {
            throw duplicateException(e);
        }
        return toResponse(memberMapper.selectBySn(mbrSn));
    }

    /**
     * 회원정보 수정 : 보낸 값만 바꾸고, 아무 값도 없으면 현재 값을 돌려준다
     * - 휴대전화번호는 정규화 뒤 다른 회원이 쓰면 거절, 본인 번호면 그대로 통과 (저장 시 서버 재검사, FN-MYP-040-05)
     * - 프로필 사진은 공개 이미지로 저장, 기본 이미지로는 비운다. 이전 사진 파일은 그대로 둔다
     */
    @Transactional
    public MemberResponse updateMember(Long mbrSn, MemberUpdateRequest req) {
        Member member = memberMapper.selectBySn(mbrSn);
        if (member == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (MemberStatus.isInactive(member.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_INACTIVE);
        }
        boolean hasPhoto = req.profilePhoto() != null && !req.profilePhoto().isEmpty();
        boolean photoReset = Boolean.TRUE.equals(req.profilePhotoReset());
        if (hasPhoto && photoReset) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        String mblTelnoEnc = null;
        String mblTelnoHmac = null;
        if (req.mblTelno() != null) {
            String normalized = PhoneNumberNormalizer.normalize(req.mblTelno());
            mblTelnoHmac = fieldCryptoService.hmac(normalized);
            Long owner = memberMapper.selectMbrSnByPhoneHmac(mblTelnoHmac);
            if (owner != null && !owner.equals(mbrSn)) {
                throw new CustomException(ErrorCode.PHONE_ALREADY_USED);
            }
            mblTelnoEnc = fieldCryptoService.encrypt(normalized);
        }
        if (mblTelnoHmac == null && !hasPhoto && !photoReset) {
            return toResponse(member);
        }
        String actorId = String.valueOf(mbrSn);
        Long atchFileSn = hasPhoto ? fileStore.storePublic(req.profilePhoto(), actorId) : null;
        try {
            memberMapper.updateProfile(mbrSn, mblTelnoEnc, mblTelnoHmac, atchFileSn, photoReset, actorId);
        } catch (DuplicateKeyException e) {
            throw duplicateException(e);
        }
        return toResponse(memberMapper.selectBySn(mbrSn));
    }

    /**
     * 회원 VO를 응답으로 변환
     */
    @Transactional(readOnly = true)
    public MemberResponse toResponse(Member member) {
        List<MemberSnsAccountSummary> snsLinks = memberMapper.selectSnsOffrCds(member.getMbrSn()).stream()
                .map(MemberSnsAccountSummary::new)
                .toList();
        return new MemberResponse(fieldCryptoService.decrypt(member.getAcntEmlEnc()), member.getMbrNcnm(),
                fieldCryptoService.decrypt(member.getMblTelnoEnc()), member.getAtchFileSn(), member.getJoinPathCd(),
                "Y".equals(member.getNcnmStngCmptnYn()), snsLinks, member.getRegDt(), member.getLastLgnDt());
    }

    /** 냥멍 + 숫자 8자리(10글자), 중복이면 다시 만든다 */
    private String createDefaultNickname() {
        for (int i = 0; i < NICKNAME_MAX_TRY; i++) {
            String nickname = NICKNAME_PREFIX + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            if (memberMapper.selectMbrSnByNickname(nickname) == null) {
                return nickname;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /** 동시 요청으로 유일 키가 부딪힌 경우 : 키 이름으로 어느 값인지 가른다 */
    private CustomException duplicateException(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("UK_MBR_1")) {
            return new CustomException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (message.contains("UK_MBR_3")) {
            return new CustomException(ErrorCode.PHONE_ALREADY_USED);
        }
        return new CustomException(ErrorCode.NICKNAME_ALREADY_USED);
    }

    /**
     * 탈퇴 보관일수가 지난 회원 번호 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> searchExpiredWithdrawn(int keepDayCnt) {
        return memberMapper.selectExpiredWithdrawnMbrSns(MemberStatus.WITHDRAWN.getCode(), keepDayCnt);
    }
}
