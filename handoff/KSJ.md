# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-003 펫뉴스 — 끝남 (승인 2026-11-02 10:12, 시작 2026-11-02 10:05, 검증 통과 2회차, 사람 확인 2026-11-02)
기준: 커뮤니티 기능명세서 v0.8·화면설계서 v0.8(2026-11-01). `docs/api/community.md` `GET /news`·`GET /news/{newsSn}` 행, `NewsSummary`·`NewsResponse`
결정(사람 2026-11-02): 수집원은 Google News RSS 검색, 검색어 `반려동물 OR 반려견 OR 반려묘 OR 강아지 OR 고양이`. 원문 주소를 풀어 `og:description`(요약)·`og:image`(썸네일)를 읽고, 실패하면 요약은 제목·썸네일은 NULL. 본문 전체는 저장하지 않는다
- 공통 파일: `community/domain/News.java`(NEWS VO), `NewsCategory.java`(CMUG06 enum: `CMUC0018` 건강·`CMUC0019` 생활·`CMUC0020` 정책·`CMUC0021` 여행, `exists()`), `NewsSource.java`(CMUG07: `CMUC0022` 외부 언론사·`CMUC0023` 관리자 등록), `community/mapper/NewsMapper.java`, `mapper/community/NewsMapper.xml`, `community/service/NewsService.java`, `community/controller/NewsController.java`, `community/dto/NewsSummary.java`·`NewsResponse.java`·`NewsSearchCond.java`. `ErrorCode` 커뮤니티 구획에 `NEWS_NOT_FOUND`(404). 로그인 필요(다른 커뮤니티 API와 같다). 조회 조건 `USE_YN = 'Y'`(NEWS에 `DEL_YN` 없음). 댓글·좋아요 없음
- FN-CMU-003-01 목록 조회: `GET /api/news` → `PageResponse<NewsSummary>`(10건, `PageQuery` `sizeOr(10)`). 정렬 `PBLCN_DT DESC, NEWS_SN DESC`. 출처 배지 `newsSrcCdNm`은 `LEFT JOIN COM_CD ... AND COM_CD_PRNT_SN IS NOT NULL`. 외부·관리자 등록을 함께 돌려준다. 완료 조건 명령: `./gradlew build -q`, `NewsControllerTest` 목록 200·필드·페이지 바인딩. 화면: Postman `GET /api/news` → 최신순 10건·total
  - `NewsSummary`에 `srcPressNm`(언론사 배지, 설계서 목록에 언론사명), `newsCn`(설계서 목록에 요약 줄, `LEFT(NEWS_CN, 200)`)을 더한다. 구현 전에 `docs/api/community.md` `NewsSummary` 절과 `GET /news` 행 설명(쿼리 `newsClsfCd`·`keyword`·`page`·`size`)을 고친다
