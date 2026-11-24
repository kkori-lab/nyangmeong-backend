package com.nyangmeong.community.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.CareRecruitment;
import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentPet;
import com.nyangmeong.community.domain.CareRecruitmentShape;
import com.nyangmeong.community.domain.CareRecruitmentStatusHistory;
import com.nyangmeong.community.dto.AdminCareMatchResponse;
import com.nyangmeong.community.dto.AdminCareMatchSummary;
import com.nyangmeong.community.dto.CareRecruitmentSummary;

/**
 * [펫돌봄 모집 Mapper]
 */
@Mapper
public interface CareRecruitmentMapper {

    /**
     * 목록 조회 (정상 게시글, 최신순. bgngDt~endDt 는 일정이 겹치는 글, endDt 는 미포함 상한)
     * - mineYn 이 아니면 거래완료(completedCd)가 된 지 hideDays 일이 지난 글은 뺀다
     */
    List<CareRecruitmentSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("pstSttsCd") String pstSttsCd,
            @Param("rcrtSttsCd") String rcrtSttsCd, @Param("mineYn") boolean mineYn, @Param("rgnSn") Long rgnSn,
            @Param("bgngDt") LocalDateTime bgngDt, @Param("endDt") LocalDateTime endDt,
            @Param("petSeCd") String petSeCd, @Param("dogCd") String dogCd,
            @Param("sizeFilter") boolean sizeFilter, @Param("minBdwg") BigDecimal minBdwg,
            @Param("maxBdwg") BigDecimal maxBdwg, @Param("completedCd") String completedCd,
            @Param("hideDays") long hideDays,
            @Param("joinedCd") String joinedCd);

    /**
     * 상세 조회 (게시글 상태 조건)
     */
    CareRecruitmentDetail selectDetail(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn,
            @Param("pstSttsCd") String pstSttsCd, @Param("joinedCd") String joinedCd);

    /**
     * 게시글 번호로 상세 조회 (펫돌봄 게시글이 아니면 null)
     */
    CareRecruitmentDetail selectDetailByPstSn(@Param("pstSn") Long pstSn, @Param("pstSttsCd") String pstSttsCd,
            @Param("joinedCd") String joinedCd);

    /**
     * 로그인 회원이 이 글로 채팅 중인지 (나가지 않은 참여 방 중 메시지가 오간 방이 있는지)
     */
    boolean existsChatting(@Param("pstSn") Long pstSn, @Param("mbrSn") Long mbrSn, @Param("joinedCd") String joinedCd);

    /**
     * 확정된 매칭의 돌보미 닉네임 (없으면 null)
     */
    String selectMatchedSitterNcnm(@Param("petPrtcMtchSn") Long petPrtcMtchSn);

    /**
     * 모집 반려동물 종류 조회 (품종명 포함)
     */
    List<CareRecruitmentPet> selectPets(@Param("petPrtcRcrtSns") List<Long> petPrtcRcrtSns);

    /**
     * 모집 돌봄 형태 조회
     */
    List<CareRecruitmentShape> selectShapes(@Param("petPrtcRcrtSns") List<Long> petPrtcRcrtSns);

    /**
     * 관리자 목록 조회 (게시글 상태 조건, 회원 조건은 보호자 또는 매칭 상대, 최신순)
     */
    List<AdminCareMatchSummary> selectAdminList(@Param("rcrtSttsCd") String rcrtSttsCd,
            @Param("pstSttsCd") String pstSttsCd, @Param("mbrSn") Long mbrSn);

    /**
     * 관리자 상세 조회 (삭제 글 제외, 매칭 목록은 채우지 않음)
     */
    AdminCareMatchResponse selectAdminDetail(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn,
            @Param("deletedCd") String deletedCd);

    /**
     * 모집 등록
     */
    int insertRecruitment(CareRecruitment recruitment);

    /**
     * 모집 반려동물 종류 등록
     */
    int insertPet(CareRecruitmentPet pet);

    /**
     * 모집 돌봄 형태 등록
     */
    int insertShape(CareRecruitmentShape shape);

    /**
     * 모집 상태 이력 등록
     */
    int insertStatusHistory(CareRecruitmentStatusHistory history);

    /**
     * 모집중 상태 모집 수정 (상태·동의 여부는 바꾸지 않음)
     */
    int updateRecruitment(@Param("recruitment") CareRecruitment recruitment,
            @Param("recruitingCd") String recruitingCd);

    /**
     * 모집 행 잠금 읽기 (매칭 확정·되돌림을 모집 단위로 직렬화, 스냅샷이 아닌 최신 커밋 값)
     */
    CareRecruitment selectForUpdate(Long petPrtcRcrtSn);

    /**
     * 자동 마감 대상 모집 번호 (정상 게시글의 모집중 글 중 수락 대기 매칭이 없고 일정이 지났거나, 일정 없이 작성 후 days 일이 지난 글)
     */
    List<Long> selectCloseTargets(@Param("recruitingCd") String recruitingCd, @Param("pstSttsCd") String pstSttsCd,
            @Param("requestedCd") String requestedCd, @Param("days") int days);

    /**
     * 이 모집이 지금도 자동 마감 대상인지 (잠근 뒤 다시 확인)
     */
    boolean existsCloseTarget(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn, @Param("recruitingCd") String recruitingCd,
            @Param("pstSttsCd") String pstSttsCd, @Param("requestedCd") String requestedCd, @Param("days") int days);

    /**
     * 모집 상태 변경 (현재 상태가 bfrSttsCd 일 때만, 확정 매칭 번호는 함께 바꿈)
     */
    int updateStatus(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("cfmtnPetPrtcMtchSn") Long cfmtnPetPrtcMtchSn);

    /**
     * 모집 반려동물 종류 삭제 (목록 교체 때)
     */
    int deletePets(Long petPrtcRcrtSn);

    /**
     * 모집 돌봄 형태 삭제 (목록 교체 때)
     */
    int deleteShapes(Long petPrtcRcrtSn);
}
