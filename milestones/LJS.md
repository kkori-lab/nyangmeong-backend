# LJS 마일스톤

기준: 공통레이아웃_기능명세서_v0.4_20261001, AI매니저_기능명세서_v0.7_20261009, 마이페이지_기능명세서_v0.4_20261019, 건강기록_기능명세서_v1.0_20261018 (DMS, 2026-10-20 확인), 관리자_기능명세서_v0.1_20261030 (DMS, 2026-10-30 확인)

## SCR-COM-002 기본 레이아웃 — 끝
- 기능: FN-COM-002-02 로그인 필수 접근 제어, FN-COM-002-15 프로필 아이콘(회원 요약), FN-COM-002-24 세션 만료 처리, FN-COM-003-01 미인증 접근 감지(화면은 SCR-COM-003), FN-COM-003-07 세션 만료 동일 처리(화면은 SCR-COM-003), FN-COM-005-02 회원 정보 표시(화면은 SCR-COM-005)
- 함께 만드는 것: `global/response`(ApiResponse·PageResponse·PageQuery), `global/error`(ErrorCode·BusinessException·GlobalExceptionHandler), `global/security`(LoginMember·JWT 발급·검증 필터·httpOnly 쿠키·SecurityConfig·401/403 응답·X-Requested-With 검사·개발용 토큰 발급), `global/security/crypto`(FieldCryptoService·TokenHashUtil), `global/log`(요청 로그·MDC·민감정보 마스킹), CORS 설정, CI 워크플로, 응답 모양을 nct-backend 형식으로 바꾼 규칙 문서, 테스트용 `@WithMockLoginMember`
- 기다리는 것: 없음. 로그인·로그아웃·재발급 API(YAY)는 이 토큰 기반 위에 만든다. 그 전까지 개발용 토큰 발급 API로 쿠키를 받는다
- 완료 조건: 쿠키 없이 `/api/v1/**`를 부르면 401 `UNAUTHORIZED`, 만료 토큰이면 401 `TOKEN_EXPIRED`가 nct 응답 모양으로 나온다. `GET /api/v1/auth/me`가 회원 번호·권한·닉네임·첨부파일 번호·마스킹 이메일을 돌려준다
- 연결 남음: 개발용 토큰 발급 (로그인·로그아웃·재발급 API가 생기면 DevTokenController 삭제, YAY)
- 연결 남음: FN-COM-002-15 (프로필 이미지 URL, 파일 조회 API SCR-MYP-010, LJS)

## SCR-ACC-002 로그인 준비 (토큰 세션 기반, 로그인 API는 YAY) — 끝
- 기능: 없음(화면 기능은 YAY). SCR-ACC-002 로그인·재발급·로그아웃이 쓸 `global/security` 기반만 만든다
- 함께 만드는 것: JWT 라이브러리 Nimbus 교체, 토큰 `sid`(RFRSH_TKN_SN)·`typ`, `LoginSessionManager`(세션 발급·회전·폐기), 필터의 세션 확인, `PasswordEncoder`, DevTokenController를 새 발급 방식으로
- 기다리는 것: 없음
- 완료 조건: 발급한 세션을 폐기하면 그 Access로 `/api/auth/me`가 401. 같은 Refresh로 두 번 회전하면 두 번째는 `UNAUTHORIZED`
- 추가 요청: 추가-1 SecurityConfigTest sid 시그니처, 추가-2 스타터 방식 화이트리스트(`app.security.permit-all-paths`)

## SCR-COM-004 알림 드롭다운 — 끝
- 기능: FN-COM-002-09 알림 아이콘·미확인 배지(화면은 SCR-COM-002), FN-COM-004-02 미읽음 개수 표시, FN-COM-004-03 유형 탭 노출, FN-COM-004-04 유형 탭 필터, FN-COM-004-05 알림 목록 조회, FN-COM-004-06 알림 클릭·읽음 처리, FN-COM-004-07 유형별 이동 대상 분기, FN-COM-004-08 공동양육 초대 즉시 처리, FN-COM-004-09 펫돌봄 거래 상태 알림 처리, FN-COM-004-10 캐릭터 완료 알림 처리, FN-COM-004-11 자동 읽음 미처리, FN-COM-004-12 7일 경과 자동 숨김, FN-COM-004-13 수신 OFF 유형 미생성, FN-COM-008-16 수신 OFF = 미생성(화면은 SCR-COM-008), FN-AIM-006-06 유형 수신 설정 확인(화면은 SCR-AIM-006), FN-AIM-006-12 헤더 뱃지 갱신(화면은 SCR-AIM-006)
- API가 남의 것: FN-COM-004-08 초대 수락·거절 (SJM)
- 함께 만드는 것: `global/notification`(NotificationEvent·MailEvent·EventPublisher), `notification` 도메인의 알림 저장 리스너
- 기다리는 것: 없음 (초대 수락·거절 SJM은 가짜 응답으로 먼저)
- 완료 조건: `EventPublisher.publish(NotificationEvent)`가 트랜잭션 안에서는 INAPP_NOTI에 저장되고 밖에서는 예외를 던진다. 미읽음 개수와 최근 5건 목록 API가 7일 지난 알림을 빼고 돌려준다
- 연결 남음: FN-COM-004-08 (공동양육 초대 수락·거절 API와 초대 알림 mvmnPath 형식, SJM)

## SCR-MYP-010 픽셀 캐릭터 관리 — 끝
- 기능: FN-MYP-001-01 내 반려동물 카드의 캐릭터 얼굴(화면은 SCR-MYP-001), FN-MYP-002-02 반려동물 슬라이드의 캐릭터 얼굴(화면은 SCR-MYP-002), FN-MYP-010-01 탭 구성, FN-MYP-010-02 내 캐릭터 목록, FN-MYP-010-03 캐릭터 만들기, FN-MYP-010-04 생성 상한, FN-MYP-010-07 공동양육 탭, FN-MYP-010-08 1단계 사진 첨부, FN-MYP-010-09 2단계 생성 중, FN-MYP-010-10 3단계 결과 확인, FN-MYP-010-12 생성 실패 시 대체, FN-MYP-010-13 미리보기·부위 슬롯, FN-MYP-010-14 보관함·카테고리 탭, FN-MYP-010-16 되돌리기·장착, FN-MYP-010-17 캐릭터 규격 고정, FN-MYP-010-18 규격 일관성 검증, FN-MYP-010-19 규격 미확보 시 대체, FN-MYP-010-23 생성 오류 처리, FN-MYP-010-24 오류 문의
- API가 남의 것: FN-MYP-010-24 1:1 문의 작성 (KSJ), FN-MYP-010-23 관리자 확인 화면 (관리자 화면 미정)
- 함께 만드는 것: `global/file`(FileStore·파일 조회 API), `global/auth`(PetAuthorizer·OwnerAuthorizer), `global/setting`(SYS_STNG 조회, FileStore의 파일 최대 크기 FILE_MAX_SZ), `global/code`(공통코드 조회, 부위 탭 MYPG01 FN-MYP-010-14), `global/external`(캐릭터 생성 API 호출)
- 기다리는 것: pet 목록·프로필 조회 (SJM), 캐릭터 생성 외부 API 결정
- 완료 조건: 캐릭터가 없는 반려동물은 종별 기본 캐릭터 얼굴 이미지를 돌려준다. 생성 요청은 반려동물당 1회만 받고 완료 시 캐릭터 알림을 발행한다
- 연결 남음: FN-MYP-010-24 (1:1 문의 작성 화면에서 receiveErrorInquiry 호출, KSJ)
- 연결 남음: FN-MYP-010-23 (관리자 확인 화면에서 finishErrorCheck 호출, 담당 미정)
- 다음으로 넘김: FN-MYP-010-19 (캐릭터 생성 외부 API 결정 뒤, LJS)

