# 공통(global) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /common-codes | 공통코드 하위 목록 조회 `?prntComCd=PETG03` *통합 때 추가* | MYP-003-03 | LJS | - | List<CommonCodeSummary> |
| GET | /regions | 하위 지역 목록(`?upRgnSn=`, 없으면 시도). 시도 → 시군구 → 읍면동 연동 콤보박스 | CMU-002-03 (issues 14) | KSJ | - | List<RegionSummary> |
| GET | /files/public/{atchFileSn} | 공개 이미지 파일(게시글 사진·생성된 캐릭터 이미지). 로그인 없이 열린다. 공개가 아니거나 없는 번호는 404 `FILE_NOT_FOUND`. 응답의 `atchFileSn`을 그대로 `<img src="/api/files/public/{atchFileSn}">`에 쓴다 | 연결 작업(파일 저장 외부 디렉토리) | LJS | - | 이미지 파일 |
| GET | /admin/settings | 운영 설정 조회. 회원·반려동물, 커뮤니티, 알림, AI, 파일 다섯 묶음과 수정일시·수정자. 관리자만(일반 회원 403, 비로그인 401) | ADM-110-01 | LJS | - | AdminSettingResponse |
| PATCH | /admin/settings | 운영 설정 수정. 보낸 필드만 바꾸고 바뀐 항목의 전·후 값을 감사 로그(`MNGR_TASK_LOG`)에 남긴다. 바뀐 게 없으면 저장·로그 없이 현재 값을 돌려준다. 범위 밖은 400 `INVALID_INPUT_VALUE` | ADM-110-02·03·04 | LJS | AdminSettingUpdateRequest | AdminSettingResponse |

## DTO

### CommonCodeSummary
쓰는 API: GET /common-codes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| comCdSn | Long | Y | | COM_CD.COM_CD_SN |
| comCd | String | Y | 코드값(8자) | COM_CD.COM_CD |
| comCdNm | String | Y | 화면에 보이는 이름 | COM_CD.COM_CD_NM |
| sortSeq | Integer | Y | 이 순서로 정렬해 돌려준다 | COM_CD.SORT_SEQ |

### RegionSummary
쓰는 API: GET /regions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rgnSn | Long | Y | | RGN.RGN_SN |
| admdstCd | String | Y | 행정구역코드 10자리 | RGN.ADMDST_CD |
| ctpvNm | String | Y | 시도명 | RGN.CTPV_NM |
| sggNm | String | Y | 시군구명, 시도 행은 빈 문자열 | RGN.SGG_NM |
| emdNm | String | N | 읍면동명 | RGN.EMD_NM |
| rgnStpCd | String | Y | 지역단계. 공통코드 RGNG01(시도 RGNC0001·시군구 RGNC0002·읍면동 RGNC0003) | RGN.RGN_STP_CD |

