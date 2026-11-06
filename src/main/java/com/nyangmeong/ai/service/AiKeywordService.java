package com.nyangmeong.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.domain.AiKeywordClass;
import com.nyangmeong.ai.dto.AiKeywordResponse;
import com.nyangmeong.ai.mapper.AiKeywordMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [AI 서버용 키워드 사전 서비스]
 * - FN-ADM-081 : AI 서버는 DB에 붙지 않아 이 조회로 사전을 받는다. 관리자가 저장하면 다음 조회부터 반영
 */
@Service
@RequiredArgsConstructor
public class AiKeywordService {

    private final AiKeywordMapper aiKeywordMapper;

    /**
     * 사용 중 키워드 (정렬순서·번호순). 용도는 일정 등록 판정·가계부 분류만
     */
    @Transactional(readOnly = true)
    public List<AiKeywordResponse> getActiveKeywords(String dictUsgCd) {
        if (!AiKeywordClass.isUsage(dictUsgCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return aiKeywordMapper.selectActiveKeywords(dictUsgCd).stream().map(AiKeywordResponse::from).toList();
    }
}
