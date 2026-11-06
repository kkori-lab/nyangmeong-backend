package com.nyangmeong.ai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.domain.AiRoutMpng;
import com.nyangmeong.ai.domain.AiRouteMoveType;
import com.nyangmeong.ai.dto.AdminAiRouteCreateResponse;
import com.nyangmeong.ai.dto.AdminAiRouteSaveRequest;
import com.nyangmeong.ai.dto.AdminAiRouteSummary;
import com.nyangmeong.ai.mapper.AiRouteMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 라우팅 매핑표 서비스]
 * - SCR-ADM-080 (FN-ADM-080-01·02·03)
 * - 우선순위는 1부터 빈틈없이 : 등록·수정·삭제 때마다 삭제 안 된 행 전부를 잠그고 다시 매긴다
 * - 저장하면 다음 조회부터 반영 (캐시 없음)
 */
@Service
@RequiredArgsConstructor
public class AdminAiRouteService {

    private static final String TARGET_TABLE = "AI_ROUT_MPNG";
    private static final String DEFAULT_DOMAIN = "ETC";
    private static final Pattern DOMAIN_IN_SCREEN_ID = Pattern.compile("^SCR-([A-Z]{3})");

    private final AiRouteMapper aiRouteMapper;
    private final AdminTaskLogService adminTaskLogService;

    /**
     * 매핑표 조회 (FN-ADM-080-01) : 사용 안 함 포함, 우선순위 순. q는 공백을 빼고 지시어에서 찾는다
     */
    @Transactional(readOnly = true)
    public List<AdminAiRouteSummary> search(String q) {
        String compact = q == null ? "" : q.replaceAll("\\s", "");
        String keyword = compact.isEmpty() ? null : escapeLike(compact);
        return aiRouteMapper.selectAdminRoutes(keyword).stream().map(AdminAiRouteSummary::from).toList();
    }

