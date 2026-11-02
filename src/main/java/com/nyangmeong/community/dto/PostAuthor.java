package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [작성자 표시 정보]
 * - 게시글·댓글 응답에 붙이는 닉네임과 프로필 사진 번호, 사진이 없으면 mbrAtchFileSn 은 null
 */
@Data
public class PostAuthor {
    private String mbrNcnm;
    private Long mbrAtchFileSn;
}
