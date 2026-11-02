package com.nyangmeong.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.dto.AdminMemberSearchCond;
import com.nyangmeong.member.dto.AdminMemberSummary;

/**
 * [회원 Mapper]
 */
@Mapper
public interface MemberMapper {

    /**
     * 로그인 아이디 HMAC로 회원 조회 (탈퇴·정지 포함)
     */
    Member selectByEmailHmac(String acntEmlHmac);

    /**
     * 회원번호로 회원 조회
     */
    Member selectBySn(Long mbrSn);

    /**
     * 휴대전화번호 HMAC를 쓰는 회원번호 (탈퇴 보관 회원 포함), 없으면 null
     */
    Long selectMbrSnByPhoneHmac(String mblTelnoHmac);

    /**
     * 닉네임을 쓰는 회원번호, 없으면 null
     */
    Long selectMbrSnByNickname(String mbrNcnm);

    /**
     * 연동 중인 SNS 제공자 코드 목록
     */
    List<String> selectSnsOffrCds(Long mbrSn);

    /**
     * 회원 등록 (mbrSn 채움)
     */
    int insert(Member member);

    /**
     * 회원 상태 이력 등록
     */
    int insertStatusHistory(@Param("mbrSn") Long mbrSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("chgRsn") String chgRsn);

    /**
     * 닉네임 저장과 설정 완료 표시
     */
    int updateNickname(@Param("mbrSn") Long mbrSn, @Param("mbrNcnm") String mbrNcnm,
            @Param("actorId") String actorId);

    /**
     * 비밀번호 해시 변경, 탈퇴 회원은 바꾸지 않는다
     */
    int updatePassword(@Param("mbrSn") Long mbrSn, @Param("pswdHash") String pswdHash,
            @Param("withdrawnSttsCd") String withdrawnSttsCd, @Param("actorId") String actorId);

    /**
     * 탈퇴 → 정상 복구, 보관 기간 안의 탈퇴 회원만 바뀐다 (0이면 기간 경과나 이미 복구)
     */
    int updateRecover(@Param("mbrSn") Long mbrSn, @Param("normalSttsCd") String normalSttsCd,
            @Param("withdrawnSttsCd") String withdrawnSttsCd, @Param("keepDayCnt") int keepDayCnt,
            @Param("actorId") String actorId);

    /**
     * 보관 번호가 없던 회원의 휴대전화번호 저장 (이미 번호가 있으면 0)
     */
    int updatePhone(@Param("mbrSn") Long mbrSn, @Param("mblTelnoEnc") String mblTelnoEnc,
            @Param("mblTelnoHmac") String mblTelnoHmac, @Param("actorId") String actorId);

    /**
     * 회원정보 수정 : null 인 값은 바꾸지 않고, photoReset 이면 프로필 사진을 비운다
     */
    int updateProfile(@Param("mbrSn") Long mbrSn, @Param("mblTelnoEnc") String mblTelnoEnc,
            @Param("mblTelnoHmac") String mblTelnoHmac, @Param("atchFileSn") Long atchFileSn,
            @Param("photoReset") boolean photoReset, @Param("actorId") String actorId);

    /**
     * 최종 로그인 일시 갱신
     */
    int updateLastLogin(@Param("mbrSn") Long mbrSn, @Param("actorId") String actorId);

    /**
     * 탈퇴 보관일수가 지난 회원 번호 목록 조회
     */
    List<Long> selectExpiredWithdrawnMbrSns(@Param("mbrSttsCd") String mbrSttsCd,
            @Param("keepDayCnt") int keepDayCnt);

    /**
     * 관리자 회원 목록 조회
     */
    List<AdminMemberSummary> selectAdminList(@Param("cond") AdminMemberSearchCond cond,
            @Param("acntEmlHmac") String acntEmlHmac, @Param("mbrNcnm") String mbrNcnm);
}
