# YAY 핸드오프

## 지금 하는 것
SCR-MYP-040 회원정보 수정 · SNS 연동 실패 문구 — 끝남 (승인 2026-11-11 16:22, 검증 1회차 통과, 사람 확인 2026-11-11). 연동 API 제공자 실패가 400 `SNS_LINK_FAILED`, SNS 로그인은 그대로 `SNS_AUTH_FAILED`임을 Postman으로 확인했다. 기획과 달라진 것: 기존 `MemberSnsLinkServiceTest`의 제공자 실패 케이스가 `SNS_AUTH_FAILED`를 기대해 이번 결정과 부딪쳐서, 새 케이스를 더하지 않고 그 케이스의 이름·기대값을 `SNS_LINK_FAILED`로 바꿨다
- 기준: 마이페이지 기능명세서 FN-MYP-040-14 SNS 계정 연동(`POST /members/me/sns-links`). 2026-11-11 SJM 요청 "연동하다 실패해도 서버 메시지가 'SNS 로그인에 실패했습니다'로 와서 토스트에 '로그인'으로 나온다. 연동 API에서는 연동용 문구로 바꿔 달라". 원인: `MemberSnsLinkService.link`가 로그인과 같은 `snsOAuthClient.fetchProfile`을 부르고, 그 실패 `SNS_AUTH_FAILED`를 그대로 내보낸다
- 결정: `ErrorCode`에 `SNS_LINK_FAILED`(400, "SNS 계정 연동에 실패했습니다. 다시 시도해 주세요.")를 SNS 구획(`SNS_PROVIDER_ALREADY_LINKED` 아래)에 추가한다. 문구는 다른 오류처럼 "~습니다"체로 한다(SJM 제안 "실패했어요"에서 바꿈). `MemberSnsLinkService.link`에서만 `SNS_AUTH_FAILED`를 잡아 `SNS_LINK_FAILED`로 바꿔 던지고, 다른 오류(`BAD_REQUEST`·409 두 개 등)와 SNS 로그인 쪽은 그대로 둔다
- FN-MYP-040-14: 바꿀 파일 `global/error/ErrorCode.java`(한 줄), `member/service/MemberSnsLinkService.java`, `src/test/java/com/nyangmeong/member/service/MemberSnsLinkServiceTest.java`(제공자 실패 시 `SNS_LINK_FAILED` 케이스 추가, 기존 케이스는 고치지 않음), `docs/api/member.md`(연동 행의 "제공자 실패 400 SNS_AUTH_FAILED"를 `SNS_LINK_FAILED`로). 부를 API: `POST /members/me/sns-links`(YAY). 완료 조건 — 명령: `./gradlew build -q`. 화면: 가짜 SNS(`app.sns.client=fake`)로 로그인 회원이 `POST /api/members/me/sns-links` `{ "snsOffrCd": "USRC0009", "code": "bad" }`(fake- 접두어 없음) → 400, `code` `SNS_LINK_FAILED`, 메시지 "SNS 계정 연동에 실패했습니다. 다시 시도해 주세요.". 같은 잘못된 코드로 `POST /api/auth/sns-login`은 그대로 400 `SNS_AUTH_FAILED`. 정상 연동 200·409 두 경우는 그대로. 프론트 연결: 없음(SJM 화면이 서버 메시지를 그대로 토스트로 쓴다. 병합 뒤 SJM에게 새 코드만 알린다)
- 하지 않는 것: SNS 로그인·가입 오류 문구 변경, 프론트 코드
- 새 의존성: 없음
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |

## 다음에 할 것

