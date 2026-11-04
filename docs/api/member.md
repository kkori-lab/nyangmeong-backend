# 회원(member) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /auth/login-attempt-limits | 입력 아이디 기준 실패 횟수·캡챠 요구 상태 조회(`?email=`, 가입 여부 비공개) | ACC-002-03·22 | YAY | - | LoginAttemptLimitResponse |
| GET | /members/me | 회원정보 수정 화면 조회. 이메일·닉네임·휴대전화·프로필 사진·가입 경로·연동 SNS | MYP-040-02·03·08·09 | SJM | - | MemberResponse |
| GET | /members/me/dashboard-widgets | 메인 건강 지표 위젯의 표시 여부·순서 조회 | MAN-006·007 | YAY | - | List<DashboardWidgetSummary> |
| POST | /members/recovery-info | 복구 대상 이메일·닉네임·남은 복구 기간·필요 동의·가린 보관 번호 조회. 로그인 없이 부르고, 토큰이 URL(로그)에 남지 않도록 본문 `MemberRecoveryInfoRequest`의 복구 확인 권한 토큰으로 본인확인한다. 권한 없음·만료·사용 완료·무효화·틀린 토큰은 이유를 가리지 않고 400 MEMBER_RECOVERY_NOT_AVAILABLE | ACC-001-44 | YAY | MemberRecoveryInfoRequest | MemberRecoveryResponse |
| GET | /auth/me | 로그인한 회원 요약(회원 번호·권한·닉네임·프로필 사진·마스킹 이메일). 구현은 `auth` 도메인(nct-backend `nct.auth`와 같은 자리) | COM-002-15, COM-005-02 | LJS | - | AuthMeResponse |
| GET | /internal/auth/check | 서버 간 내부 인증 확인(Python 미들웨어가 `/ai/**` 요청마다 사용자 쿠키를 넘겨 호출). `/api` 접두어 없음, 서버 간 키 없음(사용자 인증만). 세션 폐기 401 `UNAUTHORIZED`, 정지·탈퇴 403 `MEMBER_INACTIVE`(쿠키 삭제 `Set-Cookie`) | - (2026-10-26 사람 결정) | LJS | - | InternalAuthCheckResponse |
| GET | /members/nickname-availability | 닉네임 형식(한글·영문·숫자 1~10자)·중복 확인 | ACC-001-08, MYP-040-04·10 (기능 분배 3.1절 2번) | YAY | - | MemberNicknameAvailabilityResponse |
| GET | /members/phone-availability | 휴대전화번호 정규화(+82·하이픈)·형식·중복 확인 | ACC-001-29·30·44, MYP-040-05·11 (기능 분배 3.1절 3번) | YAY | - | MemberPhoneAvailabilityResponse |
| GET | /terms | 현재 사용 중인 약관 목록(필수 여부·버전) | ACC-001-12·44 | YAY | - | List<TermsSummary> |
| GET | /terms/{trmsSn} | 약관 전문 조회 | ACC-001-13 | YAY | - | TermsResponse |
| POST | /auth/captchas | 보안코드(캡챠) 발급·새로고침, 이전 코드 무효화 | ACC-002-18·19 | YAY | CaptchaCreateRequest | CaptchaResponse |
| POST | /auth/login | 이메일 로그인(캡챠·ID/PW 검증, 실패 누적, 첫 로그인 닉네임 재안내 여부 반환). 실패는 4xx 오류 응답이고 `data`에 `LoginAttemptLimitResponse`(실패 횟수·보안코드 요구)가 실린다. 코드는 LOGIN_FAILED(400, 없는 아이디·SNS 전용도 같음), LOGIN_CAPTCHA_REQUIRED(429, 보안코드 없음·5회째 실패), CAPTCHA_MISMATCH(400), MEMBER_WITHDRAWN(403, 비밀번호가 맞은 탈퇴 회원), MEMBER_INACTIVE(403, 정지) | ACC-002-05·16·20, ACC-001-22 | YAY | LoginRequest | LoginResponse |
| POST | /auth/logout | 로그아웃(Refresh 토큰 폐기) | COM-005-07·08, AIM-011-17 (기능 분배 3.1절 28번) | YAY | - | - |
| POST | /auth/refresh | Access 토큰 재발급. 토큰은 httpOnly 쿠키로만 주고 응답 `data`는 없다(`docs/convention/api.md`). 근거는 로그인 유지 기능(issues 6) | (명세 기능 ID 없음) | YAY | - | - |
| POST | /auth/sns-login | 네이버·카카오·구글 인가 결과 처리: 기존 회원 로그인 / 신규 가입 확인 / 이메일 중복 / 30일 복구 분기 | ACC-002-09~11, ACC-001-22·31·38·40 | YAY | SnsLoginRequest | SnsLoginResponse |
| POST | /email-certifications | 이메일 인증번호 발송·재전송(목적: 가입·비밀번호 찾기·복구). 가입 목적은 정상 회원 이메일 중복이면 거절(409 EMAIL_ALREADY_REGISTERED, 탈퇴 보관 이메일은 409 MEMBER_RECOVERY_AVAILABLE). 재전송 간격·횟수에 걸리면 400 EMAIL_CERT_RESEND_NOT_YET이고 data의 `resendAvailableDt`가 재시도 가능 시각. 복구(USRC0012) 목적은 보관 기간 안의 탈퇴 회원에게만 발송한다(기간 지남 400 MEMBER_RECOVERY_EXPIRED, 정상·정지·미가입 404 MEMBER_NOT_FOUND). 비밀번호 찾기는 가입 여부·SNS 전용 여부를 드러내지 않도록 회원을 조회하지 않고 같은 응답으로 발송 | ACC-001-03·04·38·41, ACC-011-03·07 | YAY | EmailCertificationCreateRequest | EmailCertificationResponse |
| POST | /email-certifications/{emlCertSn}/invalidate | 인증번호·재설정 권한·복구 확인 권한 무효화. 일련번호가 순번이라 본문의 `certTrgtEml`이 그 인증의 이메일과 맞아야 한다(없음·불일치·다른 목적은 404 EMAIL_CERT_NOT_FOUND, 이미 무효·사용 완료는 200). 비밀번호 찾기(재설정 취소 포함)와 복구(복구 취소) 목적을 받는다. 가입 목적은 404 | ACC-011-05, ACC-012-04, ACC-001-46 | YAY | EmailCertificationInvalidateRequest | - |
| POST | /email-certifications/{emlCertSn}/password-reset | 본인확인 결과로 비밀번호 재설정, 대상 계정 기존 로그인 종료. 권한 없음·만료·사용 완료·무효화는 400 PASSWORD_RESET_NOT_AVAILABLE, `pswd`와 `pswdCnfrm` 불일치는 400 PASSWORD_MISMATCH. 재설정 권한 조회 GET은 두지 않는다(순번 일련번호 노출 방지, 만료는 verify의 `initAuthrtExpryDt`와 이 API의 400으로 안다) | ACC-012-01~03 | YAY | EmailCertificationPasswordResetRequest | - |
| POST | /email-certifications/{emlCertSn}/verify | 인증번호 확인. 목적별 결과(가입 진행 / 재설정 대상·SNS 전용·미가입·탈퇴 보관 분기 / 복구 확인 권한) 반환 | ACC-001-06·40·43, ACC-011-08~10 | YAY | EmailCertificationVerifyRequest | EmailCertificationResponse |
| POST | /members | 회원가입(이메일·SNS 공통). 약관별 동의·번호 암호문/HMAC·기본 닉네임·최초 상태 이력·SNS 연결 저장, 서버 재검사. 201과 함께 닉네임 설정 전용 쿠키 `ncnm_setup`(로그인 쿠키 아님)을 준다. SNS 가입은 `signupTkn`으로 비밀번호 없이 저장 | ACC-001-09·10·12·14·22·24·27·29·32·38 | YAY | MemberCreateRequest | MemberResponse |
| POST | /members/me/password-verification | 현재 비밀번호 확인(회원정보 수정 진입 전 본인 확인). 서버에 상태를 남기지 않고 맞으면 200, 틀리면 400 CURRENT_PASSWORD_MISMATCH, 비밀번호 없는 SNS 회원은 400 SNS_ONLY_MEMBER, 정지·탈퇴 회원은 403 MEMBER_INACTIVE *통합 때 추가* | MYP-040-01 | YAY | PasswordVerifyRequest | - |
| POST | /members/recovery | 30일 이내 탈퇴 회원 기존 정보 복구 실행(탈퇴 → 정상, 상태 이력 "재가입 복구" 한 번). 로그인 없이 부르고 본문의 복구 확인 권한 토큰으로 본인확인하며 성공해도 로그인 쿠키를 만들지 않는다. 권한 문제는 400 MEMBER_RECOVERY_NOT_AVAILABLE, 보관 기간 지남은 400 MEMBER_RECOVERY_EXPIRED, 빠진 필수 약관 미동의는 400 REQUIRED_TERMS_NOT_AGREED, 번호는 INVALID_PHONE_NUMBER·409 PHONE_ALREADY_USED. 이미 쓴 권한이라도 그 회원이 지금 정상이면 바꾸는 것 없이 200(중복 클릭·응답 유실) | ACC-001-45 | YAY | MemberRecoverRequest | - |
| POST | /members/me/withdraw | 회원탈퇴(상태·이력 반영, 모든 로그인·인증 권한 종료, 30일 보관) | ACC-020-03·14·16, AIM-011-19, MYP-002-08 (기능 분배 3.1절 5번) | YAY | MemberWithdrawRequest | - |
| PUT | /members/me/dashboard-widgets | 메인 건강 지표 위젯 표시 여부·순서 전체 저장 | MAN-007 | YAY | DashboardWidgetSaveRequest | List<DashboardWidgetSummary> |
| PATCH | /members/me | 회원정보 수정. 휴대전화(저장 시 중복 재검사)·프로필 사진 | MYP-040-02·05·08·11 | SJM | MemberUpdateRequest | MemberResponse |
| PATCH | /members/me/nickname | 닉네임 설정 확정(로그인 회원 또는 가입 직후 받은 `ncnm_setup` 쿠키, 둘 다 없으면 401), 설정 완료 여부 반영 | ACC-001-26 | YAY | MemberNicknameUpdateRequest | MemberResponse |
| PATCH | /members/me/password | 로그인 상태에서 비밀번호 변경. 현재 비밀번호를 같은 요청에서 다시 검사(틀리면 400 CURRENT_PASSWORD_MISMATCH), `pswd`와 `pswdCnfrm` 불일치 400 PASSWORD_MISMATCH, SNS 회원 400 SNS_ONLY_MEMBER. 성공하면 모든 기기의 로그인을 끝내고 요청한 기기에는 새 로그인 쿠키를 다시 준다 *통합 때 추가* | MYP-040-06 | YAY | PasswordChangeRequest | - |
| GET | /admin/members | 관리자 회원 목록. 최신 가입순 20건씩, 정상·정지·탈퇴 전부. `q`에 @가 있으면 이메일 완전 일치, 없으면 닉네임 부분 일치. 시작일이 종료일보다 늦으면 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-010-01·02·03·04 | LJS | AdminMemberSearchCond | PageResponse<AdminMemberSummary> |
| GET | /admin/members/suspensions | 지금 정지 중인 회원 목록. 최근 정지순 20건씩. 일반 회원 403 ADMIN_ONLY | ADM-012 | LJS | AdminSuspensionSearchCond | PageResponse<AdminSuspensionSummary> |
| POST | /admin/members/{mbrSn}/suspension | 회원 정지(상태 정지, 상태 이력, 모든 세션 폐기, 감사 로그). 종료 일시는 오늘+N일 00:00. 7·30·null 외 값 400 INVALID_INPUT_VALUE, 관리자·본인·탈퇴·이미 정지 409 MEMBER_NOT_SUSPENDABLE, 없는 회원 404 MEMBER_NOT_FOUND | ADM-012-01·02·05 | LJS | AdminMemberSuspendRequest | - |
| DELETE | /admin/members/{mbrSn}/suspension | 회원 정지 해제(상태 정상, 종료 일시 비움, 상태 이력, 감사 로그). 사유를 본문으로 보낸다. 정지 중이 아니면 409 ALREADY_PROCESSED, 없는 회원 404 MEMBER_NOT_FOUND | ADM-012-03 | LJS | AdminMemberReleaseRequest | - |
| GET | /admin/members/withdrawn | 보관 기간 안의 탈퇴 회원 목록. 탈퇴 최신순 20건씩. 이메일은 마스킹, 파기 예정일은 탈퇴 일시 + 탈퇴 회원 보관 일수. 파기·복구는 실행하지 않는다. 일반 회원 403 ADMIN_ONLY | ADM-013-01 | LJS | - | PageResponse<AdminWithdrawnMemberSummary> |
| GET | /admin/members/{mbrSn} | 관리자 회원 상세 기본 정보. 정상·정지·탈퇴·파기 전부. 이메일·휴대전화는 마스킹. 일반 회원 403 ADMIN_ONLY, 없는 회원 404 MEMBER_NOT_FOUND | ADM-011-01 | LJS | - | AdminMemberDetailResponse |
| GET | /admin/members/{mbrSn}/terms-agreements | 관리자 회원 상세 약관 동의. 사용 중 약관 종류마다 1행, 최신 동의 기록. 기록이 없으면 미동의. 마케팅 동의는 선택 약관 행. 일반 회원 403 ADMIN_ONLY, 없는 회원 404 MEMBER_NOT_FOUND | ADM-011-03 | LJS | - | List\<AdminMemberTermsAgreeSummary\> |
| GET | /admin/members/{mbrSn}/status-histories | 관리자 회원 상세 상태 이력. 최신순 20건씩. 일반 회원 403 ADMIN_ONLY, 없는 회원 404 MEMBER_NOT_FOUND | ADM-011-05 | LJS | - | PageResponse\<AdminMemberStatusHistorySummary\> |
| GET | /admin/admins | 탈퇴를 뺀 관리자 목록. 지정 최신순 20건씩, 지정 기록이 없으면 맨 뒤. 이메일은 마스킹. 일반 회원 403 ADMIN_ONLY | ADM-113-01 | LJS | - | PageResponse<AdminAccountSummary> |
| POST | /admin/admins/{mbrSn} | 관리자 지정(권한 변경, 감사 로그). 대상 검색은 GET /admin/members?q=&status=USRC0001&role=USRC0004. 정지·탈퇴·이미 관리자 409 MEMBER_NOT_APPOINTABLE, 없는 회원 404 MEMBER_NOT_FOUND | ADM-113-02 | LJS | - | - |
| DELETE | /admin/admins/{mbrSn} | 관리자 해제(권한 일반회원, 감사 로그). 세션은 폐기하지 않고 다음 요청부터 일반 회원이다. 본인·마지막 정상 관리자 409 ADMIN_NOT_RELEASABLE, 관리자 아님·탈퇴 409 ALREADY_PROCESSED, 없는 회원 404 MEMBER_NOT_FOUND | ADM-113-03 | LJS | - | - |
| GET | /admin/terms | 관리자 약관 목록. 종류별 사용 중 버전 1행, 정렬순서대로. 관리 대상은 가입 약관 4종(`SERVICE`·`PRIVACY`·`AGE_14`·`MARKETING`). 기능별 동의 2종(펫돌봄 이용수칙·실종 개인정보 공개 동의)은 아직 `TRMS`에 없다. 일반 회원 403 ADMIN_ONLY | ADM-051-01·04 | LJS | - | List\<AdminTermsSummary\> |
| GET | /admin/terms/history | 관리자 약관 버전 이력(`?trmsFixIdntfKey=`). 그 종류 모든 버전 최신순, 내용 제외. 없는 종류는 빈 목록, 키가 없으면 400 INVALID_INPUT_VALUE. 일반 회원 403 ADMIN_ONLY | ADM-051-03 | LJS | - | List\<AdminTermsSummary\> |
| GET | /admin/terms/{trmsSn} | 관리자 약관 상세. 사용 안 함 버전도 내용(`trmsCn`) 포함. 없으면 404 TERMS_NOT_FOUND. 일반 회원 403 ADMIN_ONLY | ADM-051-03 | LJS | - | AdminTermsDetail |
| POST | /admin/terms | 약관 새 버전 등록(201, 감사 로그). 같은 종류 이전 버전은 사용 안 함, 정렬순서는 이전 버전을 이어받는다. 기존 회원 동의는 그대로(재동의 없음, 공지로 안내). 새 종류는 만들지 않아 없는 종류 404 TERMS_NOT_FOUND, 같은 종류 같은 버전 409 DUPLICATE_TRMS_VER, 공백만이거나 글자 수 초과 400 INVALID_INPUT_VALUE. 일반 회원 403 ADMIN_ONLY | ADM-051-02·04·05 | LJS | AdminTermsCreateRequest | AdminTermsDetail |

