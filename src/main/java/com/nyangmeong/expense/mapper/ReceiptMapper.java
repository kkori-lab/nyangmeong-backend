package com.nyangmeong.expense.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.expense.domain.Receipt;
import com.nyangmeong.expense.domain.ReceiptStatusHistory;
import com.nyangmeong.expense.dto.AdminDailyOcrRequestSummary;
import com.nyangmeong.expense.dto.AdminReceiptRow;
import com.nyangmeong.expense.dto.AdminReceiptSearchCond;
import com.nyangmeong.expense.dto.AdminReceiptSummary;

/**
 * [영수증 Mapper]
 * - RECPT, RECPT_STTS_HSTRY
 */
@Mapper
public interface ReceiptMapper {

    /**
     * 회원 영수증 조회
     */
    Receipt selectOwned(@Param("recptSn") Long recptSn, @Param("mbrSn") Long mbrSn);

    /**
     * 영수증 등록
     */
    void insert(Receipt receipt);

    /**
     * 상태 변경
     */
    int updateStatus(@Param("recptSn") Long recptSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 분석 종료 상태와 OCR 응답 저장
     */
    int updateAnalyzed(@Param("recptSn") Long recptSn, @Param("bfrSttsCd") String bfrSttsCd,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("ocrRspnsJson") String ocrRspnsJson);

    /**
     * 멱등 요청 ID 로 영수증 조회
     */
    Receipt selectByIdmptDmndId(@Param("idmptDmndId") String idmptDmndId);

    /**
     * 저장 완료 처리 : 상태, 저장 일시, 멱등 요청 ID
     */
    int updateSaved(@Param("recptSn") Long recptSn, @Param("bfrSttsCds") List<String> bfrSttsCds,
            @Param("aftrSttsCd") String aftrSttsCd, @Param("idmptDmndId") String idmptDmndId);

    /**
     * 상태 이력 등록
     */
    void insertStatusHistory(ReceiptStatusHistory history);

    /**
     * 마지막 상태 이력의 변경 사유
     */
    String selectLastChgRsn(@Param("recptSn") Long recptSn);

    /**
     * 관리자 OCR 현황 건수 묶음 : 기간·출처만 건다 (SCR-ADM-082)
     */
    AdminReceiptSummary selectAdminReceiptSummary(@Param("cond") AdminReceiptSearchCond cond,
            @Param("scsCds") List<String> scsCds, @Param("failCd") String failCd, @Param("prcsCd") String prcsCd,
            @Param("errCd") String errCd);

    /**
     * 관리자 OCR 현황 목록 : 기간·출처·상태, 최신순 (SCR-ADM-082)
     */
    List<AdminReceiptRow> selectAdminReceiptList(@Param("cond") AdminReceiptSearchCond cond);

    /**
     * 관리자 원본 보기용 첨부파일 번호 (회원 소유 검사 없음, SCR-ADM-082)
     */
    Long selectAtchFileSn(@Param("recptSn") Long recptSn);

    /**
     * 관리자 대시보드 일별 OCR 요청 수 조회 (fromDt 이상 toDt 미만)
     */
    List<AdminDailyOcrRequestSummary> selectDailyRequestCounts(@Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt);
}
