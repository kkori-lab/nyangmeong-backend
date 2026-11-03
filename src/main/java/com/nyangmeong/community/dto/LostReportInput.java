package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * [실종 제보 입력 항목]
 * - 등록·수정 요청이 같은 검증·저장 로직을 쓰도록 맞춘 공통 항목
 */
public interface LostReportInput {

    String petSeCd();

    String petNm();

    Long vrtySn();

    String vrtyDrctInptNm();

    String colrNm();

    String chrcCn();

    LocalDateTime lastWtnsDt();

    String wtnsPlcNm();

    BigDecimal wtnsPlcLat();

    BigDecimal wtnsPlcLot();

    String cnpl();

    String pstTtl();

    String pstCn();

    List<MultipartFile> images();

    Boolean prvcRlsAgreYn();
}