## DTO

### AdminMemberSearchCond
쓰는 API: GET /admin/members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| q | String | N | @가 있으면 이메일 완전 일치(HMAC, 대소문자·앞뒤 공백 무시), 없으면 닉네임 부분 일치 | MBR.ACNT_EML_HMAC, MBR.MBR_NCNM, ADM-010-02 |
| status | String | N | 회원 상태(USRG01) | MBR.MBR_STTS_CD, ADM-010-03 |
| joinPath | String | N | 가입 경로(USRG03) | MBR.JOIN_PATH_CD, ADM-010-03 |
| role | String | N | 회원 권한(USRG02) | MBR.MBR_AUTHRT_CD, ADM-010-03 |
| bgngYmd | LocalDate | N | 가입일 시작(포함) | MBR.REG_DT, ADM-010-03 |
| endYmd | LocalDate | N | 가입일 종료(포함) | MBR.REG_DT, ADM-010-03 |

### AdminMemberSummary
쓰는 API: GET /admin/members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 회원 상세 이동 값 | MBR.MBR_SN, ADM-010-04 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-010-01 |
| maskedAcntEml | String | Y | 앞 3자 + `***` + @도메인 | MBR.ACNT_EML_ENC, ADM-010-01 |
| joinPathCd | String | Y | 가입 경로(USRG03) | MBR.JOIN_PATH_CD, ADM-010-01 |
| mbrSttsCd | String | Y | 회원 상태(USRG01) | MBR.MBR_STTS_CD, ADM-010-01 |
| mbrAuthrtCd | String | Y | 회원 권한(USRG02) | MBR.MBR_AUTHRT_CD, ADM-010-01 |
| regDt | LocalDateTime | Y | 가입일 | MBR.REG_DT, ADM-010-01 |
| lastLgnDt | LocalDateTime | N | 최근 로그인. 첫 로그인 전이면 null | MBR.LAST_LGN_DT, ADM-010-01 |

