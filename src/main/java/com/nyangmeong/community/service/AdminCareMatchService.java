package com.nyangmeong.community.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.CareRecruitmentStatus;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.AdminCareMatchResponse;
import com.nyangmeong.community.dto.AdminCareMatchSearchCond;
import com.nyangmeong.community.dto.AdminCareMatchSummary;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 펫돌봄 거래 서비스]
 * - SCR-ADM-022 모집글 목록·매칭 상세 (거래 이력만, 채팅 내용 없음 : 결정 14·26)
 * - 완료 사진 열람 : 비공개 저장이라 이 모집글 완료 요청 회차 사진만 관리자에게 준다
 */
@Service
@RequiredArgsConstructor
public class AdminCareMatchService {

    private final CareRecruitmentMapper careRecruitmentMapper;
    private final CareMatchMapper careMatchMapper;
    private final FileStore fileStore;

    /**
     * 목록 조회 (정상 게시글만, 최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCareMatchSummary> search(AdminCareMatchSearchCond cond, PageQuery pq) {
        String raw = cond.getRcrtSttsCd();
        String sttsCd = raw == null || raw.isBlank() ? null : raw;
        if (sttsCd != null && !CareRecruitmentStatus.exists(sttsCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminCareMatchSummary> rows = careRecruitmentMapper.selectAdminList(sttsCd, PostStatus.NORMAL.getCode());
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 매칭 상세 조회 (블라인드 글도 연다. 삭제 글은 없음)
     */
    @Transactional(readOnly = true)
    public AdminCareMatchResponse get(Long petPrtcRcrtSn) {
        AdminCareMatchResponse detail = careRecruitmentMapper.selectAdminDetail(petPrtcRcrtSn,
                PostStatus.DELETED.getCode());
        if (detail == null) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        List<AdminCareMatchResponse.Match> mtchs = careMatchMapper.selectAdminMatches(petPrtcRcrtSn);
        if (!mtchs.isEmpty()) {
            fill(petPrtcRcrtSn, mtchs);
        }
        detail.setMtchs(mtchs);
        return detail;
    }

    /**
     * 완료 사진 조회 (이 모집글 매칭의 완료 요청 회차 사진만, 그 밖의 파일은 있는지도 알리지 않고 404)
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long petPrtcRcrtSn, Long atchFileSn) {
        if (careRecruitmentMapper.selectAdminDetail(petPrtcRcrtSn, PostStatus.DELETED.getCode()) == null) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        boolean attached = careMatchMapper.selectAdminCompletionPhotos(petPrtcRcrtSn).stream()
                .anyMatch(photo -> photo.getAtchFileSn().equals(atchFileSn));
        if (!attached) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    /**
     * 매칭마다 조건 버전·상태 이력·완료 요청 회차(사진 포함)를 나눠 담는다
     */
    private void fill(Long petPrtcRcrtSn, List<AdminCareMatchResponse.Match> mtchs) {
        Map<Long, List<AdminCareMatchResponse.ConditionVersion>> versions = group(
                careMatchMapper.selectAdminConditionVersions(petPrtcRcrtSn),
                AdminCareMatchResponse.ConditionVersion::getPetPrtcMtchSn);
        Map<Long, List<AdminCareMatchResponse.StatusHistory>> histories = group(
                careMatchMapper.selectAdminStatusHistories(petPrtcRcrtSn),
                AdminCareMatchResponse.StatusHistory::getPetPrtcMtchSn);
        List<AdminCareMatchResponse.CompletionRequest> requests = careMatchMapper
                .selectAdminCompletionRequests(petPrtcRcrtSn);
        Map<Long, List<AdminCareMatchResponse.Photo>> photos = requests.isEmpty() ? Map.of()
                : group(careMatchMapper.selectAdminCompletionPhotos(petPrtcRcrtSn),
                        AdminCareMatchResponse.Photo::getPrtcCmptnDmndCyclSn);
        for (AdminCareMatchResponse.CompletionRequest request : requests) {
            request.setImgs(photos.getOrDefault(request.getPrtcCmptnDmndCyclSn(), List.of()));
        }
        Map<Long, List<AdminCareMatchResponse.CompletionRequest>> requestsByMatch = group(requests,
                AdminCareMatchResponse.CompletionRequest::getPetPrtcMtchSn);

        for (AdminCareMatchResponse.Match mtch : mtchs) {
            Long sn = mtch.getPetPrtcMtchSn();
            mtch.setCndVers(versions.getOrDefault(sn, List.of()));
            mtch.setSttsHstry(histories.getOrDefault(sn, List.of()));
            mtch.setCmptnDmnds(requestsByMatch.getOrDefault(sn, List.of()));
        }
    }

    /**
     * 키별로 묶는다 (쿼리 정렬 순서 유지)
     */
    private <T> Map<Long, List<T>> group(List<T> rows, Function<T, Long> key) {
        return rows.stream().collect(Collectors.groupingBy(key, Collectors.toList()));
    }
}
