package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [관리자 펫돌봄 거래 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminCareMatchSearchCond {
    /** 모집상태공통코드 (PCAG01) */
    private String rcrtSttsCd;
    /** 회원일련번호 : 보호자(모집글 작성자)이거나 매칭 상대인 모집글만 (FN-ADM-042-08) */
    private Long mbrSn;
}