### AdminMemberDetailResponse
쓰는 API: GET /admin/members/{mbrSn}

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | | MBR.MBR_SN, ADM-011-01 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-011-01 |
| maskedAcntEml | String | Y | 앞 3자 + `***` + @도메인 | MBR.ACNT_EML_ENC, ADM-011-01 |
| maskedMblTelno | String | N | `010-****-1234`. 번호가 없으면 null | MBR.MBL_TELNO_ENC, ADM-011-01 |
| joinPathCd | String | Y | 가입경로공통코드 | MBR.JOIN_PATH_CD, ADM-011-01 |
| snsLinks | List\<MemberSnsAccountSummary\> | Y | 연동 중인 SNS 제공자 | SNS_ACNT_LINK, ADM-011-01 |
| regDt | LocalDateTime | Y | 가입일 | MBR.REG_DT, ADM-011-01 |
| lastLgnDt | LocalDateTime | N | 최근 로그인 | MBR.LAST_LGN_DT, ADM-011-01 |
| hldPnt | Integer | Y | 보유 포인트 | MBR.HLD_PNT, ADM-011-01 |
| mbrSttsCd | String | Y | 회원상태공통코드 | MBR.MBR_STTS_CD, ADM-011-01 |
| suspnEndDt | LocalDateTime | N | 정지 종료 일시. 영구 정지·정상은 null | MBR.SUSPN_END_DT, ADM-011-01 |