- SJM 알림: develop에 병합되면 연동 API 제공자 실패 코드가 `SNS_LINK_FAILED`("SNS 계정 연동에 실패했습니다. 다시 시도해 주세요.")로 바뀌었다고 알린다. 화면이 서버 메시지를 그대로 쓰면 프론트는 바꿀 것 없음
- 연결 남음: FN-MYP-040-14 (프론트 연결 회원정보 수정 SNS 계정 연동 성공 토스트·"연동됨" 전환·409 `SNS_ACCOUNT_LINKED_TO_OTHER`·`SNS_PROVIDER_ALREADY_LINKED` 서버 문구 토스트, 화면 SJM, 프론트 저장소에서 /start). 화면 담당이 SJM이라 프론트 YAY 마일스톤에 절을 넣지 않았다. develop에 병합되면 SJM에게 알린다
- 팀 확인(SNS, 2026-11-11): 네이버 앱이 아직 "개발 중"이라 등록한 테스터 아이디만 로그인된다(배포 뒤에도 같음). 개발자센터 멤버관리에 테스터 ID를 넣거나 검수 요청으로 서비스 적용 상태로 바꾼다. 카카오·구글 앱 상태도 같이 확인한다
- 연결 남음: 로그인 유지 (프론트 연결 `api/axios.js` 재발급 401 뒤 로그인 화면으로 보내기 전에 원래 요청 한 번 재시도, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-002 로그인 (이메일) · 재발급 실패 뒤 원래 요청 재시도" 절을 넣었다
- 알림(팀): access 쿠키가 이제 24시간 남는다(토큰 만료는 30분 그대로). 30분 뒤 요청은 401 `TOKEN_EXPIRED`라 프론트가 재발급한다
- 팀 공유(설정, 2026-11-06): 디스코드 `application.properties`와 배포 서버에서 `app.email-cert.fail-limit=1`로 바꾸고 `app.mail.from-name` 줄을 지운다(Spring Boot가 .properties를 ISO-8859-1로 읽어 보낸 사람 한글이 깨짐, 기본값 "냥멍케어"는 코드에 있다). `app.news.query`도 같은 이유로 한글이 깨지니 추가한 담당에게 알린다
- 연결 남음: FN-ACC-001-04·06, FN-ACC-012-03 (프론트 연결 인증번호 429면 확인 버튼 막고 재발송 안내, 도메인 400 문구와 "메일이 안 오면 주소·스팸함 확인" 안내, 재설정 새 비밀번호 칸의 `PASSWORD_SAME_AS_CURRENT`, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-001 회원가입 (이메일) · 인증번호 1회·도메인 오류·비밀번호 재사용 안내" 절을 넣었다
- 연결 남음: FN-MYP-040-06 (프론트 연결 회원정보 수정 비밀번호 변경의 `PASSWORD_SAME_AS_CURRENT`를 새 비밀번호 칸 아래 서버 문구로, 화면 SJM). 2026-11-09 SJM 답: 지금은 "잠시 후 다시 시도해 주세요"로 떠서 SJM이 고친다
- 알림(팀): 인증번호는 이제 한 번 틀리면 무효(가입·비밀번호 찾기·복구 모두)
- 연결 남음: FN-MAN-001·003·004·005·006·007·009 (프론트 연결 메인 히어로 캐릭터·반려동물 탭·프로필 요약·건강 지표 위젯 카드와 순서 저장·토탈 케어리스트, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-MAN-001 메인페이지 · 위젯 순서·건강 대시보드 API 연결" 절을 넣었다. API: 위젯 순서 YAY, 반려동물·캐릭터 SJM·LJS, 건강 지표·케어리스트 JAJ
- 연결 남음: FN-ACC-020-02·03·14·15·16·20, FN-AIM-011-19, FN-MYP-002-08 (프론트 연결 탈퇴 안내·문구 입력 모달·실행·실패/결과 확인·완료 화면, 마이페이지·AI 매니저 탈퇴 진입, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-020 회원탈퇴 · 탈퇴 API 연결" 절을 넣었다. 마이페이지 진입 버튼 화면은 SJM(SCR-MYP-040·002)
- 팀 확인(명세 FN-ACC-020-02·04, 팀장과 정함): 탈퇴 회원의 게시물·댓글·포인트와 남은 공동양육자의 조회 범위는 정책 미정이라 손대지 않았다. 2026-11-09 제안: 글·댓글은 남기고 작성자를 "탈퇴한 회원"으로, 포인트는 소멸(탈퇴 안내에 미리 고지)
- 연결 남음: FN-ACC-001-04·41, 비밀번호 찾기 발송 (프론트 연결 이메일 단계의 502 `MAIL_SEND_FAILED` 안내, 회원가입 문구 "인증번호를 보내지 못했습니다…"만 서버 문구로 맞추기, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-001 회원가입 (이메일) · 메일 발송 실패(502) 안내" 절을 넣었다
- 팀 메일 계정(2026-11-05 정함): `nyangmeongcare@gmail.com`. 로컬에서 Gmail SMTP(`smtp.gmail.com:587`, 앱 비밀번호) 실제 발송 확인함. 설정 공유·배포 `smtp`·개인 앱 비밀번호 삭제·배포 메일 설정은 2026-11-06 끝남(사람)
- 알림(LJS): `app.mail.client=smtp`를 켜면 케어 일정 알림 메일(`CareScheduleAlarmService`)도 실제로 나간다. 그쪽은 `MailSender`를 직접 불러 동작은 바뀌지 않았다
- 알림(프론트, 2026-11-05 확인): 비밀번호 재설정 API(`POST /email-certifications/{emlCertSn}/password-reset`)는 이미 develop에 있다. 프론트 SCR-ACC-012 연결은 기다릴 것 없음
- 연결 남음: FN-ACC-002-09·10·11, FN-ACC-001-31·32 (프론트 연결 SNS 버튼·콜백 화면과 SNS 가입 확인 화면, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-002 SNS 로그인 · SNS 로그인·가입 API 연결" 절을 넣었다
- 실제 제공자 키 확인 끝남(2026-11-04): 네이버·카카오·구글 모두 실제 인가 코드로 `SIGNUP_REQUIRED`와 실제 이메일을 받았다. 네이버 번호는 정규화돼 왔고, 카카오는 번호가 오지 않았다(동의항목에 전화번호 없음), 구글은 번호 없음·`ageCnfrmd=false`. redirect-uri는 `http://localhost:5173/login/sns/{naver|kakao|google}/callback`(콘솔 등록, 프론트 콜백 화면도 이 경로로)
- 남은 일(SNS, YAY): ① 연령 확인: 추가-5로 세 제공자 모두 `ageCnfrmd=false`(화면은 `AGE_14` 체크). "확인 완료" 표시를 쓰지 않게 된 것을 명세 담당(sannabi1)에게 알린다 ② 카카오 전화번호는 동의항목으로 받지 않기로 했다(비즈 앱 권한 필요). 로그인·회원가입 기능명세서 v0.9(2026-11-04)에서 FN-ACC-002-10을 "SNS 가입 확인 화면 직접 입력·중복확인(구글과 같음)"으로 고쳤다. 화면설계서 v0.11의 같은 문구(화면 01·22의 카카오 4번)도 고쳐야 한다. 코드는 그대로(번호가 없으면 `mblTelno` null). 기능명세서 v0.9는 아직 DMS에 올리지 않았다(문서 올린 사람이 sannabi1이라 새 판이 거절될 수 있음)
- SNS redirect-uri(추가-1, 2026-11-06): `redirect-uri`를 비우면 `app.front-base-url` + `/login/sns/{제공자}/callback`. 로컬 실제 로그인 확인, 배포 설정(프론트 주소·CORS·redirect-uri 비움)과 세 콘솔 배포 콜백 등록 끝남(사람). 프론트 인가 창의 `redirect_uri`를 환경변수로 바꾸는 것은 프론트 저장소에서
- 알림(팀, 설정): `app.sns.client`가 없으면 이제 실제 호출이다. 로컬에서 키 없이 확인하려면 `app.sns.client=fake`를 직접 둔다. 배포 서버에는 fake를 두지 않는다
- 이메일 로그인 점검(2026-11-04, 버그 없음) 메모: ① 배포 쿠키 설정(`secure`·같은 사이트)은 2026-11-06 끝남 ② 만료·폐기된 `RFRSH_TKN` 행을 지우는 배치가 없다(로그인·재발급마다 쌓임)
- 참고(SNS 가입): 가입 응답의 `joinDt`는 첫 응답에서 null이다(등록 직후 다시 읽지 않음, 이메일 가입도 같다). 화면에서 쓰게 되면 고친다
- 연결 남음: FN-MYP-040-01·06 (프론트 연결 회원정보 수정 6-1 본인 확인·6-2 비밀번호 [변경] 저장, 화면 SJM, 프론트 저장소에서 /start). 화면 담당이 SJM이라 프론트 YAY 마일스톤에 절을 넣지 않았다. SJM 프론트 마일스톤에 넣을 절을 SJM에게 전한다: "## SCR-MYP-040 회원정보 수정 · 본인 확인·비밀번호 변경 API 연결 — 대기 / 기능: 명세 기능 ID 추가 없음. 걸치는 기능: FN-MYP-040-01·06 / 기다리는 것: 백엔드 `POST /api/members/me/password-verification`, `PATCH /api/members/me/password`(YAY, 2026-11-03 완료) / 완료 조건: 6-1 [확인]은 `{pswd}`, 200이면 6-2, 400 `CURRENT_PASSWORD_MISMATCH`면 입력란 아래 문구. 6-2 비밀번호 [저장]은 `{currentPswd, pswd, pswdCnfrm}`, 400 `CURRENT_PASSWORD_MISMATCH`는 현재 비밀번호 칸, `PASSWORD_MISMATCH`는 확인 칸, `INVALID_INPUT_VALUE`(`pswd`)는 새 비밀번호 칸. 200이면 로그인 유지(새 쿠키), 다른 기기만 로그아웃"
- 알림(SJM): MYP-040 본인 확인은 서버 상태 없이 200/400만 준다. `MemberController.updateMe`의 TODO(세션 10분 제안)는 이번 YAY 변경에서 지웠다. 비밀번호 변경 요청에는 `currentPswd`가 들어간다
- 연결 남음: FN-HOS-001-01~32·FN-PLC-001~004-01 (프론트 연결 `features/place/` 검색·재검색·정렬·무한 스크롤·즐겨찾기 고정 영역·별 버튼·카카오맵 상세 새 탭, 프론트 저장소에서 /start). 프론트 마일스톤에 "SCR-HOS-001 플레이스 · 장소 검색·즐겨찾기 API 연결" 절을 넣었다. `kakaoUrlPath`가 `null`이면 상세 버튼을 숨기거나 `https://map.kakao.com/link/map/{plcConm},{plcLat},{plcLot}`, `rprsTelno`가 `null`이면 전화 줄을 숨긴다
- 연결 남음(프론트 저장소에서 /start): 회원가입 `features/member/signup/`(닉네임 요청은 `withCredentials`, 가입 요청에 `certNo`, 재전송 제한은 400 `EMAIL_CERT_RESEND_NOT_YET`의 `data.resendAvailableDt`), 비밀번호 찾기 `features/member/password/`(`certRslt`로 화면 분기, 다시 입력은 `invalidate`), 비밀번호 재설정 FN-ACC-012-01~06(`emlCertSn`·`pswdInitTkn`을 메모리로, 만료·400 `PASSWORD_RESET_NOT_AVAILABLE`이면 만료 화면, 자동 로그인 없음), 재가입 복구 FN-ACC-001-40~46(409 `MEMBER_RECOVERY_AVAILABLE` → 복구 목적 발송 → verify → `recovery-info` → `recovery`, 완료 뒤 자동 로그인 없음)
- 알림(KSJ, issues 13): 커뮤니티 지도 선택(CMU-002-11·004-10·005-05)은 지금 `GET /places`를 카테고리와 함께 쓰는 것까지다. 일반 주소·좌표 검색은 만들지 않았다
- 공용 DB 정리(2026-11-09 팀 답: 테스트 행이면 지워도 됨): `LGN_ATMPT_LMT` 번호 7·`probe@example.com` 행은 이미 없다. 남은 4행 확인함: 12는 시드 회원 200(행복한냥집사), 16·17·20은 회원이 없는 이메일의 실패 기록(1~3회, 보안코드 요구 없음). `DELETE FROM LGN_ATMPT_LMT WHERE LGN_ATMPT_LMT_SN IN (12,16,17,20);`은 AI 도구 권한에서 막혀 사람이 직접 실행한다
- 작은 정리: `member/dto/EmailCertificationCreateRequest` 클래스 주석이 아직 "가입·비밀번호 찾기 목적만 받는다"이다(복구도 받음). 다음에 이 파일을 고칠 때 같이 고친다. `DevTokenController` 삭제는 팀 공지 뒤
