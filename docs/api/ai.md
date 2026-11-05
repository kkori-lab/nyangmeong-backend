# AI 대화(ai) 내부 API·DTO

공통 규칙(응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 Python AI 서버가 호출하는 대화 저장·조회, AI 설정값 조회, AI 건강분석 리포트 입력·저장, 라우팅 매핑표 조회 내부 API와 관리자 라우팅 매핑표 API(SCR-ADM-080)를 담는다. 구현된 API의 정본은 Swagger(`/swagger-ui.html`)다.

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
| GET | /internal/ai/routes | 라우팅 매핑표(FN-AIM-004-09에서 쓰는 지시어 → 화면). 사용 중(`USE_YN='Y'`) 행만 `prrtSeq`·`aiRoutMpngSn` 순. 이동 방식이 라우팅 대상 아님·제외·보류(AIMC0034~0036)인 행도 준다(Python이 이동하지 않는 행으로 거른다). 관리자가 바꾸면 다음 호출부터 반영(캐시 없음). Python은 DB에 붙지 않아 `data/routing.yaml` 대신 이 API를 쓴다(연결은 ai 저장소) | ADM-080-04 | LJS | - | List<AiRouteResponse> |
| GET | /internal/ai/keywords?dictUsgCd= | 키워드 사전(일정 등록 판정·가계부 분류). `dictUsgCd`는 AIMC0037·AIMC0038만(뉴스 용도 AIMC0039·0040·모르는 값은 400 `INVALID_INPUT_VALUE`, 없으면 400 `BAD_REQUEST`). 사용 중(`USE_YN='Y'`) 행만 `sortSeq`·`kwrdDictSn` 순, '판정 규칙'·'시간' 행도 준다. `kwrdCn`은 관리자가 쓴 예시 문구 그대로(정규식 아님, 바꾸는 규칙은 ai 저장소가 정한다). 관리자가 바꾸면 다음 호출부터 반영(캐시 없음) | ADM-081-01 | LJS | - | List<AiKeywordResponse> |

탈퇴 회원 대화 파기(AIM-011-19)는 API가 아니라 `scheduler/AiConversationPurgeJob`이 매일 03:00 KST에 한다. Python의 `DELETE /internal/v1/members/{mbrSn}/conversations`는 쓰지 않는다.

미완료(`cmptnYn=false`) 메시지를 완료로 바꾸는 API는 두지 않는다. [다시 생성]은 새 메시지를 덧붙이고, Python이 미완료 메시지를 AI 컨텍스트에서 뺀다(2026-10-28 ai 세션 합의, FN-AIM-002-07).

## 관리자 API

관리자 쿠키로 부른다(`/api` 접두어, 아래 경로는 `/api` 뒤). 일반 회원은 403 `ADMIN_ONLY`. 등록·수정·삭제는 `X-Requested-With` 필요, 감사 로그(`MNGR_TASK_LOG`, 대상 `AI_ROUT_MPNG`)를 남긴다. 우선순위 `prrtSeq`는 삭제 안 된 행 전체에서 1부터 빈틈없이 매기고, 등록·수정·삭제 때마다 다시 매긴다(기준 데이터의 No×10은 첫 저장 때 1~N으로 바뀐다). 같은 길이로 일치하면 위쪽 행이 먼저다(FN-ADM-080-03). 화면 ID 선택지는 서버가 주지 않는다(프론트 `docs/routes.md` 기준 목록). 초기 데이터는 기준 데이터 `[3] AI 라우팅 매핑`(부록 A, FN-ADM-080-05)

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /admin/ai/routes?q= | 라우팅 매핑표. 사용 안 함 포함, `prrtSeq`·`aiRoutMpngSn` 순, 페이지 없음. `q`는 공백을 뺀 값이 공백을 뺀 지시어(`instKwrdCn`)에 들어 있는 행, 없거나 공백뿐이면 전체 | ADM-080-01 | LJS | - | List<AdminAiRouteSummary> |
| POST | /admin/ai/routes | 매핑 등록(201). `prrtSeq` 자리에 넣고 나머지를 한 칸씩 민다(없으면 맨 아래, 행 수보다 크면 맨 아래). `domnNm`은 서버가 `scrnId`의 `SCR-` 뒤 세 글자로 정한다(없거나 모양이 다르면 `ETC`). 형식이 틀리면 400 `INVALID_INPUT_VALUE` | ADM-080-02·03 | LJS | AdminAiRouteSaveRequest | AdminAiRouteCreateResponse |
| PATCH | /admin/ai/routes/{aiRoutMpngSn} | 매핑 수정(보낸 칸만). `prrtSeq`를 보내면 그 자리로 옮기고 나머지를 민다. 아무 칸도 없거나 형식이 틀리면 400 `INVALID_INPUT_VALUE`, 없는·삭제된 행 404 `AI_ROUTE_NOT_FOUND` | ADM-080-02·03 | LJS | AdminAiRouteSaveRequest | AdminAiRouteSummary |
| DELETE | /admin/ai/routes/{aiRoutMpngSn} | 매핑 삭제(`DEL_YN='Y'`), 뒤 순위를 한 칸씩 당긴다. 응답 200 data 없음. 없는·이미 삭제된 행 404 `AI_ROUTE_NOT_FOUND` | ADM-080-02 | LJS | - | - |

키워드 사전(SCR-ADM-081)은 같은 관리자 규칙에 감사 로그 대상 `KWRD_DICT`. 다루는 행은 용도·분류 여섯 묶음뿐이다: AIMC0037(일정 등록 판정)의 `등록 동사`·`날짜`·`주기`·`일정 종류`(연결 코드 HLTG19)·`검진유형`(HLTG01), AIMC0038(가계부 분류)의 `가계부 카테고리`(MYPG05). AIMC0037의 `판정 규칙`·`시간` 행은 서버 규칙이라 목록에 없고 수정·삭제는 404다(2026-10-30 결정). 뉴스 용도(AIMC0039·0040)는 커뮤니티 `/admin/news/keywords`(SCR-ADM-050). 초기 데이터는 기준 데이터 `[4] 키워드 사전`(부록 B, 가계부 분류는 v0.13 (2), FN-ADM-081-03)

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /admin/ai/keywords?dictUsgCd=&kwrdClsfNm= | 키워드 목록. `dictUsgCd` 필수(AIMC0037·AIMC0038, 그 밖은 400 `INVALID_INPUT_VALUE`, 없으면 400 `BAD_REQUEST`), `kwrdClsfNm`을 주면 그 분류만. 사용 안 함 포함, `sortSeq`·`kwrdDictSn` 순, 페이지 없음 | ADM-081-01 | LJS | - | List<AdminAiKeywordSummary> |
| POST | /admin/ai/keywords | 키워드 등록(201). 용도·분류는 위 여섯 묶음 중 하나, `sortSeq`는 같은 용도·분류의 최댓값+1(비어 있으면 10). 형식·연결 코드가 틀리면 400 `INVALID_INPUT_VALUE` | ADM-081-02 | LJS | AdminAiKeywordSaveRequest | AdminAiKeywordCreateResponse |
| PATCH | /admin/ai/keywords/{kwrdDictSn} | 키워드 수정(보낸 칸만, 용도·분류·`sortSeq`는 그대로이고 보내도 무시). 아무 칸도 없거나 형식이 틀리면 400 `INVALID_INPUT_VALUE`, 없는·삭제된·이 화면이 다루지 않는 행 404 `AI_KEYWORD_NOT_FOUND` | ADM-081-02 | LJS | AdminAiKeywordSaveRequest | AdminAiKeywordSummary |
| DELETE | /admin/ai/keywords/{kwrdDictSn} | 키워드 삭제(`DEL_YN='Y'`). 응답 200 data 없음. 없는·이미 삭제된·이 화면이 다루지 않는 행 404 `AI_KEYWORD_NOT_FOUND` | ADM-081-02 | LJS | - | - |

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

### AiRouteResponse
쓰는 API: GET /internal/ai/routes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiRoutMpngSn | Long | Y | | AI_ROUT_MPNG.AI_ROUT_MPNG_SN |
| trgtScrnNm | String | Y | | AI_ROUT_MPNG.TRGT_SCRN_NM |
| scrnId | String | N | | AI_ROUT_MPNG.SCRN_ID |
| scrnParamCn | String | N | 예 `cat=hospital` | AI_ROUT_MPNG.SCRN_PARAM_CN |
| instKwrds | List<String> | Y | 지시어를 쉼표로 나눠 앞뒤 공백을 뺀 배열, 빈 값 제외. 지시어가 없으면 빈 배열 | AI_ROUT_MPNG.INST_KWRD_CN |
| mvmnMthdCd | String | Y | AIMG09: AIMC0032 화면 이동, AIMC0033 모달 열기, AIMC0034 라우팅 대상 아님, AIMC0035 제외, AIMC0036 보류 | AI_ROUT_MPNG.MVMN_MTHD_CD |
| rprsChipYn | boolean | Y | 대표 칩 | AI_ROUT_MPNG.RPRS_CHIP_YN |
| prrtSeq | Integer | Y | 작을수록 먼저 | AI_ROUT_MPNG.PRRT_SEQ |

### AdminAiRouteSummary
쓰는 API: GET /admin/ai/routes, PATCH /admin/ai/routes/{aiRoutMpngSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiRoutMpngSn | Long | Y | | AI_ROUT_MPNG.AI_ROUT_MPNG_SN, ADM-080-01 |
| domnNm | String | Y | 서버가 화면 ID로 정함 | AI_ROUT_MPNG.DOMN_NM |
| trgtScrnNm | String | Y | | AI_ROUT_MPNG.TRGT_SCRN_NM |
| scrnId | String | N | | AI_ROUT_MPNG.SCRN_ID |
| scrnParamCn | String | N | | AI_ROUT_MPNG.SCRN_PARAM_CN |
| instKwrdCn | String | N | 쉼표 구분 한 줄 | AI_ROUT_MPNG.INST_KWRD_CN |
| mvmnMthdCd | String | Y | AIMG09 | AI_ROUT_MPNG.MVMN_MTHD_CD |
| rprsChipYn | boolean | Y | | AI_ROUT_MPNG.RPRS_CHIP_YN |
| prrtSeq | Integer | Y | 1부터 | AI_ROUT_MPNG.PRRT_SEQ, ADM-080-03 |
| rmrkCn | String | N | | AI_ROUT_MPNG.RMRK_CN |
| useYn | boolean | Y | | AI_ROUT_MPNG.USE_YN |
| mdfcnDt | LocalDateTime | Y | | AI_ROUT_MPNG.MDFCN_DT |

### AdminAiRouteSaveRequest
쓰는 API: POST /admin/ai/routes, PATCH /admin/ai/routes/{aiRoutMpngSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trgtScrnNm | String | 등록 Y | 100자, 공백만이면 400 | AI_ROUT_MPNG.TRGT_SCRN_NM |
| scrnId | String | N | 100자. 이동 방식이 AIMC0032·AIMC0033이면 필수. 모양은 검사하지 않는다(제외 행에 `SCR-ACC-001 / 002` 같은 값이 있음). 수정에서 빈 문자열은 비움 | AI_ROUT_MPNG.SCRN_ID |
| scrnParamCn | String | N | 200자. 수정에서 빈 문자열은 비움 | AI_ROUT_MPNG.SCRN_PARAM_CN |
| instKwrdCn | String | N | 1000자, 쉼표 구분 한 줄. 이동 방식이 AIMC0032·AIMC0033이면 필수. 수정에서 빈 문자열은 비움 | AI_ROUT_MPNG.INST_KWRD_CN |
| mvmnMthdCd | String | 등록 Y | AIMG09 다섯 값, 아니면 400 | AI_ROUT_MPNG.MVMN_MTHD_CD |
| rprsChipYn | Boolean | N | 등록 기본 false | AI_ROUT_MPNG.RPRS_CHIP_YN |
| prrtSeq | Integer | N | 1 이상. 등록 기본 맨 아래, 수정에서 없으면 순위 그대로 | AI_ROUT_MPNG.PRRT_SEQ, ADM-080-03 |
| rmrkCn | String | N | 1000자. 수정에서 빈 문자열은 비움 | AI_ROUT_MPNG.RMRK_CN |
| useYn | Boolean | N | 등록 기본 true | AI_ROUT_MPNG.USE_YN |

### AdminAiRouteCreateResponse
쓰는 API: POST /admin/ai/routes
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| aiRoutMpngSn | Long | Y | 등록한 매핑 번호 | AI_ROUT_MPNG.AI_ROUT_MPNG_SN |

### AiKeywordResponse
쓰는 API: GET /internal/ai/keywords
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| kwrdDictSn | Long | Y | | KWRD_DICT.KWRD_DICT_SN |
| kwrdClsfNm | String | Y | 예 `등록 동사`·`가계부 카테고리` | KWRD_DICT.KWRD_CLSF_NM |
| artclNm | String | Y | | KWRD_DICT.ARTCL_NM |
| kwrdCn | String | Y | 쉼표 구분 예시 문구 한 줄(`○일 후` 같은 자리표시 포함) | KWRD_DICT.KWRD_CN |
| intrpRsltCn | String | N | | KWRD_DICT.INTRP_RSLT_CN |
| mpngCd | String | N | 결과 코드(HLTG19·HLTG01·MYPG05) | KWRD_DICT.MPNG_CD |

### AdminAiKeywordSummary
쓰는 API: GET /admin/ai/keywords, PATCH /admin/ai/keywords/{kwrdDictSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| kwrdDictSn | Long | Y | | KWRD_DICT.KWRD_DICT_SN, ADM-081-01 |
| dictUsgCd | String | Y | AIMG10: AIMC0037·AIMC0038 | KWRD_DICT.DICT_USG_CD |
| kwrdClsfNm | String | Y | | KWRD_DICT.KWRD_CLSF_NM |
| artclNm | String | Y | | KWRD_DICT.ARTCL_NM |
| kwrdCn | String | Y | 쉼표 구분 한 줄 | KWRD_DICT.KWRD_CN |
| intrpRsltCn | String | N | | KWRD_DICT.INTRP_RSLT_CN |
| mpngCd | String | N | 연결 코드 | KWRD_DICT.MPNG_CD |
| mpngCdNm | String | N | 연결 코드 이름 | COM_CD.COM_CD_NM |
| sortSeq | Integer | Y | | KWRD_DICT.SORT_SEQ |
| rmrkCn | String | N | | KWRD_DICT.RMRK_CN |
| useYn | boolean | Y | | KWRD_DICT.USE_YN |

### AdminAiKeywordSaveRequest
쓰는 API: POST /admin/ai/keywords, PATCH /admin/ai/keywords/{kwrdDictSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dictUsgCd | String | 등록 Y | AIMC0037·AIMC0038. 수정에서는 무시 | KWRD_DICT.DICT_USG_CD |
| kwrdClsfNm | String | 등록 Y | 50자, 용도별 분류(관리자 API 설명) 중 하나. 수정에서는 무시 | KWRD_DICT.KWRD_CLSF_NM |
| artclNm | String | 등록 Y | 100자, 공백만이면 400 | KWRD_DICT.ARTCL_NM |
| kwrdCn | String | 등록 Y | 1000자, 쉼표 구분 한 줄, 공백만이면 400 | KWRD_DICT.KWRD_CN |
| intrpRsltCn | String | N | 500자. 수정에서 빈 문자열은 비움 | KWRD_DICT.INTRP_RSLT_CN |
| mpngCd | String | N | `일정 종류`·`검진유형`·`가계부 카테고리`는 필수이고 각각 HLTG19·HLTG01·MYPG05의 코드, 그 밖의 분류는 보내면 400. 수정에서 빈 문자열은 비움(필수 분류면 400) | KWRD_DICT.MPNG_CD |
| rmrkCn | String | N | 1000자. 수정에서 빈 문자열은 비움 | KWRD_DICT.RMRK_CN |
| useYn | Boolean | N | 등록 기본 true | KWRD_DICT.USE_YN |

### AdminAiKeywordCreateResponse
쓰는 API: POST /admin/ai/keywords
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| kwrdDictSn | Long | Y | 등록한 키워드 번호 | KWRD_DICT.KWRD_DICT_SN |
