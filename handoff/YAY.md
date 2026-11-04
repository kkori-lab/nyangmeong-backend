# YAY 핸드오프

## 지금 하는 것

SCR-MYP-040 비밀번호 확인·변경 — 끝남 (승인 2026-11-03 14:15, 검증 1회차 판정 "확인 못 함 있음"(내 것 없음, 연결 남음뿐), 사람 확인 2026-11-03). 로컬 DB를 v0.12+시드로 다시 만든 뒤 Postman으로 확인 200·`CURRENT_PASSWORD_MISMATCH`·빈 값 400, 변경의 세 거절(현재 비밀번호·확인값·형식), 변경 200 뒤 같은 기기 `/auth/me`·`/auth/refresh` 200, 다른 기기 refresh 401, 옛 비밀번호 로그인 실패·새 비밀번호 성공, 쿠키 없이 401, 되돌리기를 확인했다. 기획과 달라진 것: SJM의 `MemberController` TODO 주석(세션 10분 제안)은 SJM이 아니라 이번 변경에서 지웠다
- 기준: 마이페이지_기능명세서_v0.5_20261101, 마이페이지_화면설계서_v0.6_20261025 (DMS). FN-MYP-040-01·06. 화면은 SJM(회원정보 수정 6-1 본인 확인, 6-2 비밀번호 [변경] 펼침)
- 결정(확인 결과 넘기기, issues 4①②): 본인 확인은 서버에 상태를 남기지 않는다(세션·토큰 없음). `POST /members/me/password-verification`은 맞으면 200(`data` 없음), 틀리면 400이다. 화면은 통과하면 6-2로 바꾸기만 한다. 비밀번호 변경은 화면설계서대로 현재 비밀번호를 다시 받아 그 요청 안에서 검사한다. 6-2의 닉네임·휴대폰·사진 수정(SJM `PATCH /members/me`)은 이미 로그인 쿠키로 보호되므로 확인 시각을 따로 검사하지 않는다. `MemberController`의 SJM TODO(세션 10분 제안)는 이 결정으로 닫는다고 SJM에 알린다(그 주석은 SJM이 지운다)
- 결정(초안 수정, `PasswordChangeRequest`): 화면설계서 6-2의 비밀번호 펼침에 "현재 비밀번호" 칸이 있는데 초안에는 없다. `currentPswd`(String, 필수, 형식 검사 없음)를 더한다. `pswd`·`pswdCnfrm`도 실제로 필수이므로 issues 3번은 이 API를 예외(필수 Y)로 두는 것으로 적는다. 검증 규칙은 재설정과 같다(`^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,64}$`, 다르면 400 `PASSWORD_MISMATCH`). 원문이 로그에 남지 않게 `toString`을 막는다
- 결정(오류 코드, `ErrorCode`): 현재 비밀번호가 틀리면 400 `CURRENT_PASSWORD_MISMATCH`("현재 비밀번호가 일치하지 않습니다.")를 새로 만든다. 기존 `PASSWORD_MISMATCH`는 새 비밀번호와 확인값이 다를 때 쓰여 화면이 어느 칸에 문구를 띄울지 가를 수 없어서다. 확인·변경 둘 다 이 코드를 쓴다
- 결정(SNS 전용 회원, issues 4③): `PSWD_HASH`가 없으면 두 API 모두 400 `SNS_ONLY_MEMBER`다. 화면은 SNS 회원에게 6-1과 비밀번호 행을 보이지 않으므로 정상 흐름에서는 나오지 않는다. 판단은 `GET /members/me`(SJM)의 가입 방식으로 화면이 한다
- 결정(변경 뒤 로그인, issues 4④): 변경하면 이 회원의 refresh 세션을 모두 폐기한다(`LoginSessionManager.revokeAll`, 재설정과 같음). 그다음 컨트롤러가 요청한 기기에 새 세션을 발급한다(`issue`). 그래서 다른 기기만 로그아웃된다. 새 비밀번호가 현재와 같아도 막지 않는다(명세에 규칙 없음)
- 결정(회원 상태): 정지·휴면 등 비활성 회원은 `MEMBER_INACTIVE`(`updateNickname`과 같음)를 받는다. 갱신 행이 0이면(그 사이 탈퇴) `MEMBER_NOT_FOUND`다. 재설정용 `changePassword`는 `PASSWORD_RESET_NOT_AVAILABLE`을 던지므로 쓰지 않고, `memberMapper.updatePassword`를 직접 부른다
- 공유 파일(구현 전, `docs/convention/shared-files.md`): `ErrorCode`에 `CURRENT_PASSWORD_MISMATCH`를 더한다. 설정 키·DB 변경은 없다
- 초안 수정(구현 전): `docs/api/member.md`의 `PasswordChangeRequest`에 `currentPswd` 행을 더하고, `pswd`·`pswdCnfrm`의 필수를 Y로 고친다. 두 API의 설명에 오류(400 `CURRENT_PASSWORD_MISMATCH`·`SNS_ONLY_MEMBER`)와 "변경 시 다른 기기 로그아웃"을 적는다. `docs/api/issues.md` 3·4번에 결정을 단다
- FN-MYP-040-01 본인 확인: 바꿀 파일 `member/dto/PasswordVerifyRequest`(새로 만듦, `pswd` `@NotBlank`·`toString` 막음), `member/service/MemberPasswordService`(새로 만듦, `verify(mbrSn, req)`: 회원 조회 → 비활성 확인 → 해시 없으면 `SNS_ONLY_MEMBER` → `PasswordEncoder.matches` 실패면 `CURRENT_PASSWORD_MISMATCH`), `member/controller/MemberController`(`POST /me/password-verification`). 부를 API: `POST /members/me/password-verification` (YAY). 완료 조건 — 명령: `MemberPasswordServiceTest`(맞으면 통과, 틀리면 `CURRENT_PASSWORD_MISMATCH`, SNS 전용 `SNS_ONLY_MEMBER`, 비활성 `MEMBER_INACTIVE`), `MemberControllerTest`(`pswd` 빠지면 400, 쿠키 없으면 401). 화면: Swagger에서 로그인 뒤 맞는 비밀번호 200, 틀린 비밀번호 400 `CURRENT_PASSWORD_MISMATCH`. 프론트 연결: 회원정보 수정 6-1 본인 확인 (SJM 화면, 프론트 저장소에서 /start)
- FN-MYP-040-06 비밀번호 변경: 바꿀 파일 `member/dto/PasswordChangeRequest`(새로 만듦), `MemberPasswordService.change(mbrSn, req)`(`pswd`≠`pswdCnfrm`이면 `PASSWORD_MISMATCH` → 현재 비밀번호 검사(위와 같음) → BCrypt로 바꿔 `memberMapper.updatePassword`(0행이면 `MEMBER_NOT_FOUND`) → `loginSessionManager.revokeAll`), `MemberController`(`PATCH /me/password`, 서비스 뒤 `loginSessionManager.issue(me.id(), response)`). 부를 API: `PATCH /members/me/password` (YAY). 완료 조건 — 명령: `MemberPasswordServiceTest`(성공 시 새 해시 저장·`revokeAll` 호출, 확인값 다르면 `PASSWORD_MISMATCH`이고 저장 안 함, 현재 비밀번호 틀리면 `CURRENT_PASSWORD_MISMATCH`이고 저장·폐기 안 함, SNS 전용·비활성, 0행이면 `MEMBER_NOT_FOUND`), `MemberControllerTest`(세 필드 중 하나 빠지면 400, 형식 틀린 새 비밀번호 400, 성공 시 응답에 새 access·refresh 쿠키). 화면: 로컬에서 브라우저 A·B(또는 Postman 두 쿠키)로 로그인 → A에서 변경 200 → A는 계속 `GET /auth/me` 200, B는 access 만료 뒤 `/auth/refresh` 401 → 옛 비밀번호 로그인 실패, 새 비밀번호 로그인 성공. 프론트 연결: 회원정보 수정 6-2 비밀번호 [변경] 저장 (SJM 화면, 프론트 저장소에서 /start)
- 하지 않는 것: 본인 확인·변경의 틀린 횟수 제한(로그인의 보안코드 같은 것, 명세에 없음, 이미 로그인한 세션이라 issues 4에 남김), 변경 알림 메일(명세에 없음), SNS 회원의 비밀번호 새로 만들기
- 새 의존성: 없음
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |

## 다음에 할 것

- 연결 남음: FN-MYP-040-01·06 (프론트 연결 회원정보 수정 6-1 본인 확인·6-2 비밀번호 [변경] 저장, 화면 SJM, 프론트 저장소에서 /start). 화면 담당이 SJM이라 프론트 YAY 마일스톤에 절을 넣지 않았다. SJM 프론트 마일스톤에 넣을 절을 SJM에게 전한다: "## SCR-MYP-040 회원정보 수정 · 본인 확인·비밀번호 변경 API 연결 — 대기 / 기능: 명세 기능 ID 추가 없음. 걸치는 기능: FN-MYP-040-01·06 / 기다리는 것: 백엔드 `POST /api/members/me/password-verification`, `PATCH /api/members/me/password`(YAY, 2026-11-03 완료) / 완료 조건: 6-1 [확인]은 `{pswd}`, 200이면 6-2, 400 `CURRENT_PASSWORD_MISMATCH`면 입력란 아래 문구. 6-2 비밀번호 [저장]은 `{currentPswd, pswd, pswdCnfrm}`, 400 `CURRENT_PASSWORD_MISMATCH`는 현재 비밀번호 칸, `PASSWORD_MISMATCH`는 확인 칸, `INVALID_INPUT_VALUE`(`pswd`)는 새 비밀번호 칸. 200이면 로그인 유지(새 쿠키), 다른 기기만 로그아웃"
- 알림(SJM): MYP-040 본인 확인은 서버 상태 없이 200/400만 준다. `MemberController.updateMe`의 TODO(세션 10분 제안)는 이번 YAY 변경에서 지웠다. 비밀번호 변경 요청에는 `currentPswd`가 들어간다
- 연결 남음: FN-HOS-001-01~32·FN-PLC-001~004-01 (프론트 연결 `features/place/` 검색·재검색·정렬·무한 스크롤·즐겨찾기 고정 영역·별 버튼·카카오맵 상세 새 탭, 프론트 저장소에서 /start). 프론트 마일스톤에 "SCR-HOS-001 플레이스 · 장소 검색·즐겨찾기 API 연결" 절을 넣었다. `kakaoUrlPath`가 `null`이면 상세 버튼을 숨기거나 `https://map.kakao.com/link/map/{plcConm},{plcLat},{plcLot}`, `rprsTelno`가 `null`이면 전화 줄을 숨긴다
- 연결 남음(프론트 저장소에서 /start): 회원가입 `features/member/signup/`(닉네임 요청은 `withCredentials`, 가입 요청에 `certNo`, 재전송 제한은 400 `EMAIL_CERT_RESEND_NOT_YET`의 `data.resendAvailableDt`), 비밀번호 찾기 `features/member/password/`(`certRslt`로 화면 분기, 다시 입력은 `invalidate`), 비밀번호 재설정 FN-ACC-012-01~06(`emlCertSn`·`pswdInitTkn`을 메모리로, 만료·400 `PASSWORD_RESET_NOT_AVAILABLE`이면 만료 화면, 자동 로그인 없음), 재가입 복구 FN-ACC-001-40~46(409 `MEMBER_RECOVERY_AVAILABLE` → 복구 목적 발송 → verify → `recovery-info` → `recovery`, 완료 뒤 자동 로그인 없음)
- 알림(KSJ, issues 13): 커뮤니티 지도 선택(CMU-002-11·004-10·005-05)은 지금 `GET /places`를 카테고리와 함께 쓰는 것까지다. 일반 주소·좌표 검색은 만들지 않았다
- 팀 확인(공용 DB): `LGN_ATMPT_LMT`의 테스트 행(번호 7, 출처 불명)과 2026-11-03 로그인 진단 중 생긴 `probe@example.com` 실패 행을 지워도 되는지
- ACC-020 기획 때: 보관 기간이 지난 탈퇴 이메일은 지금 가입(409 `MEMBER_RECOVERY_AVAILABLE`)도 복구(400 `MEMBER_RECOVERY_EXPIRED`)도 안 된다. 회원 행 파기 방식과 함께 정한다. 반려동물·공동양육 조회(SJM)가 나왔는지 먼저 본다
- 작은 정리: `member/dto/EmailCertificationCreateRequest` 클래스 주석이 아직 "가입·비밀번호 찾기 목적만 받는다"이다(복구도 받음). 다음에 이 파일을 고칠 때 같이 고친다. `DevTokenController` 삭제는 팀 공지 뒤