### AdminMemberTermsAgreeSummary
쓰는 API: GET /admin/members/{mbrSn}/terms-agreements

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsFixIdntfKey | String | Y | 약관 종류 | TRMS.TRMS_FIX_IDNTF_KEY, ADM-011-03 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL, ADM-011-03 |
| esntlAgreYn | boolean | Y | 필수 약관 여부 | TRMS.ESNTL_AGRE_YN, ADM-011-03 |
| trmsVerId | String | N | 동의한 버전. 기록이 없으면 null | TRMS.TRMS_VER_ID, ADM-011-03 |
| agreYn | boolean | Y | 기록이 없으면 false | MBR_TRMS_AGRE.AGRE_YN, ADM-011-03 |
| agreDt | LocalDateTime | N | 기록이 없으면 null | MBR_TRMS_AGRE.AGRE_DT, ADM-011-03 |

### AdminMemberStatusHistorySummary
쓰는 API: GET /admin/members/{mbrSn}/status-histories

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSttsHstrySn | Long | Y | | MBR_STTS_HSTRY.MBR_STTS_HSTRY_SN, ADM-011-05 |
| bfrSttsCd | String | N | 변경 전 상태. 가입 이력은 null | MBR_STTS_HSTRY.BFR_STTS_CD, ADM-011-05 |
| aftrSttsCd | String | Y | 변경 후 상태 | MBR_STTS_HSTRY.AFTR_STTS_CD, ADM-011-05 |
| chgRsn | String | N | 사유 | MBR_STTS_HSTRY.CHG_RSN, ADM-011-05 |
| prcsMbrSn | Long | N | 처리 회원. 배치 처리는 null | MBR_STTS_HSTRY.PRCS_MBR_SN, ADM-011-05 |
| prcsMbrNcnm | String | N | 처리 회원 닉네임 | MBR.MBR_NCNM, ADM-011-05 |
| regDt | LocalDateTime | Y | 일시 | MBR_STTS_HSTRY.REG_DT, ADM-011-05 |

### AdminSuspensionSearchCond
쓰는 API: GET /admin/members/suspensions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| q | String | N | 닉네임 부분 일치 | MBR.MBR_NCNM, SCR-ADM-012 |
| type | String | N | PERIOD(기간)·PERMANENT(영구). 비우면 전체. 그 외 값 400 | MBR.SUSPN_END_DT, SCR-ADM-012 |

### AdminSuspensionSummary
쓰는 API: GET /admin/members/suspensions
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 회원 상세 이동 값 | MBR.MBR_SN, SCR-ADM-012 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, SCR-ADM-012 |
| suspnDt | LocalDateTime | N | 정지 일시. 최근 정지 이력 등록 일시. 이력이 없으면 null | MBR_STTS_HSTRY.REG_DT, SCR-ADM-012 |
| suspnEndDt | LocalDateTime | N | null이면 영구 정지 | MBR.SUSPN_END_DT, ADM-012-01 |
| suspnRsn | String | N | 최근 정지 이력 사유 | MBR_STTS_HSTRY.CHG_RSN, ADM-012-01 |
| prcrNcnm | String | N | 처리 관리자 닉네임 | MBR_STTS_HSTRY.PRCS_MBR_SN, ADM-012-01 |

### AdminWithdrawnMemberSummary
쓰는 API: GET /admin/members/withdrawn
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 행 구분 값. 상세 이동 없음 | MBR.MBR_SN, SCR-ADM-013 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-013-01 |
| maskedAcntEml | String | Y | 앞 3자 + `***` + @도메인 | MBR.ACNT_EML_ENC, ADM-013-01 |
| whdwlDt | LocalDateTime | Y | 탈퇴 일시 | MBR.WHDWL_DT, ADM-013-01 |
| purgeYmd | LocalDate | Y | 파기 예정일. 탈퇴 일시 + 탈퇴 회원 보관 일수 | SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT, ADM-013-01 |

### AdminAccountSummary
쓰는 API: GET /admin/admins
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 지정·해제 경로 값 | MBR.MBR_SN, ADM-113-01 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ADM-113-01 |
| maskedAcntEml | String | Y | 앞 3자 + `***` + @도메인 | MBR.ACNT_EML_ENC, ADM-113-01 |
| apntDt | LocalDateTime | N | 지정일. 최근 "관리자 지정" 작업 로그 일시. 기록이 없으면 null | MNGR_TASK_LOG.REG_DT, SCR-ADM-113 |

### AdminTermsSummary
쓰는 API: GET /admin/terms, GET /admin/terms/history
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | 상세 경로 값 | TRMS.TRMS_SN, ADM-051-01 |
| trmsFixIdntfKey | String | Y | 약관 종류. `SERVICE`·`PRIVACY`·`AGE_14`·`MARKETING` | TRMS.TRMS_FIX_IDNTF_KEY, ADM-051-04 |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID, ADM-051-01 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL, ADM-051-01 |
| esntlAgreYn | boolean | Y | 필수 약관 여부 | TRMS.ESNTL_AGRE_YN, ADM-051-01 |
| useYn | boolean | Y | 목록은 모두 true, 이력의 지난 버전은 false | TRMS.USE_YN, ADM-051-01·03 |
| regDt | LocalDateTime | Y | 등록일시 | TRMS.REG_DT, ADM-051-03 |

### AdminTermsDetail
쓰는 API: GET /admin/terms/{trmsSn}, POST /admin/terms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | | TRMS.TRMS_SN |
| trmsFixIdntfKey | String | Y | | TRMS.TRMS_FIX_IDNTF_KEY |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID |
| trmsTtl | String | Y | | TRMS.TRMS_TTL |
| trmsCn | String | Y | | TRMS.TRMS_CN, ADM-051-03 |
| esntlAgreYn | boolean | Y | | TRMS.ESNTL_AGRE_YN |
| useYn | boolean | Y | | TRMS.USE_YN |
| regDt | LocalDateTime | Y | | TRMS.REG_DT |

