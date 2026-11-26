package com.nyangmeong.seed;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nyangmeong.seed.core.Schema;

/**
 * [시드가 다루는 테이블]
 * - KEPT : 지우지도 넣지도 않는다. 기준값·뉴스·관리자 기록(사람 2026-11-26)
 * - PARTLY_KEPT : 남길 행만 남기고 지운다(SeedCleaner). 회원 계정(SNS 연동·약관 동의·상태 이력 포함)·사람이 쓴 공지·FAQ·남긴 행이 가리키는 첨부파일,
 *   장소(옛 시드가 만든 가짜 장소만 지운다, 사람 2026-11-26)
 * - CLEARED : 전부 지우고 Seeder가 다시 채운다. 실행 기록(토큰 등)·건강기록 대시보드 위젯 설정도 지운다
 * - NOT_SEEDED : CLEARED 중 Seeder가 넣지 않는 테이블. 첨부파일이 꼭 있어야 하는 행(가짜 이미지를 넣지 않는다), 아이템·캐릭터(관리자·회원이 서버에서 만든다), AI 대화
 * - 이름에 _BAK_가 든 백업 테이블과 어느 목록에도 없는 새 테이블은 건드리지 않는다(새 테이블은 경고)
 */
public final class SeedTables {

    public static final List<String> KEPT = List.of("COM_CD", "CARE_MSN", "AI_ROUT_MPNG", "KWRD_DICT", "NOTI_TMPLT",
            "TRMS", "VRTY", "RGN", "SYS_STNG", "TODAY_PHRS", "CMNTY_AI_CMTRY", "NEWS", "MNGR_TASK_LOG",
            "BTCH_EXCN_HSTRY", "EXTL_API_CALL_LOG");

    /** 남긴 회원의 계정에 딸린 행(SNS 연동·약관 동의·상태 이력). 지운 회원의 행만 지운다 */
    public static final List<String> MEMBER_ACCOUNT = List.of("SNS_ACNT_LINK", "MBR_TRMS_AGRE", "MBR_STTS_HSTRY");

    public static final List<String> PARTLY_KEPT = List.of("MBR", "SNS_ACNT_LINK", "MBR_TRMS_AGRE", "MBR_STTS_HSTRY",
            "NTC_MTTR", "FAQ", "ATCH_FILE", "PLC");

    /** 사람이 쓴 공지 중 검증용이 아닌 것(사람 2026-11-26) */
    public static final List<Long> KEPT_NOTICES = List.of(42L);

    /** 사람이 쓰거나 다시 쓴 FAQ 중 검증용·중복이 아닌 것(사람 2026-11-26) */
    public static final List<Long> KEPT_FAQS = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L, 10L, 11L, 12L, 13L, 14L, 17L, 18L,
            19L, 20L, 21L, 22L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 33L, 34L, 35L, 36L, 37L, 46L, 47L, 48L, 49L, 50L, 51L,
            52L, 53L, 54L, 55L, 56L, 57L, 58L, 59L, 60L, 61L, 62L, 63L, 64L, 65L);

    public static final List<String> CLEARED = List.of("EML_CERT", "RFRSH_TKN", "LGN_ATMPT_LMT", "NOTI_SNDNG_LOG",
            "MBR_DSHBD_WDGT", "PET",
            "BRUP_REL", "BRUP_LINK_STTS_HSTRY", "PET_MAIN_SHRTC", "JNT_BRUP_INVTN", "INVTN_TRGT_PET",
            "JNT_BRUP_INVTN_STTS_HSTRY", "RECPT", "RECPT_STTS_HSTRY", "DAILY_HLTH_RCD", "OBSRV_ARTCL",
            "OBSRV_ARTCL_STTS_HSTRY", "OBSRV_CYCL", "PIXEL_CHAR", "CHAR_PHOTO", "CHAR_CRT_JOB", "CHAR_CRT_JOB_STTS_HSTRY",
            "ITEM", "HLD_ITEM", "CHAR_ITEM_SHEET", "DAY_MSN", "PNT_GNLGR", "EXPND", "INAPP_NOTI", "PLC_BKMK", "PST",
            "PST_STTS_HSTRY", "PST_IMG", "PST_RCMDTN", "PST_INQ_HSTRY", "CMNT", "DCLR", "DCLR_STTS_HSTRY", "LOST_TPOF",
            "LOST_TPOF_STTS_HSTRY", "OTO_CHAT_ROOM", "OTO_CHAT_ROOM_STTS_HSTRY", "CHAT_PRTPNT", "CHAT_PRTPNT_STTS_HSTRY",
            "CHAT_MSG", "CHAT_MSG_PHOTO", "PET_PRTC_RCRT", "PET_PRTC_RCRT_STTS_HSTRY", "RCRT_PET_KND", "RCRT_PRTC_SHP",
            "PET_PRTC_MTCH", "PET_PRTC_MTCH_STTS_HSTRY", "MTCH_CNCL_DMND", "MTCH_CND_VER", "PRTC_CMPTN_DMND_CYCL",
            "PRTC_CMPTN_PHOTO", "OTO_INQRY", "OTO_INQRY_STTS_HSTRY", "INQRY_ATCH_PHOTO", "AI_CNVRS_ROOM", "AI_CNVRS_MSG",
            "AI_CNVRS_SMRY", "AI_HLTH_RPT", "SCHDL", "CKUP_RCD", "CKUP_RCD_STTS_HSTRY", "CKUP_RCD_ATCH", "INSP_NVL",
            "ETC_INSP", "PRSC", "PRSC_STTS_HSTRY", "REPT_PRVNT_MNG", "CARE_ARTCL", "CARE_CMPTN_HSTRY");

    public static final List<String> NOT_SEEDED = List.of("ATCH_FILE", "RECPT", "RECPT_STTS_HSTRY", "OBSRV_CYCL",
            "PST_IMG", "CHAT_MSG_PHOTO", "PRTC_CMPTN_PHOTO", "INQRY_ATCH_PHOTO", "CKUP_RCD_ATCH", "ITEM", "HLD_ITEM",
            "CHAR_ITEM_SHEET", "PIXEL_CHAR", "CHAR_PHOTO", "CHAR_CRT_JOB", "CHAR_CRT_JOB_STTS_HSTRY", "AI_CNVRS_ROOM",
            "AI_CNVRS_MSG", "AI_CNVRS_SMRY", "AI_HLTH_RPT", "EML_CERT", "RFRSH_TKN", "LGN_ATMPT_LMT", "NOTI_SNDNG_LOG");

    private SeedTables() {
    }

    public static boolean isBackup(String table) {
        return table.contains("_BAK_");
    }

    /**
     * 어느 목록에도 없는 테이블(새로 생긴 테이블). 건드리지 않고 경고만 한다
     */
    public static List<String> unknown(Schema schema) {
        Set<String> known = new LinkedHashSet<>(KEPT);
        known.addAll(PARTLY_KEPT);
        known.addAll(CLEARED);
        List<String> unknown = new ArrayList<>();
        for (String table : schema.tableNames()) {
            if (!known.contains(table) && !isBackup(table)) {
                unknown.add(table);
            }
        }
        return unknown;
    }

    /**
     * 지우는 테이블 중 DB에 있는 것
     */
    public static List<String> cleared(Schema schema) {
        return CLEARED.stream().filter(schema::hasTable).toList();
    }
}
