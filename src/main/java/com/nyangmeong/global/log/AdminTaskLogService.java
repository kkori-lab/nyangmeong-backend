package com.nyangmeong.global.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * [관리자 작업 로그 기록·조회]
 * - 관리자 작업과 같은 트랜잭션에서 남김 -> 작업이 롤백되면 로그도 없음
 * - 전후 값을 아는 service가 직접 호출 (AOP 대신 명시 호출)
 * - 관리자는 고치거나 지울 수 없고, 보관 일수가 지난 로그는 감사 로그 정리 배치(AuditLogPurgeJob)가 지운다 (FN-ADM-114-04)
 */
@Service
@RequiredArgsConstructor
public class AdminTaskLogService {

    private static final int SUMMARY_MAX_LENGTH = 500;
    private static final int IP_MAX_LENGTH = 45;
    /** 감사 로그 보관 일수 (FN-ADM-114-04, 사람 2026-11-26 : 영구 보관에서 30일로). 운영 설정 아닌 코드 상수 */
    static final int KEEP_DAYS = 30;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AdminTaskLogMapper adminTaskLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 관리자 작업 로그 기록
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long mbrSn, AdminTaskType taskType, String trgtTblNm, Long trgtSn, String summary,
            Object before, Object after) {
        AdminTaskLog log = new AdminTaskLog();
        log.setMbrSn(mbrSn);
        log.setTaskTypeCd(taskType.getCode());
        log.setTrgtTblNm(trgtTblNm);
        log.setTrgtSn(trgtSn);
        log.setTaskSmryCn(cut(summary, SUMMARY_MAX_LENGTH));
        log.setChgBfrCn(before != null ? objectMapper.writeValueAsString(before) : null);
        log.setChgAftrCn(after != null ? objectMapper.writeValueAsString(after) : null);
        log.setCntnIpAddr(cut(MdcUtils.get(MdcUtils.CLIENT_IP), IP_MAX_LENGTH));
        adminTaskLogMapper.insert(log);
    }

    /**
     * 관리자 작업 로그 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminTaskLogSummary> search(AdminTaskLogSearchCond cond, PageQuery pq) {
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminTaskLogSummary> rows = adminTaskLogMapper.selectList(cond);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 로그에 나온 관리자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AdminTaskLogAdminSummary> searchAdmins() {
        return adminTaskLogMapper.selectAdmins();
    }

    /**
     * 관리자 작업 로그 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminTaskLogDetailResponse getDetail(Long mngrTaskLogSn) {
        AdminTaskLog log = adminTaskLogMapper.selectById(mngrTaskLogSn);
        if (log == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return AdminTaskLogDetailResponse.from(log, adminTaskLogMapper.selectNcnm(log.getMbrSn()),
                parse(log.getChgBfrCn()), parse(log.getChgAftrCn()));
    }

    /**
     * 보관 일수가 지난 관리자 작업 로그 삭제
     * - 반환 : 지운 행 수
     */
    @Transactional
    public int purgeExpired() {
        return purgeExpired(LocalDate.now(ZONE));
    }

    @Transactional
    int purgeExpired(LocalDate today) {
        return adminTaskLogMapper.deleteBefore(cutoff(today));
    }

    /**
     * 삭제 기준 시각 : 오늘(KST)에서 보관 일수를 뺀 날의 00:00. 이 시각보다 앞선 로그를 지운다
     */
    static LocalDateTime cutoff(LocalDate today) {
        return today.minusDays(KEEP_DAYS).atStartOfDay();
    }

    private JsonNode parse(String json) {
        return json != null ? objectMapper.readTree(json) : null;
    }

    private static String cut(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
