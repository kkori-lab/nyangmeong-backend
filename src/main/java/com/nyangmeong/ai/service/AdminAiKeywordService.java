package com.nyangmeong.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.domain.AiKeywordClass;
import com.nyangmeong.ai.domain.KwrdDict;
import com.nyangmeong.ai.dto.AdminAiKeywordCreateResponse;
import com.nyangmeong.ai.dto.AdminAiKeywordSaveRequest;
import com.nyangmeong.ai.dto.AdminAiKeywordSummary;
import com.nyangmeong.ai.mapper.AiKeywordMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 키워드 사전 서비스]
 * - SCR-ADM-081 (FN-ADM-081-01·02)
 * - 다루는 행은 AiKeywordClass의 용도·분류뿐 : 뉴스 용도(SCR-ADM-050)와 '판정 규칙'·'시간' 행은 없는 것으로 본다
 * - 저장하면 다음 조회부터 반영 (캐시 없음)
 */
@Service
@RequiredArgsConstructor
public class AdminAiKeywordService {

    private static final String TARGET_TABLE = "KWRD_DICT";
    private static final int FIRST_SORT_SEQ = 10;

    private final AiKeywordMapper aiKeywordMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 키워드 조회 (FN-ADM-081-01) : 사용 안 함 포함, 정렬순서·번호순. kwrdClsfNm이 없으면 그 용도의 편집 분류 전부
     */
    @Transactional(readOnly = true)
    public List<AdminAiKeywordSummary> search(String dictUsgCd, String kwrdClsfNm) {
        if (!AiKeywordClass.isUsage(dictUsgCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String clsf = blankToNull(kwrdClsfNm);
        return aiKeywordMapper.selectAdminKeywords(dictUsgCd, clsf).stream()
                .filter(keyword -> AiKeywordClass.of(keyword.getDictUsgCd(), keyword.getKwrdClsfNm()) != null)
                .map(AdminAiKeywordSummary::from)
                .toList();
    }

    /**
     * 키워드 등록 (FN-ADM-081-02) : 같은 용도·분류의 맨 아래
     */
    @Transactional
    public AdminAiKeywordCreateResponse create(Long actorSn, AdminAiKeywordSaveRequest req) {
        AiKeywordClass clsf = AiKeywordClass.of(req.dictUsgCd(), blankToNull(req.kwrdClsfNm()));
        if (clsf == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        KwrdDict keyword = new KwrdDict();
        keyword.setDictUsgCd(clsf.getDictUsgCd());
        keyword.setKwrdClsfNm(clsf.getKwrdClsfNm());
        keyword.setArtclNm(blankToNull(req.artclNm()));
        keyword.setKwrdCn(blankToNull(req.kwrdCn()));
        keyword.setIntrpRsltCn(blankToNull(req.intrpRsltCn()));
        keyword.setMpngCd(blankToNull(req.mpngCd()));
        keyword.setRmrkCn(blankToNull(req.rmrkCn()));
        keyword.setUseYn(Boolean.FALSE.equals(req.useYn()) ? "N" : "Y");
        validate(clsf, keyword);

        Integer maxSortSeq = aiKeywordMapper.selectMaxSortSeq(clsf.getDictUsgCd(), clsf.getKwrdClsfNm());
        keyword.setSortSeq(maxSortSeq == null ? FIRST_SORT_SEQ : maxSortSeq + 1);

        if (aiKeywordMapper.insertKeyword(keyword, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, keyword.getKwrdDictSn(),
                "AI 키워드 등록", null, AdminAiKeywordSummary.from(getOrThrow(keyword.getKwrdDictSn())));
        return new AdminAiKeywordCreateResponse(keyword.getKwrdDictSn());
    }

    /**
     * 키워드 수정 (FN-ADM-081-02) : 보낸 칸만. 용도·분류·정렬순서는 그대로
     */
    @Transactional
    public AdminAiKeywordSummary update(Long actorSn, Long kwrdDictSn, AdminAiKeywordSaveRequest req) {
        if (isEmpty(req)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        KwrdDict before = getOrThrow(kwrdDictSn);
        AiKeywordClass clsf = AiKeywordClass.of(before.getDictUsgCd(), before.getKwrdClsfNm());

        KwrdDict keyword = copy(before);
        if (req.artclNm() != null) {
            keyword.setArtclNm(blankToNull(req.artclNm()));
        }
        if (req.kwrdCn() != null) {
            keyword.setKwrdCn(blankToNull(req.kwrdCn()));
        }
        if (req.intrpRsltCn() != null) {
            keyword.setIntrpRsltCn(blankToNull(req.intrpRsltCn()));
        }
        if (req.mpngCd() != null) {
            keyword.setMpngCd(blankToNull(req.mpngCd()));
        }
        if (req.rmrkCn() != null) {
            keyword.setRmrkCn(blankToNull(req.rmrkCn()));
        }
        if (req.useYn() != null) {
            keyword.setUseYn(req.useYn() ? "Y" : "N");
        }
        validate(clsf, keyword);

        if (aiKeywordMapper.updateKeyword(keyword, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.AI_KEYWORD_NOT_FOUND);
        }
        AdminAiKeywordSummary updated = AdminAiKeywordSummary.from(getOrThrow(kwrdDictSn));
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, kwrdDictSn, "AI 키워드 수정",
                AdminAiKeywordSummary.from(before), updated);
        return updated;
    }

    /**
     * 키워드 삭제 (FN-ADM-081-02, DEL_YN = 'Y')
     */
    @Transactional
    public void delete(Long actorSn, Long kwrdDictSn) {
        KwrdDict before = getOrThrow(kwrdDictSn);
        if (aiKeywordMapper.deleteKeyword(kwrdDictSn, String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.AI_KEYWORD_NOT_FOUND);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.DELETE, TARGET_TABLE, kwrdDictSn, "AI 키워드 삭제",
                AdminAiKeywordSummary.from(before), null);
    }

    /**
     * 항목명·키워드 필수, 결과가 코드인 분류는 그 그룹의 연결 코드 필수, 아니면 연결 코드 없음
     */
    private void validate(AiKeywordClass clsf, KwrdDict keyword) {
        if (keyword.getArtclNm() == null || keyword.getKwrdCn() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String mpngCd = keyword.getMpngCd();
        if (clsf.getCodeGroup() == null) {
            if (mpngCd != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return;
        }
        if (mpngCd == null || aiKeywordMapper.countCodeInGroup(mpngCd, clsf.getCodeGroup()) == 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 관리자 화면이 다루는 행 하나. 없거나 삭제됐거나 다른 화면의 행이면 404
     */
    private KwrdDict getOrThrow(Long kwrdDictSn) {
        KwrdDict keyword = aiKeywordMapper.selectKeyword(kwrdDictSn);
        if (keyword == null || AiKeywordClass.of(keyword.getDictUsgCd(), keyword.getKwrdClsfNm()) == null) {
            throw new CustomException(ErrorCode.AI_KEYWORD_NOT_FOUND);
        }
        return keyword;
    }

    private static KwrdDict copy(KwrdDict source) {
        KwrdDict keyword = new KwrdDict();
        keyword.setKwrdDictSn(source.getKwrdDictSn());
        keyword.setDictUsgCd(source.getDictUsgCd());
        keyword.setKwrdClsfNm(source.getKwrdClsfNm());
        keyword.setArtclNm(source.getArtclNm());
        keyword.setKwrdCn(source.getKwrdCn());
        keyword.setIntrpRsltCn(source.getIntrpRsltCn());
        keyword.setMpngCd(source.getMpngCd());
        keyword.setSortSeq(source.getSortSeq());
        keyword.setRmrkCn(source.getRmrkCn());
        keyword.setUseYn(source.getUseYn());
        return keyword;
    }

    private static boolean isEmpty(AdminAiKeywordSaveRequest req) {
        return req.artclNm() == null && req.kwrdCn() == null && req.intrpRsltCn() == null && req.mpngCd() == null
                && req.rmrkCn() == null && req.useYn() == null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
