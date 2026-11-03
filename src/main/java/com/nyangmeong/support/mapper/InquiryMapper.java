package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.domain.Inquiry;
import com.nyangmeong.support.domain.InquiryPhoto;
import com.nyangmeong.support.domain.InquiryStatusHistory;
import com.nyangmeong.support.dto.AdminInquiryHistorySummary;
import com.nyangmeong.support.dto.AdminInquirySearchCond;
import com.nyangmeong.support.dto.AdminInquirySummary;
import com.nyangmeong.support.dto.InquiryImageSummary;
import com.nyangmeong.support.dto.InquirySummary;

/**
 * [1:1 문의 Mapper]
 */
@Mapper
public interface InquiryMapper {

    /**
     * 내 문의 목록 조회 (처리 상태 없으면 전체)
     */
    List<InquirySummary> selectList(@Param("mbrSn") Long mbrSn, @Param("inqryPrcsSttsCd") String inqryPrcsSttsCd);

    /**
     * 문의 단건 조회
     */
    Inquiry selectInquiry(Long otoInqrySn);

    /**
     * 공통코드명 조회
     */
    String selectCodeName(String comCd);

    /**
     * 사용 중인 첨부사진 조회
     */
    List<InquiryPhoto> selectPhotos(Long otoInqrySn);

    /**
     * 응답용 첨부 이미지 조회 (원본 파일명 포함)
     */
    List<InquiryImageSummary> selectImageSummaries(Long otoInqrySn);

    /**
     * 문의 등록
     */
    int insertInquiry(Inquiry inquiry);

    /**
     * 상태 이력 등록
     */
    int insertStatusHistory(InquiryStatusHistory history);

    /**
     * 첨부사진 등록
     */
    int insertPhoto(InquiryPhoto photo);

    /**
     * 접수 상태 문의의 내용 수정
     */
    int updateInquiry(@Param("otoInqrySn") Long otoInqrySn, @Param("inqryTypeCd") String inqryTypeCd,
            @Param("inqryTtl") String inqryTtl, @Param("inqryCn") String inqryCn,
            @Param("receivedCd") String receivedCd);

    /**
     * 접수 상태 문의 삭제 처리
     */
    int updateInquiryDeleted(@Param("otoInqrySn") Long otoInqrySn, @Param("receivedCd") String receivedCd);

    /**
     * 문의의 첨부사진 전체 삭제 처리
     */
    int updateAllPhotosDeleted(Long otoInqrySn);

    /**
     * 첨부사진 삭제 처리
     */
    int updatePhotosDeleted(@Param("otoInqrySn") Long otoInqrySn, @Param("atchFileSns") List<Long> atchFileSns);

    /**
     * 접수 상태 문의에 답변 등록
     */
    int updateAnswer(@Param("otoInqrySn") Long otoInqrySn, @Param("prcsMbrSn") Long prcsMbrSn,
            @Param("ansCn") String ansCn, @Param("receivedCd") String receivedCd,
            @Param("answeredCd") String answeredCd);

    /**
     * 관리자 문의 목록 조회
     */
    List<AdminInquirySummary> selectAdminList(@Param("cond") AdminInquirySearchCond cond,
            @Param("inqryTypeCds") List<String> inqryTypeCds, @Param("inclDel") boolean inclDel);

    /**
     * 관리자 문의 단건 조회 (삭제 문의 포함)
     */
    Inquiry selectAdminInquiry(Long otoInqrySn);

    /**
     * 회원 닉네임 조회
     */
    String selectNickname(Long mbrSn);

    /**
     * 문의 상태 이력 조회
     */
    List<AdminInquiryHistorySummary> selectStatusHistories(Long otoInqrySn);

    /**
     * 답변 완료 문의의 답변 내용 수정
     */
    int updateAnswerContent(@Param("otoInqrySn") Long otoInqrySn, @Param("ansCn") String ansCn,
            @Param("answeredCd") String answeredCd);

    /**
     * 캐릭터 오류 처리 결과 기록
     */
    int updateCharErrResult(@Param("otoInqrySn") Long otoInqrySn,
            @Param("charErrPrcsRsltCd") String charErrPrcsRsltCd);
}