### AdminTermsCreateRequest
쓰는 API: POST /admin/terms
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsFixIdntfKey | String | Y | 이미 있는 종류만. 없으면 404 TERMS_NOT_FOUND | TRMS.TRMS_FIX_IDNTF_KEY, ADM-051-02·04 |
| trmsVerId | String | Y | 앞뒤 공백 지운 뒤 1~80자. 같은 종류에 있으면 409 DUPLICATE_TRMS_VER | TRMS.TRMS_VER_ID, ADM-051-02 |
| trmsTtl | String | Y | 앞뒤 공백 지운 뒤 1~200자 | TRMS.TRMS_TTL, ADM-051-02 |
| trmsCn | String | Y | 앞뒤 공백 지운 뒤 1~4000자 | TRMS.TRMS_CN, ADM-051-02 |
| esntlAgreYn | Boolean | Y | 필수 약관 여부 | TRMS.ESNTL_AGRE_YN, ADM-051-02 |

### AdminMemberSuspendRequest
쓰는 API: POST /admin/members/{mbrSn}/suspension
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| suspnDayCnt | Integer | N | 7 또는 30. null이면 영구 정지 | ADM-012-01 |
| suspnRsn | String | Y | 1000자 이내 | MBR_STTS_HSTRY.CHG_RSN, ADM-012-01 |

### AdminMemberReleaseRequest
쓰는 API: DELETE /admin/members/{mbrSn}/suspension
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| rlsRsn | String | Y | 1000자 이내 | MBR_STTS_HSTRY.CHG_RSN, ADM-012-03 |

### CaptchaCreateRequest
쓰는 API: POST /auth/captchas
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 이메일 형식. 이 아이디의 보안코드를 새로 발급하고 이전 코드는 무효화한다. 가입 여부와 무관하게 같은 처리. 보안코드를 요구하는 상태(5회 실패)가 아닌 아이디는 CAPTCHA_NOT_REQUIRED(400)로 거절한다 | LGN_ATMPT_LMT.LGN_ID_HMAC (HMAC 전 원문), ACC-002-19 |

### CaptchaResponse
쓰는 API: POST /auth/captchas
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| captchaImg | String | Y | 영문 대문자·숫자 각 1개 이상, 총 6글자를 그린 이미지. PNG를 Base64 data URI(`data:image/png;base64,...`)로 준다. 코드 원문은 주지 않는다 | ACC-002-18 |

### DashboardWidgetSaveItem
쓰는 API: PUT /members/me/dashboard-widgets (DashboardWidgetSaveRequest.widgets의 요소. 입력 목록 밖의 중첩 record, issues 2)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dshbdWdgtCd | String | Y | MANG01 공통코드, 8자 | MBR_DSHBD_WDGT.DSHBD_WDGT_CD |
| indctYn | Boolean | Y | | MBR_DSHBD_WDGT.INDCT_YN, MAN-006 |
| sortSeq | Integer | Y | 최대 10자리 | MBR_DSHBD_WDGT.SORT_SEQ, MAN-007 |

### DashboardWidgetSaveRequest
쓰는 API: PUT /members/me/dashboard-widgets
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| widgets | List<DashboardWidgetSaveItem> | Y | 위젯 전체 목록(PUT, 빠진 항목은 지워짐). `indctYn=true`는 최대 5개 | MAN-006·007 |

