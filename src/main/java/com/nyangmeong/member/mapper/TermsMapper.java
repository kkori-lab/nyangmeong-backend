package com.nyangmeong.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.domain.Terms;
import com.nyangmeong.member.dto.AdminMemberTermsAgreeSummary;
import com.nyangmeong.member.dto.AdminTermsDetail;
import com.nyangmeong.member.dto.AdminTermsSummary;
import com.nyangmeong.member.dto.TermsSummary;

/**
 * [약관 Mapper]
 */
@Mapper
public interface TermsMapper {

    /**
     * 사용 중인 약관 목록 (정렬순서)
     */
    List<TermsSummary> selectUsableList();

    /**
     * 사용 중 약관 종류별 회원의 최신 동의 (정렬순서)
     */
    List<AdminMemberTermsAgreeSummary> selectAdminAgreementList(@Param("mbrSn") Long mbrSn);

    /**
     * 사용 중인 약관 전문, 없으면 null
     */
    Terms selectUsableBySn(Long trmsSn);

    /**
     * 회원이 동의(AGRE_YN='Y')하지 않은 현재 사용 중 필수 약관 (정렬순서)
     */
    List<TermsSummary> selectMissingEssential(Long mbrSn);

    /**
     * 복구 때 필수 약관 동의 저장 (이미 행이 있으면 동의로 갱신)
     */
    int upsertAgreement(@Param("mbrSn") Long mbrSn, @Param("trmsSn") Long trmsSn, @Param("actorId") String actorId);

    /**
     * 회원 약관 동의 등록
     */
    int insertAgreement(@Param("mbrSn") Long mbrSn, @Param("trmsSn") Long trmsSn, @Param("agreYn") String agreYn,
            @Param("actorId") String actorId);

    /**
     * 관리자 : 종류별 사용 중 버전 (정렬순서)
     */
    List<AdminTermsSummary> selectAdminCurrentList();

    /**
     * 관리자 : 한 종류의 모든 버전 (최신순, 내용 제외)
     */
    List<AdminTermsSummary> selectAdminHistory(@Param("trmsFixIdntfKey") String trmsFixIdntfKey);

    /**
     * 관리자 : 약관 상세 (사용 안 함 포함), 없으면 null
     */
    AdminTermsDetail selectAdminDetail(@Param("trmsSn") Long trmsSn);

    /**
     * 관리자 : 같은 종류·같은 버전 상세, 없으면 null
     */
    AdminTermsDetail selectAdminDetailByVersion(@Param("trmsFixIdntfKey") String trmsFixIdntfKey,
            @Param("trmsVerId") String trmsVerId);

    /**
     * 관리자 : 종류의 가장 최근 버전 정렬순서 (그 종류 행을 잠근다), 종류가 없으면 null
     */
    Integer selectLatestSortSeqForUpdate(@Param("trmsFixIdntfKey") String trmsFixIdntfKey);

    /**
     * 관리자 : 같은 종류의 사용 중 버전을 사용 안 함으로
     */
    int updateDisableByKey(@Param("trmsFixIdntfKey") String trmsFixIdntfKey, @Param("actorId") String actorId);

    /**
     * 관리자 : 약관 새 버전 등록 (사용)
     */
    int insertAdminTerms(@Param("trmsFixIdntfKey") String trmsFixIdntfKey, @Param("trmsVerId") String trmsVerId,
            @Param("trmsTtl") String trmsTtl, @Param("trmsCn") String trmsCn, @Param("esntlAgreYn") String esntlAgreYn,
            @Param("sortSeq") Integer sortSeq, @Param("actorId") String actorId);
}
