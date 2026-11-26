# LJS 핸드오프

## 지금 하는 것

SCR-ADM-114 감사 로그 · 30일 보관 후 삭제 — 끝 (2026-11-26). 감사 로그 정리 배치 `AuditLogPurgeJob`(SYSC0034, 매일 03:30)이 오늘(KST)−30일 00:00보다 앞선 `MNGR_TASK_LOG`를 지운다(보관 일수 코드 상수 30, 관리자 삭제 API는 없음). 공통코드 v0.15 (2)는 공용 DB에 실행함. 검증 `통과`(2회차), 사람 확인 2026-11-26, 시작 2026-11-26 14:41

## 다음에 할 것

- 사람(DMS, SCR-ADM-114): 관리자 기능명세서 FN-ADM-114-04 설명을 "관리자는 고치거나 지울 수 없고, 30일이 지난 로그는 배치가 매일 지운다"로. 요구사항정의서에 감사 로그 보관 문구가 있으면 같이
- 선택: `docs/api/global.md` 배치 목록 행의 "배치 6종" 문구가 실제(14종)와 다르다
- SCR-SEED 2차(엑스트라·관리자) 시작: `/start SCR-SEED`. 새 회원으로 엑스트라 50명 이상을 실제 사용자처럼(`People.EXTRAS`에 추가, 글·댓글은 사람 말투), 관리자 화면용 신고·자동 블라인드·자동 삭제(사유는 `ReportService.AUTO_BLIND_RSN`·`AUTO_DELETE_RSN` 상수)·정지·탈퇴·답변 대기 문의·대시보드 통계
- 사람(공용 DB 적용, 발표 2026-12-08의 3~4일 전): README "시연용 시드 데이터" 11단계대로. 먼저 팀에 알린다(테스트 계정 201~216의 반려동물·글이 새 데이터로 바뀌고 가짜 장소 512곳이 지워진다), 공용 DB 전체 mysqldump 백업
- 정하기(사람): 남긴 테스트 계정 닉네임(냥멍44975548·bbbbbbbbbb·냥멍TEST·dev60 등)이 주인공 알림·댓글에 보인다. 시연 전에 바꿀지
- 도메인별 시드 확인 요청(PR에서): member YAY, pet·expense SJM, health JAJ, community·support KSJ
- 정리(사람): 로컬 확인용 DB `nyangmeong_demo_seed`·`nyangmeong_seedcheck`와 로컬에 받은 공용 DB 덤프(암호문 있음)를 지운다
- SCR-AIM-012 대화창 수정·삭제 도구 연결 남음: ai 세션(ai-ce)에 PR 번호·`@AiExclude`로 `refRange`·`clearFields`만 빠짐(도구는 실림)·400 칸 이름은 수정 경로만(삭제는 서비스 400 없음) 알림, `data/tools.snapshot.json` 갱신은 ai 세션. 프론트 연결 대화창 범용 확인 카드의 DELETE·본문 없는 call 실행 (프론트 저장소에서 /start, 프론트 `milestones/LJS.md`에 `대화창 수정·삭제 카드 실행 — 대기` 절 넣음)
- 선택: 도구 목록이 `@DecimalMin`을 싣지 않아 `draft_inspection_value_update.rsltNvl`에 `min: 0`이 없다(등록 도구에는 있음). 요청 검증 400은 그대로라 동작 영향 없음
- SCR-AIM-011 대화 요약 연결 남음: ai 세션(nyangmeong-ai-1a)에 커밋 해시·칸 이름(요청 그대로)·요청과 다른 점(마지막 msgSeq 초과 400, SQL 두 문장) 알리고, Python이 실제 Java를 부르는 확인과 ai `docs/api/internal.md` 초안 행 확정(ai 세션). PR 본문에 "DB 변경 있음 v0.15 (2), 공용 DB에는 PR 전에 실행함"
- 채팅 상대 회원 신고 관리자 화면(KSJ 요청, 2026-11-24): 프론트 연결 관리자 신고 화면 ① 대상 종류 「회원」 필터·표시(`targetType=MEMBER`) ② 회원 신고 상세에서 「블라인드」 선택지 숨김 ③ 회원 신고 상세에 대상 회원·채팅방(`otoChatRoomSn`, 목록 대상 내용은 그 방의 돌봄 글 제목) 표시, LJS 프론트 저장소에서 /start. 백엔드(`ReportMapper.xml` 대상 3분기, `AdminReportService` 회원 정지·블라인드 400·복구 409, `MemberSuspensionService.suspendBySystem`, DCLR v0.15 변경 절)는 KSJ가 고친다. KSJ 백엔드는 develop에 병합됨(1b477ce, 2026-11-24 확인), 연결 가능
- SCR-AIM-013 피부 분석 지속 관찰 카드 연결 남음: 걸치는 기능 FN-HLT-002-20~25·32 (ai 저장소 `data/tools.snapshot.json` 갱신, 피부 분석 뒤 새 항목은 `draft_observation_item`·관찰 중인 같은 항목은 `list_observation_items` 뒤 `draft_observation_cycle` 확인 카드, 판정 사진 1024px JPEG을 `file`로, 분석 결과는 `memoCn` 200자 글, 부위 HLTG16은 회원이 고름. 안구 분석(ai #108)도 같은 카드로 저장하고, 부위는 얼굴 HLTC0044, `obsrvRegnDtlNm`에 "왼쪽 눈"/"오른쪽 눈"을 적는다. 눈 코드는 불편이 생기면 그때 추가한다(사람 2026-11-24). ai-23, ai 세션)
- SCR-AIM-013 검사 수치 카드 연결 남음: 프론트 연결 대화창 PDF 첨부 허용(`ChatInput.jsx`)·`POST /ai/conversations/{aiCnvrsRoomSn}/checkup-card` 연결, LJS 프론트 저장소에서 /start. ai 쪽은 끝(ai #104·#105, 2026-11-24 확인). 프론트 PR #501(develop 대상) 올라감, 2026-11-24 병합 전. 병합되면 이 줄을 지운다
- 작업 트리(사람): 작업 전부터 있던 `src/main/resources/logback-spring.xml` 변경(`jdbc.resultsettable` 로거 삭제, root에 `ROLLING_FILE`)은 `git stash`에 보관(2026-11-23 "LJS: 기획 밖 logback-spring.xml 변경 보관…"). 쓰려면 `git stash pop` 후 따로 커밋
- 사람(SCR-MAN-001 튜토리얼): ERDCloud `MBR`에 `TUTRL_VIEW_YN`은 넣었고(2026-11-20, 도메인 `여부C1`·기본 'N'·NOT NULL) 맨 끝에 붙어 있어 화면에서 `D7_BFHD_NOTI_YN` 뒤로 옮긴다. ERD 버전 v0.15 여부 정하기. YAY에게 `member`에 컬럼 하나·API 두 개 추가를 알린다. 공용 DB 시드 회원 12(씩씩한산책러)는 확인용으로 'N'으로 바꿔 둠(2026-11-20), 되돌리려면 `UPDATE MBR SET TUTRL_VIEW_YN='Y' WHERE MBR_SN=12;`
- 사람(SCR-ADM-070 굽기를 그림 참고로만): (공용 DB 기준 SQL v0.14 (19)는 실행돼 있음, 검증용 아이템 55는 2026-11-19 지움·첨부파일 4922 `USE_YN=N`) ERDCloud `ITEM.BAKE_IMG_REF_YN`이 맨 끝에 붙어 있어 화면에서 `NTSL_YN` 뒤로 옮긴다. 기존 아이템은 서버 관리자 화면에서 픽셀 그림을 새로 올리며 굽기를 켠다(그 전에는 새 캐릭터에 굽지 않음, 서버의 옛 아이템 그림 경로 문제로 1번 main에서 49·51·53 bake-image 실패 보고 있음)
- 선택(ai, SCR-ADM-070 생성기 정리): `POST /ai/items/bake-hints` 삭제, `POST /ai/items/trial-preview`는 `bakeHint`만 받아 그림 참고 없이 구우므로 쓰려면 그림 참고로 바꾼다(프론트는 시험 굽기를 쓰지 않음). 2026-11-24 ai develop에 둘 다 남아 있음
- 남김(SCR-MYP-010 파일 저장 경로 재구성 범위 밖): 기존 공지·뉴스 이미지(`public/…`) 이동(옮긴 뒤 `FileStore.requireStored`의 `public/` 허용을 뺀다. 지금은 `public/` 아래면 뉴스 이미지가 아니어도 뉴스 썸네일로 받는다, 파일 이동·`PHYS_STRG_PATH` UPDATE·공지 본문 URL 교체, 운영 디스크 작업이라 사람), 영수증 원본 저장 중단(따로 기획), 지운·교체한 파일 정리 배치, 채팅·문의 이미지 상한 상수 10
- 참고(검증 에이전트, 2026-11-10): `application-local.properties`의 `jdbc:log4jdbc:` 주소로 bootRun하면 "Failed to determine a suitable driver class"로 뜨지 않았다. 검증은 `--spring.datasource.url=jdbc:mysql://...`로 덮어써 띄웠다. 원인은 확인하지 않음
- 확인 필요(사람) SCR-AIM-012 쓰기 도구 연결: 2026-11-24 프론트 develop에 multipart·uuid·중복 확인 처리가 없다. 그 뒤 카드 실행이 채팅 확인(`done.execute`, Python 실행)으로 바뀌어 필요 없어졌는지 정한다. 원래 항목: FN-AIM-012-05·06·07·13, FN-AIM-013 (프론트 연결 쓰기 도구 범용 확인 카드, 프론트 저장소에서 /start. 프론트 `milestones/LJS.md`에 `쓰기 도구 범용 확인 카드 — 대기` 절 넣음): `call.contentType` multipart면 form-data·`type: file`은 사진 선택, `type: array`는 `items` 줄 목록, `generated: "uuid"`는 실행 때 채움, `draft_expense` 전에 `/api/expenses/duplicate-check`
- 정하기(사람): 대화로 쓴 기록의 출처 코드를 일정 외 테이블에도 남길지(DB 변경, 2026-11-06 범위 밖으로 뺌)
- SCR-AIM-011 ai 설계 문서 정리 남음(도구 설명·스냅샷 연결은 끝, 2026-11-24 확인): ai `docs/design/tool-catalog.md` 2-1절 고칠 것: `/{pixelCharSn}/held-items`는 GET이 없다(보유는 `get_pixel_character`·`list_shop_items`의 `hldYn`), `GET /api/caregiver-invitations`는 보낸 초대(`list_sent_caregiver_invitations`)
- 선택(가계부 담당 SJM, 테스트 중 문제가 생기면 진행. 사람 2026-11-06): `get_expense_statistics.yearMonth`는 `String`이라 도구 목록에 `pattern`이 안 실리고, 틀린 형식은 서비스 `YearMonth.parse` 실패로 칸 이름 없는 `INVALID_INPUT_VALUE` 400이 된다(`ExpenseQueryService.parseMonth`). ai 도구 설명에 YYYY-MM이 이미 있고 Python은 `pattern`을 쓰지 않는다. `@Pattern`을 붙이면 400에 칸 이름이 실린다
- SCR-ADM-082 연결 남음: FN-ADM-082-03 ai 연결(프론트 검사결과지 분석 탭은 끝, 2026-11-24 확인): `app/checkup` PDF 분석 호출마다 `POST /internal/ai/external-api-calls` `{ extlApiSeCd: "SYSC0025", callRsltCd, rspnsMs, httpSttsVl, failRsn }` 통지, 검사결과지 아님 판정은 SYSC0021(성공), 시간초과 SYSC0023, API 오류 SYSC0022, ai 저장소에서 /start. 2026-11-24 ai develop에 이 통지 없음
- 데이터 문제(SCR-ADM-071, SJM·팀과 정한다): 공용 DB `CARE_MSN`을 다시 넣어 번호가 22부터라 옛 `DAY_MSN` 22,772행이 없는 `CARE_MSN_SN`(1~21)을 가리킨다. 케어미션 배치는 11-10 이후 다시 성공한다(2026-11-16 확인, 실패는 11-11~13의 3건). 남은 것은 이 옛 행을 지울지 정하는 것
- SCR-ADM-051 남김: FN-ADM-051-04 기능별 동의 2종(펫돌봄 이용수칙 `CARE_RULE`·실종 개인정보 공개 동의 `LOST_PRIVACY`). 커뮤니티가 동의 문구를 `TRMS`에서 읽게 될 때(KSJ) 넣는다. 그때 가입 쪽 쿼리 4개(`TermsMapper.xml` 가입 목록·가입 동의 검사·복구 빠진 필수 약관·관리자 회원 상세 약관 동의)가 섞이지 않게 가르는 방법(컬럼 추가 등)을 YAY와 정한다
- 정하기: 뉴스 제외 키워드(AIMC0040) 편집 화면. 기준 데이터 주석에 "관리자 SCR-ADM-050에서 편집"이라고 돼 있지만 기능명세서·화면설계서·프론트 가정에 없어 만들지 않았다. 넣는다면 명세에 기능 ID를 추가하고 같은 키워드 API에 용도 구분을 더한다
- SCR-MYP-031 연결 남음: FN-AIM-013-06 가계부 출처 영수증 다시 열기 (프론트 연결 가계부 화면의 영수증 다시 열기, 화면 SJM, 프론트 저장소에서 /start). SJM에게 알린다: `GET /api/receipts/{recptSn}`을 `petSn` 없이 부르면 된다(응답 `petSn` null, `docs/api/receipt.md`). 프론트 저장소 가계부 담당(SJM) 마일스톤 `## 제외` 앞에 넣을 절(사람): `## SCR-MYP-031 영수증 업로드 · 가계부 출처 영수증 다시 열기 연결 — 대기`(기능: 명세 기능 ID 추가 없음, 걸치는 기능 FN-AIM-013-06·FN-MYP-031-03 / 기다리는 것: 백엔드 SCR-MYP-031 가계부 출처 다시 열기(LJS, 끝) / 완료 조건: 가계부 화면에서 올린 영수증을 `petSn` 없이 다시 열어 인식 결과 확인 모달이 뜬다)
- 참고(SCR-ADM-070): 새 아이템 12개는 이미지가 빨간 리본·노란 우비와 같다(디자인 이미지가 오면 교체)
- SCR-ADM-042 펫돌봄 분쟁 작성자 매칭 목록 연결 남음: FN-ADM-042-08 (프론트 연결 관리자 1:1 문의 상세 `features/adminInquiry` 「분쟁」 문의 작성자 옆 「펫돌봄 매칭 보기」 → `features/adminCommunity` 펫돌봄 목록 `?mbrSn=`, 행 `mbrSn`이 같으면 보호자·다르면 돌보미, LJS 프론트 저장소에서 /start). KSJ에게 `community` 관리자 목록 조회 조건(`mbrSn`)이 늘었다고 알린다
- 굽기 켜기 전(AI 저장소와 함께): 백그라운드 호출이 쿠키를 쓰므로 `access_token`(30분)이 생성·굽기 중에 만료되면 성공 보고·시트 저장이 401이다. 성공 보고를 잃은 작업은 10분 뒤 시간 초과로 실패하고 다시 요청할 수 있다
- 실제 DB 확인 범위(SCR-ADM-020): 사람 확인 내용은 `확인`이라고만 기록됐다. 검증 에이전트는 공용 DB라 블라인드·복구·댓글 삭제 성공 경로를 실행하지 않았다
- 실행해 보지 못한 SQL(SCR-ADM-011): 재동의 시 최신 1행, 동의 기록 없는 약관(`TermsMapper.selectAdminAgreementList`), 해제된 연동 제외(`PetMapper.selectAdminList`). 자동 테스트는 매퍼 목이고 공용 DB에 해당 데이터가 없어 SQL 읽기로만 확인했다. 파기 회원 상세도 데이터가 없어 코드로만 확인했다
- 판단 필요(SCR-ADM-113): 지정 기록이 없는 기존 관리자는 `apntDt`가 null이다. `MBR_AUTHRT_CD`에 인덱스가 없어 해제 트랜잭션 동안 `MBR`의 많은 행이 잠길 수 있다(부하 상황 확인 안 함)
- 탈퇴 파기 남은 범위: 채팅 원문(REQ-NFR-002, 채팅 구현 뒤 KSJ와 `scheduler`로 묶는다), 프로필 파일 실물(`FileStore`에 삭제 없음, 지금은 연결만 끊는다), 게시물·댓글·포인트·반려동물·건강기록(정책 미확정, `docs/api/issues.md` 8번)
- 판단 필요: 구현 계획에는 정지 안내 알림이 있으나 기능명세서에 없어 넣지 않았다. `suspnDayCnt`가 빠진 정지 요청은 영구 정지로 처리된다(프론트 계약). 만료 배치는 하루 한 번이라 00:00에 서버가 내려가 있으면 수동 실행 전까지 정지가 이어진다
- 확인 못 함: 검증의 `dev-tokens`(회원 1·2)가 공용 DB에 세션을 남겼는지 확인 못 함
- SCR-ADM-112 연결 남음: FN-ADM-112-02 ai 연결(프론트 화면은 끝, 프론트 #491): Python이 `POST /internal/ai/external-api-calls`를 부른다, 호출마다 쿠키가 있어야 하고 쿠키 없는 백그라운드 호출은 기록되지 않는다, ai 세션. 토큰은 수만 본다: 입력 = input + 캐시 읽기 + 캐시 쓰기, 출력 = output, 캐시 칸은 추가하지 않는다(사람 2026-11-24, ai-23에 전달). 2026-11-24 ai develop에 호출 없음
- 배치 위험(판단 필요): 서버 기동 때 공용 DB의 실행중 행을 서버 구분 없이 실패로 바꾼다(끝나면 실제 결과로 덮어씀)
- PR 전 확인(사람): `LoginService`에 관리자 로그인 기록을 넣은 것을 YAY와 협의. 저장소에 협의 기록 없음
- 공통코드 화면(SCR-ADM-100) 제외 후속(사람, 2026-11-01 결정: 코드 변경이 드물어 만들지 않고 SQL로 처리): 요구사항정의서 REQ-ADM-010, 관리자 기능명세서·화면설계서 SCR-ADM-100, IA 구조도(DMS)를 고치고 프론트 사이드바 메뉴를 뺀다. `docs/admin/implementation-plan.md`는 반영함. 다시 만들게 되면: 공통코드 enum 44개 중 값마다 로직이 갈리는 enum은 추가해도 쓸 수 없고, `InspectionArticle` 등 허용 목록 enum은 DB 검사로 바꿔야 추가한 코드를 쓸 수 있다
- 관리자 다음 마일스톤 전 확인(사람): 구현 계획 6절 확인 항목(인기 포토 제외·RSS 피드·알림 기본값 저장 위치, 기준정보 28번). REQ-ADM-014~022 요구사항정의서 반영은 별도 에이전트가 맡는다(2026-10-30 사람). 다른 담당 도메인 화면은 계획 2절 "협의" 표대로 먼저 이야기한다. 감사 로그가 필요한 관리자 기능은 `AdminTaskLogService.record`를 `@Transactional` 안에서 부른다
- SCR-AIM-002 연결 남음: FN-AIM-002-02·11 (Python AI 서버가 `GET /internal/ai/settings`를 붙인다, ai 세션). PR을 올리면 ai 세션에 PR 번호와 계약(경로·응답 `AiSettingResponse`)을 알린다(ai 저장소 `docs/api/internal.md`와 같은 때 병합) 2026-11-24 ai develop `get_ai_settings()`는 아직 고정값
- 운영(사람): 서버에 저장 폴더와 쓰기 권한, nginx `client_max_body_size` 10m 이상. SJM에게 알린다: 캐릭터 결과·착용 이미지는 로그인 회원 누구나(`FileCategory` SHARED), 착용 시트는 도메인 API 전용이다. 공동양육자에게만 보이게 하려면 `FileCategory`를 OWNER로 바꾸고 캐릭터 이미지 API로 준다
- 프론트 저장소: 같은 이름 스크립트에 같은 macOS 수정(`$( )` 안 `case (패턴)`, `sed -E`, `sed -i` 대신 임시 파일·`mv`, `{…;}`, 한글 앞 `cut -c` 대신 `sed`)을 적용하고, 커밋 전인 `verify-loop.sh` `scripts` 보호·verify·end 스킬 수정과 함께 프론트 PR
- 하네스 아직 못 돌려 본 것: `pr-prep.sh --allow-dirty`(다음 PR 때), 검증 3회 실패 넘기기(다음 화면 작업) / 정하기: 프론트 `start` 스킬에도 `docs/api`(백엔드 저장소) 기준 줄을 넣을지
- 정하기: 휴대폰 일정 가져오기(기획에 없음, JAJ 영역). 넣는다면 QR 짝짓기 `.ics` 업로드, iPhone 단축어, 웹 → 휴대폰은 `webcal://` 구독 QR
- 정하기: 파일 저장소 운영(지금 서버 로컬 디스크 폴더. 서버 여러 대·재배포 대비 영구 볼륨 또는 S3), 영상 업로드 형식·용량
- 정하기: HLD_ITEM 유일 키 `UK_HLD_ITEM_1`에 DEL_YN이 없다. 보유 아이템 소프트 삭제가 생기면 재구매가 막히므로 그때 키를 고친다
- 디스코드 공유 `application.properties`: `app.front-base-url=http://localhost:5173` 추가, 팀 공용 `app.crypto.*`·`app.jwt.secret-base64` 키를 정해 넣고 `mybatis.configuration.map-underscore-to-camel-case`는 넣지 않는다. 팀 안건: application.properties를 일반 설정(커밋) + git 제외 `application-secret.properties`로 나눌지 `docs/OPEN_ISSUES.md`에 올린다