- FN-CMU-003-02 카테고리 필터: 쿼리 `newsClsfCd`(없거나 공백이면 전체) → `N.NEWS_CLSF_CD = #{newsClsfCd}`(인덱스 `IDX_NEWS_1`). CMUG06 하위 코드가 아니면 400 `INVALID_INPUT_VALUE`. 완료 조건 명령: `NewsServiceTest` 없는 코드 → 400, 공백 → 조건 없음. 화면: `?newsClsfCd=CMUC0018` → 건강만, `?newsClsfCd=XXXX` → 400
- FN-CMU-003-03 키워드 검색: 쿼리 `keyword`(앞뒤 공백 제거, 빈 값은 조건 없음) → `(N.NEWS_TTL LIKE ... OR N.NEWS_CN LIKE ...)`, 카테고리와 AND. 완료 조건 명령: `NewsMapperSqlTest` 카테고리·키워드를 함께 주면 AND와 괄호 OR. 화면: `?keyword=입양` → 제목·요약에 "입양"이 든 것만
- FN-CMU-003-04 상세 조회: `GET /api/news/{newsSn}` → `NewsResponse`(초안 그대로). 없거나 `USE_YN = 'N'`이면 404 `NEWS_NOT_FOUND`. 외부 기사의 `newsCn`은 요약뿐, `orgtxtPath`가 원문보기 링크. 관리자 등록 뉴스는 `orgtxtPath` null(원문보기 미노출은 프론트가 `newsSrcCd`로 판단). 하단 최신 5건은 별도 API 없이 `GET /api/news?size=5`. 완료 조건 명령: `NewsServiceTest` 없는 번호 → 404. 화면: 상세 200·`newsSrcCdNm`, 없는 번호 404
- FN-CMU-003-06 뉴스 수집: 
  - `global/external/NewsFeedClient.java`(인터페이스)·`GoogleNewsFeedClient.java`·`NewsFeedItem.java`·`NewsFeedProperties.java`(`app.news.*`). RSS 호출과 파싱은 Spring `RestClient` + JDK XML(DOM). 기사마다 ① RSS 링크에서 원문 주소 풀기(기사 페이지의 `data-n-a-sg`·`data-n-a-ts` → `batchexecute` POST, 비공식) ② 원문 페이지 앞부분(최대 512KB)에서 `og:description`·`og:image` 읽기. 단계마다 5초 제한, 실패는 WARN 로그 후 대체값, 기사 사이 간격 `app.news.request-interval-ms`(기본 1000)
  - `community/service/NewsService.collect(LocalDate)`: 어제~오늘 검색(`after:`·`before:`) → `ORGTXT_PATH`가 이미 있는 기사는 건너뜀(원문 주소 풀기 전) → 제목 끝 ` - 언론사` 제거, `SRC_PRESS_NM` = `<source>`, `PBLCN_DT` = `pubDate`(KST), `NEWS_SRC_CD` = `CMUC0022`, 길이 넘치면 자름 → 분류 → `INSERT IGNORE`(UK_NEWS_1). 반환은 저장 건수. 네트워크 호출 중 트랜잭션을 잡지 않는다
  - `ORGTXT_PATH`는 RSS의 Google News 링크(브라우저에서 원문으로 이동)를 그대로 저장한다. 원문 주소 풀기가 성공하든 실패하든 같은 값이라 중복 판정이 흔들리지 않는다
  - 분류: `KWRD_DICT`의 `DICT_USG_CD = 'AIMC0039'`(뉴스 분류) `USE_YN = 'Y' AND DEL_YN = 'N'` 행을 `SORT_SEQ` 순으로 읽어 `KWRD_CN`(쉼표 구분)이 제목에 들면 `MPNG_CD`, 하나도 안 걸리면 `CMUC0019` 생활. 사전 행은 관리자 SCR-ADM-050(LJS)이 고친다
  - `community/service/NewsCollectJob.java`(`BatchJob`, `BatchType.NEWS_COLLECT` = `SYSC0028` 뉴스 수집, cron `0 0 6,18 * * *`, `app.news.batch-enabled=true`일 때만 등록). 관리자 수동 실행은 기존 `POST /api/admin/batches/SYSC0028/run`
  - `db/` 기준 파일: `insert_base_data` v0.11 (4) — `SYSC0028` 뉴스 수집(SYSG02), `KWRD_DICT` 뉴스 분류 초기 행(건강: 건강·질병·병원·백신·예방접종·진드기·수의사…, 정책: 정부·법·제도·의무화·과태료·지자체·조례…, 여행: 여행·동반·숙소·캠핑·휴가…). 변경 절에 기존 DB용 INSERT 주석
  - `application.properties.example` 커뮤니티 구획: `app.news.rss-url=https://news.google.com/rss/search`, `app.news.query=반려동물 OR 반려견 OR 반려묘 OR 강아지 OR 고양이`, `app.news.request-interval-ms=1000`, `# app.news.batch-enabled=true`
  - 완료 조건 명령: `GoogleNewsFeedClientTest`(RSS 예제 XML 파싱: 제목·언론사·링크·KST 발행일, og 메타 추출, 원문 주소 풀기 실패 시 빈 값), `NewsServiceTest`(키워드 분류·미분류는 생활, 이미 있는 링크는 원문 주소를 풀지 않음, og 실패 시 요약 = 제목·썸네일 null, 제목 언론사 꼬리 제거). 화면: 로컬에서 `app.news.batch-enabled=true` 또는 관리자 수동 실행 → `NEWS`에 기사가 쌓이고 두 번 돌려도 늘지 않음, 샘플 5건 중 요약·썸네일이 채워진 건수 확인(0건이면 원문 주소 풀기가 막힌 것, 대체값으로 저장되는지 확인)