## SCR-COM-008 설정 (알림) — 끝
- 기능: FN-COM-008-02 알림 채널 설정, FN-COM-008-03 이메일 주소 고정 표시, FN-COM-008-04 양 채널 OFF 처리, FN-COM-008-05 케어 일정 수신 설정, FN-COM-008-06 사전 알림 시점 설정, FN-COM-008-07 펫돌봄 거래 상태 수신 설정, FN-COM-008-08 커뮤니티 수신 설정, FN-COM-008-09 가계부 알림 수신 설정, FN-COM-008-10 한도 미설정 시 안내, FN-COM-008-11 항상 받는 알림 표시, FN-COM-008-12 캐릭터 완료 알림 배치, FN-COM-008-13 기본값 적용, FN-COM-008-14 저장 처리, FN-COM-008-20 계정 단위 적용
- API가 남의 것: FN-COM-008-10 월 지출 한도 조회 (SJM) → MBR.MM_EXPND_LIM_AMT를 직접 읽어 연결 불필요
- 기다리는 것: 없음
- 완료 조건: 설정 조회가 MBR의 수신 여부 컬럼을 true/false로 돌려준다. 저장(PUT)이 항상 받는 알림은 바꾸지 않는다

## SCR-MYP-011 아이템 상점 모달 — 끝
- 기능: FN-MYP-011-02 카테고리 탭, FN-MYP-011-03 아이템 목록·보유중 표시, FN-MYP-011-05 구매, FN-MYP-011-06 권한
- 기다리는 것: pet 포인트 차감 (SJM)
- 완료 조건: 이미 보유한 아이템은 보유중으로 표시된다. 포인트가 부족하면 `POINT_NOT_ENOUGH`로 구매를 막는다
- 연결 남음: FN-MYP-011-05 (pet 포인트 조회·차감 FN-MYP-020, SJM)

## SCR-AIM-006 케어 일정 사전 알림 — 끝
- 기능: FN-AIM-006-01 발송 대상 추출, FN-AIM-006-02 알림 대상 범위, FN-AIM-006-03 발송 시점 산출, FN-AIM-006-04 배치 스캔 실행, FN-AIM-006-05 채널 판정, FN-AIM-006-07 발송 주소 결정, FN-AIM-006-09 템플릿 치환, FN-AIM-006-10 이메일 발송, FN-AIM-006-11 인앱 알림 생성, FN-AIM-006-13 알림 클릭 이동, FN-AIM-006-14 수신 해제 안내, FN-AIM-006-15 발송 이력 기록, FN-AIM-006-16 실패 재시도, FN-AIM-006-17 중복 발송 방지, FN-AIM-006-18 스캔 시점 판정, FN-AIM-006-19 스캔 이후 등록분 처리, FN-AIM-006-20 수신자 결정, FN-COM-008-19 수신 해제 링크 도착(화면은 SCR-COM-008)
- 함께 만드는 것: `global/mail` 메일 발송, `global/batch`(스케줄러 공통 설정, 배치 스캔 FN-AIM-006-04)
- 기다리는 것: health 일정 조회 (JAJ), pet 공동양육자 조회 (SJM). 배치 위치(`notification`·`scheduler`)는 docs/OPEN_ISSUES.md에서 정한다
- 완료 조건: 09:00 스캔이 D-N이 맞는 병원 일정만 골라 수신자별로 한 번만 보낸다. 발송 이력이 NOTI_SNDNG_LOG에 남는다
- 연결 남음: FN-AIM-006-01·02 (health 병원 일정 조회, JAJ), FN-AIM-006-20 (pet 연동 양육자 조회, SJM), FN-AIM-006-11·13 화면 확인 (위 두 연결 뒤, LJS), FN-AIM-006-10 실제 SMTP 발송 (팀 SMTP 계정, 팀)

## SCR-AIM-008 영수증 촬영·업로드 (1차: 업로드·OCR·영수증 상태) — 끝
- 기능: FN-AIM-008-02 역할 분리, FN-AIM-008-03 대상 반려동물 선택, FN-AIM-008-04 촬영·파일 업로드, FN-AIM-008-05 업로드 제약 검증, FN-AIM-008-06 OCR API 호출, FN-AIM-008-07 분석 대기 표시, FN-AIM-008-09 원본 이미지 보관, FN-AIM-009-04 OCR 값 → 필드 변환, FN-AIM-009-16 반려동물 탭 구성, FN-AIM-009-17 반려동물 자동 연결·선택(결과 응답 쪽), FN-AIM-009-20 하단 할인 비율 배분, FN-AIM-009-21 검진 내용 자동 채움, FN-AIM-010-01 인식 실패·일시 오류 판정, FN-AIM-010-05 부분 인식 처리, FN-AIM-010-06 원본 이미지 유지, FN-AIM-010-07 실패 사유 구분 안내
- API가 남의 것: 연동 반려동물 목록 (SJM, 가짜 `CaregiverPetFake`)
- 함께 만드는 것: `global/external`(OCR 호출 FN-AIM-008-06, 가짜 `FakeOcrClient`), `global/file/FileStore` 실제 파일 형식 검사
- 기다리는 것: OCR 외부 API 규격
- 위치: `expense` 도메인(`docs/OPEN_ISSUES.md`, 2026-10-21)
- 완료 조건: 업로드가 jpg·jpeg·png·webp 10MB 이하만 받고(실제 형식 검사) RECPT에 원본을 남긴다. 분석 결과가 인식성공·부분인식·인식실패·일시오류(사유 2종)·취소로 RECPT 상태와 RECPT_STTS_HSTRY에 남는다
- 연결 남음: FN-AIM-009-17 (연동 반려동물 목록, SJM — 지금은 `CaregiverPetFake` 빈 목록)
- 연결 남음: FN-AIM-008-06 (OCR 외부 API 규격, 담당 미정 — 지금은 `FakeOcrClient`)

