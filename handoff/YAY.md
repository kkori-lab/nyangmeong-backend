# YAY 핸드오프

## 지금 하는 것

SCR-MAN-001 메인페이지 — 끝남 (승인 2026-11-05 15:41, 검증 1회차 확인 못 함 있음(연결 남음만), 사람 확인 2026-11-05). `GET`·`PUT /api/members/me/dashboard-widgets`로 위젯 순서 기본값 조회·저장·거절(400 `INVALID_INPUT_VALUE`)을 로컬에서 확인했다. 기획과 달라진 것 없음
- 기준: 메인페이지_기능명세서_v0.10_20261103 FN-MAN-001~009, 화면설계서 v0.10. 이 저장소에서 내가 만들 API는 위젯 순서 2개(FN-MAN-007)뿐이고, 나머지는 이미 있는 남의 API를 프론트가 부른다
- 결정(위젯 표시 여부·개수 제한 없앰): 명세 v0.10은 "6종 모두 노출, 카드 클릭으로 순서만 조정"이다. API 초안의 `indctYn`과 "표시 최대 5개", 마일스톤 완료 조건의 `DASHBOARD_WIDGET_LIMIT_EXCEEDED`는 옛 명세 기준이라 따르지 않는다. 구현 전에 `docs/api/member.md` 초안과 `milestones/YAY.md` 완료 조건을 고친다. `ErrorCode.DASHBOARD_WIDGET_LIMIT_EXCEEDED`는 이번에 쓰지 않으며, 다른 곳에서도 쓰지 않으므로 지운다
- 결정(대상 코드): MANG01 하위 코드 전부(`USE_YN='Y'`, 지금 MANC0001~0007 7개)를 순서 대상으로 둔다. MANC0006 토탈 케어리스트는 위젯 영역 밖 섹션이지만 코드가 있어 함께 내려 주고, 프론트가 쓸지 정한다. 케어리스트 항목의 노출·순서는 건강기록 `PUT /pets/{petSn}/care-item-exposures`(JAJ)라 여기서 하지 않는다
- 결정(조회): 저장한 행이 없거나 일부만 있으면 저장된 것은 `MBR_DSHBD_WDGT.SORT_SEQ`, 없는 것은 `COM_CD.SORT_SEQ`로 정렬한 기본 목록을 준다(조회만으로 행을 만들지 않는다). `COM_CD` LEFT JOIN `MBR_DSHBD_WDGT` 한 쿼리
- 결정(저장): 요청은 순서대로 늘어놓은 코드 목록 `dshbdWdgtCds: List<String>`(초안의 `widgets[{dshbdWdgtCd, indctYn, sortSeq}]` 대신. 순서만 저장하므로 목록 순서가 곧 순서이고, sortSeq 중복·빈칸 검사가 필요 없다). MANG01 코드 전부를 정확히 한 번씩 담지 않으면(빠짐·중복·없는 코드) 400 `INVALID_INPUT_VALUE`. 저장은 `INSERT … ON DUPLICATE KEY UPDATE SORT_SEQ`(UK `MBR_SN, DSHBD_WDGT_CD`), `SORT_SEQ`는 10, 20, … `INDCT_YN`은 기본값 'Y' 그대로. 응답은 저장 뒤 조회와 같은 목록
- FN-MAN-001 히어로 캐릭터: 바꿀 파일 없음. 부를 API `GET /pets`(SJM, 있음, 대표 반려동물)·`GET /pets/{petSn}/character-image`(LJS, 있음). 비로그인·캐릭터 없음은 프론트의 기본 캐릭터. 완료 조건 — 화면: 로그인 회원으로 두 API 200. 프론트 연결: 메인 히어로 캐릭터·캡션 (프론트 저장소에서 /start)
- FN-MAN-003·004 프로필 요약·반려동물 탭: 바꿀 파일 없음. 부를 API `GET /pets`·`GET /pets/{petSn}`(SJM, 있음, 공동양육 포함). 완료 조건 — 화면: `GET /api/pets` 200. 프론트 연결: SCR-HLT-011 탭·요약 바 컴포넌트 재사용 (프론트 저장소에서 /start)
- FN-MAN-005·006 기준일·건강 지표: 바꿀 파일 없음. 부를 API `GET /pets/{petSn}/health-indicators`(JAJ, 있음, `baseYmd`·`inqDt`와 5종), AI 건강분석 리포트는 미결(명세)이라 연결하지 않는다. 완료 조건 — 화면: 내 반려동물로 200. 프론트 연결: 위젯 6종 카드·기준일 표시 (프론트 저장소에서 /start)
- FN-MAN-006·007 위젯 순서 조회·저장: 바꿀 파일 `member/dto/DashboardWidgetSummary`(새로, `dshbdWdgtCd`·`dshbdWdgtCdNm`·`sortSeq`), `member/dto/DashboardWidgetSaveRequest`(새로, record `@NotNull List<@NotBlank @Size(max=8) String> dshbdWdgtCds`), `member/mapper/DashboardWidgetMapper`·`mapper/member/DashboardWidgetMapper.xml`(새로, `selectWidgetList(mbrSn)`·`upsertWidgetSort`), `member/service/DashboardWidgetService`(새로, `getWidgets`·`@Transactional saveWidgets`), `member/controller/MemberController`(`GET`·`PUT /me/dashboard-widgets`), `docs/api/member.md`(초안 고침), `global/error/ErrorCode`(상수 하나 지움), `milestones/YAY.md`(완료 조건 고침). 부를 API: `GET`·`PUT /members/me/dashboard-widgets`(YAY). 완료 조건 — 명령: `DashboardWidgetServiceTest`(전체 코드 순서대로면 upsert를 그 순서로 부름, 빠짐·중복·없는 코드면 `INVALID_INPUT_VALUE`이고 upsert 안 부름), `./gradlew build -q`. 화면: Postman으로 로그인 → `GET /api/members/me/dashboard-widgets` 200 기본 순서 7개 → `PUT {dshbdWdgtCds:[MANC0005,MANC0001,…7개]}` 200 그 순서 → 다시 GET 같은 순서 → 6개만·중복·`MANC9999`면 400 `INVALID_INPUT_VALUE` → DB `MBR_DSHBD_WDGT` 7행. 프론트 연결: 위젯 카드 순서 조정과 저장 (프론트 저장소에서 /start)
- FN-MAN-009 토탈 케어리스트 미리보기: 바꿀 파일 없음. 부를 API `GET /pets/{petSn}/care-items`·`GET /pets/{petSn}/care-completions`(JAJ, 있음). 완료 조건 — 화면: 내 반려동물로 두 API 200. 프론트 연결: 읽기 전용 케어리스트 섹션 (프론트 저장소에서 /start)
- 하지 않는 것: AI 건강분석 리포트 위젯(명세 미결), 비로그인 메인의 위젯 대체 방식(명세 "별도 확정 필요"), 메인 조합 API(issues 12번 결정대로 만들지 않음)
- 새 의존성: 없음
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | (SCR-ACC-002 SNS 로그인) `global/external/RealSnsOAuthClient`·`SnsOAuthProperties`(주석), `application.properties.example`, `RealSnsOAuthClientRedirectUriTest`(새로) | 명령: `./gradlew build -q`, `RealSnsOAuthClientRedirectUriTest`(`redirect-uri`가 비면 `app.front-base-url` + `/login/sns/{naver\|kakao\|google}/callback`, 끝 `/` 하나 제거, 값이 있으면 그 값). 화면: 로컬 `redirect-uri` 비우고 실제 카카오·구글 로그인 `SIGNUP_REQUIRED` | 요청 2026-11-06 "리다이렉트 url 개발은 localhost:5173, 배포는 pawever.cloud, 환경에 맞춰 처리" |

