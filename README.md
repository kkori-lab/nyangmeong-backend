# 냥멍케어 백엔드

반려동물 케어 웹서비스 냥멍케어의 백엔드. Java 25, Spring Boot 4.1, MyBatis, MySQL 8.

- 규칙: `AGENTS.md`, 나머지는 `docs/convention/`
- 아직 정하지 않은 것: `docs/OPEN_ISSUES.md`

## 처음 받았을 때

```mermaid
flowchart LR
  E[application.properties 받기] --> A[setup 실행] --> B[앱 다시 켜기] --> C[MCP 인증 한 번] --> D["/start"]
```

### setup

| OS | 실행 |
| --- | --- |
| Windows | `setup.bat` (cmd·PowerShell·탐색기 더블클릭) |
| 맥 | `bash setup.sh` |

- 여러 번 실행해도 된다. 끝나면 IDE·데스크탑 앱을 다시 켠다(새 PATH 반영).
- Windows에서 `bash setup.sh`는 WSL이 떠서 안 된다. Codex를 쓰려면 Codex를 먼저 설치한다.

| setup이 하는 일 | 내용 |
| --- | --- |
| Git | 사용자 설정, 커밋 훅(`Co-Authored-By` 줄 삭제), 이니셜 브랜치 |
| AI 도구 | Claude Code·Codex 감지. 둘 다 없으면 Claude Code 설치 |
| IDE | VS Code·STS·둘 다 중 선택. 다시 고르려면 `setup.bat --ide` |
| JDK 25 | 없으면 설치 |
| VS Code | 확장(Java Pack, Spring Boot, Claude Code), JDK 등록 |
| STS | 없으면 설치, Lombok, 공용 포맷·UTF-8·LF |
| MCP | `dms`(요구사항정의서·화면설계서·기능명세서) |
| 확인 | 빌드, 공용 DB 접속, 서버 기동 |

직접 할 일:

- `application.properties`를 디스코드에서 받아 `src/main/resources/`에 둔다. 이름이 `.txt`로 바뀌지 않았는지 본다.
- STS는 처음 한 번 File > Import > Gradle > Existing Gradle Project로 `gradlew`가 있는 `backend` 폴더를 가져온다.
- DB 접속이 안 되면(학원 밖 등) setup이 할 일로 알려 준다.

### application.properties

비밀값이 들어 있어 git에 올리지 않고 디스코드로 공유한다. 키 목록은 커밋된 `application.properties.example`에 있다.

| 키 | 예 |
| --- | --- |
| `spring.datasource.url` | `jdbc:mysql://호스트:3306/PAWEVER` |
| `spring.datasource.username` | |
| `spring.datasource.password` | |
| `dms.mcp-url` | DMS MCP 서버 주소(claude.ai 커넥터로 `dms`가 연결돼 있으면 비워도 된다) |

- 키를 추가·변경하면 `application.properties.example`과 디스코드의 파일도 같이 고친다.
- `application-local.properties`(git 제외)가 있으면 그 DB 설정이 우선한다. 공용 DB를 쓰려면 이 파일을 두지 않는다.
- 운영 설정은 `application-prod.properties`(비밀값 없음, 커밋)에 둔다.

### MCP

- claude.ai 커넥터로 이미 연결돼 있으면 Claude Code에는 등록하지 않는다.
- 인증: Claude Code는 `/mcp` → 서버 선택 → Authenticate, 데스크탑 앱은 Connectors 화면(확인 필요), Codex는 `codex mcp login dms`.

### 직접 실행

```
./gradlew bootRun
```

- `http://localhost:8080`에 뜬다. 8080을 다른 프로그램(수업 프로젝트 등)이 쓰고 있으면 먼저 끈다.
- API 명세: `http://localhost:8080/swagger-ui.html`
- 한글이 깨지면 PowerShell에서 `chcp 65001`.
- `db/`의 SQL은 setup이 실행하지 않는다. 테이블을 바꾼 PR 작성자가 병합 직후 공용 DB에 실행한다(`docs/convention/shared-files.md`).

### 로그인 (dev-token)

로그인 API(YAY)가 나오기 전에는 회원 번호로 토큰 쿠키를 받는다(운영 프로필에서는 꺼진다).

- Postman: `POST http://localhost:8080/api/auth/dev-tokens`, Headers `X-Requested-With: XMLHttpRequest`, Body(raw JSON) `{"mbrSn": 1}` → 200. 쿠키는 Postman이 저장해 다음 요청부터 붙인다(요청 오른쪽 Cookies에서 확인·삭제).
- 브라우저: 프론트 화면(`http://localhost:5173`)의 개발자 도구 콘솔에서 실행하고 새로고침한다.