    /**
     * 매핑 등록 (FN-ADM-080-02·03) : prrtSeq 자리에 넣고 나머지를 민다. 없으면 맨 아래
     */
    @Transactional
    public AdminAiRouteCreateResponse create(Long actorSn, AdminAiRouteSaveRequest req) {
        List<AiRoutMpng> order = aiRouteMapper.selectRouteOrderForUpdate();

        AiRoutMpng route = new AiRoutMpng();
        route.setTrgtScrnNm(req.trgtScrnNm());
        route.setScrnId(blankToNull(req.scrnId()));
        route.setScrnParamCn(blankToNull(req.scrnParamCn()));
        route.setInstKwrdCn(blankToNull(req.instKwrdCn()));
        route.setMvmnMthdCd(req.mvmnMthdCd());
        route.setRprsChipYn(Boolean.TRUE.equals(req.rprsChipYn()) ? "Y" : "N");
        route.setRmrkCn(blankToNull(req.rmrkCn()));
        route.setUseYn(Boolean.FALSE.equals(req.useYn()) ? "N" : "Y");
        route.setPrrtSeq(order.size() + 1);
        normalize(route);

        String actorId = String.valueOf(actorSn);
        if (aiRouteMapper.insertRoute(route, actorId) != 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<AiRoutMpng> withNew = new ArrayList<>(order);
        withNew.add(route);
        List<Long> others = snsOf(order);
        applyOrder(withNew, placeAt(others, route.getAiRoutMpngSn(),
                req.prrtSeq() == null ? others.size() + 1 : req.prrtSeq()), actorId);

        AdminAiRouteSummary created = AdminAiRouteSummary.from(getOrThrow(route.getAiRoutMpngSn()));
        adminTaskLogService.record(actorSn, AdminTaskType.CREATE, TARGET_TABLE, route.getAiRoutMpngSn(),
                "라우팅 매핑 등록", null, created);
        return new AdminAiRouteCreateResponse(route.getAiRoutMpngSn());
    }

    /**
     * 매핑 수정 (FN-ADM-080-02·03) : 보낸 칸만. prrtSeq를 보내면 그 자리로 옮긴다
     */
    @Transactional
    public AdminAiRouteSummary update(Long actorSn, Long aiRoutMpngSn, AdminAiRouteSaveRequest req) {
        if (isEmpty(req)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<AiRoutMpng> order = aiRouteMapper.selectRouteOrderForUpdate();
        AiRoutMpng before = getOrThrow(aiRoutMpngSn);

        AiRoutMpng route = copy(before);
        if (req.trgtScrnNm() != null) {
            route.setTrgtScrnNm(req.trgtScrnNm());
        }
        if (req.scrnId() != null) {
            route.setScrnId(blankToNull(req.scrnId()));
        }
        if (req.scrnParamCn() != null) {
            route.setScrnParamCn(blankToNull(req.scrnParamCn()));
        }
        if (req.instKwrdCn() != null) {
            route.setInstKwrdCn(blankToNull(req.instKwrdCn()));
        }
        if (req.mvmnMthdCd() != null) {
            route.setMvmnMthdCd(req.mvmnMthdCd());
        }
        if (req.rprsChipYn() != null) {
            route.setRprsChipYn(req.rprsChipYn() ? "Y" : "N");
        }
        if (req.rmrkCn() != null) {
            route.setRmrkCn(blankToNull(req.rmrkCn()));
        }
        if (req.useYn() != null) {
            route.setUseYn(req.useYn() ? "Y" : "N");
        }
        normalize(route);

        String actorId = String.valueOf(actorSn);
        if (aiRouteMapper.updateRoute(route, actorId) != 1) {
            throw new CustomException(ErrorCode.AI_ROUTE_NOT_FOUND);
        }
        List<Long> current = snsOf(order);
        List<Long> next = req.prrtSeq() == null ? current
                : placeAt(current.stream().filter(sn -> !sn.equals(aiRoutMpngSn)).toList(), aiRoutMpngSn,
                        req.prrtSeq());
        applyOrder(order, next, actorId);

        AdminAiRouteSummary updated = AdminAiRouteSummary.from(getOrThrow(aiRoutMpngSn));
        adminTaskLogService.record(actorSn, AdminTaskType.UPDATE, TARGET_TABLE, aiRoutMpngSn, "라우팅 매핑 수정",
                AdminAiRouteSummary.from(before), updated);
        return updated;
    }

    /**
     * 매핑 삭제 (FN-ADM-080-02, DEL_YN = 'Y') : 뒤 순위를 당긴다
     */
    @Transactional
    public void delete(Long actorSn, Long aiRoutMpngSn) {
        List<AiRoutMpng> order = aiRouteMapper.selectRouteOrderForUpdate();
        AiRoutMpng before = getOrThrow(aiRoutMpngSn);
        String actorId = String.valueOf(actorSn);
        if (aiRouteMapper.deleteRoute(aiRoutMpngSn, actorId) != 1) {
            throw new CustomException(ErrorCode.AI_ROUTE_NOT_FOUND);
        }
        applyOrder(order, snsOf(order).stream().filter(sn -> !sn.equals(aiRoutMpngSn)).toList(), actorId);
        adminTaskLogService.record(actorSn, AdminTaskType.DELETE, TARGET_TABLE, aiRoutMpngSn, "라우팅 매핑 삭제",
                AdminAiRouteSummary.from(before), null);
    }

    /**
     * sn을 requested 자리(1~행 수로 맞춤)에 넣은 순서
     */
    static List<Long> placeAt(List<Long> others, Long sn, int requested) {
        List<Long> next = new ArrayList<>(others);
        int index = Math.min(Math.max(requested, 1), others.size() + 1) - 1;
        next.add(index, sn);
        return next;
    }

    /**
     * next 순서대로 1부터 매기고, 값이 바뀐 행만 고친다
     */
    private void applyOrder(List<AiRoutMpng> current, List<Long> next, String actorId) {
        Map<Long, Integer> seqBySn = new HashMap<>();
        current.forEach(route -> seqBySn.put(route.getAiRoutMpngSn(), route.getPrrtSeq()));
        for (int i = 0; i < next.size(); i++) {
            Long sn = next.get(i);
            int seq = i + 1;
            Integer old = seqBySn.get(sn);
            if (old == null || old != seq) {
                aiRouteMapper.updatePrrtSeq(sn, seq, actorId);
            }
        }
    }

    /**
     * 대상 화면명 필수, 이동 방식 AIMG09, 이동하는 행은 화면 ID·지시어 필수, 도메인명은 화면 ID에서
     */
    private void normalize(AiRoutMpng route) {
        String trgtScrnNm = route.getTrgtScrnNm() == null ? "" : route.getTrgtScrnNm().strip();
        if (trgtScrnNm.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        route.setTrgtScrnNm(trgtScrnNm);
        AiRouteMoveType moveType = AiRouteMoveType.from(route.getMvmnMthdCd());
        if (moveType == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (moveType.isRoutable() && (route.getScrnId() == null || route.getInstKwrdCn() == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        route.setDomnNm(domainOf(route.getScrnId()));
    }

    /**
     * 화면 ID의 SCR- 뒤 세 글자, 없거나 모양이 다르면 ETC
     */
    static String domainOf(String scrnId) {
        if (scrnId == null) {
            return DEFAULT_DOMAIN;
        }
        Matcher matcher = DOMAIN_IN_SCREEN_ID.matcher(scrnId);
        return matcher.find() ? matcher.group(1) : DEFAULT_DOMAIN;
    }

    private AiRoutMpng getOrThrow(Long aiRoutMpngSn) {
        AiRoutMpng route = aiRouteMapper.selectRoute(aiRoutMpngSn);
        if (route == null) {
            throw new CustomException(ErrorCode.AI_ROUTE_NOT_FOUND);
        }
        return route;
    }

    private static List<Long> snsOf(List<AiRoutMpng> routes) {
        return routes.stream().map(AiRoutMpng::getAiRoutMpngSn).toList();
    }

    private static AiRoutMpng copy(AiRoutMpng source) {
        AiRoutMpng route = new AiRoutMpng();
        route.setAiRoutMpngSn(source.getAiRoutMpngSn());
        route.setTrgtScrnNm(source.getTrgtScrnNm());
        route.setScrnId(source.getScrnId());
        route.setScrnParamCn(source.getScrnParamCn());
        route.setInstKwrdCn(source.getInstKwrdCn());
        route.setMvmnMthdCd(source.getMvmnMthdCd());
        route.setRprsChipYn(source.getRprsChipYn());
        route.setPrrtSeq(source.getPrrtSeq());
        route.setRmrkCn(source.getRmrkCn());
        route.setUseYn(source.getUseYn());
        return route;
    }

    private static boolean isEmpty(AdminAiRouteSaveRequest req) {
        return req.trgtScrnNm() == null && req.scrnId() == null && req.scrnParamCn() == null
                && req.instKwrdCn() == null && req.mvmnMthdCd() == null && req.rprsChipYn() == null
                && req.prrtSeq() == null && req.rmrkCn() == null && req.useYn() == null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