## SCR-AIM-008 영수증 저장 (2차: 확인 후 저장) — 끝
- 기능: FN-AIM-009-01 원본 이미지 대조 표시, FN-AIM-009-02 공통 항목 표시·수정, FN-AIM-009-03 처방 약품 다건 행 표시, FN-AIM-009-07 필수값 검증, FN-AIM-009-08 확인 후 저장, FN-AIM-009-09 검진기록 생성·연동, FN-AIM-009-10 가계부 기록, FN-AIM-009-11 투약 항목 자동 추가 미적용, FN-AIM-009-12 저장 후 이동, FN-AIM-009-13 금액 불일치 안내, FN-AIM-009-18 반려동물 추가, FN-AIM-009-19 배정 필요 목록, FN-AIM-009-22 가계부에도 기록 선택, FN-AIM-009-23 가계부 중복 확인, FN-AIM-009-24 일괄 저장, FN-AIM-009-26 입력 길이 제한, FN-AIM-009-27 영수증 상태 갱신, FN-AIM-010-08 인식 실패율 수집(명세서 "이번 차수 제외"), FN-HLT-002-34 원본 이미지(화면은 SCR-HLT-002), FN-HLT-002-35 검진일·병원명(화면은 SCR-HLT-002), FN-HLT-002-36 반려동물별 기록 탭(화면은 SCR-HLT-002), FN-HLT-002-37 검진 종류(화면은 SCR-HLT-002), FN-HLT-002-38 검진 내용·검진 메모(화면은 SCR-HLT-002), FN-HLT-002-39 금액(화면은 SCR-HLT-002), FN-HLT-002-40 처방 약품(화면은 SCR-HLT-002), FN-HLT-002-41 가계부에도 기록(화면은 SCR-HLT-002), FN-HLT-002-42 영수증 금액·할인 배분(화면은 SCR-HLT-002), FN-HLT-002-43 배정 필요 목록(화면은 SCR-HLT-002), FN-HLT-002-44 가계부 중복 확인(화면은 SCR-HLT-002), FN-HLT-002-45 다시 촬영하기·저장하기(화면은 SCR-HLT-002), FN-HLT-002-46 가까운 예정 일정 확인(화면은 SCR-HLT-002)
- API가 남의 것: FN-AIM-009-09·FN-HLT-002-46 검진기록·일정 등록 (JAJ), FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 지출 생성·중복 확인 (SJM)
- 함께 만드는 것: `global/external`(OCR 호출 FN-AIM-008-06), `global/idempotency`(멱등 요청, nct-backend에서 가져옴)
- 기다리는 것: health 일정·검진기록 등록 (JAJ), expense 지출 생성·중복 확인 (SJM), OCR 외부 API. 영수증 저장 위치는 docs/OPEN_ISSUES.md에서 정한다
- 완료 조건: 업로드가 jpg·jpeg·png·webp 10MB 이하만 받고 RECPT에 원본을 남긴다. 저장이 반려동물별 검진기록·가계부를 한 트랜잭션으로 만들고 같은 멱등 요청 ID는 한 번만 처리한다
- 연결 남음: FN-AIM-009-09·FN-HLT-002-46 검진기록·일정 등록과 앞뒤 7일 일정 조회 (JAJ — 지금은 `CheckupRecordFake`)
- 연결 남음: FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 지출 생성·중복 확인 (SJM — 지금은 `ExpenseFake`)
- 달라진 것: `global/idempotency`는 가져오지 않고 `RECPT.IDMPT_DMND_ID` 유일 키로 멱등 처리했다. 저장 전 확인·저장 API는 `ReceiptSaveController`

## SCR-AIM-008 영수증 저장 (3차: 가계부 사용처) — 끝
- 기능: FN-AIM-009-10 가계부 기록, FN-AIM-009-23 가계부 중복 확인, FN-HLT-002-41 가계부에도 기록(화면은 SCR-HLT-002), FN-HLT-002-44 가계부 중복 확인(화면은 SCR-HLT-002)
- API가 남의 것: 지출 생성·중복 확인 (SJM — 지금은 `ExpenseFake`)
- 연결 남음: FN-AIM-009-10·009-23·FN-HLT-002-41·002-44 (지출 생성·중복 조회에 사용처 `expndPlcusNm` 전달, SJM — 지금은 `ExpenseFake`)

## SCR-AIM-011 AI 대화창 (대화 저장·조회 내부 API) — 끝
- 기능: FN-AIM-011-07 새 대화 생성(첫 메시지와 함께 저장), FN-AIM-011-08 반려동물 선택·펫 태그(목록 필터·소유 검사), FN-AIM-011-09 대화 목록 조회, FN-AIM-011-10 대화 선택·이어 쓰기(상세 조회·메시지 추가), FN-AIM-011-11 대화 제목 암호화 저장·200자 검증, FN-AIM-011-13 대화 삭제(물리 삭제), FN-AIM-011-14 대화 이력 서버 저장, FN-AIM-011-19 이력 보관·파기(탈퇴 회원 보관일수 경과 후)
- 함께 만드는 것: `ai` 도메인 패키지 신설(층 표 3층, `ArchitectureTest`), `/internal/ai/**` 경로
- 기다리는 것: pet 소유 검사(`global/auth` PetAuthorizer로 가능), member 탈퇴 일시(YAY)
- 완료 조건: 대화 저장·목록·상세·삭제가 본인 것만 동작하고 남의 대화는 404 `NOT_FOUND`다. 제목·내용은 암호문으로 저장된다. 보관일수가 지난 탈퇴 회원의 대화가 파기된다
- 연결 남음: Python AI 서버가 이 API로 DB 직접 접근을 바꾼다(ai 저장소, 2026-10-26 결정)

## SCR-MYP-010 픽셀 캐릭터 관리 · 파일 저장 외부 디렉토리·OS별 기본 경로·서빙 — 끝
- 기능: 없음(연결 작업, 명세 기능 ID 추가 없음). 파일 저장 루트를 `/home/ggorilab/attachment`로 옮기고, 개발 OS(Windows·Mac)별 기본 경로와 공개 이미지 서빙을 만든다. 프론트 LJS 세션 요청(2026-10-27)
- 구현 방식(처음 요청과 다름): `WebConfig` 리소스 핸들러가 아니라 번호로 여는 `FileController`(`GET /api/files/public/{atchFileSn}`)가 `public/` 경로 접두어로 공개 여부를 판정한다. 비공개 범용 `GET /api/files/{fileSn}`은 만들지 않는다(`global`은 소유 검사를 못 함). 비공개는 도메인 API(`/receipts/{recptSn}/image`, `/pets/{petSn}/character-image`, `/items/{itemSn}/image`)가 소유 검사 후 준다. 보안은 `permit-all-paths`에 `/api/files/public/**`만 추가. 개발 Linux는 기본 경로 없음(키 없으면 시작 실패), 경로 철자 `attachment`, Windows는 프로젝트 드라이브 루트 아래, Mac은 `/Users/Shared/ggorilab/attachment`, prod는 키 필수·쓰기 실패 시 시작 중단
- 기다리는 것: 공개·비공개 분류 확인(반려동물·캐릭터 공동양육자 한정 여부 SJM), 운영 서버 폴더 준비와 nginx `client_max_body_size`(10MB 이상)
- 완료 조건: 시작 로그에 해석된 절대 경로가 한 줄 나온다. 공개 이미지가 `/api/files/public/{atchFileSn}`로 열리고, 공개가 아니거나 없는 번호는 404 `FILE_NOT_FOUND`다(비공개는 도메인 API로). prod에서 폴더에 쓸 수 없으면 앱이 시작되지 않는다. Windows에서 프로젝트를 C:와 D:에 두고 띄우면 각 드라이브 아래에 폴더가 생긴다
- 연결 남음: 게시글·캐릭터 이미지를 `<img src="/api/files/public/{atchFileSn}">`로 전환(프론트 연결 공통 파일 이미지 컴포넌트, 프론트 저장소 LJS 마일스톤. 게시글·캐릭터는 담당 KSJ·SJM에게 안내. 캐릭터는 응답에 번호가 없어 `character-image` API 사용). 공개·비공개 분류 확인(SJM), 운영 서버 폴더·nginx `client_max_body_size`(사람)
- 추가-1(2026-10-28): 개발 로그 폴더도 같은 OS별 규칙(`app.log.dir`, `LogDirEnvironmentPostProcessor`). PR #43 병합

## SCR-AIM-002 검색 질문·응답 (AI 서버 연결: 답변 저장 길이·AI 설정값 조회) — 끝
- 기능: FN-AIM-002-02 응답 최대 토큰(설정값 제공), FN-AIM-002-03 의료 고지 포함 저장(AI 답변 길이), FN-AIM-002-07 미완료 답변 저장(`cmptnYn`, 이미 구현·검사만 보강), FN-AIM-002-11 최근 N턴 컨텍스트(설정값 제공). 화면 기능은 Python AI 서버, 이 저장소는 내부 API만. 기준 AI매니저_기능명세서_v0.9_20261026(DMS, 2026-10-28 확인)
- 함께 만드는 것: 없음(`global/setting` SystemSettingService 재사용)
- 기다리는 것: 없음
- 완료 조건: AI 역할 메시지가 1,000자를 넘어도 10,000자까지 저장되고 사용자 역할은 1,000자 그대로다. `GET /internal/ai/settings`가 토큰 상한·대화 턴 수를 준다
- 연결 남음: FN-AIM-002-02·11 (Python AI 서버의 `GET /internal/ai/settings` 연결, ai 세션)

