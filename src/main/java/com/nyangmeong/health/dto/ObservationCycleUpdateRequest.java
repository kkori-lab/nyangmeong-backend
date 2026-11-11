package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [관찰 회차 수정 요청]
 * - multipart/form-data : 보낸 항목만 수정, 사진을 보내면 교체 (FN-HLT-002-19)
 * - 크기·변화·메모를 비우려면 그 필드 이름을 clearFields로 보냄(빈 칸은 "안 보냄"과 구분되지 않음)
 */
public record ObservationCycleUpdateRequest(
        MultipartFile file,
        LocalDateTime obsrvDt,
        @Digits(integer = 5, fraction = 2) BigDecimal obsrvSz,
        @Size(max = 8) String obsrvChgCd,
        @Size(max = 200) String memoCn,
        List<@Pattern(regexp = CLEAR_SIZE + "|" + CLEAR_CHANGE + "|" + CLEAR_MEMO) String> clearFields) {

    public static final String CLEAR_SIZE = "obsrvSz";
    public static final String CLEAR_CHANGE = "obsrvChgCd";
    public static final String CLEAR_MEMO = "memoCn";

    /**
     * 비울 필드인지
     */
    public boolean clears(String field) {
        return clearFields != null && clearFields.contains(field);
    }

    /**
     * 같은 필드에 값과 비우기를 함께 보내지 않았는지
     */
    @AssertTrue
    public boolean isClearFieldsDistinct() {
        return !(clears(CLEAR_SIZE) && obsrvSz != null)
                && !(clears(CLEAR_CHANGE) && obsrvChgCd != null)
                && !(clears(CLEAR_MEMO) && memoCn != null && !memoCn.isBlank());
    }
}
