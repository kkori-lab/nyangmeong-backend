package com.nyangmeong.health.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * [회원 단위 일정 목록 검색 조건]
 * - petSns: 쉼표로 구분한 반려동물 번호, 비우면 볼 수 있는 반려동물 전체
 * - 목록 타입은 AI 도구 칸으로 쓸 수 없어 문자열로 받음
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MyCareScheduleSearchCond extends CareScheduleSearchCond {

    /** 반려동물 번호 목록(쉼표 구분) */
    @Pattern(regexp = "^$|^\\d+(,\\d+)*$")
    private String petSns;
}