## SCR-AIM-012 AI 일정 생성 확인 (등록 카드 컨텍스트 내부 API) — 끝
- 기능: FN-AIM-012-04 반려동물 결정(회원의 반려동물 목록 제공), FN-AIM-012-08 같은 이름 케어 항목 안내(반려동물별 케어 항목 이름 제공). 판단·카드는 Python AI 서버, 이 저장소는 내부 API만. 기준 AI매니저_기능명세서_v0.10_20261029(DMS, 2026-10-29 확인). ai 세션 요청(2026-10-29)
- 함께 만드는 것: 없음(`pet` PetService.searchMyPets 재사용)
- 기다리는 것: 케어 항목 목록 service(JAJ, SCR-HLT-003 토탈 케어리스트 대기). 그 전까지 가짜
- 완료 조건: `GET /internal/ai/register-context`가 쿠키 회원의 반려동물 전부를 `[{petSn, petNm, careArtclNms}]`로 준다. 반려동물이 없으면 빈 배열, 인증 없으면 401
- 연결 남음: FN-AIM-012-04·08 (Python의 FakeRegisterContextRepository를 이 API로, ai 세션)

## SCR-AIM-013 대화창 · 영수증 분석 (출처 코드 AI 대화창·결과 조회) — 끝
- 기능: FN-AIM-013-04 분석 요청·대기(요청 출처 'AI 대화창' 기록), FN-AIM-013-06 확인 후 저장(본창 모달이 이 영수증의 분석 결과를 다시 연다), FN-AIM-013-08 원본 이미지 보관(기존 영수증 업로드 재사용). 기준 AI매니저_기능명세서_v0.10_20261029(DMS, 2026-10-29 확인)
- 함께 만드는 것: 공통코드 `AIMC0031` AI 대화창(AIMG07, 2026-10-29 사람 결정, ai 세션 합의)
- 기다리는 것: 없음(ai 저장소 PR과 같은 때 병합)
- 완료 조건: `POST /api/receipts`에 출처 `AIMC0031`을 주면 `RECPT.OCR_DMND_SRC_CD`가 `AIMC0031`, 주지 않으면 `AIMC0026`이다. `GET /api/receipts/{recptSn}`이 본인 영수증의 분석 결과를 주고, 남의·없는 영수증은 404 `RECEIPT_NOT_FOUND`다
- 연결 남음: FN-AIM-013-04 (프론트 연결 AI 대화창 [＋] 업로드가 `POST /api/receipts`에 `ocrDmndSrcCd=AIMC0031`, 프론트 저장소에서 /start), FN-AIM-013-06 (프론트 연결 본창 건강 일지 "인식 결과 확인" 모달을 `GET /api/receipts/{recptSn}?petSn=`로 열기, 프론트 저장소에서 /start. 모달 열기 파라미터는 JAJ와 협의)
- 추가: 추가-1 일시 오류 `failRsn`을 마지막 상태 이력에서 읽는 Mapper 쿼리, 추가-2 공통코드는 migration 없이 기준 데이터 SQL에만(각 DB에 한 줄 직접 넣기)

## SCR-AIM-013 대화창 · 영수증 분석 (Python 중심: 원본 저장·결과 보고 내부 API, 결과 조회) — 끝
- 기능: FN-AIM-008-09·013-08 원본 이미지 보관(Python → Java 원본 저장), FN-AIM-010-06 실패해도 원본 유지, FN-AIM-010-01·013-04 판정·출처 기록(Python → Java 결과 보고), FN-AIM-013-06 확인 후 저장(React → Java 결과 조회를 `{recptSn, petSn, ocr}` 모양으로). 기준 AI매니저_기능명세서_v0.11_20261029(DMS, 2026-10-29 확인). 2026-10-29 사람 결정 (나): React → Python `POST /ai/receipts`가 분석 중심. 명세 기능 ID 추가 없음(대조 수 그대로)
- 함께 만드는 것: 없음(`ReceiptService` 상태 처리 재사용)
- 기다리는 것: 없음(ai 저장소 `FakeReceiptRepository` 연결은 ai 세션)
- 완료 조건: Python이 쿠키로 원본을 올리면 RECPT(분석중)가 생기고 `recptSn`을 돌려받는다. 결과 보고가 RECPT 상태·OCR 응답·이력에 남는다. React가 본인 영수증 결과를 `{recptSn, petSn, ocrPrcsSttsCd, failRsn, ocr}`로 받는다
- 연결 남음: FN-AIM-008-09·013-08·010-01·013-04 (Python `FakeReceiptRepository`를 원본 저장·결과 보고 내부 API로, ai 세션), FN-AIM-013-06 (프론트 연결 본창 "인식 결과 확인" 모달 `getChatReceiptResult`를 `GET /api/receipts/{recptSn}?petSn=`로, 프론트 저장소에서 /start)
- 추가: 없음. `ocrRslt` 허용값 밖은 공통 요청 형식 오류 400 `BAD_REQUEST`(기획은 `INVALID_INPUT_VALUE`, 없을 때만 `INVALID_INPUT_VALUE`)

## SCR-AIM-013 대화창 · 영수증 분석 (영수증 정리: Java 동기 OCR 삭제) — 끝
- 기능: FN-AIM-008-07 다시 분석·취소·제한 시간(이제 Python 몫, Java 경로 삭제). 기준 AI매니저_기능명세서_v0.11_20261029(DMS, 2026-10-29 확인). 근거: 2026-10-29 결정 (나), frontend-62 회신(프론트에 Java `POST /api/receipts` 호출 없음)
- 함께 만드는 것: 없음
- 기다리는 것: 없음
- 완료 조건: `POST /api/receipts`·`/{recptSn}/analyze`·`/{recptSn}/cancel`이 없고(404), `OcrClient`·`FakeOcrClient`·`OcrProperties`·동기 분석 코드가 없다. 결과 조회·원본 이미지·저장·내부 API는 그대로 동작한다
- 연결 남음: 없음. 분석중으로 남은 RECPT 정리 배치는 뺐다(2026-10-30 사람 결정, Python이 실패 때 `finally`로 보고)
- 추가: 추가-1 `docs/api/health.md` 검사결과지 분석 설명의 `app.ocr.timeout` 빗댐 문구를 "(설정값)"으로

## SCR-AIM-012 AI 일정 생성 확인 · 케어 항목 이름 연결 — 끝
- 기능: FN-AIM-012-08 같은 이름 케어 항목 안내(연결만, 명세 기능 ID 추가 없음). `CareArticleNameFake`를 JAJ `CareItemService.searchNamesByPets`로 바꾼다
- 함께 만드는 것: 없음(`health` CareItemService 재사용, ai 3층 → health 2층)
- 기다리는 것: 없음
- 완료 조건: `GET /internal/ai/register-context`의 `careArtclNms`가 CARE_ARTCL의 삭제 안 된 항목명을 등록순으로 준다. `CareArticleNameFake`가 없다
- 연결 남음: 없음(FN-AIM-012-04·08 Python 연결은 위 "등록 카드 컨텍스트 내부 API" 절에 그대로)

