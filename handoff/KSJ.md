# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-003 펫뉴스 · 주제 무관 기사 거르기 — 끝남 (승인 2026-11-02 14:41, 시작 2026-11-02 14:39, 검증 통과 1회차(추가 요청 2건 포함), 사람 확인 2026-11-02)
기준: 명세 기능 ID 추가 없음(사람 요청 2026-11-02, `milestones/KSJ.md` 완료 조건). 걸치는 기능 FN-CMU-003-06 뉴스 수집. 문서는 앞 마일스톤 기준(커뮤니티 기능명세서·화면설계서 v0.8)과 같아 다시 받지 않았다. API·DTO·테이블 구조·`ErrorCode` 변경 없음. 기준 데이터(공통코드 1개·키워드 사전 1행) 추가 있음
- 사전 용도 코드: 공통코드 `AIMC0040` 「뉴스 제외」(그룹 `AIMG10` 사전용도, 정렬 40, 설명 「RSS 기사 제목 제외 키워드, 하나라도 들면 수집하지 않음」). `db/20261030_insert_base_data_v0.11.sql` 공통코드 절 끝(`SYSC0028` 다음 줄)에 붙여 `COM_CD_SN`을 공용 DB와 맞춘다
- 제외 키워드 행: `KWRD_DICT` 1행 `('AIMC0040','뉴스 제외','게임·창작물','게임,H5,웹툰,웹소설',NULL,NULL,200,'기사 제목에 하나라도 들면 수집하지 않음(영문 대소문자 무시). 관리자 SCR-ADM-050에서 편집. FN-CMU-003-06')`. `미니게임`은 `게임`에 포함돼 따로 넣지 않는다. 키워드 사전 절 끝에 붙인다
- 기준 파일 변경 절: 맨 위에 `v0.11 (7) 뉴스 제외 키워드 (2026-11-02, KSJ, 근거: 커뮤니티_기능명세서_v0.8 FN-CMU-003-06 — 검색어 고양이·강아지 단독 매칭으로 게임·웹소설 기사가 수집됨)` 항목으로 위 INSERT 두 개를 주석으로 적는다. 머리말 합계(공통코드 308→309건, 키워드 사전 19→20건)와 키워드 사전 절 설명 줄도 고친다
- 판정: `NewsService.collect()`에서 원문 경로 중복 확인 다음, 중복 제목 판정(`NewsDuplicateChecker`) 앞에서 제목(자르기 전 원래 제목)을 본다. 제목과 키워드를 모두 `toLowerCase(Locale.ROOT)`로 바꿔 `contains`로 비교하고 하나라도 들면 건너뛴다. 원문 미리보기를 부르지 않고 checker에도 더하지 않는다. 로그 INFO `뉴스 수집 : 주제 무관 건너뜀 '{제목}' (키워드 '{키워드}')`, 끝 로그는 `피드 N건 중 새로 저장 M건, 중복 건너뜀 K건, 주제 무관 건너뜀 J건`
- 사전 읽기: `NewsMapper.selectExcludeKeywords()` → `List<String>`(`KWRD_CN`만, `DICT_USG_CD = 'AIMC0040' AND USE_YN = 'Y' AND DEL_YN = 'N'`, `SORT_SEQ, KWRD_DICT_SN` 순). 쉼표로 나눠 앞뒤 공백을 지우고 빈 값은 버린다. 행이 없으면 아무것도 거르지 않는다(지금과 같음)
- 바꿀 파일: `community/service/NewsService.java`(`collect()`, 판정 메서드 `static String findExcludeKeyword(String title, List<String> keywords)` — 걸린 키워드, 없으면 null. `classify`와 같은 꼴), `community/mapper/NewsMapper.java`·`mapper/community/NewsMapper.xml`(`selectExcludeKeywords`), `db/20261030_insert_base_data_v0.11.sql`, 테스트 `NewsServiceTest`·`NewsMapperSqlTest`
- 완료 조건 명령: `NewsServiceTest` 「제외_키워드가_제목에_있으면_미리보기_없이_건너뛴다」(넵튠 「총잡이 고양이」 H5 게임 꼴, `fetchPreview`·`insert` 안 불림), 「제외_키워드는_영문_대소문자를_무시한다」(`h5`), 「제외_키워드가_없으면_그대로_저장한다」(「노령견 식단」·「고양이 예방접종 일정」 꼴), `NewsMapperSqlTest` 「제외_키워드는_뉴스_제외_용도의_삭제되지_않은_행만_읽는다」, 기존 `NewsServiceTest`·`NewsDuplicateCheckerTest`가 그대로 통과(기존 테스트는 `selectExcludeKeywords` 빈 목록), `./gradlew build -q` 종료 코드 0
- 완료 조건 화면: 공용 DB에 변경 절 v0.11 (7)을 실행한 뒤 로컬에서 `POST /api/admin/batches/SYSC0028/run` → 로그에 `주제 무관 건너뜀`이 INFO로 나오고, `GET /api/news?keyword=게임` 결과에 새로 수집된 기사가 없다
- 이미 쌓인 무관 기사(사람이 공용 DB에서 실행, 일회성이라 `db/`에 넣지 않음): 핸드오프 「사람이 할 일」에 확인용 `SELECT NEWS_SN, NEWS_TTL FROM NEWS WHERE NEWS_SRC_CD = 'CMUC0022' AND USE_YN = 'Y' AND (NEWS_TTL LIKE '%게임%' OR UPPER(NEWS_TTL) LIKE '%H5%' OR NEWS_TTL LIKE '%웹툰%' OR NEWS_TTL LIKE '%웹소설%')`와 사람이 고른 번호로 `UPDATE NEWS SET USE_YN = 'N' WHERE NEWS_SN IN (…)`를 남긴다(`NEWS`는 `DEL_YN`이 없고 숨김은 `USE_YN`). 앞 마일스톤에서 외부 기사를 지우고 다시 수집하기로 했으면 이 SQL은 필요 없다
### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `db/20261030_insert_base_data_v0.11.sql` (v0.11 (7) 변경 절·기준 INSERT·머리말 합계) | 명령: `./gradlew build -q` 종료 코드 0. 화면: 제외 키워드 행이 분류별 5행(게임·창작물, 주식·경제, 축산·가축, 부동산·광고, 다른 개념)이고 머리말 합계가 맞다. 검색어(`app.news.query`)는 그대로 | 요청 2026-11-02 "제외 키워드를 제안한 대로 분류별로 나눠 넣고, 검색어는 기존 그대로 둔다" |
| 추가-2 | `db/20261030_insert_base_data_v0.11.sql` (새 변경 절 v0.11 (8)·기준 INSERT의 「다른 개념」 행 삭제·머리말 합계) | 명령: `./gradlew build -q` 종료 코드 0. 화면: 공용 DB의 AIMC0040 행이 4행(200~230)이고 「다른 개념」은 `DEL_YN='Y'`다 | 요청 2026-11-02 "반려식물·반려가전·반려로봇·반려돌은 제외 키워드에서 뺀다", 이어서 "개미 투자자도 삭제" |

