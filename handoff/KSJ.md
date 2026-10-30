# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-001 공지사항 — 끝남 (승인 2026-10-30 14:49, 시작 2026-10-30 14:19, 검증 통과 2회차 루프 2회차, 사람 확인 2026-10-30)
기준: 고객센터 기능명세서 v0.5(2026-10-29)·화면설계서 v0.4(2026-10-25). `docs/api/support.md` `GET /notices`·`GET /notices/{ntcMttrSn}` 행, `NoticeSummary`·`NoticeResponse`
- 공통 파일: `support/domain/Notice.java`(NTC_MTTR VO), `support/mapper/NoticeMapper.java`, `mapper/support/NoticeMapper.xml`, `support/service/NoticeService.java`, `support/controller/NoticeController.java`, `support/dto/NoticeSummary.java`·`NoticeResponse.java`·`NoticeSearchCond.java`. 테스트 `NoticeServiceTest`·`NoticeControllerTest`. 로그인 필요(문의와 같다). 조회 조건 `USE_YN = 'Y'`(이 테이블엔 `DEL_YN` 없음). 없는 글·`USE_YN='N'`은 404 `NOTICE_NOT_FOUND`(이미 있음)
- FN-CSC-001-01 목록 조회: `GET /api/notices` → `PageResponse<NoticeSummary>`(10건, `PageQuery` `sizeOr(10)`, 전체 건수는 `PageResponse` total). 완료 조건 명령: `./gradlew build -q`, `NoticeControllerTest` 목록 200·페이지 바인딩. 화면: Postman `GET /api/notices` → 10건·total
  - "최대 10페이지"는 페이지 번호 10개 묶음(FN-CSC-001-04)으로 보고 서버에서 자르지 않는다
- FN-CSC-001-02 상단 고정: `ORDER BY UPEND_FIX_YN DESC, REG_DT DESC, NTC_MTTR_SN DESC`(인덱스 `IDX_NTC_MTTR_1`). 응답 `upendFixYn`(boolean). 완료 조건 화면: 작성일이 가장 오래된 공지를 DB에서 `UPEND_FIX_YN='Y'`로 바꾸면 1페이지 맨 위
- FN-CSC-001-03 제목 검색: 쿼리 `keyword`(`NoticeSearchCond`, 앞뒤 공백 제거, 빈 값은 조건 없음) → `NTC_MTTR_TTL LIKE CONCAT('%', #{keyword}, '%')`. 고정 공지도 검색에 걸려야 나온다. 완료 조건: 서비스 테스트에 공백 keyword → null, 화면: `?keyword=점검` → 제목에 "점검"만, 0건이면 빈 목록·total 0
- FN-CSC-001-04 페이지네이션: `page`·`size`(PageQuery). 완료 조건 화면: `?page=2` → 11번째부터
- FN-CSC-001-05 상세: `GET /api/notices/{ntcMttrSn}` → `NoticeResponse`(제목·본문·작성일). 조회수는 쓰지 않는다(사람 결정 2026-10-30)라 응답에서 `inqCnt`를 뺀다. 작성자는 화면에서 "관리자" 고정(issues 10, 필드 추가 없음). 구현 전에 `docs/api/support.md` 상세 행 설명의 "조회수 세션 기준 1회 증가"와 `NoticeResponse`의 `inqCnt` 행을 지우고 기능 ID에서 07을 뺀다. 완료 조건 명령: `NoticeServiceTest`에 없는 글 404. 화면: 상세 200, 없는 번호 404, 같은 공지를 여러 번 열어도 DB `INQ_CNT` 그대로
- FN-CSC-001-06 최신 5개: 목록 API에 쿼리 `latestYn`(Boolean, 기본 false)을 더해 `true`면 고정 무시하고 `REG_DT DESC`만 → 프론트가 `?latestYn=true&size=5`로 부른다. 초안과 다름(이유: 초안은 목록 API를 그대로 쓰는데, 고정 우선 정렬이면 최신 5건이 아니라 고정 공지가 먼저 나온다). 구현 전에 `docs/api/support.md` `GET /notices` 행 설명에 `latestYn`을 적는다. 완료 조건 화면: 고정 공지가 있어도 `?latestYn=true&size=5`는 작성일 최신 5건
- FN-CSC-001-07 조회수 증가, FN-CSC-001-08 관리자 작성: 제외(마일스톤 "제외" 절). 07은 사람 결정(공지는 조회수를 쓰지 않음)이라 DB 변경·조회 이력 테이블·`@Transactional` 쓰기가 없다. `INQ_CNT` 컬럼은 건드리지 않는다
- 프론트 연결: SCR-CSC-001 공지 리스트(검색·고정 배경·전체 n건·0건 안내·페이지네이션)와 상세(최신 5개는 `latestYn=true&size=5`) (프론트 저장소에서 /start)
이번에 하지 않는 것: 비로그인 공지 열람(명세에 없음, 열려면 `permit-all-paths` 결정 필요), 공지 본문 이미지(issues 10, 첨부 테이블 없음 — `NTC_MTTR_CN` 텍스트 그대로 내린다), 관리자 작성
새 의존성: 없음

## 다음에 할 것
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
