# YAY 핸드오프

## 지금 하는 것

SCR-ACC-020 회원탈퇴 — 끝남 (승인 2026-11-05 12:39, 검증 1회차 확인 못 함 있음(연결 남음만), 사람 확인 2026-11-05). `POST /api/members/me/withdraw`로 상태·탈퇴 시각·이력 반영과 세션 전체 폐기·쿠키 삭제를 로컬에서 확인했다. 기획과 달라진 것: 탈퇴 뒤 남은 쿠키는 403 `MEMBER_INACTIVE`가 아니라 401(아래 결과 확인 결정)
- 기준: 로그인·회원가입_기능명세서_v0.8_20261101 FN-ACC-020-01~20, 화면설계서 v0.11 화면 30~35. API 초안 `POST /members/me/withdraw`(`MemberWithdrawRequest {noticeConfirmed, confirmPhrase}`, 응답 data 없음)를 그대로 쓴다. 명세 기능 ID 추가 없음
- 이미 있는 것(바꾸지 않음): 요청마다 회원 상태를 보는 `JwtAuthenticationFilter`(탈퇴 뒤 남은 access 토큰은 403 `MEMBER_INACTIVE`), 탈퇴 회원 로그인 403 `MEMBER_WITHDRAWN`(`LoginService`·`SnsLoginProcessor`), 30일 안 복구(`MemberService.updateRecover`), 30일 뒤 파기 배치(`MemberPurgeJob`, 매일 03:30), 비밀번호 재설정 저장은 탈퇴 회원이면 0행(`updatePassword`), `ErrorCode.WITHDRAW_PHRASE_MISMATCH`. 파일을 지우는 배치는 없어 FN-ACC-020-14 "복구 대상 파일만 먼저 지우지 않게"는 할 일 없음
- 결정(탈퇴 처리): 회원 행을 지우지 않고 `MBR_STTS_CD` 정상 → 탈퇴(`USRC0003`), `WHDWL_DT = NOW()`. `WHERE MBR_STTS_CD = 정상`이라 0행이면(이미 탈퇴·정지) `MEMBER_INACTIVE`. 상태 이력 `insertStatusHistory(정상 → 탈퇴, "회원 탈퇴")`. 모든 refresh 세션 폐기(`LoginSessionManager.revokeAll`)와 요청자 쿠키 삭제(controller에서 `revoke(request, response)`, 로그아웃과 같음). 반려동물·건강기록·캐릭터·공동양육 연결·SNS 연동·이메일/번호 암호문과 HMAC은 건드리지 않는다(복구 때 그대로 이어짐). 양육권 자동 이전 없음
- 결정(검증): `noticeConfirmed`는 `@NotNull @AssertTrue` → 400 `INVALID_INPUT_VALUE`(필드 `noticeConfirmed`). `confirmPhrase`는 `@NotNull`, 그 밖에는 서비스가 `"정말로 탈퇴하겠습니다."`와 `equals`(trim 없음)로 비교해 다르면 400 `WITHDRAW_PHRASE_MISMATCH`. 문구 원문은 저장·로그하지 않는다. 이메일 인증·비밀번호 재입력·SNS 재인증 없음(FN-ACC-020-04)
- 결정(결과 확인, FN-ACC-020-14·15·16, 2026-11-05 14:16 사람 확인 중 고침 "추천안대로"): 새 API를 만들지 않는다. 필터(`JwtAuthenticationFilter`)는 회원 상태보다 세션을 먼저 보므로, 탈퇴로 세션이 모두 폐기된 뒤 남은 access 쿠키는 403 `MEMBER_INACTIVE`가 아니라 401 `UNAUTHORIZED`다. 응답이 끊기면 프론트가 `GET /auth/me`(LJS, 있음)로 확인한다: 200이면 탈퇴 안 됨(안내부터 다시), 401이면 로그인 방법 선택으로 보내고(FN-ACC-020-16), 그 계정으로 로그인하면 403 `MEMBER_WITHDRAWN`으로 탈퇴가 확인된다. 같은 요청을 다시 보내도 401이라 두 번 탈퇴되지 않는다. 한 트랜잭션이라 일부만 반영된 상태가 남지 않는다. 필터 순서는 바꾸지 않는다(공용 보안 필터)
- 결정(보관 기간 지난 이메일, 핸드오프 메모): 파기 배치가 하루 한 번 이메일 HMAC을 `PURGED…`로 바꿔 새 가입이 풀린다. 기간이 지나고 배치 전까지(최대 하루)만 가입 409·복구 400이 된다. 이번에 바꾸지 않는다
- FN-ACC-020-02 반려동물·공동양육 영향 확인: 바꿀 파일 없음. 부를 API `GET /pets`(SJM, 있음, `brupRoleCd`로 주양육·공동양육 수를 센다). 완료 조건 — 화면: 로그인 회원으로 `GET /api/pets` 200, 각 항목에 `brupRoleCd`. 프론트 연결: 탈퇴 안내 2번 "주양육 N마리 · 공동양육 N마리" (프론트 저장소에서 /start)
- FN-ACC-020-03·14·20, FN-AIM-011-19, FN-MYP-002-08 회원탈퇴 실행: 바꿀 파일 `member/dto/MemberWithdrawRequest`(새로 만듦, record), `member/service/MemberWithdrawService`(새로 만듦, `@Transactional withdraw(mbrSn, req)`), `member/mapper/MemberMapper`·`mapper/member/MemberMapper.xml`(`updateWithdraw`), `member/controller/MemberController`(`POST /me/withdraw`). 부를 API: `POST /members/me/withdraw`(YAY). 완료 조건 — 명령: `MemberWithdrawServiceTest`(문구 일치면 `updateWithdraw`·상태 이력·`revokeAll` 호출, 문구에 공백·마침표가 하나라도 다르면 `WITHDRAW_PHRASE_MISMATCH`이고 아무것도 바꾸지 않음, `updateWithdraw` 0행이면 `MEMBER_INACTIVE`), `MemberControllerTest` 또는 같은 수준의 웹 테스트(`noticeConfirmed=false`·null이면 400 `INVALID_INPUT_VALUE`, 성공이면 200과 access·refresh 삭제 쿠키), `./gradlew build -q`. 화면: Postman으로 로그인 → `POST /api/members/me/withdraw {noticeConfirmed:true, confirmPhrase:"정말로 탈퇴하겠습니다"}`(마침표 없음) 400 `WITHDRAW_PHRASE_MISMATCH` → 정확한 문구로 200 → 같은 쿠키로 `GET /api/auth/me` 401 `UNAUTHORIZED` → `POST /api/auth/refresh` 401 → 다시 로그인 403 `MEMBER_WITHDRAWN` → DB `MBR.MBR_STTS_CD='USRC0003'`, `WHDWL_DT` 채워짐, `MBR_STTS_HSTRY` 한 줄, `RFRSH_TKN` 모두 폐기. 프론트 연결: 탈퇴 안내 체크·문구 입력 모달·실행·완료 화면, 마이페이지·AI 매니저 탈퇴 진입 (프론트 저장소에서 /start)
- FN-ACC-020-15·16 실패·결과 확인: 바꿀 파일 없음(위 결정). 완료 조건 — 화면: 위 흐름에서 탈퇴 뒤 `GET /api/auth/me` 401 `UNAUTHORIZED`(같은 탈퇴 요청 재전송도 401), 탈퇴 전 200, 탈퇴 뒤 로그인 403 `MEMBER_WITHDRAWN`. 프론트 연결: 실패·확인 중 안내와 결과 확인 버튼 (프론트 저장소에서 /start)
- 하지 않는 것: 게시물·댓글·포인트 처리, 남은 공동양육자의 조회 범위(명세가 "도메인 정책 확인 필요"), 공동양육 연결 해제, 탈퇴 사유 수집, 동의 증적 저장, 파기 배치 시각 변경
- 새 의존성: 없음
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |

