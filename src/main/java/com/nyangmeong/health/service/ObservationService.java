package com.nyangmeong.health.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.ObservationCycle;
import com.nyangmeong.health.domain.ObservationItem;
import com.nyangmeong.health.domain.ObservationStatus;
import com.nyangmeong.health.dto.ObservationCycleCreateRequest;
import com.nyangmeong.health.dto.ObservationCycleResponse;
import com.nyangmeong.health.dto.ObservationCycleSearchCond;
import com.nyangmeong.health.dto.ObservationCycleSummary;
import com.nyangmeong.health.dto.ObservationCycleUpdateRequest;
import com.nyangmeong.health.dto.ObservationItemCreateRequest;
import com.nyangmeong.health.dto.ObservationItemResponse;
import com.nyangmeong.health.dto.ObservationItemSearchCond;
import com.nyangmeong.health.dto.ObservationItemSummary;
import com.nyangmeong.health.mapper.ObservationMapper;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.service.CareMissionService;

import lombok.RequiredArgsConstructor;

/**
 * [관찰 서비스]
 * - 관찰 항목 목록·등록·종료, 관찰 회차 목록·사진·추가·수정·삭제 (SCR-HLT-002, FN-HLT-002-15~25·32)
 * - 회차 번호는 저장하지 않고 조회 때 관찰일 순으로 매김(삭제 뒤 1회차부터 다시 번호가 매겨짐)
 */