```js
fetch('/api/auth/dev-tokens',{method:'POST',headers:{'Content-Type':'application/json','X-Requested-With':'XMLHttpRequest'},body:JSON.stringify({mbrSn:1})})
```

- 토큰은 30분짜리다. 재발급 API가 아직 없어 만료되면(401, `/auth/refresh` 404) 다시 받는다.

### 프론트와 같이 띄우기

| 띄울 것 | 명령 | 포트 |
| --- | --- | --- |
| 백엔드 | `./gradlew bootRun` | 8080 |
| 프론트(`frontend` 저장소) | `yarn dev` | 5173 |

- 프론트는 `/api` 요청을 8080으로 넘긴다(`vite.config.js` 프록시). 두 포트가 꼭 8080·5173이어야 한다.
- 5173이 이미 쓰이고 있으면 Vite가 5174로 뜨고, 백엔드 CORS(`app.cors.allowed-origins`, 기본 5173)에 막혀 POST가 403(`Invalid CORS request`)이 된다. 5173을 쓰는 다른 프로젝트를 먼저 끈다.
- 로그인은 위 "로그인 (dev-token)"의 브라우저 방법.

### 파일 저장

- 파일 저장 루트는 `app.file.root-dir`이다. 비우면 Windows는 프로젝트가 있는 드라이브 루트 아래 `home/ggorilab/attachment`(C:와 D:에 프로젝트를 두면 각 드라이브에 따로 생긴다), Mac은 `/Users/Shared/ggorilab/attachment`다. 서버를 켜면 로그에 `[FileRoot] 파일 저장 루트=…` 한 줄이 나온다. 예전 `uploads/` 폴더의 파일은 새 경로로 옮긴다.
- `app.security.permit-all-paths`에 `/api/files/public/**`가 있어야 공개 이미지가 로그인 없이 열린다. 내 `application.properties`에 이 값을 직접 적었다면 추가한다.

### 시연용 시드 데이터

발표 시연용 데이터는 `src/seed`의 생성기가 넣는다. DB를 다시 만들지 않고 업무 데이터만 골라 비운 뒤, 남겨 둔 행 위에 주인공·엑스트라 데이터를 넣는다. 이미지·첨부파일은 넣지 않는다(사진이 필요한 것은 사람이 화면에서 올린다).

| 하고 싶은 것 | 명령 |
| --- | --- |
| 업무 데이터 비우기 | `./gradlew seedClean -Ptarget=shared -Pconfirm=<DB이름>` |
| 시연 데이터 넣기(비운 뒤에만) | `./gradlew seedData -Ptarget=shared -Pconfirm=<DB이름>` |
| 내 PC에서 먼저 확인하기 | 공용 DB 덤프를 `nyangmeong_`로 시작하는 로컬 DB에 넣고 `-Ptarget=local -Pdatabase=<그 DB이름>` |

- 시드 계정 비밀번호는 git에 올리지 않는 `src/main/resources/application-local.properties`의 `app.seed.password`에 둔다. 공용 대상이어도 이 파일에서만 읽고, 값이 없으면 DB에 붙기 전에 멈춘다.
- 꼭 있어야 하는 계정(팀 공용 계정·관리자)은 같은 파일의 `app.seed.fixed-accounts`(메일, 쉼표로 구분)와 `app.seed.fixed-password`에 둔다(디스코드로 공유). `seedData`는 이 계정이 남긴 회원에 없으면 넣기 전에 멈추고, 있으면 비밀번호를 그 값으로 맞춘다. 키가 없어도 멈춘다.
- 남기는 것: 기준값(공통코드·케어미션·라우팅 매핑표·키워드 사전·알림 템플릿·약관·품종·지역·운영 설정), 장소(옛 시드가 만든 가짜 장소 — 이름이 'n호점'으로 끝나거나 카카오 장소 ID가 `fake-`로 시작 — 는 지운다), 뉴스, 오늘의 문구·AI 코멘트, 관리자 기록(감사 로그·배치 이력·외부 API 로그), 이름에 `_BAK_`가 든 백업 테이블.
- 남기는 회원: 시드가 만들지 않은 회원(팀원 테스트 계정), 관리자, 관리자 기록이 가리키는 회원, `SeedTables.KEPT_MEMBERS`에 적은 회원(옛 시드 회원 133). 계정·약관 동의·SNS 연동은 그대로 두고 반려동물·기록·글 같은 활동만 비운다. 보유 포인트는 0으로 맞춘다.
- 남기는 공지·FAQ: 사람이 쓴 것 중 검증용이 아닌 것(`SeedTables.KEPT_NOTICES`·`KEPT_FAQS`). 남긴 행이 가리키는 첨부파일 행만 남기고, 지운 첨부파일의 서버 파일은 지우지 않는다.
- 아이템·픽셀 캐릭터·AI 대화는 비우고 넣지 않는다. 아이템은 관리자 화면에서, 캐릭터는 회원 화면에서 실제 흐름으로 만든다.
- 날짜는 실행한 날 기준이다. 넣는 데이터의 번호는 테이블의 다음 번호부터라 지운 번호를 다시 쓰지 않는다.
- 두 명령 모두 한 트랜잭션이고, 커밋 전 검사(FK·공통코드 그룹·포인트 잔액·케어미션 배점 등)에 걸리면 롤백한다.