## 다음에 할 것

- 연결 남음: FN-ACC-020-02·03·14·15·16·20, FN-AIM-011-19, FN-MYP-002-08 (프론트 연결 탈퇴 안내·문구 입력 모달·실행·실패/결과 확인·완료 화면, 마이페이지·AI 매니저 탈퇴 진입, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-020 회원탈퇴 · 탈퇴 API 연결" 절을 넣었다. 마이페이지 진입 버튼 화면은 SJM(SCR-MYP-040·002)
- 팀 확인(명세 FN-ACC-020-02·04): 탈퇴 회원의 게시물·댓글·포인트와 남은 공동양육자의 조회 범위는 정책 미정이라 손대지 않았다
- 연결 남음: FN-ACC-001-04·41, 비밀번호 찾기 발송 (프론트 연결 이메일 단계의 502 `MAIL_SEND_FAILED` 안내, 회원가입 문구 "인증번호를 보내지 못했습니다…"만 서버 문구로 맞추기, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-001 회원가입 (이메일) · 메일 발송 실패(502) 안내" 절을 넣었다
- 팀 메일 계정(2026-11-05 정함): `nyangmeongcare@gmail.com`. 로컬에서 Gmail SMTP(`smtp.gmail.com:587`, 앱 비밀번호) 실제 발송 확인함(3통 발송, 응답 3.8~7.3초). 남은 일: 디스코드 설정 파일 메일 칸을 채워 첨부로 공유(팀원 로컬은 `app.mail.client=log` 권장) → 배포 설정에 `smtp` → 개인 네이버 앱 비밀번호 삭제
- 배포 전(메일 스팸): 새 Gmail 계정으로 보낸 인증 메일이 받는 쪽에서 모두 스팸함으로 갔다(발송 이력 없음·같은 제목 짧은 본문 반복·도메인 인증 없음). 개발·시연은 받는 계정에서 "스팸 아님"과 주소록 추가로 넘긴다. 배포 전에 도메인과 발송 서비스(SES·SendGrid·네이버 클라우드 Outbound Mailer 등, SPF·DKIM)를 정하고, 필요하면 메일 본문(HTML·안내 문구)을 새로 기획한다. 코드는 `spring.mail.*` 설정만 바꾸면 된다
- 알림(SJM): 공동양육 초대 메일도 `CertificationMailEvent`라 발송 실패 시 초대가 저장되지 않고 502 `MAIL_SEND_FAILED`가 된다(전에는 저장되고 메일만 빠짐)
- 알림(LJS): `app.mail.client=smtp`를 켜면 케어 일정 알림 메일(`CareScheduleAlarmService`)도 실제로 나간다. 그쪽은 `MailSender`를 직접 불러 동작은 바뀌지 않았다
- 알림(프론트, 2026-11-05 확인): 비밀번호 재설정 API(`POST /email-certifications/{emlCertSn}/password-reset`)는 이미 develop에 있다. 프론트 SCR-ACC-012 연결은 기다릴 것 없음
- 연결 남음: FN-ACC-002-09·10·11, FN-ACC-001-31·32 (프론트 연결 SNS 버튼·콜백 화면과 SNS 가입 확인 화면, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-002 SNS 로그인 · SNS 로그인·가입 API 연결" 절을 넣었다
- 실제 제공자 키 확인 끝남(2026-11-04): 네이버·카카오·구글 모두 실제 인가 코드로 `SIGNUP_REQUIRED`와 실제 이메일을 받았다. 네이버 번호는 정규화돼 왔고, 카카오는 번호가 오지 않았다(동의항목에 전화번호 없음), 구글은 번호 없음·`ageCnfrmd=false`. redirect-uri는 `http://localhost:5173/login/sns/{naver|kakao|google}/callback`(콘솔 등록, 프론트 콜백 화면도 이 경로로)
- 남은 일(SNS, YAY): ① 연령 확인: 추가-5로 세 제공자 모두 `ageCnfrmd=false`(화면은 `AGE_14` 체크). "확인 완료" 표시를 쓰지 않게 된 것을 명세 담당(sannabi1)에게 알린다 ② 카카오 전화번호는 동의항목으로 받지 않기로 했다(비즈 앱 권한 필요). 로그인·회원가입 기능명세서 v0.9(2026-11-04)에서 FN-ACC-002-10을 "SNS 가입 확인 화면 직접 입력·중복확인(구글과 같음)"으로 고쳤다. 화면설계서 v0.11의 같은 문구(화면 01·22의 카카오 4번)도 고쳐야 한다. 코드는 그대로(번호가 없으면 `mblTelno` null). 기능명세서 v0.9는 아직 DMS에 올리지 않았다(문서 올린 사람이 sannabi1이라 새 판이 거절될 수 있음)
- 알림(팀, 설정): `app.sns.client`가 없으면 이제 실제 호출이다. 로컬에서 키 없이 확인하려면 `app.sns.client=fake`를 직접 둔다. 배포 서버에는 fake를 두지 않는다
- 이메일 로그인 점검(2026-11-04, 버그 없음) 메모: ① 탭 여러 개가 동시에 `/auth/refresh`를 부르면 진 쪽 응답의 쿠키 삭제가 늦게 도착해 로그아웃될 수 있다(탭 하나 안은 프론트 `api/axios.js`가 묶어 막는다) ② 배포 때 `app.cookie.secure=true`, 프론트와 API는 같은 사이트로(SameSite=Lax) ③ 만료·폐기된 `RFRSH_TKN` 행을 지우는 배치가 없다(로그인·재발급마다 쌓임)
- 참고(SNS 가입): 가입 응답의 `joinDt`는 첫 응답에서 null이다(등록 직후 다시 읽지 않음, 이메일 가입도 같다). 화면에서 쓰게 되면 고친다
- 연결 남음: FN-MYP-040-01·06 (프론트 연결 회원정보 수정 6-1 본인 확인·6-2 비밀번호 [변경] 저장, 화면 SJM, 프론트 저장소에서 /start). 화면 담당이 SJM이라 프론트 YAY 마일스톤에 절을 넣지 않았다. SJM 프론트 마일스톤에 넣을 절을 SJM에게 전한다: "## SCR-MYP-040 회원정보 수정 · 본인 확인·비밀번호 변경 API 연결 — 대기 / 기능: 명세 기능 ID 추가 없음. 걸치는 기능: FN-MYP-040-01·06 / 기다리는 것: 백엔드 `POST /api/members/me/password-verification`, `PATCH /api/members/me/password`(YAY, 2026-11-03 완료) / 완료 조건: 6-1 [확인]은 `{pswd}`, 200이면 6-2, 400 `CURRENT_PASSWORD_MISMATCH`면 입력란 아래 문구. 6-2 비밀번호 [저장]은 `{currentPswd, pswd, pswdCnfrm}`, 400 `CURRENT_PASSWORD_MISMATCH`는 현재 비밀번호 칸, `PASSWORD_MISMATCH`는 확인 칸, `INVALID_INPUT_VALUE`(`pswd`)는 새 비밀번호 칸. 200이면 로그인 유지(새 쿠키), 다른 기기만 로그아웃"
- 알림(SJM): MYP-040 본인 확인은 서버 상태 없이 200/400만 준다. `MemberController.updateMe`의 TODO(세션 10분 제안)는 이번 YAY 변경에서 지웠다. 비밀번호 변경 요청에는 `currentPswd`가 들어간다
- 연결 남음: FN-HOS-001-01~32·FN-PLC-001~004-01 (프론트 연결 `features/place/` 검색·재검색·정렬·무한 스크롤·즐겨찾기 고정 영역·별 버튼·카카오맵 상세 새 탭, 프론트 저장소에서 /start). 프론트 마일스톤에 "SCR-HOS-001 플레이스 · 장소 검색·즐겨찾기 API 연결" 절을 넣었다. `kakaoUrlPath`가 `null`이면 상세 버튼을 숨기거나 `https://map.kakao.com/link/map/{plcConm},{plcLat},{plcLot}`, `rprsTelno`가 `null`이면 전화 줄을 숨긴다
- 연결 남음(프론트 저장소에서 /start): 회원가입 `features/member/signup/`(닉네임 요청은 `withCredentials`, 가입 요청에 `certNo`, 재전송 제한은 400 `EMAIL_CERT_RESEND_NOT_YET`의 `data.resendAvailableDt`), 비밀번호 찾기 `features/member/password/`(`certRslt`로 화면 분기, 다시 입력은 `invalidate`), 비밀번호 재설정 FN-ACC-012-01~06(`emlCertSn`·`pswdInitTkn`을 메모리로, 만료·400 `PASSWORD_RESET_NOT_AVAILABLE`이면 만료 화면, 자동 로그인 없음), 재가입 복구 FN-ACC-001-40~46(409 `MEMBER_RECOVERY_AVAILABLE` → 복구 목적 발송 → verify → `recovery-info` → `recovery`, 완료 뒤 자동 로그인 없음)
- 알림(KSJ, issues 13): 커뮤니티 지도 선택(CMU-002-11·004-10·005-05)은 지금 `GET /places`를 카테고리와 함께 쓰는 것까지다. 일반 주소·좌표 검색은 만들지 않았다
- 팀 확인(공용 DB): `LGN_ATMPT_LMT`의 테스트 행(번호 7, 출처 불명)과 2026-11-03 로그인 진단 중 생긴 `probe@example.com` 실패 행을 지워도 되는지
- 작은 정리: `member/dto/EmailCertificationCreateRequest` 클래스 주석이 아직 "가입·비밀번호 찾기 목적만 받는다"이다(복구도 받음). 다음에 이 파일을 고칠 때 같이 고친다. `DevTokenController` 삭제는 팀 공지 뒤
