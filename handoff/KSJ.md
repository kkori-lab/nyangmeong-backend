# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-005 우리동네 펫돌봄 채팅 · 알림 이동 경로 — 진행 중 (승인 2026-11-13 12:13, 시작 2026-11-13 12:11, 검증 1회차 화면 확인 못 함으로 실패, 사람 확인은 완료)

- 근거: 펫돌봄 거래 알림은 모두 `CareMatchService.notifyReceiver`(매칭 요청·수락·완료 요청·완료 확인·거래 완료·나가기 취소·14일 무응답)에서 `mvmnPath=null`로 발행된다. 공용 DB 최근 AIMC0009 5건 모두 `MVMN_PATH` NULL(2026-11-13 확인). 프론트 `NotificationDropdown`은 AIMC0009를 누르면 `mvmnPath`의 `otoChatRoomSn` 쿼리로 채팅 모달 방을 여는데, null이면 0이 되어 목록만 열린다. 프론트 채팅 화면 경로는 `/chat?otoChatRoomSn=`(`ChatPage`)
- 걸치는 FN-COM-004-09·FN-CMU-005-06~10 알림 이동 경로: 바꿀 파일
  - `src/main/java/com/nyangmeong/community/service/CareMatchService.java`: `notifyReceiver`가 `mvmnPath`에 `/chat?otoChatRoomSn={otoChatRoomSn}`을 넣는다(상수 `CHAT_ROOM_PATH = "/chat?otoChatRoomSn=%d"`, `LostReportService.EXPIRY_ALARM_PATH`와 같은 방식)
  - `src/test/java/com/nyangmeong/community/service/CareMatchServiceTest.java`: 거래 알림 이벤트의 `mvmnPath`가 그 방 경로인지 검사하는 테스트 추가(기존 테스트는 고치지 않는다)
  - `docs/api/notification.md`: `mvmnPath` 설명에 「펫돌봄 거래(AIMC0009)는 `/chat?otoChatRoomSn={방 번호}`, 프론트가 쿼리로 채팅 모달을 연다」
  - `docs/api/issues.md` 공통레이아웃 11번: 펫돌봄 거래 알림은 `mvmnPath` 쿼리를 프론트가 해석하는 것으로 정했다고 한 줄 덧붙인다(공동양육 초대는 그대로 미정)
  - 부를 API: 없음(알림 저장은 `EventPublisher.publish`, 조회 `GET /notifications`는 응답 형식 같음)
  - 완료 조건 — 명령: `./gradlew build -q`. 화면: 서버 재시작 뒤 펫돌봄 거래 단계 하나 실행(예: 보호자로 매칭 요청 `POST /api/care-matches`, 상대가 그 채팅방을 보고 있지 않을 때) → 상대 회원으로 `GET /api/notifications` 첫 항목 `mvmnPath`가 `/chat?otoChatRoomSn={방}`. 공용 DB `INAPP_NOTI` 새 행 `MVMN_PATH`도 같은 값
- 이번엔 안 함: 이미 저장된 NULL 알림은 고치지 않는다(알림 행만으로 방을 알 수 없음, 새 알림부터). 공동양육 초대 알림 식별자(issues 11번 앞부분)는 pet·notification 담당 몫
- 프론트 연결: 없음(드롭다운이 이미 이 형식을 읽는다)
- 새 의존성: 없음

## 지난 작업