| 회원 | 메일 | 용도 |
| --- | --- | --- |
| 1 콩이네 | `tester@nyangmeong.dev` | 주인공. 전주 덕진동, 콩이(말티즈 11살, 슬개골)·보리(웰시 코기 3살, 다이어트)·두부(코숏 13살, 신장병). 최근 3개월 건강기록·가계부·케어미션, 글·실종(찾았어요)·펫돌봄 모집과 채팅, 문의 2건 |
| 새 엑스트라 6명 | `boridad`·`dubu`·`cheese`·`danchu`·`somi`·`gureum` `@nyangmeong.dev` | 보리아빠(보리 공동양육자), 두부이모(두부 공동양육 초대 대기), 치즈집사·단추아빠·솜이맘·구름이네(글·댓글, 단추아빠는 주인공 펫돌봄 채팅) |
| 6·93·201~216 | 그대로 | 남긴 회원. 계정은 그대로, 반려동물·기록·글만 새로 넣는다 |

공용 DB에 적용하는 순서(발표 3~4일 전, 배치 이력·AI 리포트가 다시 쌓이도록):

1. 팀에 알린다(테스트 계정의 반려동물·글이 새 데이터로 바뀐다).
2. 배포 서버와 공용 DB에 붙은 팀원 서버를 모두 끈다.
3. 공용 DB 전체를 `mysqldump`로 백업한다.
4. `seedClean -Ptarget=shared -Pconfirm=<DB이름>`
5. `seedData -Ptarget=shared -Pconfirm=<DB이름>`
6. 배포 서버를 켠다.
7. 주인공(회원 1)으로 배포 서버에 로그인해 본다(생성기와 서버의 암호화 키가 같은지 확인).
8. 관리자 화면에서 픽셀 아이템을 처음부터 등록하고 굽는다.
9. 주인공 반려동물의 캐릭터를 만든다.
10. 주인공 프로필·게시글 사진 등 사진이 필요한 것을 화면에서 올린다.
11. 관리자 화면에서 FAQ 51번의 질문 칸을 고친다(질문 칸에 답변이 들어가 있다).

시연 순서 초안: 로그인 → 메인 → 반려동물·캐릭터·케어미션 → 건강기록(두부 신장 수치, 보리 체중) → 가계부 → 커뮤니티·장소(실종 찾았어요, 펫돌봄 채팅) → 알림·문의 → AI 대화 → 관리자.

## AI로 작업하기

```mermaid
flowchart LR
  subgraph tools[도구]
    A[데스크탑 앱]
    B[VS Code 확장]
    C[터미널 claude]
  end
  tools --> SK[".claude/skills<br/>/start · /impl · /verify · /end"]
  SK --> HO["handoff/이니셜.md<br/>기획 · 진행 상황"]
  SK --> MS["milestones/이니셜.md<br/>화면 ID별 할 일"]
  SK --> MCP["MCP<br/>dms"]
```

