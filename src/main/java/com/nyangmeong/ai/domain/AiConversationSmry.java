package com.nyangmeong.ai.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [AI 대화 요약 VO]
 * - AI_CNVRS_SMRY 1:1, 방마다 한 행, 요약은 암호문
 */
@Data
public class AiConversationSmry {
    /** AI대화요약일련번호 */
    private Long aiCnvrsSmrySn;
    /** AI대화방일련번호 */
    private Long aiCnvrsRoomSn;
    /** 요약내용암호문 */
    private String smryCnEnc;
    /** 요약끝메시지순서 */
    private Integer smryEndMsgSeq;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
}