- 프론트 연결: SCR-CMU-003 펫뉴스 리스트(카테고리 칩 → `newsClsfCd`, 검색 `keyword`, 언론사·「관리자 등록」 배지, 요약 줄, 썸네일 없으면 기본 이미지, 페이지네이션)·상세(출처·작성일, 원문보기는 외부 기사만, 하단 최신 5건 `?size=5`) (프론트 저장소에서 /start)
이번에 하지 않는 것: FN-CMU-003-05 공유하기(화면만), 관리자 뉴스 등록·숨김·카테고리 변경·키워드 편집(SCR-ADM-050, LJS), 외부 API 호출 기록(`EXTL_API_CALL_LOG`), AI 요약, 기사 본문 저장
새 의존성: 없음 (HTTP 호출은 JDK `java.net.http`, XML 파싱은 JDK, JSON은 Jackson)

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 근거 |
| --- | --- | --- | --- |
| 추가-1 | `src/test/java/com/nyangmeong/global/batch/AdminBatchServiceTest.java` | 명령: `./gradlew build -q` 종료 코드 0. 배치 목록 기대값에 `SYSC0028`(뉴스 수집)이 들어간다 | 요청 2026-11-02 "승인": `BatchType.NEWS_COLLECT` 추가로 배치 코드 목록이 다섯 개가 되어 기대값이 네 개로 못 박힌 기존 테스트가 실패 |
| 추가-2 | `BatchType.java`, `db/20261030_insert_base_data_v0.11.sql`, `application.properties.example`, `AdminBatchServiceTest.java` | 명령: `./gradlew build -q` 종료 코드 0. 뉴스 수집 배치 코드가 `SYSC0028`이고 `SYSC0026`·`SYSC0027`은 쓰지 않는다 | 요청 2026-11-02 사람 확인 중 공용 DB에서 `SYSC0026`은 「회원 정지 만료 해제」(LJS, PR #94), `SYSC0027`은 「탈퇴 회원 파기」(LJS)로 이미 쓰임: 코드 충돌이라 번호를 `SYSC0028`로 |
| 추가-3 | `src/main/java/com/nyangmeong/community/service/NewsCollectJob.java`, `src/main/resources/application.properties.example` | 명령: `./gradlew build -q` 종료 코드 0. 뉴스 수집 cron이 `0 0 6,18 * * *`(하루 2번, 06시·18시)이다 | 요청 2026-11-02 "추천한대로 06시와 18시로 바꿀게": 하루 2번 수집 |
| 추가-4 | `src/main/resources/application-prod.properties` | 명령: `./gradlew build -q` 종료 코드 0. 운영(prod)에서 `app.news.batch-enabled=true`로 뉴스 수집 배치가 켜진다(케어미션과 같은 방식, 주석 한 줄 포함) | 요청 2026-11-02 "운영 설정은 추천대로 운영에서 켜도록 한다" |

## 다음에 할 것
- 연결 남음: SCR-CMU-003 펫뉴스 화면 (프론트 연결 리스트(카테고리 칩 `newsClsfCd`, 검색 `keyword`, 언론사 `srcPressNm`·「관리자 등록」 `newsSrcCdNm` 배지, 요약 줄 `newsCn`, 썸네일 없으면 기본 이미지, 페이지네이션)와 상세(원문보기는 외부 기사만, 하단 최신 5건은 `?size=5`), 프론트 저장소에서 /start)
- 사람이 할 일: 병합 직후 PR에 "DB 변경 있음, v0.11 (4)"와 완료 댓글(공용 DB에 `SYSC0028` 뉴스 수집·뉴스 분류 키워드 3행 실행 완료, 2026-11-02). 공용 DB `NEWS`에 확인용으로 수집한 기사 약 107건이 프론트 개발용으로 남아 있다
- 사람이 할 일: `origin/LJS`의 `BatchType`(`SYSC0027` 탈퇴 회원 파기)·`AdminBatchServiceTest`·`db/…sql`과 병합 때 겹친다. 병합 뒤 배치 코드는 `SYSC0026`·`SYSC0027`·`SYSC0028` 순이다. 새 코드는 쓰기 전에 공용 DB `SELECT MAX(COM_CD) FROM COM_CD WHERE COM_CD LIKE 'SYSC%'`로 확인한다
- 이번에 안 한 것: FN-CMU-003-05 공유하기(화면만), 관리자 뉴스 등록·숨김·키워드 편집(SCR-ADM-050, LJS), 영상 기사 제외(`og:type=video*`), 주제 무관 기사(게임·웹소설) 정리, 외부 API 호출 기록(`EXTL_API_CALL_LOG`), AI 요약, 썸네일 공통 이미지(`noimg.png`) 거르기
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