## SCR-AIM-011 AI 대화창 (첫 답변 요약 제목: 제목 수정 내부 API) — 끝
- 기능: FN-AIM-011-11 대화 제목 자동 생성(첫 답변 요약 제목을 방 생성 뒤 한 번 저장, 명세 기능 ID 추가 없음). 기준 AI매니저_기능명세서_v0.11_20261029(DMS, 2026-10-30 확인). ai 세션 요청(2026-10-30, ai-2e)
- 함께 만드는 것: 없음(`AiConversationService`·`FieldCryptoService` 재사용)
- 기다리는 것: 없음(ai 저장소 Python 구현은 끝, 가짜 Java로 테스트 통과)
- 완료 조건: `PATCH /internal/ai/conversations/{aiCnvrsRoomSn}`이 쿠키 회원 본인 대화의 제목을 암호문으로 바꾸고 200 `ConversationSummary`를 준다. 남의·없는 대화는 404 `NOT_FOUND`, 빈 값·공백만·201자는 400
- 연결 남음: FN-AIM-011-11 (Python이 이 API를 실제로 부르는 확인과 ai `docs/api/internal.md` 초안 행 확정, ai 세션)

## SCR-ADM-110 운영 설정 — 끝
- 기능: FN-ADM-110-01 설정 조회, FN-ADM-110-02 설정 수정, FN-ADM-110-03 최저시급 설정, FN-ADM-110-04 AI 대화 설정
- 함께 만드는 것: `MNGR_TASK_LOG` 테이블(기준 파일 v0.11과 그 변경 절)과 `global/log` 감사 로그 기록 수단(관리자 API 전부가 쓴다)
- 기다리는 것: 없음
- 완료 조건: `PATCH /api/admin/settings`가 범위 밖 값을 400으로 막는다. 저장하면 바뀐 항목의 전후 값이 `MNGR_TASK_LOG`에 남고 다음 `GET`에 바로 보인다
- 연결 남음: FN-ADM-110-01~04 (프론트 연결 관리자 운영 설정 화면 SCR-ADM-110, 프론트 저장소에서 /start)

## SCR-ADM-114 감사 로그 — 끝
- 기능: FN-ADM-114-01 감사 로그 조회, FN-ADM-114-02 감사 로그 상세, FN-ADM-114-03 기록 대상, FN-ADM-114-04 수정·삭제 불가
- 기다리는 것: 감사 로그 기록 수단 (SCR-ADM-110, LJS). 관리자 로그인 기록은 `auth` 로그인 (YAY와 협의)
- 완료 조건: 관리자·작업 유형·기간으로 거른 목록이 최신순으로 나온다. 수정·삭제 API가 없다
- 연결 남음: FN-ADM-114-01·02 (프론트 연결 관리자 감사 로그 화면 SCR-ADM-114와 상세 모달, 프론트 저장소에서 /start)

## SCR-ADM-111 배치 현황 — 끝
- 기능: FN-ADM-111-01 배치 목록 조회, FN-ADM-111-02 실행 이력 조회, FN-ADM-111-03 수동 실행
- 함께 만드는 것: `BTCH_EXCN_HSTRY` 테이블과 기존 배치(`CareMissionJob` 등)의 실행 이력 기록
- 기다리는 것: 배치 담당과 이력 기록 위치 협의 (SJM·JAJ)
- 완료 조건: 배치가 돌 때마다 시작·종료·결과가 이력에 남는다. 실패 건을 수동 실행할 수 있다
- 결정: 실패 건수는 컬럼을 추가하지 않고 응답에서 뺐다. 목록은 코드에 있는 4종(2026-11-01)
- 완료 조건 결과: 검증 `확인 못 함 있음`(내 것 없음, 2026-11-01, 1·2회차 기획 밖 문서로 실패 뒤 추가-1로 재검증), 사람 확인 2026-11-01
- 연결 남음: FN-ADM-111-01·02·03 (프론트 연결 관리자 배치 현황 화면 SCR-ADM-111, 프론트 저장소에서 /start)

## SCR-ADM-112 외부 API 사용량 — 끝
- 기능: FN-ADM-112-01 사용량 조회, FN-ADM-112-02 AI 토큰 사용량, FN-ADM-112-03 실패 목록 조회, FN-ADM-112-04 로그 보관
- 함께 만드는 것: `EXTL_API_CALL_LOG` 테이블과 `global/external` 호출부 기록
- 기다리는 것: 로그 보관 기간 결정(사람), AI 토큰 수 전달 방식 (Python AI 서버)
- 완료 조건: 종류별 일별 호출 수·실패율이 나온다
- 결정: 종류는 명세 7종에 맞춰 공통코드 `SYSC0024`·`SYSC0025`를 추가하고 `SYSC0019`는 enum에서만 뺐다. Java 호출부 기록은 실제 구현을 붙일 때 한다. 로그 보관은 영구(FN-ADM-112-04), 토큰은 Python 통지 `POST /internal/ai/external-api-calls`로 받는다(2026-11-01)
- 완료 조건 결과: 검증 `확인 못 함 있음`(내 것 없음, 2026-11-01, 1·2회차 SQL 실행 근거 없음과 기획 밖 문서로 실패 뒤 Mapper SQL 테스트·추가-1로 재검증), 임시 로컬 DB에서 Mapper SQL 5개 실행 확인, 사람 확인 2026-11-02
- 연결 남음: FN-ADM-112-01·02·03 (프론트 연결 관리자 외부 API 사용량 화면 SCR-ADM-112, 프론트 저장소에서 /start), FN-ADM-112-02 (Python 호출 통지, ai 세션)

## SCR-ADM-010 회원 목록 — 끝
- 기능: FN-ADM-010-01 회원 목록 조회, FN-ADM-010-02 회원 검색, FN-ADM-010-03 회원 필터, FN-ADM-010-04 회원 상세 이동
- 기다리는 것: `member` 관리자 조회 협의 (YAY)
- 완료 조건: 상태·가입 경로·가입일로 거른 목록이 페이징되고 개인정보는 마스킹된다
- 결정: `member`의 `AdminMemberController`에 둔다. 조회만이라 감사 로그는 남기지 않는다. 정지 종료일 표시는 SCR-ADM-012에서 정한다. 가입 경로는 `MBR.JOIN_PATH_CD`로 충분해 `SNS_ACNT_LINK`는 쓰지 않는다(2026-11-02)
- 완료 조건 결과: 검증 `확인 못 함 있음`(내 것 없음, 2026-11-02, 8082 실제 호출로 SQL 확인), 사람 확인 2026-11-02
- 연결 남음: FN-ADM-010-01 (프론트 연결 관리자 회원 목록 화면 `features/adminMember/api.js` `getAdminMembers`, 프론트 저장소에서 /start), FN-ADM-010-04 (프론트 연결 행 클릭 이동, 프론트 저장소에서 /start)