SCR-CMU-004 목록 기본 정렬(2026-11-13), SCR-CMU-002 운영 지역 변환 설정(PR #226, 2026-11-11), SCR-CMU-001 내 글만 보기(PR #211, 2026-11-10), SCR-CMU-001 케어미션 반영(PR #209, 2026-11-10), SCR-CMU-003 검색어 설정 인코딩(PR #204, 2026-11-09), SCR-CMU-002 만료 알림 경로(PR #203, 2026-11-09, 공용 DB SYSC0032 입력·확인용 알림 정리 끝, 외부 뉴스 비우고 재수집 끝), SCR-CMU-003 검색어 반려 3종(PR #201, 2026-11-09), SCR-CMU-004 작성자 조회수 제외(커밋 0328347, 2026-11-09), SCR-CMU-002 작성자 조회수 제외(커밋 f0cafb8, 2026-11-09), SCR-CMU-002 품종 선택 항목·만료 7일·1일 전 알림(PR #190, 2026-11-06), SCR-CMU-005 14일 무응답 자동 종료(PR #186, 2026-11-06), SCR-CMU-001·002·004·005 탈퇴 회원 표시(PR #164·#166·#170·#172)·SCR-ADM-022 완료 사진 열람(2026-11-05), SCR-CMU-004 펫돌봄 대기 4건(2026-11-04), SCR-CMU-002 전북 밖 좌표 거절(PR #154)·시드 게시판 코드(PR #147), SCR-CMU-005 펫돌봄 채팅(PR #139) — 모두 끝남

## 다음에 할 것
- 검증 루프 1회차 실패로 보관 중. 기획은 고치지 않는다. 루프를 버리려면 `! sh scripts/verify-loop.sh clear`. 실패 항목: 화면 확인 못 함(거래 알림 `mvmnPath` 실서버·공용 DB 확인을 검증 에이전트가 공용 DB 쓰기 권한 거부로 못 함). 코드 고칠 곳은 없다
- 사람 확인 기록(2026-11-13): 재시작한 서버에서 보호자 96이 방 268로 매칭 요청 → 회원 1 알림 `INAPP_NOTI` 13:45:56 행 `MVMN_PATH=/chat?otoChatRoomSn=268`, 매칭 108 취소(정리) → 13:50:12 취소 알림도 같은 경로(서버 로그). 다음 세션에서 `/verify`를 다시 돌려 읽기 조회(`SELECT MVMN_PATH FROM INAPP_NOTI WHERE MBR_SN = 1 AND NOTI_TYPE_CD = 'AIMC0009' ORDER BY INAPP_NOTI_SN DESC LIMIT 2`)로 판정받는다
- 사람이 할 일: 기획 담당에게 커뮤니티 기능명세서 FN-CMU-004-01 「기본 최신순」을 「모집 상태(모집중 → 매칭완료 → 거래완료) → 같은 상태는 최신순」으로 고쳐 달라고 알린다. 관리자 펫돌봄 목록은 최신순 그대로
- 사람이 할 일: 배포 담당에게 운영 서버 비공개 설정·환경변수에 `app.kakao-local.rest-api-key`가 있는지 확인(없으면 배포 뒤 실종·펫돌봄 글 작성이 502). 운영에 `app.region.client`를 따로 정해 두었는지도 확인
- 사람이 할 일: 장소 검색 `app.kakao-local.client`가 운영에서도 설정이 없어 `fake`라고 YAY·LJS에게 알린다
- 사람이 할 일: 디스코드의 `application.properties`에서 `app.news.query` 줄을 지우거나 주석으로 바꾸고 팀에 알린다(한글이 깨져 뉴스 수집이 0건이 됨)
- 사람이 할 일: 공용 DB에 `SYSC0031`(「v0.13 (3)」, PR #186) INSERT가 들어갔는지 확인(`SET NAMES utf8mb4;` 먼저, PR에 완료 댓글). 팀에 "무응답 배치가 첫 실행 때 시드의 오래된 매칭(2026-11-06 확인 15건)을 정리한다"고 알린다
- 확인 방법 메모: 배치 수동 실행은 대상 전체를 바로 처리한다. 사람 확인 전에 대상 건수부터 조회한다
- 다음 작업: 로그아웃 때 채팅 소켓 끊기는 백엔드 작업 없음(사람 결정 2026-11-11). 로그아웃한 창은 프론트가 이미 끊고(`UserHeader`의 `useChatUnreadCount(Boolean(me))` → `deactivate`), 다른 탭·채팅 창에 로그아웃을 알리는 것은 프론트 저장소에서 /start. 탈퇴 회원 표시는 커뮤니티 끝남(문의·신고는 관리자 전용이라 대상 없음), 남은 후보는 공동 보호자 목록(`PetMapper.selectCaregivers`, pet 담당 확인)·세 회원 DB 테스트(새 의존성, 팀 합의)
- 사람이 할 일: 프론트에 「작성자를 눌러 이동하는 곳이 있나, 탈퇴 회원을 막아야 하나」를 묻는다(필요하면 후속으로 그 응답에만 탈퇴 여부 필드. 상세·댓글 응답에는 `mbrSn`이 그대로 나간다). LJS에게 `PostMapper.xml`·`CommentMapper.xml` 관리자 쿼리 병합 충돌을 미리 알린다
- 메모: 시드 사진은 첨부 루트(`app.file.root-dir`, 비우면 `FileRootResolver`의 OS별 기본 경로) 아래 `seed/`에 있어야 보인다. 이 PC는 `uploads/seed`를 복사했고 완료 사진 78장은 있는 펫돌봄 사진을 돌려 채웠다(2026-11-09). 아직 파일 없는 시드 사진 `seed/chat` 159·`seed/inquiry` 102·`seed/news` 14는 채우지 않기로 함(사람 결정 2026-11-09). 팀원에게 `uploads/seed` 압축본과 복사 위치를 공유한다.
- 사람이 할 일: PR 병합 뒤 팀에 "공용 DB 펫돌봄·실종 시드 글 지역을 전북 읍면동으로 바꿨고, `GET /regions`가 전북만 준다"고 알린다. 로컬 `app.region.client`는 `kakao`로 팀에 공유함(2026-11-11, 키 필요)
- 사람이 할 일: 공지 정렬 확인으로 `UPEND_FIX_YN='Y'`로 바꾼 공지가 있으면 `UPDATE NTC_MTTR SET UPEND_FIX_YN='N' WHERE NTC_MTTR_SN=<번호>;`로 되돌린다. 공지 작성은 DB가 아닌 관리자 화면이 생기기 전까지 직접 넣는다
- 정리: 시험용 실종 글 101~104(공용 DB) 중 지우지 않은 것은 `DELETE /api/lost-reports/{lostTpofSn}`로 지운다(글 104 포함)
- 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `spring.servlet.multipart.max-request-size`를 100MB로 바꾼다(배포 서버 설정이 있으면 같이)
- 정리: 검증·확인용으로 만든 문의 223·225·226·227·229·203(203은 삭제 확인 중 DEL_YN='Y'로 이미 지움, 공용 DB, 첨부 파일 4082~4085·4088~4097 포함)와 `uploads/`의 테스트 이미지. 본인 접수 상태 문의는 `DELETE /api/inquiries/{번호}`로 지울 수 있고, 답변된 문의는 직접 정한다
