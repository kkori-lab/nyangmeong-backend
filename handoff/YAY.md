# YAY 핸드오프

## 지금 하는 것

SCR-ACC-001 회원가입 (이메일) · 실제 메일 발송(SMTP) — 끝남 (승인 2026-11-05 10:22, 검증 1회차 통과, 사람 확인 2026-11-05). 네이버 SMTP(개인 계정 임시)로 실제 인증 메일 도착, 비밀번호가 틀리면 535 → 502 `MAIL_SEND_FAILED`가 연속으로 나와 재전송 제한에 걸리지 않음, 설정 없으면 로그 발송을 확인했다. 기획과 달라진 것: 설정 클래스 이름은 `MailSenderProperties`(Boot의 `MailProperties`와 헷갈리지 않게), 메일 제목은 기존 값 `[냥멍케어] 이메일 인증번호 안내`
- 기준: 로그인·회원가입_기능명세서_v0.8_20261101 (DMS) FN-ACC-001-04 "발송 실패는 공통 알림과 AI로 재시도를 안내한다. 실패를 발송 성공이나 가입 완료로 처리하지 않는다". 명세 기능 ID 추가 없음. 같은 발송기를 쓰는 곳: 가입·복구·비밀번호 찾기 인증번호(`EmailCertificationService`), 공동양육 초대 메일(SJM `CaregiverInvitationService`, `CertificationMailEvent`), 케어 일정 알림 메일(LJS `CareScheduleAlarmService`, `MailSender` 직접)
- 결정(발송기 고르기): `app.mail.client=log|smtp`. 줄이 없으면 `log`(지금의 `LoggingMailSender`). 설정을 받지 않은 팀원 로컬·테스트에서 실제 사람에게 메일이 나가지 않게 하려고 SNS(없으면 real)와 반대로 둔다. `smtp`는 배포와 사람 확인 때 직접 켠다
- 결정(SMTP 발송기, `global/mail`): `SmtpMailSender`가 Spring `JavaMailSender`로 보낸다. 본문은 지금처럼 일반 텍스트 UTF-8, 보낸 사람은 `app.mail.from`(주소)·`app.mail.from-name`(표시 이름, 기본 "냥멍케어"). 서버·계정은 Spring 표준 `spring.mail.host·port·username·password`, STARTTLS와 연결·읽기·쓰기 시간 제한 5000ms는 `spring.mail.properties.mail.smtp.*`. 실패는 `MailException`을 그대로 던진다(`MailSender` 계약 "실패 시 예외"). 로그는 성공 시 제목만(받는 주소는 남기지 않음, `docs/convention/common.md`)
- 결정(발송 실패, FN-ACC-001-04): `CertificationMailListener`를 커밋 뒤 발송·예외 무시(AFTER_COMMIT)에서 커밋 전 발송(BEFORE_COMMIT)으로 바꾼다. 발송이 실패하면 502 `MAIL_SEND_FAILED`를 던져 트랜잭션이 롤백되므로 인증 행이 남지 않는다. 그래서 응답이 성공으로 나가지 않고, 실패한 발송이 재전송 간격·횟수에 세어지지 않아 곧바로 다시 요청할 수 있다. 발송 동안 DB 연결을 잡고 있지만 시간 제한 5초로 막는다. 메일은 나갔는데 커밋이 실패하는 드문 경우는 받는 사람에게 쓸 수 없는 번호가 가는 것뿐이라 받아들인다
- 결정(영향): 공동양육 초대(SJM)도 같은 이벤트라 메일 실패 시 초대가 저장되지 않고 502가 된다(지금은 저장되고 메일만 빠짐). SJM에 알린다. 케어 일정 알림(LJS)은 `MailSender`를 직접 부르고 자체 실패 기록·재시도가 있어 바꾸지 않는다. `smtp`를 켜면 이 알림도 실제로 나간다
- 결정(오류 코드, `ErrorCode`): `MAIL_SEND_FAILED(BAD_GATEWAY, "메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.")`를 더한다. 외부 호출 실패를 502로 쓰는 `EXTERNAL_API_ERROR`와 같은 상태지만 화면이 메일 재시도를 안내하도록 따로 둔다
- 공유 파일(구현 전, `docs/convention/shared-files.md`): `application.properties.example`에 `app.mail.client=log`, `app.mail.from=`, `app.mail.from-name=냥멍케어`, `spring.mail.host=`, `spring.mail.port=587`, `spring.mail.username=`, `spring.mail.password=`, `spring.mail.properties.mail.smtp.auth=true`, `spring.mail.properties.mail.smtp.starttls.enable=true`, 시간 제한 세 줄(5000)을 더한다(실제 계정은 디스코드 파일). `ErrorCode`에 `MAIL_SEND_FAILED`. DB 변경은 없다
- FN-ACC-001-04 실제 발송·실패 안내: 바꿀 파일 `build.gradle`(의존성), `global/mail/MailProperties`(새로 만듦, `app.mail`), `global/mail/SmtpMailSender`(새로 만듦, `app.mail.client=smtp`일 때만), `global/mail/LoggingMailSender`(`app.mail.client=log`이거나 없을 때만), `global/mail/MailSender`(주석), `global/mail/CertificationMailListener`, `global/error/ErrorCode`, `application.properties.example`. 부를 API: 바뀌는 API 없음(`POST /email-certifications`가 실패 시 502를 줄 수 있게 됨, YAY). 완료 조건 — 명령: `SmtpMailSenderTest`(받는 주소·제목·UTF-8 본문·보낸 사람 주소와 표시 이름이 메시지에 들어감, `JavaMailSender` 실패면 예외가 그대로 나감), `MailSenderSelectionTest`(`ApplicationContextRunner`: 설정 없음·`log`면 `LoggingMailSender`만, `smtp`면 `SmtpMailSender`만), `CertificationMailListenerTest`(성공이면 그대로, 발송 예외면 `MAIL_SEND_FAILED`), `./gradlew build -q`. 화면: 지금 설정(`app.mail.client` 없음)으로 `POST /api/email-certifications` 200, 로그에 `[Mail] 가짜 발송`이 남음. 메일 계정을 받으면 사람이 `smtp`로 바꿔 받은편지함 도착(제목 `[냥멍케어] 이메일 인증`, 보낸 사람 냥멍케어)과, 비밀번호를 틀리게 두면 502 `MAIL_SEND_FAILED` → 곧바로 다시 요청해도 `EMAIL_CERT_RESEND_NOT_YET`이 아님을 확인한다. 프론트 연결: 가입·비밀번호 찾기·복구 이메일 단계의 502 `MAIL_SEND_FAILED` 안내 (프론트 저장소에서 /start)
- 하지 않는 것: HTML 메일 템플릿, 비동기 발송, 인증 메일 발송 이력 테이블, 보내는 계정 정하기(팀), 배포 서버 설정
- 새 의존성: `org.springframework.boot:spring-boot-starter-mail` (버전은 Spring Boot 4.1.1 BOM이 정함). 이유: SMTP 발송용 `JavaMailSender`
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |

## 다음에 할 것

- 연결 남음: FN-ACC-001-04·41, 비밀번호 찾기 발송 (프론트 연결 이메일 단계의 502 `MAIL_SEND_FAILED` 안내, 회원가입 문구 "인증번호를 보내지 못했습니다…"만 서버 문구로 맞추기, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-001 회원가입 (이메일) · 메일 발송 실패(502) 안내" 절을 넣었다
- 팀 결정(메일): 보내는 계정. 지금은 개인 네이버 계정으로 로컬에서만 확인했다(디스코드 공유 안 함). 팀 전용 계정(또는 SES 등 발송 서비스)을 정하면 `app.mail.from`·`spring.mail.*`을 디스코드 파일로 공유하고 배포 설정에 `app.mail.client=smtp`를 둔다
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
- ACC-020 기획 때: 보관 기간이 지난 탈퇴 이메일은 지금 가입(409 `MEMBER_RECOVERY_AVAILABLE`)도 복구(400 `MEMBER_RECOVERY_EXPIRED`)도 안 된다. 회원 행 파기 방식과 함께 정한다. 반려동물·공동양육 조회(SJM)가 나왔는지 먼저 본다
- 작은 정리: `member/dto/EmailCertificationCreateRequest` 클래스 주석이 아직 "가입·비밀번호 찾기 목적만 받는다"이다(복구도 받음). 다음에 이 파일을 고칠 때 같이 고친다. `DevTokenController` 삭제는 팀 공지 뒤
