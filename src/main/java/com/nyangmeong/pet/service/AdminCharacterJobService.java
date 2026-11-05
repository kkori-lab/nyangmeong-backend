package com.nyangmeong.pet.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.dto.AdminCharacterJobListResponse;
import com.nyangmeong.pet.dto.AdminCharacterJobRow;
import com.nyangmeong.pet.dto.AdminCharacterJobSearchCond;
import com.nyangmeong.pet.dto.AdminCharacterJobStatusCount;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 캐릭터 생성 현황 서비스]
 * - SCR-ADM-082 (FN-ADM-082-02) 상태별 건수·목록
 * - 결과 이미지·원본 사진은 주지 않는다 (결정 26)
 */
@Service
@RequiredArgsConstructor
public class AdminCharacterJobService {

    private final PixelCharacterMapper pixelCharacterMapper;

    /**
     * 상태별 건수(CHRG01 네 개, 없는 상태는 0)와 목록(최신순)
     */
    @Transactional(readOnly = true)
    public AdminCharacterJobListResponse search(AdminCharacterJobSearchCond cond, PageQuery pq) {
        AdminCharacterJobSearchCond c = cond == null ? new AdminCharacterJobSearchCond() : cond;
        String sttsCd = c.getCrtJobSttsCd();
        c.setCrtJobSttsCd(sttsCd == null || sttsCd.isBlank() ? null : sttsCd.trim());
        if (c.getCrtJobSttsCd() != null
                && Arrays.stream(CharacterJobStatus.values()).noneMatch(s -> s.is(c.getCrtJobSttsCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Map<String, Long> counted = pixelCharacterMapper.selectAdminJobStatusCounts().stream()
                .collect(Collectors.toMap(AdminCharacterJobStatusCount::getCrtJobSttsCd,
                        AdminCharacterJobStatusCount::getCnt, Long::sum));
        List<AdminCharacterJobStatusCount> sttsCnts = Arrays.stream(CharacterJobStatus.values())
                .map(s -> new AdminCharacterJobStatusCount(s.getCode(), counted.getOrDefault(s.getCode(), 0L)))
                .toList();
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminCharacterJobRow> rows = pixelCharacterMapper.selectAdminJobList(c);
        return AdminCharacterJobListResponse.of(sttsCnts, PageResponse.of(new PageInfo<>(rows)));
    }
}
