package com.nyangmeong.health.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.BasicCareItem;
import com.nyangmeong.health.domain.CareCompletion;
import com.nyangmeong.health.domain.CareCycle;
import com.nyangmeong.health.domain.CareItem;
import com.nyangmeong.health.domain.CareZone;
import com.nyangmeong.health.dto.CareCompletionCreateRequest;
import com.nyangmeong.health.dto.CareCompletionResponse;
import com.nyangmeong.health.dto.CareCompletionSearchCond;
import com.nyangmeong.health.dto.CareCompletionSummary;
import com.nyangmeong.health.dto.CareItemCreateRequest;
import com.nyangmeong.health.dto.CareItemExposureSaveRequest;
import com.nyangmeong.health.dto.CareItemResponse;
import com.nyangmeong.health.dto.CareItemSearchCond;
import com.nyangmeong.health.dto.CareItemSummary;
import com.nyangmeong.health.dto.CareItemUpdateRequest;
import com.nyangmeong.health.mapper.CareItemMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

import lombok.RequiredArgsConstructor;

/**
 * [토탈 케어리스트]
 * - 케어 항목 조회·추가·수정·삭제, 완료 체크·취소, 메인 표시 저장 (SCR-HLT-003, FN-HLT-003-12·13·21~24·38)
 * - 기본 항목은 반려동물의 첫 조회·저장 때 만듦 : 기본 항목이 0건일 때만
 */
