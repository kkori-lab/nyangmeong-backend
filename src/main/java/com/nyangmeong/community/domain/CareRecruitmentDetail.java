package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * [펫돌봄 모집 상세 조회 결과]
 * - PET_PRTC_RCRT 에 게시글·작성자를 조인한 서비스 내부용, 응답으로 그대로 반환하지 않는다
 * - aplcntCnt : 메시지가 1건 이상 오간 이 글의 채팅방 수
 */
@Getter
@Setter
public class CareRecruitmentDetail extends CareRecruitment {
    private Long mbrSn;
    private String mbrNcnm;
    private Long mbrAtchFileSn;
    private String pstSttsCd;
    private String pstTtl;
    private String pstCn;
    private Integer inqCnt;
    private Integer rcmdtnCnt;
    private LocalDateTime pstRegDt;
    private Integer aplcntCnt;
}