### AdminSettingResponse
쓰는 API: GET /admin/settings, PATCH /admin/settings. 묶음별 객체로 나눈다
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| memberPet.petMaxCnt | Long | Y | 반려동물 최대 수 | SYS_STNG.PET_MAX_CNT |
| memberPet.jntBrupMaxNope | Long | Y | 공동양육 최대 인원수 | SYS_STNG.JNT_BRUP_MAX_NOPE |
| memberPet.invtnVldDayCnt | Long | Y | 초대 유효 일수 | SYS_STNG.INVTN_VLD_DAY_CNT |
| memberPet.lgnFailLmtNmtm | Long | Y | 로그인 실패 제한 횟수 | SYS_STNG.LGN_FAIL_LMT_NMTM |
| memberPet.whdwlMbrKpngDayCnt | Long | Y | 탈퇴 회원 보관 일수 | SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT |
| memberPet.dshbdCareMaxExpsrCnt | Long | Y | 대시보드 케어 최대 노출 수 | SYS_STNG.DSHBD_CARE_MAX_EXPSR_CNT |
| community.pstImgMaxCnt | Long | Y | 게시글 이미지 최대 수 | SYS_STNG.PST_IMG_MAX_CNT |
| community.blindDclrAcmlCnt | Long | Y | 블라인드 신고 누적 수 | SYS_STNG.BLIND_DCLR_ACML_CNT |
| community.lostTpofExpryCrtrDayCnt | Long | Y | 실종 제보 만료 기준 일수 | SYS_STNG.LOST_TPOF_EXPRY_CRTR_DAY_CNT |
| community.mtchNoRspnsEndCrtrDayCnt | Long | Y | 매칭 무응답 종료 기준 일수 | SYS_STNG.MTCH_NO_RSPNS_END_CRTR_DAY_CNT |
| community.lowstHrwgAplcnYr | String | Y | 최저시급 적용 연도 4자리 | SYS_STNG.LOWST_HRWG_APLCN_YR |
| community.lowstHrwgAmt | Long | Y | 최저시급 금액 | SYS_STNG.LOWST_HRWG_AMT |
| community.dscvAutoCmntCn | String | Y | 찾았어요 자동 댓글 내용 | SYS_STNG.DSCV_AUTO_CMNT_CN |
| community.aiCmtryBscCn | String | Y | AI 코멘트 기본 내용 | SYS_STNG.AI_CMTRY_BSC_CN |
| notification.notiHideCrtrDayCnt | Long | Y | 알림 숨김 기준 일수 | SYS_STNG.NOTI_HIDE_CRTR_DAY_CNT |
| ai.aiCnvrsTurnCnt | Long | Y | AI 대화 턴 수 | SYS_STNG.AI_CNVRS_TURN_CNT |
| ai.aiTknUplmtCnt | Long | Y | AI 토큰 상한 수 | SYS_STNG.AI_TKN_UPLMT_CNT |
| file.fileMaxSz | Long | Y | 파일 최대 크기(바이트) | SYS_STNG.FILE_MAX_SZ |
| mdfcnDt | LocalDateTime | N | 수정일시. 설정 행이 아직 없으면 없다 | SYS_STNG.MDFCN_DT |
| mdfrId | String | N | 수정자 ID(관리자 회원 번호). 설정 행이 아직 없으면 없다 | SYS_STNG.MDFR_ID |

### AdminSettingUpdateRequest
쓰는 API: PATCH /admin/settings. 평평한 필드, 모두 선택(보낸 것만 바꾼다). 범위는 제안값이며 DDL 자릿수 안이다
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petMaxCnt | Long | N | 1~100 | SYS_STNG.PET_MAX_CNT |
| jntBrupMaxNope | Long | N | 1~100 | SYS_STNG.JNT_BRUP_MAX_NOPE |
| invtnVldDayCnt | Long | N | 1~3650 | SYS_STNG.INVTN_VLD_DAY_CNT |
| lgnFailLmtNmtm | Long | N | 1~100 | SYS_STNG.LGN_FAIL_LMT_NMTM |
| whdwlMbrKpngDayCnt | Long | N | 1~3650 | SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT |
| dshbdCareMaxExpsrCnt | Long | N | 1~20 | SYS_STNG.DSHBD_CARE_MAX_EXPSR_CNT |
| pstImgMaxCnt | Long | N | 1~100 | SYS_STNG.PST_IMG_MAX_CNT |
| blindDclrAcmlCnt | Long | N | 1~100 | SYS_STNG.BLIND_DCLR_ACML_CNT |
| lostTpofExpryCrtrDayCnt | Long | N | 1~99 | SYS_STNG.LOST_TPOF_EXPRY_CRTR_DAY_CNT |
| mtchNoRspnsEndCrtrDayCnt | Long | N | 1~99 | SYS_STNG.MTCH_NO_RSPNS_END_CRTR_DAY_CNT |
| lowstHrwgAplcnYr | String | N | 2020~2100 (4자리 숫자) | SYS_STNG.LOWST_HRWG_APLCN_YR |
| lowstHrwgAmt | Long | N | 0~1,000,000 | SYS_STNG.LOWST_HRWG_AMT |
| dscvAutoCmntCn | String | N | 1~1000자, 공백만 불가 | SYS_STNG.DSCV_AUTO_CMNT_CN |
| aiCmtryBscCn | String | N | 1~1000자, 공백만 불가 | SYS_STNG.AI_CMTRY_BSC_CN |
| notiHideCrtrDayCnt | Long | N | 1~99 | SYS_STNG.NOTI_HIDE_CRTR_DAY_CNT |
| aiCnvrsTurnCnt | Long | N | 1~50 | SYS_STNG.AI_CNVRS_TURN_CNT |
| aiTknUplmtCnt | Long | N | 500~100000 | SYS_STNG.AI_TKN_UPLMT_CNT |
| fileMaxSz | Long | N | 1048576~52428800 (1~50MB) | SYS_STNG.FILE_MAX_SZ |