## SCR-ADM-012 회원 제재 — 끝
- 기능: FN-ADM-012-01 회원 정지, FN-ADM-012-02 정지 효과, FN-ADM-012-03 정지 해제, FN-ADM-012-04 기간 만료 자동 해제, FN-ADM-012-05 정지 대상 제한, FN-ADM-012-06 강제 탈퇴 제외
- 기다리는 것: `MBR` 정지종료일시 migration, 정지 회원 요청마다 차단 여부(구현 계획 6절, 사람), `member` 협의 (YAY)
- 완료 조건: 정지한 회원은 로그인과 API 호출이 막히고 기간이 지나면 풀린다. 관리자는 정지할 수 없다
- 결정: `member`의 `MemberSuspensionService`·`MemberSuspensionExpiryJob`에 둔다. 종료 일시는 오늘+N일 00:00, 만료 판정은 Java 시각. 정지할 때 세션을 폐기한다. 정지 일시·사유·처리자는 `MBR_STTS_HSTRY`에서 읽는다. 정지 안내 알림은 기능명세서에 없어 넣지 않았다(2026-11-02)
- 완료 조건 결과: 검증 `확인 못 함 있음`(내 것 없음, 2026-11-02, 컬럼 없는 DB라 실제 호출 일부만), 사람 확인 2026-11-02(v0.11 (4) 실행 뒤)
- 연결 남음: FN-ADM-012-01·03·05 (프론트 연결 관리자 회원 제재 화면 `features/adminMember/api.js` `suspendMember`·`releaseMember`와 회원 정지 모달, 프론트 저장소에서 /start), 정지 회원 목록 (프론트 연결 `getAdminSuspensions`, 프론트 저장소에서 /start)

## SCR-ADM-013 탈퇴 회원 — 끝
- 기능: FN-ADM-013-01 탈퇴 회원 목록 조회, FN-ADM-013-02 파기 현황 연결
- 기다리는 것: `member` 협의 (YAY)
- 완료 조건: 보관 기간 안의 탈퇴 회원이 마스킹되어 파기 예정일과 함께 나온다
- 완료 조건 결과: 검증 `확인 못 함 있음`(2026-11-02, 확인 못 함(내 것) 없음), 사람 확인 2026-11-02
- 연결 남음: FN-ADM-013-01 (프론트 연결 관리자 탈퇴 회원 화면 `features/adminMember/api.js` `getAdminWithdrawnMembers`, 프론트 저장소에서 /start), FN-ADM-013-02 (프론트 연결 안내 문구와 [배치 현황] 이동 → SCR-ADM-111, 프론트 저장소에서 /start)

## SCR-ADM-113 관리자 계정 — 대기
- 기능: FN-ADM-113-01 관리자 목록 조회, FN-ADM-113-02 관리자 지정, FN-ADM-113-03 관리자 해제
- 기다리는 것: `member` `AdminMemberController` 협의 (YAY)
- 완료 조건: 지정·해제가 `MBR_AUTHRT_CD`를 바꾸고 감사 로그에 남는다. 마지막 관리자는 해제할 수 없다

## SCR-ADM-011 회원 상세 — 대기
- 기능: FN-ADM-011-01 기본 정보 조회, FN-ADM-011-02 반려동물 탭, FN-ADM-011-03 약관 동의 탭, FN-ADM-011-04 포인트 탭, FN-ADM-011-05 상태 이력 탭, FN-ADM-011-06 민감 데이터 비노출
- 기다리는 것: member·pet 조회 service (YAY·SJM)
- 완료 조건: `admin`이 여러 도메인 service로 조합한 상세가 나오고 건강기록·가계부·대화 내용은 없다

## SCR-ADM-020 게시글·댓글 — 대기
- 기능: FN-ADM-020-01 게시글 목록 조회, FN-ADM-020-02 게시글 검색·필터, FN-ADM-020-03 게시글 상세 조회, FN-ADM-020-04 게시글 블라인드, FN-ADM-020-05 블라인드 복구, FN-ADM-020-06 댓글 조회·삭제
- 기다리는 것: `community` 블라인드 service 협의 (KSJ)
- 완료 조건: 블라인드한 글은 사용자 목록에서 빠지고 복구하면 돌아온다. 상태 이력이 남는다

## SCR-ADM-030 신고 처리 — 대기
- 기능: FN-ADM-030-01 신고 목록 조회, FN-ADM-030-02 신고 필터, FN-ADM-030-03 신고 상세 조회, FN-ADM-030-04 신고 처리 완료, FN-ADM-030-05 신고 반려, FN-ADM-030-06 처리 결과 통보, FN-ADM-030-07 자동 블라인드 건 확인, FN-ADM-030-08 신고 대상 범위
- 기다리는 것: `DCLR` 조치 코드 migration, `community` 신고 service 협의 (KSJ), 회원 정지 (SCR-ADM-012)
- 완료 조건: 처리·반려하면 신고자에게 인앱 알림이 가고, 조치 코드가 작성자 정지면 회원이 정지된다

## SCR-ADM-040 공지사항 — 대기
- 기능: FN-ADM-040-01 공지 목록 조회, FN-ADM-040-02 공지 등록, FN-ADM-040-03 공지 알림 발송, FN-ADM-040-04 공지 수정·비노출, FN-ADM-040-05 상단 고정
- 기다리는 것: `NTC_MTTR` 발송 컬럼 migration, `support` 협의 (KSJ)
- 완료 조건: 알림 발송을 체크해 등록하면 전체 회원 인앱 알림이 한 번만 발행된다

## SCR-ADM-041 FAQ — 대기
- 기능: FN-ADM-041-01 FAQ 목록 조회, FN-ADM-041-02 FAQ 등록·수정, FN-ADM-041-03 FAQ 비노출, FN-ADM-041-04 FAQ 카테고리
- 기다리는 것: `support` 협의 (KSJ)
- 완료 조건: 비노출한 FAQ는 사용자 고객센터에서 빠진다

## SCR-ADM-042 1:1 문의 — 대기
- 기능: FN-ADM-042-01 문의 목록 조회, FN-ADM-042-02 문의 필터, FN-ADM-042-03 문의 상세 조회, FN-ADM-042-04 답변 등록, FN-ADM-042-05 답변 수정, FN-ADM-042-06 삭제된 문의 처리, FN-ADM-042-07 캐릭터 오류 처리, FN-ADM-042-08 펫돌봄 분쟁 연결
- 기다리는 것: `support` 협의 (KSJ). 캐릭터 오류 처리는 `pet` 캐릭터 service (LJS, SCR-MYP-010 `finishErrorCheck` 연결)
- 완료 조건: 답변하면 작성자에게 알림이 가고 상태가 답변 완료가 된다. 답변 삭제 API는 없다

## SCR-ADM-070 아이템 — 대기
- 기능: FN-ADM-070-01 아이템 목록 조회, FN-ADM-070-02 아이템 등록, FN-ADM-070-03 아이템 수정·판매 중지, FN-ADM-070-04 아이템 정렬
- 기다리는 것: 없음
- 완료 조건: 판매 중지한 아이템은 상점 목록에서 빠지고 이미 산 회원의 보유는 그대로다

## SCR-ADM-090 알림 템플릿 — 대기
- 기능: FN-ADM-090-01 템플릿 목록 조회, FN-ADM-090-02 템플릿 등록·수정, FN-ADM-090-03 기본 문구 사용
- 기다리는 것: 없음
- 완료 조건: 수정한 템플릿이 다음 발송부터 쓰이고, 템플릿이 없으면 기본 문구로 보낸다

## SCR-ADM-091 발송 이력 — 대기
- 기능: FN-ADM-091-01 발송 이력 조회, FN-ADM-091-02 재발송 없음
- 기다리는 것: 없음
- 완료 조건: 기간·채널·결과로 거른 이력과 실패 사유가 나온다

## SCR-ADM-021 실종제보 — 대기
- 기능: FN-ADM-021-01 실종제보 목록 조회, FN-ADM-021-02 실종제보 상세 조회, FN-ADM-021-03 기간 변경 없음
- 기다리는 것: `community` 협의 (KSJ)
- 완료 조건: '찾았어요' 이후 건은 제목·상태·처리 시각만 나온다

