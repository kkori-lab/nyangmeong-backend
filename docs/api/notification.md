# 알림(notification) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

이 도메인의 API는 모두 구현됐다(2026-10-22 구현에 맞춤). 정본은 Swagger다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /members/me/notification-settings | 알림 수신 설정 조회(채널·유형·사전 알림 시점·한도 설정 여부) | COM-008-02·03·05~10 | LJS | - | NotificationSettingResponse |
| GET | /notifications | 알림 드롭다운 목록 페이지. 기본 5건·최대 50(넘으면 50), 목록 끝에서 다음 쪽을 더 불러온다(안 읽음 우선, 같은 상태끼리 최신순, 탭 필터, 7일 경과 숨김 제외, 조회만으로 읽음 처리 안 함). 스크롤 중 새 알림이 오면 쪽 경계 행이 겹칠 수 있어 `inappNotiSn`으로 거른다 `?tab=ALL&page=1&size=5` | COM-004-03~05·11·12 | LJS | - | Page<NotificationSummary> |
| GET | /notifications/unread-count | 미읽음 알림 수(헤더 배지·드롭다운 헤더) | COM-002-09, COM-004-02 | LJS | - | UnreadCountResponse |
| POST | /notifications/{inappNotiSn}/read | 알림 개별 읽음 처리. 남의·없는 알림은 404 `NOTIFICATION_NOT_FOUND`. 펫돌봄 거래 알림은 그 채팅방을 읽을 때(`POST /chat-rooms/{otoChatRoomSn}/read`)도 읽음이 된다 | COM-004-06 | LJS | - | - |
| POST | /notifications/read-all | 알림 모두 읽음. 탭과 상관없이 숨김 기준(7일) 안의 내 미읽음 알림 전부 읽음 처리(드롭다운을 여는 것만으로는 읽음 처리 안 함, COM-004-11). 0건이어도 200. 남의 알림은 조건에서 빠지므로 404 없음 | COM-004-17(명세 추가 예정) | LJS | - | NotificationReadAllResponse |
| PUT | /members/me/notification-settings | 알림 수신 설정 일괄 저장. 케어 일정 ON에 `leadTimes`가 비면 400 `INVALID_INPUT_VALUE` | COM-008-02·05~09·14·20 | LJS | NotificationSettingUpdateRequest | NotificationSettingResponse |
| GET | /admin/notification-templates | 관리자 알림 템플릿 목록. 등록된 템플릿만 유형·채널순, 페이지 없음(없는 유형·채널 조합은 화면이 "기본 문구"로 표시). 쿼리 `notiChnlCd`(AIMG03) 없거나 공백이면 전체. 모르는 채널 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-090-01 | LJS | - | List<AdminNotificationTemplateSummary> |
| POST | /admin/notification-templates | 알림 템플릿 등록(201, 감사 로그). 템플릿을 읽는 유형은 케어 일정(AIMC0008)뿐이고 다른 유형은 400 NOTIFICATION_TEMPLATE_NOT_SUPPORTED(기본 문구로 보냄). 치환 변수는 `{{이름}}` 8종(닉네임·반려동물명·검진유형·병원명·일정일·시간·D-N·캘린더링크), 목록 밖 변수·모르는 채널 400 INVALID_INPUT_VALUE, 같은 유형·채널 409 NOTIFICATION_TEMPLATE_DUPLICATED. 발송마다 다시 읽어 다음 발송부터 적용 | ADM-090-02·03 | LJS | AdminNotificationTemplateCreateRequest | AdminNotificationTemplateCreateResponse |
| PATCH | /admin/notification-templates/{notiTmpltSn} | 알림 템플릿 수정(제목·본문·사용 세 칸을 모두 덮어씀, 유형·채널은 안 바꿈, 감사 로그). 삭제 API 없음(사용을 끄면 기본 문구). 없는 템플릿 404 NOTIFICATION_TEMPLATE_NOT_FOUND, 변수 규칙은 등록과 같음 | ADM-090-02 | LJS | AdminNotificationTemplateUpdateRequest | - |
| GET | /admin/notification-logs | 관리자 알림 발송 이력. 최신순 20건씩, 알림 본문은 주지 않는다. 쿼리는 모두 선택: `bgngYmd`·`endYmd`(종료일은 그날 끝까지), `notiChnlCd`(AIMG03), `notiTypeCd`(AIMG02), `sndngRsltCd`(AIMG04). 시작일 > 종료일·모르는 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY. 재발송 API 없음 | ADM-091-01·02 | LJS | AdminNotificationLogSearchCond | PageResponse<AdminNotificationLogSummary> |

