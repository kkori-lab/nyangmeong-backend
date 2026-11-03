package com.nyangmeong.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.dto.AiSettingResponse;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;

/**
 * [AI 설정값 서비스]
 * - SCR-AIM-002 Python AI 서버가 응답 토큰 상한·대화 턴 수를 읽는다
 * - 설정 행이 없으면 SystemSetting 기본값(4500·6)
 */
@Service
@RequiredArgsConstructor
public class AiSettingService {

    private final SystemSettingService systemSettingService;

    /**
     * AI 설정값 조회
     */
    @Transactional(readOnly = true)
    public AiSettingResponse getSetting() {
        SystemSetting setting = systemSettingService.get();
        return new AiSettingResponse(setting.getAiTknUplmtCnt().intValue(), setting.getAiCnvrsTurnCnt().intValue());
    }
}