@Service
@RequiredArgsConstructor
public class ObservationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ObservationMapper observationMapper;
    private final PetAuthorizer petAuthorizer;
    private final FileStore fileStore;
    private final CareMissionService careMissionService;

    /**
     * 관찰 항목 목록 조회
     * - 관찰 중 항목이 먼저, 같은 상태 안에서는 최근 관찰일 내림차순
     */
    @Transactional(readOnly = true)
    public List<ObservationItemSummary> searchItems(Long mbrSn, Long petSn, ObservationItemSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        if (cond.getObsrvSttsCd() != null && ObservationStatus.of(cond.getObsrvSttsCd()).isEmpty()) {
            throw invalid();
        }
        cond.setPetSn(petSn);
        List<ObservationItemSummary> items = observationMapper.selectItems(cond);

        Map<Long, List<ObservationCycleSummary>> cyclesByItem = new LinkedHashMap<>();
        for (ObservationCycleSummary cycle : observationMapper.selectPetCycles(petSn)) {
            cyclesByItem.computeIfAbsent(cycle.getObsrvArtclSn(), key -> new ArrayList<>()).add(cycle);
        }
        for (ObservationItemSummary item : items) {
            fillCycles(item, cyclesByItem.getOrDefault(item.getObsrvArtclSn(), List.of()));
        }
        items.sort(Comparator
                .comparing((ObservationItemSummary item) -> !ObservationStatus.ONGOING.getCode()
                        .equals(item.getObsrvSttsCd()))
                .thenComparing(this::lastObservedAt, Comparator.reverseOrder()));
        return items;
    }

    /**
     * 관찰 회차 목록 조회(관찰일 오름차순)
     */
    @Transactional(readOnly = true)
    public List<ObservationCycleSummary> searchCycles(Long mbrSn, Long petSn, ObservationCycleSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getItem(petSn, cond.getObsrvArtclSn());
        cond.setPetSn(petSn);
        return observationMapper.selectCycles(cond);
    }

    /**
     * 관찰 사진 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getPhoto(Long mbrSn, Long petSn, Long obsrvCyclSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return fileStore.load(getCycle(petSn, obsrvCyclSn).getAtchFileSn());
    }

    /**
     * 관찰 중 항목이 있는 반려동물 번호 조회(케어미션 출제용)
     */
    @Transactional(readOnly = true)
    public Set<Long> searchObservingPetSns() {
        return new HashSet<>(observationMapper.selectObservingPetSns(ObservationStatus.ONGOING.getCode()));
    }

    /**
     * 관찰 항목 등록(항목 + 상태 이력 + 1회차)
     */
    @Transactional
    public ObservationItemResponse createItem(Long mbrSn, Long petSn, ObservationItemCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        checkCode(HealthCodeGroup.OBSERVATION_REGION, req.obsrvRegnCd());
        checkCycleValue(req.obsrvDt(), req.obsrvSz());
        if (req.file().isEmpty()) {
            throw invalid();
        }
        String actorId = String.valueOf(mbrSn);

        ObservationItem item = new ObservationItem();
        item.setPetSn(petSn);
        item.setObsrvRegnCd(req.obsrvRegnCd());
        item.setObsrvSttsCd(ObservationStatus.ONGOING.getCode());
        item.setObsrvArtclNm(req.obsrvArtclNm().trim());
        item.setObsrvRegnDtlNm(blankToNull(req.obsrvRegnDtlNm()));
        item.setRgtrId(actorId);
        item.setMdfrId(actorId);
        observationMapper.insertItem(item);
        observationMapper.insertItemStatusHistory(item.getObsrvArtclSn(), mbrSn, null, item.getObsrvSttsCd());

        ObservationCycle cycle = newCycle(item.getObsrvArtclSn(), req.file(), req.obsrvDt(), req.obsrvSz(), null,
                req.memoCn(), actorId);
        observationMapper.insertCycle(cycle);

        careMissionService.reflect(petSn, CareMission.OBSERVATION_RECORD, mbrSn);
        return new ObservationItemResponse(item.getObsrvArtclSn(), item.getObsrvArtclNm(), item.getObsrvRegnCd(),
                item.getObsrvRegnDtlNm(), item.getObsrvSttsCd(), cycle.getObsrvCyclSn());
    }

    /**
     * 관찰 종료(상태만 바꾸고 기록은 유지, 이미 종료면 그대로)
     */
    @Transactional
    public void endItem(Long mbrSn, Long petSn, Long obsrvArtclSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ObservationItem item = getItem(petSn, obsrvArtclSn);
        if (ObservationStatus.ENDED.getCode().equals(item.getObsrvSttsCd())) {
            return;
        }
        observationMapper.updateItemEnd(obsrvArtclSn, ObservationStatus.ENDED.getCode(), String.valueOf(mbrSn));
        observationMapper.insertItemStatusHistory(obsrvArtclSn, mbrSn, item.getObsrvSttsCd(),
                ObservationStatus.ENDED.getCode());
    }

    /**
     * 관찰 회차 추가(종료된 항목 불가)
     */
    @Transactional
    public ObservationCycleResponse createCycle(Long mbrSn, Long petSn, ObservationCycleCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ObservationItem item = getItem(petSn, req.obsrvArtclSn());
        if (ObservationStatus.ENDED.getCode().equals(item.getObsrvSttsCd())) {
            throw new CustomException(ErrorCode.OBSERVATION_ITEM_ENDED);
        }
        checkCycleValue(req.obsrvDt(), req.obsrvSz());
        if (req.obsrvChgCd() != null) {
            checkCode(HealthCodeGroup.OBSERVATION_CHANGE, req.obsrvChgCd());
        }

        ObservationCycle cycle = newCycle(item.getObsrvArtclSn(), req.file(), req.obsrvDt(), req.obsrvSz(),
                req.obsrvChgCd(), req.memoCn(), String.valueOf(mbrSn));
        observationMapper.insertCycle(cycle);

        careMissionService.reflect(petSn, CareMission.OBSERVATION_RECORD, mbrSn);
        return ObservationCycleResponse.from(getSummary(petSn, item.getObsrvArtclSn(), cycle.getObsrvCyclSn()));
    }

    /**
     * 관찰 회차 수정(보낸 값만, 사진을 보내면 교체)
     */
    @Transactional
    public ObservationCycleResponse updateCycle(Long mbrSn, Long petSn, Long obsrvCyclSn,
            ObservationCycleUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ObservationCycle cycle = getCycle(petSn, obsrvCyclSn);
        String actorId = String.valueOf(mbrSn);

        if (req.obsrvDt() != null) {
            checkCycleValue(req.obsrvDt(), null);
            cycle.setObsrvDt(req.obsrvDt());
        }
        if (req.obsrvSz() != null) {
            checkCycleValue(null, req.obsrvSz());
            cycle.setObsrvSz(req.obsrvSz());
        }
        if (req.obsrvChgCd() != null) {
            checkCode(HealthCodeGroup.OBSERVATION_CHANGE, req.obsrvChgCd());
            cycle.setObsrvChgCd(req.obsrvChgCd());
        }
        if (req.memoCn() != null) {
            cycle.setMemoCn(blankToNull(req.memoCn()));
        }
        if (req.file() != null && !req.file().isEmpty()) {
            cycle.setAtchFileSn(fileStore.store(req.file(), actorId));
        }
        observationMapper.updateCycle(cycle, actorId);
        return ObservationCycleResponse.from(getSummary(petSn, cycle.getObsrvArtclSn(), obsrvCyclSn));
    }

    /**
     * 관찰 회차 삭제(마지막 회차면 항목도 삭제)
     */
    @Transactional
    public void deleteCycle(Long mbrSn, Long petSn, Long obsrvCyclSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ObservationCycle cycle = getCycle(petSn, obsrvCyclSn);
        String actorId = String.valueOf(mbrSn);
        observationMapper.deleteCycle(obsrvCyclSn, actorId);
        if (observationMapper.countCycles(cycle.getObsrvArtclSn()) == 0) {
            observationMapper.deleteItem(cycle.getObsrvArtclSn(), actorId);
        }
    }

    /**
     * 항목 요약에 첫·최근 회차, 회차 수, 마지막 회차 증감을 채움
     */
    private void fillCycles(ObservationItemSummary item, List<ObservationCycleSummary> cycles) {
        if (cycles.isEmpty()) {
            item.setCyclCnt(0);
            return;
        }
        ObservationCycleSummary last = cycles.get(cycles.size() - 1);
        item.setFirstCycle(cycles.get(0));
        item.setLastCycle(last);
        item.setCyclCnt(last.getCyclNo());
        item.setLastSzDiff(lastSizeDiff(cycles));
    }

    /**
     * 마지막 회차 크기 - 크기를 입력한 그 이전 회차 크기(어느 쪽이든 없으면 null)
     */
    private BigDecimal lastSizeDiff(List<ObservationCycleSummary> cycles) {
        BigDecimal lastSize = cycles.get(cycles.size() - 1).getObsrvSz();
        if (lastSize == null) {
            return null;
        }
        for (int i = cycles.size() - 2; i >= 0; i--) {
            BigDecimal size = cycles.get(i).getObsrvSz();
            if (size != null) {
                return lastSize.subtract(size);
            }
        }
        return null;
    }

    private LocalDateTime lastObservedAt(ObservationItemSummary item) {
        return item.getLastCycle() == null ? LocalDateTime.MIN : item.getLastCycle().getObsrvDt();
    }

    private ObservationCycle newCycle(Long obsrvArtclSn, MultipartFile file, LocalDateTime obsrvDt,
            BigDecimal obsrvSz, String obsrvChgCd, String memoCn, String actorId) {
        if (file.isEmpty()) {
            throw invalid();
        }
        ObservationCycle cycle = new ObservationCycle();
        cycle.setObsrvArtclSn(obsrvArtclSn);
        cycle.setAtchFileSn(fileStore.store(file, actorId));
        cycle.setObsrvDt(obsrvDt);
        cycle.setObsrvSz(obsrvSz);
        cycle.setObsrvChgCd(obsrvChgCd);
        cycle.setMemoCn(blankToNull(memoCn));
        cycle.setRgtrId(actorId);
        cycle.setMdfrId(actorId);
        return cycle;
    }

    /**
     * 회차 값 검사 : 관찰일은 오늘 이후 불가, 크기는 0보다 커야 함
     */
    private void checkCycleValue(LocalDateTime obsrvDt, BigDecimal obsrvSz) {
        if (obsrvDt != null && obsrvDt.toLocalDate().isAfter(LocalDate.now(ZONE))) {
            throw invalid();
        }
        if (obsrvSz != null && obsrvSz.signum() <= 0) {
            throw invalid();
        }
    }

    private void checkCode(HealthCodeGroup group, String comCd) {
        if (!observationMapper.existsChildCode(group.getCode(), comCd)) {
            throw invalid();
        }
    }

    private ObservationItem getItem(Long petSn, Long obsrvArtclSn) {
        ObservationItem item = observationMapper.selectItem(petSn, obsrvArtclSn);
        if (item == null) {
            throw new CustomException(ErrorCode.OBSERVATION_ITEM_NOT_FOUND);
        }
        return item;
    }

    private ObservationCycle getCycle(Long petSn, Long obsrvCyclSn) {
        ObservationCycle cycle = observationMapper.selectCycle(petSn, obsrvCyclSn);
        if (cycle == null) {
            throw new CustomException(ErrorCode.OBSERVATION_CYCLE_NOT_FOUND);
        }
        return cycle;
    }

    /**
     * 응답용 회차 1건 조회(회차 번호 포함)
     */
    private ObservationCycleSummary getSummary(Long petSn, Long obsrvArtclSn, Long obsrvCyclSn) {
        ObservationCycleSearchCond cond = new ObservationCycleSearchCond();
        cond.setPetSn(petSn);
        cond.setObsrvArtclSn(obsrvArtclSn);
        cond.setObsrvCyclSn(obsrvCyclSn);
        return observationMapper.selectCycles(cond).get(0);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