@Service
@RequiredArgsConstructor
public class CareItemService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAIN_LIMIT = 6;
    private static final String DEFAULT_ICON = "task_alt";

    /** 기본 항목 완료 시 반영할 케어미션 */
    private static final Map<BasicCareItem, CareMission> MISSIONS = new EnumMap<>(BasicCareItem.class);

    static {
        MISSIONS.put(BasicCareItem.TOILET_CLEANING, CareMission.TOILET_CLEANING);
        MISSIONS.put(BasicCareItem.TOOTH_BRUSHING, CareMission.TOOTH_BRUSHING);
        MISSIONS.put(BasicCareItem.BRUSHING, CareMission.BRUSHING);
        MISSIONS.put(BasicCareItem.PAW_CARE, CareMission.PAW_CARE);
        MISSIONS.put(BasicCareItem.PLAY, CareMission.PLAY);
        MISSIONS.put(BasicCareItem.EAR_CLEANING, CareMission.EAR_CLEANING);
        MISSIONS.put(BasicCareItem.NAIL_CARE, CareMission.NAIL_CARE);
        MISSIONS.put(BasicCareItem.BATH, CareMission.BATH);
        MISSIONS.put(BasicCareItem.EYE_CARE, CareMission.EYE_CARE);
        MISSIONS.put(BasicCareItem.PAW_HAIR_CARE, CareMission.PAW_HAIR_CARE);
        MISSIONS.put(BasicCareItem.MOUTH_CHECK, CareMission.MOUTH_CHECK);
    }

    private final CareItemMapper careItemMapper;
    private final PetAuthorizer petAuthorizer;
    private final CareMissionService careMissionService;

    /**
     * 완료 체크 결과
     * - created : 새로 체크했는지(201), 이미 체크한 항목이면 false(200)
     */
    public record Completed(CareCompletionResponse response, boolean created) {
    }

    /**
     * 케어 항목 목록 조회
     */
    @Transactional
    public List<CareItemSummary> search(Long mbrSn, Long petSn, CareItemSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        createBasicItems(mbrSn, petSn);
        return careItemMapper.selectList(petSn, cond, today(), CareZone.REGULAR.getCode());
    }

    /**
     * 반려동물별 케어 항목명 목록 조회
     * - AI 대화 같은 이름 항목 확인용 (FN-AIM-012-08) : 반려동물 권한은 호출하는 쪽에서 검사
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> searchNamesByPets(List<Long> petSns) {
        Map<Long, List<String>> names = new LinkedHashMap<>();
        if (petSns == null || petSns.isEmpty()) {
            return names;
        }
        petSns.stream().filter(Objects::nonNull).forEach(petSn -> names.put(petSn, new ArrayList<>()));
        if (names.isEmpty()) {
            return names;
        }
        for (CareItem item : careItemMapper.selectNamesByPets(List.copyOf(names.keySet()))) {
            names.get(item.getPetSn()).add(item.getCareArtclNm());
        }
        return names;
    }

    /**
     * 케어 완료 기록 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CareCompletionSummary> searchCompletions(Long mbrSn, Long petSn, CareCompletionSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return careItemMapper.selectCompletionList(petSn, cond);
    }

    /**
     * 케어 항목 추가
     */
    @Transactional
    public CareItemResponse create(Long mbrSn, Long petSn, CareItemCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        LocalDate today = today();
        CareZone zone = CareZone.of(req.careZoneCd()).orElseThrow(this::invalid);

        CareItem item = new CareItem();
        item.setPetSn(petSn);
        item.setCareZoneCd(zone.getCode());
        item.setCareArtclNm(nameOf(req.careArtclNm()));
        item.setRprsIconId(iconOf(req.rprsIconId()));
        item.setMainExpsrYn("N");
        item.setMainSortSeq(0);
        item.setBscArtclYn("N");
        item.setRgtrId(String.valueOf(mbrSn));
        item.setMdfrId(String.valueOf(mbrSn));

        LocalDate lastCmptnYmd = null;
        if (zone == CareZone.REGULAR) {
            CareCycle cycle = applyCycle(item, req.careCycleCd(), req.cycleDrctInptDayCnt());
            lastCmptnYmd = req.lastCmptnYmd();
            if (lastCmptnYmd != null && lastCmptnYmd.isAfter(today)) {
                throw new CustomException(ErrorCode.CARE_COMPLETION_FUTURE_DATE);
            }
            item.setLastCmptnYmd(lastCmptnYmd);
            item.setNxtmPrnmntYmd(cycle.next(lastCmptnYmd != null ? lastCmptnYmd : today,
                    item.getCycleDrctInptDayCnt()));
        }

        careItemMapper.insertItem(item);
        if (lastCmptnYmd != null) {
            careItemMapper.upsertCompletion(item.getCareArtclSn(), mbrSn, lastCmptnYmd, 1);
        }
        return CareItemResponse.from(careItemMapper.selectSummary(item.getCareArtclSn(), today));
    }

    /**
     * 케어 항목 수정
     */
    @Transactional
    public CareItemResponse update(Long mbrSn, Long petSn, Long careArtclSn, CareItemUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CareItem item = getOwnItem(petSn, careArtclSn);
        checkNotLinked(item);

        if (isBasic(item)) {
            /** 기본 항목은 정기관리의 주기만 변경 */
            boolean isOtherChanged = req.careArtclNm() != null || req.rprsIconId() != null
                    || req.careZoneCd() != null;
            boolean isDailyCycleChanged = CareZone.DAILY.is(item.getCareZoneCd())
                    && (req.careCycleCd() != null || req.cycleDrctInptDayCnt() != null);
            if (isOtherChanged || isDailyCycleChanged) {
                throw new CustomException(ErrorCode.CARE_ITEM_DEFAULT_LIMITED);
            }
        } else {
            if (req.careArtclNm() != null) {
                item.setCareArtclNm(nameOf(req.careArtclNm()));
            }
            if (req.rprsIconId() != null) {
                item.setRprsIconId(iconOf(req.rprsIconId()));
            }
            if (req.careZoneCd() != null) {
                item.setCareZoneCd(CareZone.of(req.careZoneCd()).orElseThrow(this::invalid).getCode());
            }
        }

        if (CareZone.REGULAR.is(item.getCareZoneCd())) {
            String careCycleCd = req.careCycleCd() != null ? req.careCycleCd() : item.getCareCycleCd();
            Integer dayCnt = req.cycleDrctInptDayCnt() != null ? req.cycleDrctInptDayCnt()
                    : item.getCycleDrctInptDayCnt();
            applyCycle(item, careCycleCd, dayCnt);
        } else {
            item.setCareCycleCd(null);
            item.setCycleDrctInptDayCnt(null);
        }
        refreshDates(item, String.valueOf(mbrSn));
        return CareItemResponse.from(careItemMapper.selectSummary(careArtclSn, today()));
    }

    /**
     * 케어 항목 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long careArtclSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CareItem item = getOwnItem(petSn, careArtclSn);
        checkNotLinked(item);
        if (isBasic(item)) {
            throw new CustomException(ErrorCode.CARE_ITEM_DEFAULT_NOT_DELETABLE);
        }
        careItemMapper.deleteItem(careArtclSn, String.valueOf(mbrSn));
    }

    /**
     * 메인 표시 항목 전체 저장
     */
    @Transactional
    public List<CareItemSummary> save(Long mbrSn, Long petSn, CareItemExposureSaveRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        List<Long> careArtclSns = req.careArtclSns();
        if (careArtclSns.size() > MAIN_LIMIT) {
            throw new CustomException(ErrorCode.CARE_ITEM_MAIN_LIMIT_EXCEEDED);
        }
        if (careArtclSns.stream().anyMatch(Objects::isNull)
                || new HashSet<>(careArtclSns).size() != careArtclSns.size()) {
            throw invalid();
        }

        createBasicItems(mbrSn, petSn);
        if (!careArtclSns.isEmpty() && careItemMapper.countItemsIn(petSn, careArtclSns) != careArtclSns.size()) {
            throw invalid();
        }

        String actorId = String.valueOf(mbrSn);
        careItemMapper.updateMainExposureClear(petSn, actorId);
        for (int i = 0; i < careArtclSns.size(); i++) {
            careItemMapper.updateMainExposure(careArtclSns.get(i), i + 1, actorId);
        }

        CareItemSearchCond cond = new CareItemSearchCond();
        cond.setMainExpsrYn(true);
        return careItemMapper.selectList(petSn, cond, today(), CareZone.REGULAR.getCode());
    }

    /**
     * 케어 완료 체크
     */
    @Transactional
    public Completed complete(Long mbrSn, Long petSn, CareCompletionCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CareItem item = getOwnItem(petSn, req.careArtclSn());
        LocalDate today = today();
        String actorId = String.valueOf(mbrSn);

        CareCompletion completion = careItemMapper.selectCompletionByDateForUpdate(item.getCareArtclSn(), today);
        boolean isFirstToday = completion == null;
        boolean isAlive = completion != null && "N".equals(completion.getDelYn());
        boolean isCreated = false;

        if (!isAlive) {
            careItemMapper.upsertCompletion(item.getCareArtclSn(), mbrSn, today, 1);
            isCreated = true;
        } else if (item.getPrscSn() != null && completion.getCmptnCycl() < dayMdcnNmtm(item)) {
            careItemMapper.updateCompletionCycle(completion.getCareCmptnHstrySn(), completion.getCmptnCycl() + 1,
                    actorId);
        }

        completion = careItemMapper.selectCompletionByDateForUpdate(item.getCareArtclSn(), today);
        if (isCreated) {
            refreshDates(item, actorId);
            /** 취소 뒤 다시 체크한 날은 미션에 다시 반영하지 않음 */
            if (isFirstToday) {
                reflectMission(mbrSn, petSn, item);
            }
        }
        return new Completed(CareCompletionResponse.from(completion, item, completion.getCmptnCycl()), isCreated);
    }

    /**
     * 케어 완료 취소
     */
    @Transactional
    public void cancel(Long mbrSn, Long petSn, Long careCmptnHstrySn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        CareCompletion peek = careItemMapper.selectCompletion(careCmptnHstrySn);
        if (peek == null) {
            throw notFoundCompletion();
        }
        CareItem item = careItemMapper.selectItemForUpdate(peek.getCareArtclSn());
        if (item == null || !petSn.equals(item.getPetSn())) {
            throw notFoundCompletion();
        }
        CareCompletion completion = careItemMapper.selectCompletionForUpdate(careCmptnHstrySn);
        if (completion == null) {
            throw notFoundCompletion();
        }

        String actorId = String.valueOf(mbrSn);
        if (item.getPrscSn() != null && completion.getCmptnCycl() > 1) {
            careItemMapper.updateCompletionCycle(careCmptnHstrySn, completion.getCmptnCycl() - 1, actorId);
        } else {
            careItemMapper.deleteCompletion(careCmptnHstrySn, actorId);
        }
        refreshDates(item, actorId);
    }

    /**
     * 기본 항목 생성
     */
    private void createBasicItems(Long mbrSn, Long petSn) {
        if (careItemMapper.countBasicItems(petSn) > 0) {
            return;
        }
        /** 같은 반려동물의 동시 첫 조회를 순서대로 처리 */
        if (careItemMapper.selectPetForUpdate(petSn) == null) {
            throw new CustomException(ErrorCode.PET_NOT_FOUND);
        }
        if (careItemMapper.countBasicItemsForUpdate(petSn) > 0) {
            return;
        }

        LocalDate today = today();
        String actorId = String.valueOf(mbrSn);
        for (BasicCareItem basic : BasicCareItem.values()) {
            CareItem item = new CareItem();
            item.setPetSn(petSn);
            item.setCareZoneCd(basic.getZone().getCode());
            item.setCareArtclNm(basic.getCareArtclNm());
            item.setRprsIconId(basic.getRprsIconId());
            item.setMainExpsrYn(basic.getMainSortSeq() > 0 ? "Y" : "N");
            item.setMainSortSeq(basic.getMainSortSeq());
            item.setBscArtclYn("Y");
            item.setRgtrId(actorId);
            item.setMdfrId(actorId);
            if (basic.getCycle() != null) {
                item.setCareCycleCd(basic.getCycle().getCode());
                item.setNxtmPrnmntYmd(basic.getCycle().next(today, null));
            }
            careItemMapper.insertItem(item);
        }
    }

    /**
     * 최종 완료일·다음 예정일 다시 계산
     * - 정기관리 : 남은 완료 기록 중 가장 최근 날짜 + 주기, 기록이 없으면 등록일 + 주기
     */
    private void refreshDates(CareItem item, String actorId) {
        LocalDate latest = careItemMapper.selectLatestCompletionYmd(item.getCareArtclSn());
        item.setLastCmptnYmd(latest);
        item.setNxtmPrnmntYmd(null);
        if (CareZone.REGULAR.is(item.getCareZoneCd())) {
            CareCycle cycle = CareCycle.of(item.getCareCycleCd()).orElse(null);
            boolean isCycleReady = cycle != null
                    && (cycle != CareCycle.CUSTOM || item.getCycleDrctInptDayCnt() != null);
            if (isCycleReady) {
                LocalDate base = latest != null ? latest : item.getRegDt().toLocalDate();
                item.setNxtmPrnmntYmd(cycle.next(base, item.getCycleDrctInptDayCnt()));
            }
        }
        careItemMapper.updateItem(item, actorId);
    }

    /**
     * 주기 검증·반영
     */
    private CareCycle applyCycle(CareItem item, String careCycleCd, Integer dayCnt) {
        CareCycle cycle = CareCycle.of(careCycleCd).orElseThrow(this::invalid);
        if (cycle == CareCycle.CUSTOM && (dayCnt == null || dayCnt < 1 || dayCnt > 9999)) {
            throw invalid();
        }
        item.setCareCycleCd(cycle.getCode());
        item.setCycleDrctInptDayCnt(cycle == CareCycle.CUSTOM ? dayCnt : null);
        return cycle;
    }

    /**
     * 기본 항목 완료의 케어미션 반영
     */
    private void reflectMission(Long mbrSn, Long petSn, CareItem item) {
        if (!isBasic(item)) {
            return;
        }
        BasicCareItem.of(item.getCareArtclNm()).map(MISSIONS::get)
                .ifPresent(mission -> careMissionService.reflect(petSn, mission, mbrSn));
    }

    /**
     * 경로 반려동물의 케어 항목 잠금 조회
     */
    private CareItem getOwnItem(Long petSn, Long careArtclSn) {
        CareItem item = careItemMapper.selectItemForUpdate(careArtclSn);
        if (item == null || !petSn.equals(item.getPetSn())) {
            throw new CustomException(ErrorCode.CARE_ITEM_NOT_FOUND);
        }
        return item;
    }

    private void checkNotLinked(CareItem item) {
        if (item.getPrscSn() != null || item.getReptPrvntMngSn() != null) {
            throw new CustomException(ErrorCode.CARE_ITEM_LINKED_NOT_EDITABLE);
        }
    }

    private int dayMdcnNmtm(CareItem item) {
        Integer dayMdcnNmtm = careItemMapper.selectDayMdcnNmtm(item.getPrscSn());
        return dayMdcnNmtm == null ? 1 : dayMdcnNmtm;
    }

    private boolean isBasic(CareItem item) {
        return "Y".equals(item.getBscArtclYn());
    }

    private String nameOf(String careArtclNm) {
        if (careArtclNm == null || careArtclNm.isBlank()) {
            throw invalid();
        }
        return careArtclNm.trim();
    }

    private String iconOf(String rprsIconId) {
        return rprsIconId == null || rprsIconId.isBlank() ? DEFAULT_ICON : rprsIconId.trim();
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private CustomException notFoundCompletion() {
        return new CustomException(ErrorCode.CARE_COMPLETION_NOT_FOUND);
    }
}
