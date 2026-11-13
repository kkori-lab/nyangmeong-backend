package com.nyangmeong.pet.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.PetCodeGroup;
import com.nyangmeong.pet.domain.PetMainShrtc;
import com.nyangmeong.pet.dto.PetShortcutSaveRequest;
import com.nyangmeong.pet.dto.PetShortcutSummary;
import com.nyangmeong.pet.mapper.PetShortcutMapper;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 메인 바로가기 서비스]
 * - FN-MAN-008, 회원·반려동물마다 바로가기 종류 전부를 행으로 두고 보이는 것만 INDCT_YN='Y'
 * - 행이 없거나 행이 전부 'N'인 반려동물 -> 빈 목록
 */
@Service
@RequiredArgsConstructor
public class PetShortcutService {

    private static final int SORT_STEP = 10;

    private final PetShortcutMapper petShortcutMapper;
    private final CommonCodeService commonCodeService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 양육 중인 반려동물 전부의 바로가기 조회
     */
    @Transactional(readOnly = true)
    public List<PetShortcutSummary> search(Long mbrSn) {
        Set<String> activeCds = new HashSet<>(searchActiveCds());
        Map<Long, List<PetMainShrtc>> rowsByPet = new LinkedHashMap<>();
        for (PetMainShrtc row : petShortcutMapper.selectList(mbrSn)) {
            rowsByPet.computeIfAbsent(row.getPetSn(), k -> new ArrayList<>()).add(row);
        }
        List<PetShortcutSummary> result = new ArrayList<>();
        for (Long petSn : petShortcutMapper.selectCaregivingPetSns(mbrSn)) {
            List<PetMainShrtc> rows = rowsByPet.get(petSn);
            List<String> cds = new ArrayList<>();
            if (rows != null) {
                for (PetMainShrtc row : rows) {
                    if ("Y".equals(row.getIndctYn()) && activeCds.contains(row.getShrtcCd())) {
                        cds.add(row.getShrtcCd());
                    }
                }
            }
            result.add(new PetShortcutSummary(petSn, cds));
        }
        return result;
    }

    /**
     * 반려동물별 바로가기 저장 : 본문에 없는 반려동물은 그대로
     */
    @Transactional
    public List<PetShortcutSummary> save(Long mbrSn, List<PetShortcutSaveRequest> req) {
        if (req == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<String> activeCds = searchActiveCds();
        checkRequest(req, new HashSet<>(activeCds));
        req.forEach(line -> petAuthorizer.checkCaregiver(mbrSn, line.petSn()));

        for (PetShortcutSaveRequest line : req) {
            List<String> shown = line.shrtcCds();
            for (int i = 0; i < shown.size(); i++) {
                petShortcutMapper.upsert(mbrSn, line.petSn(), shown.get(i), "Y", (i + 1) * SORT_STEP);
            }
            for (String cd : activeCds) {
                if (!shown.contains(cd)) {
                    petShortcutMapper.upsert(mbrSn, line.petSn(), cd, "N", 0);
                }
            }
        }
        return search(mbrSn);
    }

    /**
     * 저장 요청 형식 검사 : 반려동물 번호 중복, 반려동물 안 코드 중복, 사용하지 않는 코드
     */
    private void checkRequest(List<PetShortcutSaveRequest> req, Set<String> activeCds) {
        Set<Long> petSns = new HashSet<>();
        for (int i = 0; i < req.size(); i++) {
            PetShortcutSaveRequest line = req.get(i);
            String at = "[" + i + "]";
            if (line == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, at);
            }
            if (line.petSn() == null || !petSns.add(line.petSn())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, at + ".petSn");
            }
            if (line.shrtcCds() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, at + ".shrtcCds");
            }
            Set<String> cds = new HashSet<>();
            for (int j = 0; j < line.shrtcCds().size(); j++) {
                String cd = line.shrtcCds().get(j);
                if (cd == null || !activeCds.contains(cd) || !cds.add(cd)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, at + ".shrtcCds[" + j + "]");
                }
            }
        }
    }

    private List<String> searchActiveCds() {
        return commonCodeService.search(PetCodeGroup.MAIN_SHORTCUT.getCode()).stream()
                .map(CommonCodeSummary::getComCd)
                .toList();
    }
}