## 다음에 할 것

- 연결 남음: FN-MAN-001·003·004·005·006·007·009 (프론트 연결 메인 히어로 캐릭터·반려동물 탭·프로필 요약·건강 지표 위젯 카드와 순서 저장·토탈 케어리스트, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-MAN-001 메인페이지 · 위젯 순서·건강 대시보드 API 연결" 절을 넣었다. API: 위젯 순서 YAY, 반려동물·캐릭터 SJM·LJS, 건강 지표·케어리스트 JAJ
- 연결 남음: FN-ACC-020-02·03·14·15·16·20, FN-AIM-011-19, FN-MYP-002-08 (프론트 연결 탈퇴 안내·문구 입력 모달·실행·실패/결과 확인·완료 화면, 마이페이지·AI 매니저 탈퇴 진입, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-020 회원탈퇴 · 탈퇴 API 연결" 절을 넣었다. 마이페이지 진입 버튼 화면은 SJM(SCR-MYP-040·002)
- 팀 확인(명세 FN-ACC-020-02·04): 탈퇴 회원의 게시물·댓글·포인트와 남은 공동양육자의 조회 범위는 정책 미정이라 손대지 않았다
- 연결 남음: FN-ACC-001-04·41, 비밀번호 찾기 발송 (프론트 연결 이메일 단계의 502 `MAIL_SEND_FAILED` 안내, 회원가입 문구 "인증번호를 보내지 못했습니다…"만 서버 문구로 맞추기, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-001 회원가입 (이메일) · 메일 발송 실패(502) 안내" 절을 넣었다
- 팀 메일 계정(2026-11-05 정함): `nyangmeongcare@gmail.com`. 로컬에서 Gmail SMTP(`smtp.gmail.com:587`, 앱 비밀번호) 실제 발송 확인함. 설정 공유·배포 `smtp`·개인 앱 비밀번호 삭제·배포 메일 설정은 2026-11-06 끝남(사람)
- 알림(SJM): 공동양육 초대 메일도 `CertificationMailEvent`라 발송 실패 시 초대가 저장되지 않고 502 `MAIL_SEND_FAILED`가 된다(전에는 저장되고 메일만 빠짐)
- 알림(LJS): `app.mail.client=smtp`를 켜면 케어 일정 알림 메일(`CareScheduleAlarmService`)도 실제로 나간다. 그쪽은 `MailSender`를 직접 불러 동작은 바뀌지 않았다
- 알림(프론트, 2026-11-05 확인): 비밀번호 재설정 API(`POST /email-certifications/{emlCertSn}/password-reset`)는 이미 develop에 있다. 프론트 SCR-ACC-012 연결은 기다릴 것 없음
- 연결 남음: FN-ACC-002-09·10·11, FN-ACC-001-31·32 (프론트 연결 SNS 버튼·콜백 화면과 SNS 가입 확인 화면, 프론트 저장소에서 /start). 프론트 YAY 마일스톤에 "SCR-ACC-002 SNS 로그인 · SNS 로그인·가입 API 연결" 절을 넣었다
- 실제 제공자 키 확인 끝남(2026-11-04): 네이버·카카오·구글 모두 실제 인가 코드로 `SIGNUP_REQUIRED`와 실제 이메일을 받았다. 네이버 번호는 정규화돼 왔고, 카카오는 번호가 오지 않았다(동의항목에 전화번호 없음), 구글은 번호 없음·`ageCnfrmd=false`. redirect-uri는 `http://localhost:5173/login/sns/{naver|kakao|google}/callback`(콘솔 등록, 프론트 콜백 화면도 이 경로로)
- 남은 일(SNS, YAY): ① 연령 확인: 추가-5로 세 제공자 모두 `ageCnfrmd=false`(화면은 `AGE_14` 체크). "확인 완료" 표시를 쓰지 않게 된 것을 명세 담당(sannabi1)에게 알린다 ② 카카오 전화번호는 동의항목으로 받지 않기로 했다(비즈 앱 권한 필요). 로그인·회원가입 기능명세서 v0.9(2026-11-04)에서 FN-ACC-002-10을 "SNS 가입 확인 화면 직접 입력·중복확인(구글과 같음)"으로 고쳤다. 화면설계서 v0.11의 같은 문구(화면 01·22의 카카오 4번)도 고쳐야 한다. 코드는 그대로(번호가 없으면 `mblTelno` null). 기능명세서 v0.9는 아직 DMS에 올리지 않았다(문서 올린 사람이 sannabi1이라 새 판이 거절될 수 있음)
- SNS redirect-uri(추가-1, 2026-11-06): `redirect-uri`를 비우면 `app.front-base-url` + `/login/sns/{제공자}/callback`. 로컬 실제 로그인 확인, 배포 설정(프론트 주소·CORS·redirect-uri 비움)과 세 콘솔 배포 콜백 등록 끝남(사람). 프론트 인가 창의 `redirect_uri`를 환경변수로 바꾸는 것은 프론트 저장소에서
- 알림(팀, 설정): `app.sns.client`가 없으면 이제 실제 호출이다. 로컬에서 키 없이 확인하려면 `app.sns.client=fake`를 직접 둔다. 배포 서버에는 fake를 두지 않는다
- 이메일 로그인 점검(2026-11-04, 버그 없음) 메모: ① 탭 여러 개가 동시에 `/auth/refresh`를 부르면 진 쪽 응답의 쿠키 삭제가 늦게 도착해 로그아웃될 수 있다(탭 하나 안은 프론트 `api/axios.js`가 묶어 막는다) ② 배포 쿠키 설정(`secure`·같은 사이트)은 2026-11-06 끝남 ③ 만료·폐기된 `RFRSH_TKN` 행을 지우는 배치가 없다(로그인·재발급마다 쌓임)
- 참고(SNS 가입): 가입 응답의 `joinDt`는 첫 응답에서 null이다(등록 직후 다시 읽지 않음, 이메일 가입도 같다). 화면에서 쓰게 되면 고친다
- 연결 남음: FN-MYP-040-01·06 (프론트 연결 회원정보 수정 6-1 본인 확인·6-2 비밀번호 [변경] 저장, 화면 SJM, 프론트 저장소에서 /start). 화면 담당이 SJM이라 프론트 YAY 마일스톤에 절을 넣지 않았다. SJM 프론트 마일스톤에 넣을 절을 SJM에게 전한다: "## SCR-MYP-040 회원정보 수정 · 본인 확인·비밀번호 변경 API 연결 — 대기 / 기능: 명세 기능 ID 추가 없음. 걸치는 기능: FN-MYP-040-01·06 / 기다리는 것: 백엔드 `POST /api/members/me/password-verification`, `PATCH /api/members/me/password`(YAY, 2026-11-03 완료) / 완료 조건: 6-1 [확인]은 `{pswd}`, 200이면 6-2, 400 `CURRENT_PASSWORD_MISMATCH`면 입력란 아래 문구. 6-2 비밀번호 [저장]은 `{currentPswd, pswd, pswdCnfrm}`, 400 `CURRENT_PASSWORD_MISMATCH`는 현재 비밀번호 칸, `PASSWORD_MISMATCH`는 확인 칸, `INVALID_INPUT_VALUE`(`pswd`)는 새 비밀번호 칸. 200이면 로그인 유지(새 쿠키), 다른 기기만 로그아웃"
- 알림(SJM): MYP-040 본인 확인은 서버 상태 없이 200/400만 준다. `MemberController.updateMe`의 TODO(세션 10분 제안)는 이번 YAY 변경에서 지웠다. 비밀번호 변경 요청에는 `currentPswd`가 들어간다
- 연결 남음: FN-HOS-001-01~32·FN-PLC-001~004-01 (프론트 연결 `features/place/` 검색·재검색·정렬·무한 스크롤·즐겨찾기 고정 영역·별 버튼·카카오맵 상세 새 탭, 프론트 저장소에서 /start). 프론트 마일스톤에 "SCR-HOS-001 플레이스 · 장소 검색·즐겨찾기 API 연결" 절을 넣었다. `kakaoUrlPath`가 `null`이면 상세 버튼을 숨기거나 `https://map.kakao.com/link/map/{plcConm},{plcLat},{plcLot}`, `rprsTelno`가 `null`이면 전화 줄을 숨긴다
- 연결 남음(프론트 저장소에서 /start): 회원가입 `features/member/signup/`(닉네임 요청은 `withCredentials`, 가입 요청에 `certNo`, 재전송 제한은 400 `EMAIL_CERT_RESEND_NOT_YET`의 `data.resendAvailableDt`), 비밀번호 찾기 `features/member/password/`(`certRslt`로 화면 분기, 다시 입력은 `invalidate`), 비밀번호 재설정 FN-ACC-012-01~06(`emlCertSn`·`pswdInitTkn`을 메모리로, 만료·400 `PASSWORD_RESET_NOT_AVAILABLE`이면 만료 화면, 자동 로그인 없음), 재가입 복구 FN-ACC-001-40~46(409 `MEMBER_RECOVERY_AVAILABLE` → 복구 목적 발송 → verify → `recovery-info` → `recovery`, 완료 뒤 자동 로그인 없음)
- 알림(KSJ, issues 13): 커뮤니티 지도 선택(CMU-002-11·004-10·005-05)은 지금 `GET /places`를 카테고리와 함께 쓰는 것까지다. 일반 주소·좌표 검색은 만들지 않았다
- 팀 확인(공용 DB): `LGN_ATMPT_LMT`의 테스트 행(번호 7, 출처 불명)과 2026-11-03 로그인 진단 중 생긴 `probe@example.com` 실패 행을 지워도 되는지
- 작은 정리: `member/dto/EmailCertificationCreateRequest` 클래스 주석이 아직 "가입·비밀번호 찾기 목적만 받는다"이다(복구도 받음). 다음에 이 파일을 고칠 때 같이 고친다. `DevTokenController` 삭제는 팀 공지 뒤