### DashboardWidgetSummary
쓰는 API: GET /members/me/dashboard-widgets; PUT /members/me/dashboard-widgets (응답 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dshbdWdgtCd | String | Y | MANG01 공통코드 | MBR_DSHBD_WDGT.DSHBD_WDGT_CD |
| dshbdWdgtCdNm | String | Y | 위젯 카드 이름(체중·식사량 등) | COM_CD.COM_CD_NM, MAN-006 (추가) |
| indctYn | boolean | Y | | MBR_DSHBD_WDGT.INDCT_YN, MAN-006 |
| sortSeq | Integer | Y | | MBR_DSHBD_WDGT.SORT_SEQ, MAN-007 |

### EmailCertificationCreateRequest
쓰는 API: POST /email-certifications
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertPrpsCd | String | Y | 공통코드 USRG04(가입·비밀번호 찾기·복구) | EML_CERT.EML_CERT_PRPS_CD |
| certTrgtEml | String | Y | 이메일 형식. 가입 목적이면 정상 회원 이메일 중복 시 거절 | EML_CERT.CERT_TRGT_EML_ENC, ACC-001-03, ACC-011-02 |

### EmailCertificationPasswordResetRequest
쓰는 API: POST /email-certifications/{emlCertSn}/password-reset
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pswdInitTkn | String | Y | 인증 확인 때 받은 재설정 권한 토큰. 만료·사용 완료면 거절 | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-012-06 |
| pswd | String | Y | 8자 이상, 문자와 숫자 조합 | MBR.PSWD_HASH (해시 전 원문), ACC-012-01 |
| pswdCnfrm | String | Y | `pswd`와 일치. 서버가 다시 검사 | ACC-012-02·03 |

### EmailCertificationResponse
쓰는 API: POST /email-certifications; POST /email-certifications/{emlCertSn}/verify
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertSn | Long | Y | | EML_CERT.EML_CERT_SN |
| emlCertPrpsCd | String | Y | 공통코드 USRG04 | EML_CERT.EML_CERT_PRPS_CD |
| certExpryDt | LocalDateTime | Y | 남은 시간 표시 기준(서버 만료시각) | EML_CERT.CERT_EXPRY_DT, ACC-001-05 |
| resendAvailableDt | LocalDateTime | N | 재전송 제한 중이면 재시도 가능 시점 | ACC-011-07 |
| certCmptnDt | LocalDateTime | N | 인증 확인 성공 시각. verify 이후 | EML_CERT.CERT_CMPTN_DT |
| certRslt | String | N | verify 결과 분기. 가입 목적은 `SIGNUP_READY`, 비밀번호 찾기 목적은 `PASSWORD_RESET_READY`(재설정 대상, 정지 회원 포함) / `SNS_ONLY`(비밀번호 없는 회원) / `MEMBER_NOT_FOUND`(미가입, 보관 기간이 지난 탈퇴) / `MEMBER_WITHDRAWN`(보관 기간 안의 탈퇴)로 확정. 복구 목적은 `RECOVERY_READY`(보관 기간 안의 탈퇴 회원, `rcvryAuthrtTkn`·`initAuthrtExpryDt` 포함)로 확정하고, 보관 기간 지남은 400 `MEMBER_RECOVERY_EXPIRED`, 정상·정지·미가입은 404 `MEMBER_NOT_FOUND` | ACC-001-06·43, ACC-011-08 |
| snsOffrCds | List<String> | N | `certRslt`가 SNS 전용일 때 실제 연결된 제공자(USRG03) | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-011-10 |
| pswdInitTkn | String | N | 재설정 대상일 때만. 비밀번호 재설정 권한 토큰 원문 | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-011-08 |
| initAuthrtExpryDt | LocalDateTime | N | 재설정 권한 만료 시각 | EML_CERT.INIT_AUTHRT_EXPRY_DT |
| rcvryAuthrtTkn | String | N | 복구 목적 `RECOVERY_READY`일 때만. 복구 확인 권한 토큰 원문. `EML_CERT.PSWD_INIT_TKN_HASH`에 `hmac("rcvry:"+토큰)`으로 저장하고(목적이 행마다 달라 재설정 토큰과 같은 칸을 쓴다) 만료는 `initAuthrtExpryDt` | EML_CERT.PSWD_INIT_TKN_HASH, ACC-001-43 |

### EmailCertificationInvalidateRequest
쓰는 API: POST /email-certifications/{emlCertSn}/invalidate
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| certTrgtEml | String | Y | 이메일 형식. 인증한 이메일과 같아야 한다(일련번호 추측으로 남의 인증을 끄지 못하게) | EML_CERT.CERT_TRGT_EML_HMAC, ACC-011-05 |

### EmailCertificationVerifyRequest
쓰는 API: POST /email-certifications/{emlCertSn}/verify
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| certNo | String | Y | 숫자 6자리. 다른 목적·이미 사용한 번호는 거절 | EML_CERT.CERT_NO_HASH (해시 전 원문), ACC-001-05, ACC-001-42 |

### LoginAttemptLimitResponse
쓰는 API: GET /auth/login-attempt-limits
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| scrtyCdRqtYn | boolean | Y | 보안코드 입력이 필요한지. 기록 없는 아이디는 false | LGN_ATMPT_LMT.SCRTY_CD_RQT_YN, ACC-002-22 |
| cntnuCertFailNmtm | Integer | Y | 연속 인증 실패 횟수. 기록 없는 아이디는 0(가입 여부 비공개) | LGN_ATMPT_LMT.CNTNU_CERT_FAIL_NMTM, ACC-002-03 |

### LoginRequest
쓰는 API: POST /auth/login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 미입력 불가, 이메일 형식 | MBR.ACNT_EML_ENC, ACC-002-03 |
| pswd | String | Y | 미입력 불가 | MBR.PSWD_HASH (해시 전 원문), ACC-002-04 |
| scrtyCd | String | N | 보안코드 요구 상태면 필수. 영문·숫자 6글자, 소문자는 대문자로 처리 | LGN_ATMPT_LMT.SCRTY_CD_HASH (해시 전 원문), ACC-002-20 |

### LoginResponse
쓰는 API: POST /auth/login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| ncnmReguide | Boolean | Y | 닉네임 모달 재안내 여부. 닉네임 설정 미완료이고 기존 최종로그인일시가 없을 때만 true | MBR.NCNM_STNG_CMPTN_YN, MBR.LAST_LGN_DT, ACC-001-22 |
| mbrNcnm | String | Y | 재안내 모달에 미리 채울 저장값 | MBR.MBR_NCNM, ACC-001-22 |

### MemberCreateRequest
쓰는 API: POST /members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| joinPathCd | String | Y | 공통코드 USRG03(이메일·네이버·카카오·구글) | MBR.JOIN_PATH_CD |
| emlCertSn | Long | N | 이메일 가입이면 필수, SNS 가입이면 보내면 400이다. 가입 목적 인증이 완료·유효해야 한다. 이메일은 이 인증의 대상 이메일을 쓴다 | EML_CERT.EML_CERT_SN, ACC-001-14 |
| certNo | String | N | 이메일 가입이면 필수. 숫자 6자리 인증번호를 한 번 더 확인한다. `emlCertSn`이 순번이라 남이 인증해 둔 번호로 가입하는 것을 막는다(issues 로그인 6) | EML_CERT.CERT_NO_HASH, ACC-001-14 |
| signupTkn | String | N | SNS 가입이면 필수, 이메일 가입이면 보내지 않는다. `POST /auth/sns-login`이 준 신규 가입 확인 토큰. 토큰의 제공자가 `joinPathCd`와 다르거나 만료·변조면 400 `SNS_SIGNUP_TOKEN_INVALID`. 이메일은 토큰 값을 쓴다. 토큰에 번호가 있으면 `mblTelno`가 그 번호와 같아야 한다(다르면 400 `INVALID_INPUT_VALUE`). 같은 토큰의 재요청은 그 사이 만들어진 같은 회원의 결과만 돌려주고, 그 밖에 이미 연동된 계정이면 409 `SNS_ACCOUNT_ALREADY_LINKED` | ACC-001-31·32 |
| mblTelno | String | Y | 공백·하이픈 제거, +82 표기 통일 후 형식 검사. 다른 회원(30일 보관 포함)과 중복 불가 | MBR.MBL_TELNO_ENC, MBR.MBL_TELNO_HMAC, ACC-001-29·30 |
| pswd | String | N | 이메일 가입이면 필수, SNS 가입이면 보내면 400이다. 8자 이상 64자 이하, 영문과 숫자를 함께 포함하는 공백 없는 ASCII(BCrypt 72바이트 한계) | MBR.PSWD_HASH (해시 전 원문), ACC-001-09, ACC-001-32 |
| pswdCnfrm | String | N | 이메일 가입이면 필수. `pswd`와 일치 | ACC-001-10 |
| trmsAgres | List<TermsAgreeRequest> | Y | 화면에 보여 준 약관 전부. 필수 약관(서비스 이용약관·개인정보 수집 및 이용·만 14세 이상) 모두 `agreYn=true`. 마케팅 미동의도 false로 보낸다 | MBR_TRMS_AGRE, ACC-001-12 |
| idmptDmndId | String | N | 같은 가입 시도의 재요청 식별값. 저장 컬럼이 없어 받기만 한다. 중복 클릭·응답 유실 재요청은 같은 인증(`emlCertSn`·`certNo`)의 사용 기록으로 가려 닉네임 쿠키 유효시간 안에서 기존 결과만 돌려준다 | ACC-001-14, ACC-001-32 |

#### TermsAgreeRequest (요청 record, 쓰는 DTO: MemberCreateRequest, MemberRecoverRequest)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | 현재 사용 중인 약관 버전이어야 한다 | MBR_TRMS_AGRE.TRMS_SN, ACC-001-12 |
| agreYn | Boolean | Y | | MBR_TRMS_AGRE.AGRE_YN |

### MemberNicknameAvailabilityResponse
쓰는 API: GET /members/nickname-availability
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| available | Boolean | Y | 초안 그대로. 현재 본인 닉네임이면 true | ACC-001-08 |

### MemberNicknameUpdateRequest
쓰는 API: PATCH /members/me/nickname
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrNcnm | String | N | 한글·영문·숫자 1~10자, 공백·특수문자·이모지 불가, 중복 불가. 저장 시 설정 완료 여부도 Y로 바꾼다. 기본 닉네임을 그대로 확정할 때도 현재 값을 보낸다(issues 3) | MBR.MBR_NCNM, MBR.NCNM_STNG_CMPTN_YN, ACC-001-07·26 |

### MemberPhoneAvailabilityResponse
쓰는 API: GET /members/phone-availability
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| normalizedMblTelno | String | Y | 공백·하이픈 제거, +82 표기를 국내 표기 숫자(`01012345678`)로 통일한 결과 | ACC-001-29 |
| available | Boolean | Y | 초안 그대로 | ACC-001-30 |

### MemberRecoveryInfoRequest
쓰는 API: POST /members/recovery-info
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertSn | Long | Y | 인증번호 확인 응답의 `emlCertSn` | EML_CERT.EML_CERT_SN, ACC-001-43 |
| rcvryAuthrtTkn | String | Y | 인증번호 확인 응답의 복구 확인 권한 토큰(최대 100자) | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-001-43·44 |

### MemberRecoverRequest
쓰는 API: POST /members/recovery
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| emlCertSn | Long | Y | 인증번호 확인 응답의 `emlCertSn` | EML_CERT.EML_CERT_SN, ACC-001-45 |
| rcvryAuthrtTkn | String | Y | 복구 확인 권한 토큰. 복구 실행 때 권한·만료·사용 여부와 보관 기간을 다시 검사한다 | EML_CERT.PSWD_INIT_TKN_HASH (해시 전 원문), ACC-001-45 |
| trmsAgres | List<TermsAgreeRequest> | N | 같은 버전으로 이미 동의한 약관은 빼고, 빠진 현재 버전 필수 약관만. 빠진 필수 약관이 있으면 필수 | MBR_TRMS_AGRE, ACC-001-44·45 |
| mblTelno | String | N | 보관 번호가 없을 때만 필수. 정규화·형식·중복 검사는 가입과 같다 | MBR.MBL_TELNO_ENC, ACC-001-29, ACC-001-44 |

### MemberRecoveryResponse
쓰는 API: POST /members/recovery-info
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 본인확인을 마친 복구 대상 이메일 | MBR.ACNT_EML_ENC, ACC-001-44 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, ACC-001-44 |
| whdwlDt | LocalDateTime | Y | | MBR.WHDWL_DT, ACC-001-44 |
| rcvryExpryDt | LocalDateTime | Y | 보관 만료시각 = 탈퇴일시 + 탈퇴회원보관일수. 남은 기간 표시 기준 | MBR.WHDWL_DT, SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT, ACC-001-44 |
| maskedMblTelno | String | N | 보관 중인 번호를 가려서. 없으면 null(직접 입력) | MBR.MBL_TELNO_ENC, ACC-001-44, ACC-020-01 |
| missingTrms | List<TermsSummary> | Y | 다시 받아야 할 현재 버전 약관. 없으면 빈 목록 | TRMS, MBR_TRMS_AGRE, ACC-001-44 |

### MemberResponse
쓰는 API: GET /members/me; PATCH /members/me; PATCH /members/me/nickname; POST /members
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| acntEml | String | Y | 복호화한 로그인 이메일(수정 불가 표시) | MBR.ACNT_EML_ENC, MYP-040-03 |
| mbrNcnm | String | Y | | MBR.MBR_NCNM, MYP-040-04, ACC-001-24·27 |
| mblTelno | String | Y | 복호화한 휴대전화번호 | MBR.MBL_TELNO_ENC, MYP-040-05 |
| atchFileSn | Long | N | 프로필 사진. null이면 기본 이미지 | MBR.ATCH_FILE_SN, MYP-040-02 |
| joinPathCd | String | Y | 가입 경로(USRG03). SNS 회원은 본인 확인 생략 | MBR.JOIN_PATH_CD, MYP-040-01 |
| ncnmStngCmptnYn | boolean | Y | 닉네임 설정 완료 여부 | MBR.NCNM_STNG_CMPTN_YN, ACC-001-26·27 |
| snsAcntLinks | List<MemberSnsAccountSummary> | Y | 연동된 SNS 계정. 없으면 빈 목록 | SNS_ACNT_LINK, MYP-040-09 |
| joinDt | LocalDateTime | Y | 가입일(회원 등록일시). 계정 정보 표시 | MBR.REG_DT, MYP-040-13 |
| lastLgnDt | LocalDateTime | N | 최근 로그인. 첫 로그인 전이면 null | MBR.LAST_LGN_DT, MYP-040-13 |

#### MemberSnsAccountSummary (MemberResponse.snsAcntLinks)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsOffrCd | String | Y | SNS 제공자(USRG03) | SNS_ACNT_LINK.SNS_OFFR_CD, MYP-040-09 |

### AuthMeResponse
쓰는 API: GET /auth/me
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 로그인한 회원 번호 | MBR.MBR_SN |
| mbrAuthrtCd | String | Y | USRG02 공통코드. 관리자 메뉴 표시용 | MBR.MBR_AUTHRT_CD |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| atchFileSn | Long | N | 미등록이면 응답에서 빠짐 → 기본 이미지 | MBR.ATCH_FILE_SN, COM-002-15 |
| maskedAcntEml | String | Y | 마스킹한 계정 이메일(`jin****@gmail.com`, @ 앞 3자 이하면 첫 글자만) | MBR.ACNT_EML_ENC, COM-005-02 |

### InternalAuthCheckResponse
쓰는 API: GET /internal/auth/check
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 쿠키의 회원 번호(인증 필터가 판정) | MBR.MBR_SN |
| mbrAuthrtCd | String | Y | USRG02 공통코드 | MBR.MBR_AUTHRT_CD |

### MemberUpdateRequest
쓰는 API: PATCH /members/me
`multipart/form-data`(issues 5번)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mblTelno | String | N | 공백·하이픈 제거, +82를 국내 표기로 통일한 뒤 형식 검사. 다른 회원과 중복 불가(저장 시 서버 재검사) | MBR.MBL_TELNO_ENC, MYP-040-05·11 |
| profilePhoto | MultipartFile | N | 새 프로필 사진. 형식·용량 규칙은 명세에 없음 | MBR.ATCH_FILE_SN, MYP-040-02·08 |
| profilePhotoReset | Boolean | N | `true`면 기본 이미지로 되돌림(`profilePhoto`와 함께 보내면 400) | MBR.ATCH_FILE_SN, MYP-040-02 |

### MemberWithdrawRequest
쓰는 API: POST /members/me/withdraw
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| noticeConfirmed | Boolean | Y | 초안 그대로. true여야 실행 | ACC-020-03 |
| confirmPhrase | String | Y | 초안 그대로. "정말로 탈퇴하겠습니다."와 완전 일치(띄어쓰기·마침표·앞뒤 공백 포함). 원문을 저장하지 않는다 | ACC-020-20, ACC-020-14 |

### PasswordChangeRequest
쓰는 API: PATCH /members/me/password
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| currentPswd | String | Y | 현재 비밀번호. 미입력 불가, 형식 규칙은 적용하지 않는다. 화면설계서 6-2의 "현재 비밀번호" 칸 | MBR.PSWD_HASH (해시 전 원문), MYP-040-06 |
| pswd | String | Y | 새 비밀번호. 8자 이상, 문자와 숫자 조합(가입·재설정 규칙 준용). PATCH 규칙의 예외로 필수(issues 3) | MBR.PSWD_HASH (해시 전 원문), MYP-040-06, ACC-001-09, ACC-012-01 |
| pswdCnfrm | String | Y | `pswd`와 일치(재설정 규칙 준용). PATCH 규칙의 예외로 필수(issues 3) | MYP-040-06, ACC-012-02 |

### PasswordVerifyRequest
쓰는 API: POST /members/me/password-verification
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pswd | String | Y | 현재 비밀번호. 미입력 불가(로그인 비밀번호 미입력 규칙 준용). 형식 규칙은 적용하지 않는다 | MBR.PSWD_HASH (해시 전 원문), MYP-040-01, ACC-002-04 |

### SnsLoginRequest
쓰는 API: POST /auth/sns-login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsOffrCd | String | Y | 공통코드 USRG03(네이버·카카오·구글) | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-002-09~11 |
| code | String | Y | 제공자 인가 결과 코드. 프론트가 인가 창에서 받아 넘긴다(Spring 리다이렉트 흐름 아님, issues 로그인 2 결정). 로컬 가짜 제공자(`app.sns.client=fake`)는 `fake-이름` 형식 | ACC-002-09~11 |
| state | String | N | 네이버는 필수(토큰 교환에 필요). 서버는 확인하지 않고 그대로 넘긴다. 로그인 CSRF 확인은 프론트가 한다. `redirectUri`는 받지 않고 서버 설정에 고정한다 | ACC-002-09 |

### SnsLoginResponse
쓰는 API: POST /auth/sns-login
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| snsLoginRslt | String | Y | `LOGIN`(연결된 정상 회원, 로그인 쿠키가 함께 내려간다) / `SIGNUP_REQUIRED`(처음 보는 계정) / `EMAIL_DUPLICATE`(연결 없이 같은 이메일의 정상·정지 회원이 있음, 새 회원·연결을 만들지 않는다) / `RECOVERY_AVAILABLE`(연결된 또는 같은 이메일의 탈퇴 보관 회원, 이메일 복구 흐름으로 이어진다). 연결된 정지 회원은 403 `MEMBER_INACTIVE`. 제공자 실패는 400 `SNS_AUTH_FAILED`, 이메일을 받지 못하면 400 `SNS_EMAIL_NOT_PROVIDED` | ACC-002-09~11 |
| ncnmReguide | Boolean | N | 기존 회원 로그인일 때만. `LoginResponse`와 같은 기준 | MBR.NCNM_STNG_CMPTN_YN, MBR.LAST_LGN_DT, ACC-001-22 |
| mbrNcnm | String | N | 기존 회원 로그인일 때만. 재안내 모달 저장값 | MBR.MBR_NCNM, ACC-001-22 |
| snsOffrCd | String | N | 신규 가입 확인일 때 읽기 전용 표시 | SNS_ACNT_LINK.SNS_OFFR_CD, ACC-001-31 |
| acntEml | String | N | 신규 가입 확인·복구일 때 읽기 전용 표시(제공자가 준 이메일) | ACC-001-31, ACC-001-40 |
| mblTelno | String | N | 신규 가입 확인일 때 제공자가 준 번호. 미제공이면 null(직접 입력) | MBR.MBL_TELNO_ENC, ACC-001-29 |
| ageCnfrmd | Boolean | N | 신규 가입 확인일 때 외부 연령 확인이 검증됐는지. 지금은 세 제공자 모두 연령을 받아 오지 않아 항상 false이고, 화면은 만 14세 이상 약관(`AGE_14`) 체크로 받는다. true면 "확인 완료" 표시 | ACC-001-12, ACC-001-31 |
| signupTkn | String | N | 신규 가입 확인일 때 `POST /members`로 넘길 확인 토큰. 저장소 없이 암호화한 값(제공자·계정 ID·이메일·번호·연령 확인·만료), 유효시간 `app.signup.sns-token-valid-minutes`(기본 30분) | ACC-001-31 |

복구(`RECOVERY_AVAILABLE`)에서 SNS 인증을 본인확인으로 쓰는 `rcvryAuthrtTkn`은 두지 않는다. 복구 권한이 `EML_CERT` 행에 묶여 있어, 프론트가 `acntEml`로 기존 이메일 복구 인증번호 흐름을 이어 간다.

### TermsResponse
쓰는 API: GET /terms/{trmsSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | | TRMS.TRMS_SN |
| trmsFixIdntfKey | String | Y | | TRMS.TRMS_FIX_IDNTF_KEY |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID, ACC-001-13 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL |
| trmsCn | String | Y | 전문. 길이 때문에 자르지 않는다 | TRMS.TRMS_CN, ACC-001-13 |
| esntlAgreYn | boolean | Y | | TRMS.ESNTL_AGRE_YN |

### TermsSummary
쓰는 API: GET /terms (`MemberRecoveryResponse.missingTrms`에서도 씀)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| trmsSn | Long | Y | | TRMS.TRMS_SN |
| trmsFixIdntfKey | String | Y | 항목 구분. 내용보기 없는 연령 확인 항목 식별에도 쓴다 | TRMS.TRMS_FIX_IDNTF_KEY, ACC-001-13 |
| trmsVerId | String | Y | | TRMS.TRMS_VER_ID, ACC-001-12 |
| trmsTtl | String | Y | | TRMS.TRMS_TTL |
| esntlAgreYn | boolean | Y | | TRMS.ESNTL_AGRE_YN, ACC-001-12 |
| sortSeq | Integer | Y | | TRMS.SORT_SEQ |