## SCR-ADM-022 펫돌봄 거래 — 대기
- 기능: FN-ADM-022-01 모집글 목록 조회, FN-ADM-022-02 매칭 상세 조회, FN-ADM-022-03 채팅 내용 비노출
- 기다리는 것: `community` 협의 (KSJ)
- 완료 조건: 매칭 상세에 상태 이력·조건 버전·완료 사진이 나오고 채팅 내용은 없다

## SCR-ADM-023 노출 콘텐츠 — 대기
- 기능: FN-ADM-023-01 오늘의 건강팁 현황, FN-ADM-023-02 건강팁 지정·해제, FN-ADM-023-03 지정 글 자동 대체, FN-ADM-023-04 오늘의 이야기 문구 관리, FN-ADM-023-05 오늘의 이야기 선택, FN-ADM-023-06 AI 코멘트 관리
- 기다리는 것: 오늘의 이야기 저장 위치(구현 계획 6절, 사람), `community` 협의 (KSJ)
- 완료 조건: 지정한 건강팁이 메인 노출 3건에 들어가고 지정 글이 삭제되면 자동 선정 글로 바뀐다

## SCR-ADM-050 펫뉴스 — 대기
- 기능: FN-ADM-050-01 뉴스 목록 조회, FN-ADM-050-02 관리자 뉴스 등록·수정, FN-ADM-050-03 수집 기사 관리, FN-ADM-050-05 분류 키워드 관리, FN-ADM-050-06 수집 기사 자동 분류, FN-ADM-050-07 뉴스 수집 실행
- 기다리는 것: `KWRD_DICT` migration, `community` 뉴스 협의 (KSJ)
- 완료 조건: 숨긴 수집 기사는 사용자 뉴스에서 빠진다

## SCR-ADM-101 품종·지역 — 대기
- 기능: FN-ADM-101-01 품종 관리, FN-ADM-101-02 지역 조회
- 기다리는 것: 품종 마스터 협의 (KSJ)
- 완료 조건: 추가한 품종이 반려동물 등록의 품종 목록에 나온다

## SCR-ADM-051 약관 — 대기
- 기능: FN-ADM-051-01 약관 목록 조회, FN-ADM-051-02 약관 새 버전 등록, FN-ADM-051-03 약관 버전 이력 조회, FN-ADM-051-04 관리 대상 약관, FN-ADM-051-05 기존 회원 재동의
- 기다리는 것: 약관 2종 코드 migration, `member` 약관 협의 (YAY)
- 완료 조건: 새 버전을 등록하면 이전 버전은 이력으로 남고 가입 화면은 새 버전을 보여 준다

## SCR-ADM-060 장소 — 대기
- 기능: FN-ADM-060-01 장소 목록 조회, FN-ADM-060-02 장소 노출 설정, FN-ADM-060-03 place_url 수동 매칭, FN-ADM-060-04 공공데이터 수집 실행, FN-ADM-060-05 장소 직접 등록 제외
- 기다리는 것: `place` 협의 (YAY)
- 완료 조건: 노출을 끈 장소는 사용자 장소 검색에서 빠진다

## SCR-ADM-071 케어미션 — 대기
- 기능: FN-ADM-071-01 케어미션 목록 조회, FN-ADM-071-02 케어미션 수정, FN-ADM-071-03 변경 반영 시점
- 기다리는 것: `pet` 케어미션 협의 (SJM)
- 완료 조건: 수정한 배점이 다음 출제부터 반영되고 이미 낸 미션은 그대로다

## SCR-ADM-072 포인트 — 대기
- 기능: FN-ADM-072-01 포인트 원장 조회, FN-ADM-072-02 포인트 수동 조정, FN-ADM-072-03 회원 화면 표시
- 기다리는 것: `pet` 포인트 협의 (SJM)
- 완료 조건: 수동 조정이 잔액과 원장에 함께 반영되고 사유 없이는 막힌다. 잔액이 음수가 되지 않는다

## SCR-ADM-080 라우팅 매핑표 — 대기
- 기능: FN-ADM-080-01 매핑표 조회, FN-ADM-080-02 매핑 등록·수정·삭제, FN-ADM-080-03 우선순위 변경, FN-ADM-080-04 매핑표 제공, FN-ADM-080-05 초기 데이터
- 기다리는 것: `AI_ROUT_MPNG` migration·초기 데이터, Python AI 서버가 매핑표 조회 API를 붙이는 것 (ai 세션)
- 완료 조건: 수정한 매핑표가 내부 조회 API에 바로 나온다

## SCR-ADM-081 키워드 사전 — 대기
- 기능: FN-ADM-081-01 키워드 조회, FN-ADM-081-02 키워드 등록·수정·삭제, FN-ADM-081-03 초기 데이터
- 기다리는 것: `KWRD_DICT` migration·초기 데이터
- 완료 조건: 용도별(일정·가계부) 키워드가 내부 조회 API에 바로 나온다

## SCR-ADM-082 처리 현황 — 대기
- 기능: FN-ADM-082-01 영수증 OCR 현황, FN-ADM-082-02 캐릭터 생성 현황, FN-ADM-082-03 검사결과지 분석 현황
- 기다리는 것: 검진 PDF 분석 실패 집계 service (JAJ)
- 완료 조건: 상태별 건수와 실패율이 나온다

## SCR-ADM-001 대시보드 — 대기
- 기능: FN-ADM-001-01 관리자 접근 제어, FN-ADM-001-02 운영 현황 조회, FN-ADM-001-03 처리 대기 건수, FN-ADM-001-04 이용 통계 조회, FN-ADM-001-05 통계 기간 선택
- 기다리는 것: 각 도메인 집계 service (다른 관리자 마일스톤 뒤)
- 완료 조건: 일반 회원은 403이다. 처리 대기(신고·문의·캐릭터 오류·OCR·알림 실패) 건수가 각 목록 건수와 같다

## SCR-ADM-114 감사 로그 · 감사 로그 API develop 병합 — 끝
- 기능: 명세 기능 ID 추가 없음. FN-ADM-114-01·02
- 기다리는 것: 프론트 SCR-ADM-114 감사 로그 화면(끝, 2026-11-01)
- 완료 조건: `GET /api/admin/audit-logs`, `/admins`, `/{mngrTaskLogSn}`가 develop에 있다
- 이미 바뀐 것: 커밋 38f930a (origin/LJS)

## SCR-ADM-112 외부 API 사용량 · 사용량 조회 API·SYSG05 코드 수정·호출 로그 — 대기
- 기능: 명세 기능 ID 추가 없음. FN-ADM-112-01·02·03
- 기다리는 것: 프론트 SCR-ADM-112 외부 API 사용량 화면(끝, 2026-11-01)
- 완료 조건: 프론트 `features/apiUsage/api.js` 가짜를 바꿀 수 있게 `/api/admin/external-api-usage` 사용량·일별·토큰·실패 목록이 나온다. SYSG05가 명세 7종(AI 대화·AI 분류·OCR·캐릭터 생성·PDF 분석·카카오 로컬·메일)과 맞는다. Java 호출과 Python 통지가 `EXTL_API_CALL_LOG`에 남는다
- 남은 것(2026-11-02): 조회 API 4종·SYSG05·Python 통지 API는 SCR-ADM-112에서 끝. Java 호출 기록만 남았다. 지금 구현이 전부 가짜라 하지 않기로 했고(2026-11-01 사람), 실제 구현을 붙일 때 한다. 먼저 고를 작업이 아니다

