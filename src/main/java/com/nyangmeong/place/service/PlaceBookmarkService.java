package com.nyangmeong.place.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.place.domain.PlaceBookmark;
import com.nyangmeong.place.domain.PlaceCategory;
import com.nyangmeong.place.dto.AdminFavoritePlaceSummary;
import com.nyangmeong.place.dto.PlaceBookmarkCreateRequest;
import com.nyangmeong.place.dto.PlaceBookmarkResponse;
import com.nyangmeong.place.dto.PlaceBookmarkSummary;
import com.nyangmeong.place.mapper.PlaceBookmarkMapper;
import com.nyangmeong.place.mapper.PlaceMapper;

import lombok.RequiredArgsConstructor;

/**
 * [장소 즐겨찾기 서비스]
 * - SCR-HOS-001 (FN-HOS-001-09·17·23·29·32)
 * - 해제는 PLC_BKMK.USE_YN = 'N', 같은 장소를 다시 저장하면 그 행을 되살림 : UK_PLC_BKMK_1 때문에 새 행을 만들 수 없음
 */
@Service
@RequiredArgsConstructor
public class PlaceBookmarkService {

    /** 관리자 대시보드 즐겨찾기 상위 장소 수 */
    private static final int FAVORITE_PLACE_LIMIT = 10;

    private static final String USE = "Y";
    private static final String UNUSE = "N";

    private final PlaceBookmarkMapper placeBookmarkMapper;
    private final PlaceMapper placeMapper;

    /**
     * 즐겨찾기 저장
     */
    @Transactional
    public PlaceBookmarkResponse create(Long mbrSn, PlaceBookmarkCreateRequest req) {
        if (!placeMapper.existsUsablePlace(req.plcSn())) {
            throw new CustomException(ErrorCode.PLACE_NOT_FOUND);
        }
        PlaceBookmark bookmark = placeBookmarkMapper.selectByMemberAndPlace(mbrSn, req.plcSn());
        if (bookmark == null) {
            bookmark = new PlaceBookmark();
            bookmark.setPlcSn(req.plcSn());
            bookmark.setMbrSn(mbrSn);
            try {
                placeBookmarkMapper.insert(bookmark);
            } catch (DuplicateKeyException e) {
                /** 동시 저장으로 다른 요청이 먼저 넣음 */
                throw new CustomException(ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS);
            }
        } else if (USE.equals(bookmark.getUseYn())
                || placeBookmarkMapper.updateUseYn(bookmark.getPlcBkmkSn(), mbrSn, UNUSE, USE) != 1) {
            throw new CustomException(ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS);
        }
        return new PlaceBookmarkResponse(bookmark.getPlcBkmkSn(), req.plcSn());
    }

    /**
     * 즐겨찾기 해제
     */
    @Transactional
    public void delete(Long mbrSn, Long plcBkmkSn) {
        if (placeBookmarkMapper.updateUseYn(plcBkmkSn, mbrSn, USE, UNUSE) != 1) {
            throw new CustomException(ErrorCode.PLACE_BOOKMARK_NOT_FOUND);
        }
    }

    /**
     * 내 즐겨찾기 목록, 카테고리별 저장 최신순
     * - 카테고리를 생략(빈 값 포함)하면 전체 분류 (AI 대화창, 2026-11-11)
     */
    @Transactional(readOnly = true)
    public List<PlaceBookmarkSummary> search(Long mbrSn, String plcClsfCd) {
        if (plcClsfCd == null || plcClsfCd.isBlank()) {
            return placeBookmarkMapper.selectList(mbrSn, null);
        }
        PlaceCategory category = PlaceCategory.from(plcClsfCd);
        return placeBookmarkMapper.selectList(mbrSn, category.getCode());
    }

    /**
     * 관리자 대시보드 즐겨찾기 상위 장소 조회 (기간 무관, 많은 순 상위 10)
     */
    @Transactional(readOnly = true)
    public List<AdminFavoritePlaceSummary> getTopBookmarkedPlaces() {
        return placeBookmarkMapper.selectTopBookmarkedPlaces(FAVORITE_PLACE_LIMIT);
    }
}
