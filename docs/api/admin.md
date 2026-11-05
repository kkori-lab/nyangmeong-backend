# 관리자(admin) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 여러 도메인을 합쳐 보여 주는 관리자 조회(`admin` 패키지)만 담는다. 한 도메인 소유의 관리자 API는 그 도메인 파일에 있다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /admin/dashboard | 관리자 대시보드 운영 현황·처리 대기. 기간과 무관한 지금 값. 회원 수는 일반 회원 권한(USRC0004)만 센다(관리자 계정 제외). 게시글은 관리자 게시글 목록(`GET /admin/posts`) 기본 조건과 같게 정상·블라인드·삭제 글을 모두 센다. 처리 대기 건수는 각 목록 화면의 필터 건수와 같은 조건이다(아래 `AdminDashboardResponse`). 일반 회원 403 ADMIN_ONLY | ADM-001-01·02·03 | LJS | - | AdminDashboardResponse |

## DTO

### AdminDashboardResponse
쓰는 API: GET /admin/dashboard
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| totMbrCnt | long | Y | 전체 회원 = 정상 + 정지(탈퇴 제외). `GET /admin/members?role=USRC0004&status=USRC0001`과 `…&status=USRC0002`의 `totalElements` 합 | MBR.MBR_STTS_CD, ADM-001-02 |
| nrmlMbrCnt | long | Y | 정상(USRC0001) | MBR.MBR_STTS_CD |
| stopMbrCnt | long | Y | 정지(USRC0002) | MBR.MBR_STTS_CD |
| todayJoinCnt | long | Y | 오늘 가입. 상태 무관(오늘 가입 후 탈퇴한 회원 포함). `GET /admin/members?role=USRC0004&bgngYmd=<오늘>` 건수와 같다 | MBR.REG_DT |
| weekJoinCnt | long | Y | 최근 7일 가입(오늘 포함 7일). 상태 무관. `bgngYmd=<오늘-6일>` 건수와 같다 | MBR.REG_DT |
| totPstCnt | long | Y | 전체 게시글(정상·블라인드·삭제 전부). `GET /admin/posts`(조건 없음)의 `totalElements` | PST |
| recentMbrs | List<AdminRecentMemberSummary> | Y | 최근 가입 5명. 정상·정지 회원 중 `REG_DT` 최신순(같으면 번호 내림차순) | MBR |
| recentPsts | List<AdminRecentPostSummary> | Y | 최근 게시글 5건. 상태 무관 `REG_DT` 최신순(같으면 번호 내림차순), `GET /admin/posts` 첫 5행과 같다 | PST |
| dclrRcptCnt | long | Y | 접수(CMUC0015) 신고. `GET /admin/reports?status=CMUC0015`의 `totalElements` | DCLR.DCLR_PRCS_STTS_CD, ADM-001-03 |
| unansInqryCnt | long | Y | 미답변(접수 CSCC0012) 1:1 문의, 회원이 삭제한 문의 제외. `GET /admin/inquiries?inqryPrcsSttsCd=CSCC0012` 건수 | OTO_INQRY.INQRY_PRCS_STTS_CD |
| pixelInqryCnt | long | Y | 그중 픽셀 캐릭터 유형(CSCC0006). `…&inqryTypeCd=CSCC0006` 건수 | OTO_INQRY.INQRY_TYPE_CD |
| charFailCnt | long | Y | 생성 실패(CHRC0004) 캐릭터 생성 작업. `GET /admin/processing/character-jobs`의 `sttsCnts` CHRC0004 값 | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| ocrFailCnt | long | Y | 인식 실패(AIMC0004) 영수증, 출처·기간 무관. `GET /admin/processing/receipts`(조건 없음)의 `summary.failCnt` | RECPT.OCR_PRCS_STTS_CD |
| notiFailCnt | long | Y | 최근 24시간(지금 - 24시간 이후) 발송 실패(AIMC0019). 발송 이력 화면은 날짜 단위 필터라 같은 숫자를 보려면 `GET /admin/notification-logs?sndngRsltCd=AIMC0019&bgngYmd=<어제>`에서 24시간 안 행을 센다 | NOTI_SNDNG_LOG.SNDNG_RSLT_CD·SNDNG_DT |
| btchFailCnt | long | Y | 마지막 실행 결과가 실패(SYSC0014)인 배치 수(실행 이력 건수가 아니다). `GET /admin/batches`에서 마지막 실행 결과가 실패인 행 수 | BTCH_EXCN_HSTRY.EXCN_RSLT_CD |

### AdminRecentMemberSummary
쓰는 API: GET /admin/dashboard (`recentMbrs` 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 회원 상세(`/admin/members/{mbrSn}`)로 이동 | MBR.MBR_SN |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| regDt | LocalDateTime | Y | 가입 일시 | MBR.REG_DT |

### AdminRecentPostSummary
쓰는 API: GET /admin/dashboard (`recentPsts` 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pstSn | Long | Y | 게시글 상세(`/admin/posts/{pstSn}`)로 이동 | PST.PST_SN |
| bbsCd | String | Y | 공통코드 CMUG01 | PST.BBS_CD |
| bbsNm | String | Y | 게시판 이름 | COM_CD.COM_CD_NM |
| pstTtl | String | Y | | PST.PST_TTL |
| regDt | LocalDateTime | Y | 작성 일시 | PST.REG_DT |