## SCR-ADM-010 회원 목록 · 회원 목록 조회 API — 끝
- 기능: 명세 기능 ID 추가 없음. FN-ADM-010-01·02·03·04
- 기다리는 것: 프론트 SCR-ADM-010 회원 목록 화면(끝, 2026-11-01)
- 완료 조건: 프론트 `features/adminMember/api.js` 가짜를 바꿀 수 있게 `GET /api/admin/members`가 최신 가입순 20건씩 나온다. 닉네임 부분 일치·이메일 완전 일치 검색과 상태·가입 경로·권한·가입일 기간 필터가 AND로 걸린다. 이메일은 앞 3자만 보이게 마스킹된다. 프론트가 가정한 필드 이름(`maskedAcntEml`)과 조회 조건 이름(`q`·`status`·`joinPath`·`role`·`bgngYmd`·`endYmd`)을 맞춘다
- 결과: `GET /api/admin/members` 완료, 조회 조건 이름과 `maskedAcntEml`은 프론트 가정과 같다(2026-11-02). 실제 연결은 프론트 저장소 몫

## SCR-ADM-012 회원 제재 · 정지·해제·정지 회원 목록 API — 끝
- 기능: 명세 기능 ID 추가 없음. FN-ADM-012-01·02·03·04·05
- 기다리는 것: 프론트 SCR-ADM-012 회원 제재 화면(끝, 2026-11-02)
- 완료 조건: 프론트 `features/adminMember/api.js` 가짜를 바꿀 수 있게 `POST`·`DELETE /api/admin/members/{mbrSn}/suspension`과 정지 회원 목록 API가 있다. 정지·해제가 회원 상태·상태 이력에 남고, 관리자·본인·탈퇴 회원 정지는 거절되며, 기간 만료 회원은 배치가 해제한다
- 완료 조건 결과: 위 SCR-ADM-012 회원 제재 절과 함께 끝. 목록 경로는 `GET /api/admin/members/suspensions`. 해제는 본문 있는 `DELETE`라 프론트가 사유를 `data`로 보낸다(2026-11-02)

## 제외
- FN-COM-002-01·03·04·05·06·07·08·10·12·13·14·16·17·18·19·20·21·22·23: 화면만 있다(서버 동작 없음)
- FN-COM-002-11: 채팅 안 읽은 수 API는 KSJ(채팅, 마일스톤 담당 표 COM-009)
- FN-COM-003-02·03·04·06·08·09: 화면만 있다(복귀 파라미터·랜딩 이동은 프론트)
- FN-COM-003-05: 로그인 처리는 YAY(로그인·로그아웃 API). 복귀 이동은 화면만
- FN-COM-003-10: AI 대화창 세션은 Python AI 서버와 프론트(이 저장소 아님)
- FN-COM-004-01·15·16: 화면만 있다
- FN-COM-004-14: 채팅방을 보는 동안 거래 알림을 만들지 않는 판단은 알림을 발행하는 채팅 쪽 KSJ
- FN-COM-005-01·03·04·05·06·08·10: 화면만 있다
- FN-COM-005-07: 로그아웃 API는 YAY(`POST /auth/logout`)
- FN-COM-005-09: AI 대화 이력은 Python AI 서버(이 저장소 아님)
- FN-COM-006-01~09: 화면만 있다(GNB 2차 메뉴)
- FN-COM-007-01~07: 화면만 있다(AI 새 창 런처)
- FN-COM-008-01·15·17·18·21: 화면만 있다
- FN-COM-009-01~20: 채팅 모달 API는 KSJ(마일스톤 담당 표 COM-009)
- FN-AIM-011-01~06·17·18·20: 화면만 있다(새 창, 사이드바, 창 종료·세션 공유는 프론트와 Python 미들웨어)
- FN-AIM-011-15·16: AI 컨텍스트 범위·개인 데이터 비전달은 Python AI 서버(이 저장소 아님)
- FN-AIM-001-01~06, FN-AIM-002-01~11, FN-AIM-004-01~13, FN-AIM-005-01~07: AI매니저 대화 API는 Python AI 서버(이 저장소 아님, 마일스톤 담당 표 LJS 줄)
- FN-AIM-012-01~03·05~07·09~12·15~18: AI매니저 대화의 일정 생성 확인은 Python AI 서버(이 저장소 아님, 04·08은 내부 API만 LJS)
- FN-AIM-012-13·14: 일정 저장은 JAJ(`health` 일정 등록 재사용, 마일스톤 담당 표 AIM-012-13·14)
- FN-AIM-006-08: SNS 가입 이메일 확보는 YAY(가입), 발송 쪽 서버 동작 없음
- FN-AIM-008-01: 진입 버튼과 등록 방법 선택은 JAJ(FN-HLT-002-33)
- FN-AIM-008-08: 직접 입력은 JAJ의 검진기록 등록·수정 모달
- FN-AIM-009-05·06·14·15·25: 화면만 있다(행 추가·삭제, 점선 표기, 다시 촬영, 저장 안내)
- FN-AIM-010-02·03: 화면만 있다(실패 안내, 재촬영 전환)
- FN-AIM-010-04: 직접 입력은 JAJ의 검진기록 등록·수정 모달(FN-HLT-002-26~31)
- FN-MYP-001-02~05: 마이페이지 홈은 SJM(캐릭터 얼굴인 001-01만 LJS)
- FN-MYP-002-01·03~08: 반려동물 프로필은 SJM(캐릭터 얼굴인 002-02만 LJS)
- FN-MYP-003-01~07, FN-MYP-004-01~04, FN-MYP-005-01~08, FN-MYP-006-01~05: 반려동물·공동양육은 SJM
- FN-MYP-010-05: 대표 반려동물 지정은 SJM(마일스톤 담당 표 "MYP-010, 05 제외")
- FN-MYP-010-15·22: 화면만 있다(상점 열기, 0마리 안내)
- FN-MYP-011-01·04: 화면만 있다(임시 착용 미리보기, 담은 목록)
- FN-MYP-020-01~10: 포인트·케어미션은 SJM
- FN-MYP-030-01~21, FN-MYP-031-01~06: 가계부는 SJM
- FN-MYP-040-01~12: 회원정보 수정은 SJM
- FN-AIM-013-01·02·03·05·07: 대화창 화면과 Python AI 서버(첨부 버튼·분기·반려동물 되묻기·결과 카드·실패 안내). 파일 검증과 결과 데이터는 SCR-AIM-008 `POST /receipts` 그대로
- FN-AIM-013-09: 요약 메시지 작성은 Python AI 서버, 저장은 SCR-AIM-011 대화 저장 내부 API 그대로
- FN-HLT-002-01·05·06·09~33, FN-HLT-003-01·12·13·21~24·29~40, FN-HLT-005-01~05·11~15, FN-HLT-007-01~06, FN-HLT-011-01~10: 건강기록은 JAJ(영수증 결과 확인인 002-34~46만 LJS)

## 대조
- 공통레이아웃: 명세 117 = 마일스톤 35 + 제외 82
- AI매니저: 명세 138 = 마일스톤 63 + 제외 75
- 마이페이지: 명세 112 = 마일스톤 23 + 제외 89
- AI매니저 v0.10 SCR-AIM-013(v0.9 신설): 명세 9 = 마일스톤 3 + 제외 6
- 건강기록: 명세 86 = 마일스톤 13 + 제외 73
- 관리자: 명세 135 = 마일스톤 128 + 제외 7 (FN-ADM-100-01~04 공통코드 화면, 2026-11-01 사람 결정: 코드 변경이 드물어 SQL로 처리 / FN-ADM-023-07·050-04·073-01, 2026-10-30 결정, 구현 계획 B-3·B-4·B-5). 기능명세서 v0.4 사본 기준, DMS 업로드 전
