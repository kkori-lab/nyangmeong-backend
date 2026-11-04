# AI 대화(ai) 내부 API·DTO

공통 규칙(응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 Python AI 서버가 호출하는 대화 저장·조회, AI 설정값 조회, AI 건강분석 리포트 입력·저장 내부 API만 담는다. 구현된 API의 정본은 Swagger(`/swagger-ui.html`)다.

## 호출 규칙

- 경로는 `/api` 접두어가 없는 `/internal/ai/conversations`·`/internal/ai/settings`다(`/internal/auth/check`와 같다). nginx 밖에서만 부른다.
- 회원은 쿠키로 판정한다(2026-10-27 ai 저장소 합의). Python은 React에서 받은 `Cookie` 헤더(`access_token`)를 그대로 실어 부르고, 요청에 `mbrSn`을 넣지 않는다. 회원 번호를 받으면 서버 간 키가 없는 상태에서 누구나 남의 대화를 읽고 지울 수 있다.
- 인증 실패는 기존 필터와 같다: 세션 폐기 401 `UNAUTHORIZED`, 정지·탈퇴 403 `MEMBER_INACTIVE`(쿠키 삭제 `Set-Cookie`). Python은 이 본문을 그대로 React에 넘긴다.
- 상태를 바꾸는 요청(POST·DELETE)은 `X-Requested-With: XMLHttpRequest` 헤더가 필요하다(`RequestedWithFilter`). Python은 React의 헤더를 같이 넘긴다.
- 제목·메시지 내용은 서버가 `app.crypto.aes-key-base64`로 암호화해 저장하고 응답에서 복호화한다. Python은 평문만 다룬다.
- 남의 대화나 없는 번호는 같은 404 `NOT_FOUND`다. 없는 반려동물 `petSn`과 양육하지 않는 반려동물 `petSn`도 존재 여부를 알려 주지 않으려고 같은 404 `NOT_FOUND`다(생성·목록 조회 공통).
- `roleCd`는 공통코드 AIMG06이다: `AIMC0024` 사용자, `AIMC0025` AI.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| POST | /internal/ai/conversations | 대화 생성(방과 첫 메시지를 한 트랜잭션으로 저장, 201) | AIM-011-07·11·14 | LJS | ConversationCreateRequest | ConversationSummary |
| GET | /internal/ai/conversations?petSn= | 대화 목록(최신순). petSn을 주면 그 태그만(태그 없는 「전체」 대화 제외) | AIM-011-08·09 | LJS | - | List<ConversationSummary> |
| GET | /internal/ai/conversations/{aiCnvrsRoomSn} | 대화 상세(메시지 `msgSeq` 오름차순) | AIM-011-10 | LJS | - | ConversationResponse |
| POST | /internal/ai/conversations/{aiCnvrsRoomSn}/messages | 메시지 추가(사용자·AI, 순서는 서버가 매김, 201) | AIM-011-10·14 | LJS | ConversationMessageCreateRequest | ConversationMessageResponse |
| PATCH | /internal/ai/conversations/{aiCnvrsRoomSn} | 대화 제목 수정(첫 답변 요약 제목, Python이 1회 호출, 200) | AIM-011-11 | LJS | ConversationTitleUpdateRequest | ConversationSummary |
| DELETE | /internal/ai/conversations/{aiCnvrsRoomSn} | 대화 삭제(물리 삭제, 메시지 CASCADE, 200) | AIM-011-13 | LJS | - | - |
| GET | /internal/ai/settings | AI 설정값(응답 토큰 상한·대화 턴 수). 설정 행이 없으면 기본 4500·6 | AIM-002-02·11 | LJS | - | AiSettingResponse |
| GET | /internal/ai/register-context | 등록 카드 컨텍스트. 쿠키 회원이 양육하는 반려동물 전부(공동양육 포함)와 각자의 케어 항목 이름. 반려동물이 없으면 빈 배열. 반려동물 목록은 외부 AI에 보내지 않는다 | AIM-012-04·08 | LJS | - | List<RegisterContextPet> |
| GET | /internal/ai/pets/{petSn}/health-report-input?baseYmd= | AI 건강분석 리포트(메인 위젯 MANC0007)의 분석 입력과 저장본. 쿠키 회원이 양육하는 반려동물만(남의 반려동물 403 `PET_ACCESS_DENIED`, 없는 반려동물 404 `PET_NOT_FOUND`). `baseYmd`(KST 오늘, 필수, 없거나 형식이 틀리면 400 `BAD_REQUEST`) 포함 14일 일상기록과 `baseYmd` 이전 가장 최근 완료(HLTC0006) 검진 1회의 참고 범위 밖 검사 수치. `rfrshPsblYn`은 저장본이 없거나 저장 기준일이 `baseYmd`와 다르거나 그날 새로고침 0회면 true, `autoRgnrPsblYn`은 저장본이 없거나 저장 기준일이 다르거나 그날 자동 재생성 5회 미만이면 true. 명세 밖 추가 요청(ai 저장소 `docs/api/healthreport.md`) | MAN-006 | LJS | - | HealthReportContextResponse |
| PUT | /internal/ai/pets/{petSn}/health-report | AI 건강분석 리포트 저장. 반려동물당 1행 덮어쓰기(`AI_HLTH_RPT`). 저장본이 없거나 기준일이 바뀌면 두 횟수를 0부터 센다. `rfrshYn: true`면 새로고침 횟수 +1(그날 이미 1회면 409 `HEALTH_REPORT_REFRESH_LIMIT`), 같은 기준일에 `rfrshYn: false`면 자동 재생성 횟수 +1(그날 이미 5회면 409 `HEALTH_REPORT_AUTO_LIMIT`). 그날 첫 생성은 세지 않는다. 저장본보다 이른 기준일·형식 오류는 400 `INVALID_INPUT_VALUE`. 응답 200 data 없음. `X-Requested-With` 필요 | MAN-006 | LJS | HealthReportSaveRequest | - |

탈퇴 회원 대화 파기(AIM-011-19)는 API가 아니라 `scheduler/AiConversationPurgeJob`이 매일 03:00 KST에 한다. Python의 `DELETE /internal/v1/members/{mbrSn}/conversations`는 쓰지 않는다.

미완료(`cmptnYn=false`) 메시지를 완료로 바꾸는 API는 두지 않는다. [다시 생성]은 새 메시지를 덧붙이고, Python이 미완료 메시지를 AI 컨텍스트에서 뺀다(2026-10-28 ai 세션 합의, FN-AIM-002-07).

## DTO

### ConversationCreateRequest
쓰는 API: POST /internal/ai/conversations
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | N | null이면 「전체」 대화. 값이 있으면 양육자만 | AI_CNVRS_ROOM.PET_SN, AIM-011-08 |
| cnvrsTtl | String | Y | 1~200자, 서버가 암호화 | AI_CNVRS_ROOM.CNVRS_TTL_ENC, AIM-011-11 |
| cnvrsCn | String | Y | 1~1000자, 첫 메시지(사용자 역할, `msgSeq` 1) | AI_CNVRS_MSG.CNVRS_CN_ENC, AIM-011-07 |

### ConversationMessageCreateRequest
쓰는 API: POST /internal/ai/conversations/{aiCnvrsRoomSn}/messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| roleCd | String | Y | `AIMC0024`·`AIMC0025` | AI_CNVRS_MSG.ROLE_CD |
| cnvrsCn | String | Y | 사용자(`AIMC0024`) 1~1,000자, AI(`AIMC0025`) 1~10,000자. 넘으면 400 `INVALID_INPUT_VALUE`. AI 답변은 의료 고지를 포함해 자르지 않고 저장한다 | AI_CNVRS_MSG.CNVRS_CN_ENC, AIM-001-04, AIM-002-03 |
| cmptnYn | Boolean | N | 기본 true. false면 미완료 답변 | AI_CNVRS_MSG.CMPTN_YN, AIM-002-07 |

### ConversationTitleUpdateRequest
쓰는 API: PATCH /internal/ai/conversations/{aiCnvrsRoomSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cnvrsTtl | String | Y | 1~200자, 공백만 불가, 서버가 암호화 | AI_CNVRS_ROOM.CNVRS_TTL_ENC, AIM-011-11 |

### ConversationSummary
쓰는 API: POST·PATCH·GET /internal/ai/conversations
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiCnvrsRoomSn | Long | Y | | AI_CNVRS_ROOM.AI_CNVRS_ROOM_SN |
| petSn | Long | N | 태그 없으면 null | AI_CNVRS_ROOM.PET_SN |
| cnvrsTtl | String | Y | 복호화한 제목 | AI_CNVRS_ROOM.CNVRS_TTL_ENC |
| regDt | LocalDateTime | Y | | AI_CNVRS_ROOM.REG_DT |

### ConversationResponse
쓰는 API: GET /internal/ai/conversations/{aiCnvrsRoomSn}. ConversationSummary 필드 + 아래.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| messages | List<ConversationMessageResponse> | Y | `msgSeq` 오름차순 | AI_CNVRS_MSG |

### ConversationMessageResponse
쓰는 API: GET 상세, POST .../messages
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiCnvrsMsgSn | Long | Y | | AI_CNVRS_MSG.AI_CNVRS_MSG_SN |
| roleCd | String | Y | `AIMC0024`·`AIMC0025` | AI_CNVRS_MSG.ROLE_CD |
| cnvrsCn | String | Y | 복호화한 내용 | AI_CNVRS_MSG.CNVRS_CN_ENC |
| msgSeq | Integer | Y | 대화 안의 순서(1부터) | AI_CNVRS_MSG.MSG_SEQ |
| cmptnYn | boolean | Y | 완료 여부 | AI_CNVRS_MSG.CMPTN_YN |
| regDt | LocalDateTime | Y | | AI_CNVRS_MSG.REG_DT |

### AiSettingResponse
쓰는 API: GET /internal/ai/settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiTknUplmtCnt | int | Y | 응답 최대 생성 토큰 수(기본 4500) | SYS_STNG.AI_TKN_UPLMT_CNT, AIM-002-02 |
| aiCnvrsTurnCnt | int | Y | 컨텍스트로 넘기는 최근 대화 턴 수(기본 6) | SYS_STNG.AI_CNVRS_TURN_CNT, AIM-002-11 |

### RegisterContextPet
쓰는 API: GET /internal/ai/register-context
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN, AIM-012-04 |
| petNm | String | Y | 문장 속 이름과 대조 | PET.PET_NM, AIM-012-04 |
| careArtclNms | List<String> | Y | 그 반려동물의 케어 항목 이름(삭제 제외, 등록순). 없으면 빈 배열. 케어 항목 목록 service(JAJ) 연결 전까지 빈 배열 | CARE_ARTCL.CARE_ARTCL_NM, AIM-012-08 |

### HealthReportContextResponse
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| input | HealthReportInputResponse | Y | 분석 입력 | MAN-006 |
| report | HealthReportResponse | N | 그 반려동물의 저장본(기준일과 관계없이 최근 1건). 없으면 null | AI_HLTH_RPT |
| rfrshPsblYn | Boolean | Y | `baseYmd`에 새로고침할 수 있는가 | AI_HLTH_RPT.RFRSH_NMTM |
| autoRgnrPsblYn | Boolean | Y | `baseYmd`에 자동 재생성할 수 있는가 | AI_HLTH_RPT.AUTO_RGNR_NMTM |

### HealthReportInputResponse
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petNm | String | Y | | PET.PET_NM |
| petSeCd | String | Y | PETG01 | PET.PET_SE_CD |
| vrtyNm | String | N | 품종 이름, 품종이 없으면 직접 입력명 | VRTY.VRTY_NM, PET.VRTY_DRCT_INPT_NM |
| ageMm | Integer | Y | `baseYmd` 기준 만 개월 | PET.PET_BRDT |
| nutrYn | Boolean | Y | 중성화 | PET.NUTR_YN |
| goalBdwg | BigDecimal | N | 목표체중 | PET.GOAL_BDWG |
| weights | List<HealthReportDailySummary> | Y | 체중(HLTC0014). 그날 마지막 기록. 날짜 오름차순, 기록 없는 날은 빠짐 | DAILY_HLTH_RCD |
| meals | List<HealthReportDailySummary> | Y | 식사량(HLTC0015). 그날 합계 | DAILY_HLTH_RCD |
| waters | List<HealthReportDailySummary> | Y | 음수량(HLTC0016). 그날 합계 | DAILY_HLTH_RCD |
| walks | List<HealthReportDailySummary> | Y | 산책(HLTC0018). 그날 합계 | DAILY_HLTH_RCD |
| stools | List<HealthReportStoolSummary> | Y | 배변(HLTC0017). 기록마다 한 줄, 기록 시각 오름차순 | DAILY_HLTH_RCD |
| latestCkupYmd | LocalDate | N | `baseYmd` 이전 가장 최근 완료 검진일. 없으면 null | SCHDL.SCHDL_DT |
| inspOutOfRange | List<HealthReportInspectionSummary> | Y | 그 검진의 참고 범위 밖 검사 수치. 없으면 빈 배열 | INSP_NVL |

### HealthReportDailySummary
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ymd | LocalDate | Y | 기록일 | DAILY_HLTH_RCD.RCD_DT |
| rcdNvl | BigDecimal | Y | 하루 값 | DAILY_HLTH_RCD.RCD_NVL |

### HealthReportStoolSummary
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ymd | LocalDate | Y | 기록일 | DAILY_HLTH_RCD.RCD_DT |
| crsdSttsCdNm | String | Y | 배변 상태 이름(HLTG10) | DAILY_HLTH_RCD.CRSD_STTS_CD |

### HealthReportInspectionSummary
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inspArtclNm | String | Y | 검사항목 이름(HLTG12), 직접 입력 항목이면 직접 입력명 | INSP_NVL.INSP_ARTCL_CD·INSP_ARTCL_DRCT_INPT_NM |
| rsltNvl | BigDecimal | Y | | INSP_NVL.RSLT_NVL |
| unitNm | String | Y | | INSP_NVL.UNIT_NM |
| refLlmtNvl | BigDecimal | N | | INSP_NVL.REF_LLMT_NVL |
| refUplmtNvl | BigDecimal | N | | INSP_NVL.REF_UPLMT_NVL |

### HealthReportResponse
쓰는 API: GET /internal/ai/pets/{petSn}/health-report-input
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rptTtl | String | Y | | AI_HLTH_RPT.RPT_TTL |
| tips | List<String> | Y | | AI_HLTH_RPT.ANLS_CN(JSON 배열) |
| inputHash | String | Y | 소문자 16진 64자 | AI_HLTH_RPT.INPT_HASH |
| crtDt | LocalDateTime | Y | 리포트를 저장한 시각 | AI_HLTH_RPT.CRT_DT |

### HealthReportSaveRequest
쓰는 API: PUT /internal/ai/pets/{petSn}/health-report
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| baseYmd | LocalDate | Y | 기준일(KST 오늘). 저장본보다 이르면 400 | AI_HLTH_RPT.BASE_YMD |
| rptTtl | String | Y | 1~20자 | AI_HLTH_RPT.RPT_TTL |
| tips | List<String> | Y | 1~3개, 각 1~40자 | AI_HLTH_RPT.ANLS_CN |
| inputHash | String | Y | 16진 64자(SHA-256), 소문자로 저장 | AI_HLTH_RPT.INPT_HASH |
| rfrshYn | Boolean | Y | 새로고침으로 만든 리포트인가 | AI_HLTH_RPT.RFRSH_NMTM |