이번에 하지 않는 것: 포함 키워드 방식(2026-11-02 확인으로 쓰지 않음), 검색어(`app.news.query`) 변경, 요약(`NEWS_CN`)까지 보는 판정, 관리자 제외 키워드 편집 화면(SCR-ADM-050), 영상 기사 제외, 썸네일 공통 이미지 거르기
새 의존성: 없음

## 다음에 할 것
- 사람이 할 일: 병합 직후 PR에 "DB 변경 있음, v0.11 (7)·(8)"와 완료 댓글(공용 DB에 `AIMC0040` 뉴스 제외 코드·키워드 4행 실행 완료, 2026-11-02). 제외 키워드는 분류별 4행이고 제목에 하나라도 들면 수집하지 않는다. 키워드를 늘릴 때는 수집 로그의 `주제 무관 건너뜀`을 보고 정상 기사가 걸리는지 먼저 본다. 이미 쌓인 무관 기사는 `SELECT NEWS_SN, NEWS_TTL FROM NEWS WHERE NEWS_SRC_CD='CMUC0022' AND USE_YN='Y' AND (NEWS_TTL LIKE '%게임%' OR UPPER(NEWS_TTL) LIKE '%H5%' OR NEWS_TTL LIKE '%웹툰%' OR NEWS_TTL LIKE '%웹소설%')`로 찾아 번호를 골라 `UPDATE NEWS SET USE_YN='N' WHERE NEWS_SN IN (…)`로 숨긴다(앞 항목처럼 외부 기사를 지우고 다시 수집하면 필요 없다)
- 사람이 할 일(공용 DB, 일회성이라 `db/`에 넣지 않음): 시드와 확인용으로 쌓인 기존 뉴스를 지우고 새 중복 판정으로 다시 수집한다(사람 결정 2026-11-02). 대상은 외부 언론사(`CMUC0022`) 143건만이다. 관리자 등록(`CMUC0023`) 시드 14건은 프론트의 「관리자 등록」 배지·원문보기 미노출 확인용으로 남긴다(사람 결정 2026-11-02). 지운 뒤 로컬에서 `app.news.batch-enabled=true` 또는 `POST /api/admin/batches/SYSC0028/run`을 한 번 돌려 로그의 `중복 건너뜀 K건`과 `GET /api/news`를 확인하고, 팀에 공용 DB를 비웠다고 알린다
- 연결 남음: SCR-CMU-003 펫뉴스 화면 (프론트 연결 리스트(카테고리 칩 `newsClsfCd`, 검색 `keyword`, 언론사 `srcPressNm`·「관리자 등록」 `newsSrcCdNm` 배지, 요약 줄 `newsCn`, 썸네일 없으면 기본 이미지, 페이지네이션)와 상세(원문보기는 외부 기사만, 하단 최신 5건은 `?size=5`), 프론트 저장소에서 /start)
- 사람이 할 일: 병합 직후 PR에 "DB 변경 있음, v0.11 (6)"와 완료 댓글(공용 DB에 `SYSC0028` 뉴스 수집·뉴스 분류 키워드 3행 실행 완료, 2026-11-02). 공용 DB `NEWS`에 확인용으로 수집한 기사 약 107건이 프론트 개발용으로 남아 있다
- 사람이 할 일: `origin/LJS`의 `BatchType`(`SYSC0027` 탈퇴 회원 파기)·`AdminBatchServiceTest`·`db/…sql`과 병합 때 겹친다. 병합 뒤 배치 코드는 `SYSC0026`·`SYSC0027`·`SYSC0028` 순이다. 새 코드는 쓰기 전에 공용 DB `SELECT MAX(COM_CD) FROM COM_CD WHERE COM_CD LIKE 'SYSC%'`로 확인한다
- 이번에 안 한 것: FN-CMU-003-05 공유하기(화면만), 관리자 뉴스 등록·숨김·키워드 편집(SCR-ADM-050, LJS), 영상 기사 제외(`og:type=video*`), 외부 API 호출 기록(`EXTL_API_CALL_LOG`), AI 요약, 썸네일 공통 이미지(`noimg.png`) 거르기
- 연결 남음: SCR-CSC-002 FAQ 화면 (프론트 연결 카테고리 탭 → `faqClsfCd`, 검색 `keyword`, 아코디언 Q/A는 목록 `ansCn`, 0건 「찾는 질문이 없어요」·[전체 질문 보기], 페이지네이션, 항목 안 카테고리 칩은 뺐으니 `faqClsfCdNm`은 안 써도 됨, 프론트 저장소에서 /start)
- 연결 남음: SCR-CSC-001 공지사항 화면 (프론트 연결 공지 리스트(검색·고정 배경·전체 n건·0건 안내·페이지네이션)와 상세, 상세 하단 최신 5개는 `?latestYn=true&size=5`, 조회수 표시는 뺀다, 프론트 저장소에서 /start)
- 사람이 할 일: 공지 정렬 확인으로 `UPEND_FIX_YN='Y'`로 바꾼 공지가 있으면 `UPDATE NTC_MTTR SET UPEND_FIX_YN='N' WHERE NTC_MTTR_SN=<번호>;`로 되돌린다. 공지 작성은 DB가 아닌 관리자 화면이 생기기 전까지 직접 넣는다
- 이번에 안 한 것: FN-CSC-001-07 조회수(사람 결정으로 제외, 명세·설계서의 「조회」 표시와 다르니 기획자와 문서 정리), FN-CSC-001-08 관리자 작성, 비로그인 공지 열람, 공지 본문 이미지(issues 10)
- 연결 남음: SCR-CMU-002 실종센터 · 내 글만 보기 (프론트 연결 리스트의 「내 글만 보기」 토글(`mineYn=true`), 만료 배지(`CMUC0008`), 목록 `owner`로 「내 글」 배지(전체 목록에서도), 프론트 저장소에서 /start)
- 연결 남음: SCR-CMU-002 실종센터 화면 전체 (프론트 연결 리스트·상세·작성자 상세·글쓰기·찾았어요/연장 모달·목격 장소 모달, 프론트 저장소에서 /start). 목격 장소 선택은 YAY의 `GET /places` 결과 좌표를 등록 요청에 넣는다
- 사람이 할 일: 병합 직후 공용 DB에 `db/migration/20261029_02_add_cmug01_lost.sql` 실행(PR에 완료 댓글), PR 본문에 "DB 변경 있음". 공용 DB `RGN`에 전북 시군구·읍면동이 거의 없다(시드에 전북 2건). 지역 데이터 적재 담당을 정한다
- 정리: 시험용 실종 글 101~104(공용 DB) 중 지우지 않은 것은 `DELETE /api/lost-reports/{lostTpofSn}`로 지운다. `app.region.client`는 `fake`로 되돌린다
- 이번에 안 한 것: 만료 7일·1일 전 알림 배치, 로그인 회원 설정 지역 우선 정렬, 카카오 역지오코딩 실제 연결(`RegionResolver` 구현을 YAY의 카카오 로컬 클라이언트로 교체), `isPetStory()` 대신 `BoardType.exists()`로 펫이야기 게시판만 구분
- 연결 남음: SCR-CSC-003 1:1 문의 삭제 (프론트 연결 문의 상세의 삭제 버튼(미답변일 때만)과 삭제 뒤 목록 이동, 프론트 저장소에서 /start)
- 사람이 할 일: 삭제 API PR 병합 직후 공용 DB에 `OTO_INQRY.DEL_YN` ALTER 실행(이미 실행했으면 PR에 완료 댓글만), ERD에 `OTO_INQRY.DEL_YN` 추가. 기준 파일 `db/20261029_create_tables_v0.10.sql`에 직접 반영했으므로 PR 본문에 "DB 변경 있음"을 적는다
- 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `spring.servlet.multipart.max-request-size`를 100MB로 바꾼다(배포 서버 설정이 있으면 같이)
- 확인 남음: 펫이야기 글 사진 중 `PostService`가 `storePublic`으로 바꾸기 전에 `store`로 올린 것(`public/` 아님)은 `GET /api/files/public/{atchFileSn}`가 404일 수 있다
- 정리: 검증·확인용으로 만든 문의 223·225·226·227·229·203(203은 삭제 확인 중 DEL_YN='Y'로 이미 지움, 공용 DB, 첨부 파일 4082~4085·4088~4097 포함)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