## DTO

### AdminNotificationLogSearchCond
쓰는 API: GET /admin/notification-logs (쿼리, 모두 선택)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| bgngYmd | LocalDate | N | 발송 시작일 | NOTI_SNDNG_LOG.SNDNG_DT, ADM-091-01 |
| endYmd | LocalDate | N | 발송 종료일(그날 끝까지 포함). 시작일보다 이르면 400 | NOTI_SNDNG_LOG.SNDNG_DT, ADM-091-01 |
| notiChnlCd | String | N | AIMG03 | NOTI_SNDNG_LOG.NOTI_CHNL_CD |
| notiTypeCd | String | N | AIMG02 | NOTI_SNDNG_LOG.NOTI_TYPE_CD |
| sndngRsltCd | String | N | AIMG04 | NOTI_SNDNG_LOG.SNDNG_RSLT_CD |

### AdminNotificationLogSummary
쓰는 API: GET /admin/notification-logs (목록 행, 최신순)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| notiSndngLogSn | Long | Y | | NOTI_SNDNG_LOG.NOTI_SNDNG_LOG_SN |
| mbrSn | Long | Y | 수신 회원 | NOTI_SNDNG_LOG.MBR_SN |
| mbrNcnm | String | N | 회원 행이 없으면 null | MBR.MBR_NCNM |
| notiTypeCd | String | Y | AIMG02 | NOTI_SNDNG_LOG.NOTI_TYPE_CD |
| notiChnlCd | String | Y | AIMG03 | NOTI_SNDNG_LOG.NOTI_CHNL_CD |
| sndngRsltCd | String | Y | AIMG04 | NOTI_SNDNG_LOG.SNDNG_RSLT_CD |
| rtryNmtm | Integer | N | 재시도 횟수 | NOTI_SNDNG_LOG.RTRY_NMTM |
| failRsn | String | N | 실패 사유 | NOTI_SNDNG_LOG.FAIL_RSN |
| sndngDt | LocalDateTime | Y | | NOTI_SNDNG_LOG.SNDNG_DT |

### AdminNotificationTemplateCreateRequest
쓰는 API: POST /admin/notification-templates
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| notiTypeCd | String | Y | AIMG02. 지금은 케어 일정(AIMC0008)만 받는다 | NOTI_TMPLT.NOTI_TYPE_CD, ADM-090-03 |
| notiChnlCd | String | Y | AIMG03 | NOTI_TMPLT.NOTI_CHNL_CD |
| notiTtl | String | N | 200자 이하. 비거나 공백이면 기본 제목 | NOTI_TMPLT.NOTI_TTL, ADM-090-02 |
| notiCn | String | Y | 4000자 이하. 변수는 `{{이름}}` 8종만 | NOTI_TMPLT.NOTI_CN, ADM-090-02 |
| useYn | Boolean | Y | 끄면 기본 문구로 보낸다 | NOTI_TMPLT.USE_YN |

### AdminNotificationTemplateCreateResponse
쓰는 API: POST /admin/notification-templates
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| notiTmpltSn | Long | Y | 만든 템플릿 번호 | NOTI_TMPLT.NOTI_TMPLT_SN |