| 도구 | 시작 |
| --- | --- |
| 데스크탑 앱 | [다운로드](https://claude.com/download) → Code 탭 → New session → Local, 이 폴더 |
| VS Code 확장 | 이 폴더 열기 → 왼쪽 막대의 Claude 아이콘 |
| 터미널 | 이 폴더에서 `claude` |

셋 다 같은 스킬과 MCP를 쓴다. STS만 쓰면 VS Code 확장(`anthropic.claude-code`)은 직접 설치한다.

### 작업 흐름

```mermaid
flowchart TD
  S["/start<br/>작업 고르기 · 기획 (Opus)"] --> P{기획 확인}
  P -- 고칠 점 --> S
  P -- 승인 --> I["/impl 구현 (Sonnet)<br/>./gradlew build"]
  I --> V["검증<br/>별도 에이전트가 판정 (Opus)"]
  V -- "실패(확인 못 함·내 것 포함), 3회 미만" --> I
  V -- 3회 실패 --> X["사람에게 넘김<br/>직접 수정 · /start 다시 기획 · 3회 더"]
  V -- "통과 · 연결 남음만" --> H["/end 전에 확인할 것<br/>사람 확인<br/>bootRun · Postman"]
  H -- 문제 --> I
  H -- 확인 --> D["/end<br/>끝 판정 · 커밋 제안"]
  D --> N["새 대화 → /start"]
```

| 할 일 | 입력 |
| --- | --- |
| 다음 작업 시작 | `/start` |
| 특정 화면 시작 | `/start COM-003` |
| 기획 승인 (구현·검증 루프 시작) | `승인` 또는 `/impl` |
| 다시 검증 (직접 고친 뒤) | `/verify` |
| 마무리 | `/end` → `확인` → `커밋` → `푸시` → (마일스톤이 끝나면) `PR`. 병합은 GitHub에서 사람이 |
| 중간 저장 | `/end` → (확인 없이 `진행`으로 남김) → `커밋` → `푸시`. 다음은 `/start`로 이어 간다 |
| 새 대화 | 터미널·VS Code `/clear`(VS Code는 확인 필요, 안 되면 Open in New Tab), 데스크탑 앱 Ctrl+N |

- 마일스톤 파일이 없으면 첫 `/start`가 먼저 만든다(시간이 더 걸린다). 스킬이 `/` 목록에 없으면 새 대화를 연다.
- 모델은 바꾸지 않는다. 기본 Sonnet, `/start`와 검증만 Opus.
- 마일스톤 `끝`은 `/end`만 정한다. 남의 API·화면을 기다리는 "연결 남음"은 `끝`을 막지 않고 마일스톤과 핸드오프 "다음에 할 것"에 남는다(`docs/convention/milestone.md`).
- Codex(`$start`·`$impl`·`$end`)도 같은 흐름이다. 다른 점: 검증도 GPT로 돈다(Opus 지정은 Claude Code 전용). 기준 파일 잠금 훅(`.codex/hooks.json`)은 처음 폴더를 열 때 "신뢰" → `/hooks`에서 승인해야 돈다(훅이 바뀌면 다시 승인).

### 사람 확인

`/impl`이 끝에 주는 "`/end` 전에 확인할 것"(직접 `/verify`를 돌렸으면 그 보고)의 "사람 확인" 목록대로 확인한다. Postman은 데스크톱 앱을 쓴다(웹 버전은 `localhost`에 닿지 않는다).

```mermaid
flowchart LR
  R["./gradlew bootRun"] --> L["Postman<br/>POST /api/auth/dev-tokens<br/>(로그인이 필요할 때)"]
  L --> P["Postman<br/>목록의 요청"]
  P -- 200 · 4xx 응답 --> OK[기대 결과와 비교]
  P -- 만료·정지처럼 만들기 어려운 조건 --> T["./gradlew test --tests"]
  T --> OK
```

1. `./gradlew bootRun` → `Started … in N seconds`가 나오면 준비 완료(공용 DB, "application.properties" 참고).
2. Postman 컬렉션 Variables에 `baseUrl` = `http://localhost:8080`. 요청 주소는 `{{baseUrl}}/api/...`.
3. 로그인이 필요하면 "로그인 (dev-token)"의 Postman 방법으로 쿠키를 받는다.
4. 목록의 요청(메서드·URL, Headers, Body)을 그대로 보낸다. POST·PUT·PATCH·DELETE에는 `X-Requested-With: XMLHttpRequest`(없으면 403 `FORBIDDEN`).
5. 응답 코드와 JSON을 기대 결과와 비교한다. 화면 확인은 "프론트와 같이 띄우기"대로 띄운다.
6. Postman으로 만들기 어려운 조건(만료 토큰·정지 회원 등)은 목록의 테스트로: `./gradlew test --tests '<클래스>'`, 결과 `build/reports/tests/test/index.html`.
7. 확인용으로 만든 데이터는 목록의 정리 방법대로 지운다(공용 DB). Postman Cookies의 `localhost` 쿠키도 지운다.
8. 목록의 "바뀐 파일"을 훑어보고 기획에 없는 파일이면 문제로 답한다. Ctrl+C로 끈다.

Swagger(`http://localhost:8080/swagger-ui.html`)에서 API 목록과 요청·응답 모양을 볼 수 있다. 쿠키와 `X-Requested-With`가 필요한 요청은 Postman으로 확인한다.

## PR 전 확인

```
./gradlew build
```

테스트는 DB에 붙지 않는다.