### AdminNotificationTemplateSummary
쓰는 API: GET /admin/notification-templates (목록 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| notiTmpltSn | Long | Y | | NOTI_TMPLT.NOTI_TMPLT_SN |
| notiTypeCd | String | Y | AIMG02 | NOTI_TMPLT.NOTI_TYPE_CD |
| notiChnlCd | String | Y | AIMG03 | NOTI_TMPLT.NOTI_CHNL_CD |
| notiTtl | String | N | 비어 있으면 기본 제목 | NOTI_TMPLT.NOTI_TTL |
| notiCn | String | Y | | NOTI_TMPLT.NOTI_CN |
| useYn | boolean | Y | | NOTI_TMPLT.USE_YN |

### AdminNotificationTemplateUpdateRequest
쓰는 API: PATCH /admin/notification-templates/{notiTmpltSn} (세 칸 모두 덮어씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| notiTtl | String | N | 200자 이하. 비거나 공백이면 기본 제목 | NOTI_TMPLT.NOTI_TTL, ADM-090-02 |
| notiCn | String | Y | 4000자 이하. 변수 규칙은 등록과 같음 | NOTI_TMPLT.NOTI_CN, ADM-090-02 |
| useYn | Boolean | Y | | NOTI_TMPLT.USE_YN |

### NotificationSettingResponse
쓰는 API: GET /members/me/notification-settings; PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| maskedAcntEml | String | Y | 마스킹한 가입 이메일, 변경 불가 표시 | MBR.ACNT_EML_ENC, COM-008-03 |
| emlNotiRcptnYn | boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| leadTimes | List<String> | Y | 사전 알림 시점. `TODAY`·`D1`·`D3`·`D7` 중 켜진 것. 없으면 빈 목록 | MBR.THDY·D1·D3·D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |
| expndLimSetYn | boolean | Y | 월 지출 한도 설정 여부(계산) | MBR.MM_EXPND_LIM_AMT 존재 여부, COM-008-10 |

### NotificationSettingUpdateRequest
쓰는 API: PUT /members/me/notification-settings
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlNotiRcptnYn | Boolean | Y | | MBR.EML_NOTI_RCPTN_YN, COM-008-02 |
| inappNotiRcptnYn | Boolean | Y | | MBR.INAPP_NOTI_RCPTN_YN, COM-008-02 |
| careSchdlNotiRcptnYn | Boolean | Y | | MBR.CARE_SCHDL_NOTI_RCPTN_YN, COM-008-05 |
| leadTimes | List<String> | Y | `TODAY`·`D1`·`D3`·`D7` 복수 선택. 케어 일정 ON이면 1개 이상 | MBR.THDY·D1·D3·D7_BFHD_NOTI_YN, COM-008-06 |
| mtchDlngNotiRcptnYn | Boolean | Y | | MBR.MTCH_DLNG_NOTI_RCPTN_YN, COM-008-07 |
| cmntNotiRcptnYn | Boolean | Y | | MBR.CMNT_NOTI_RCPTN_YN, COM-008-08 |
| lostExpryNotiRcptnYn | Boolean | Y | | MBR.LOST_EXPRY_NOTI_RCPTN_YN, COM-008-08 |
| expndLimNotiRcptnYn | Boolean | Y | | MBR.EXPND_LIM_NOTI_RCPTN_YN, COM-008-09 |

### NotificationSummary
쓰는 API: GET /notifications (목록 행. 안 읽음 우선·같은 상태끼리 최신순, 7일 경과 숨김 제외)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| inappNotiSn | Long | Y | | INAPP_NOTI.INAPP_NOTI_SN |
| notiTypeCd | String | Y | AIMG02 공통코드 | INAPP_NOTI.NOTI_TYPE_CD, COM-004-04 |
| tab | String | Y | 행의 탭(`CARE`·`COMMUNITY`·`INFO`). 유형 라벨은 화면이 정한다 | COM-004-04 |
| notiTtl | String | Y | | INAPP_NOTI.NOTI_TTL |
| notiCn | String | Y | | INAPP_NOTI.NOTI_CN |
| mvmnPath | String | N | 유형별 이동 대상. 펫돌봄 거래(AIMC0009)는 `/chat?otoChatRoomSn={방 번호}`이고 프론트가 쿼리의 방 번호로 채팅 모달을 연다 | INAPP_NOTI.MVMN_PATH, COM-004-07 |
| prslDt | LocalDateTime | N | null이면 미읽음 | INAPP_NOTI.PRSL_DT, COM-004-06·11 |
| regDt | LocalDateTime | Y | 받은 시각 | INAPP_NOTI.REG_DT |

### UnreadCountResponse
쓰는 API: GET /notifications/unread-count
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| count | Long | Y | 헤더 배지(0이면 숨김, 99 초과 '99+')·드롭다운 헤더 | INAPP_NOTI.PRSL_DT IS NULL 건수, COM-002-09, COM-004-02 |

### NotificationReadAllResponse
쓰는 API: POST /notifications/read-all
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| count | Integer | Y | 이번에 읽음 처리한 건수. 이미 0건이면 0 | INAPP_NOTI.PRSL_DT를 채운 건수, COM-004-17 |
