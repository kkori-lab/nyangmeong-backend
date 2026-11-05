# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-001 펫이야기 · 탈퇴 회원 표시 — 진행 중 (승인 2026-11-05 11:44, 시작 2026-11-05 11:00, 검증 2회차 실패로 보관, 사람 확인 2026-11-05)
기준: 명세 기능 ID 추가 없음(팀장 요청 2026-11-04, 사람 결정 2026-11-05). 파기 회원(`MemberPurgeService`가 닉네임 `탈퇴회원#번호`, 이메일 HMAC `PURGED…`로 바꾼 회원)을 사용자 화면에서 「탈퇴한 회원」·사진 null로 준다. 탈퇴했지만 파기 전인 회원은 그대로. 응답에 탈퇴 여부 필드는 넣지 않는다. 관리자 쿼리는 건드리지 않는다
- 파기 판단: 닉네임 접두어가 아니라 `MBR_STTS_CD`=탈퇴(`MemberStatus.WITHDRAWN`) AND `ACNT_EML_HMAC LIKE 'PURGED%'`(`MemberMapper.updatePurge`·`MemberPurgeService`). 사진은 파기 때 이미 null이지만 조각이 한 번 더 null로 막는다
- 공통 SQL 조각: 닉네임·사진 두 컬럼을 만드는 `<sql>`을 한 곳에 둔다(후보: `mapper/global/` 아래 조각 전용 XML, 안 되면 `PostMapper.xml`에 두고 다른 community Mapper가 `refid="…PostMapper.xxx"`로 include). 「탈퇴한 회원」 문구와 `PURGED` 접두어 상수는 한 곳(조각)에만 둔다. 구현할 때 `mapper-locations`(`mapper/**/*.xml`)로 로드되는지 확인한다
- FN-CMU-001-01·06·07 (목록·인기 포토·건강팁): 바꿀 파일 `PostMapper.xml` `summaryColumns`(`M.MBR_NCNM`, `M.ATCH_FILE_SN` → 조각), `selectPopularPhoto`가 쓰는 작성자 조회. 응답 `PostSummary`·`PostResponse`. 완료 조건 `명령:` 아래 테스트, `화면:` `GET /api/posts` 목록의 파기 회원 글이 「탈퇴한 회원」, 사진 null
- FN-CMU-001-04 (상세): `PostMapper.selectAuthor`(현재 `MBR_NCNM`·`ATCH_FILE_SN` 직접 읽음, `PostService:371`, `CommentService:79` 사용)를 조각으로. `화면:` `GET /api/posts/{pstSn}` 파기 회원 글 작성자가 「탈퇴한 회원」
- FN-CMU-001-11 (댓글): `CommentMapper.xml` `selectList`(37행 `M.MBR_NCNM`과 사진)를 조각으로. 삭제 댓글은 지금처럼 null. `화면:` `GET /api/posts/{pstSn}/comments`
- 건드리지 않는 것: `selectMbrNcnm`(관리자 신고·관리자 게시글 상세가 씀), `selectAdmin*`, `PRCS_MBR_NCNM`. 이름을 `selectMbrNcnm`처럼 둔 쿼리가 사용자 화면에서도 쓰이는지 구현 때 호출처를 다시 확인한다
- 테스트: 파기 회원 / 탈퇴했지만 파기 전 회원 / 정상 회원 세 가지가 목록·상세·댓글에서 기대한 닉네임·사진으로 나오는 Mapper 또는 서비스 테스트. 닉네임을 읽는 사용자 화면 쿼리 중 조각을 안 쓰는 것을 잡는 테스트는 이번엔 `community`의 펫이야기 쿼리까지만 두고, 나머지 화면은 후속 절에서 넓힌다
- 문서: `docs/api/community.md`의 `PostSummary`·`PostResponse`·`CommentSummary`의 `mbrNcnm`·`mbrAtchFileSn` 설명에 파기 회원 규칙을 적는다(필드 추가 없음)
- 프론트 연결: 없음(서버가 준 이름을 그대로 보여 준다). 프론트에 「작성자를 눌러 이동하는 곳이 있나, 탈퇴 회원을 막아야 하나」는 따로 묻는다. 필요하면 후속 작업으로 그 응답에만 탈퇴 여부 필드를 추가한다
- 다음 절로 나누는 것(이번에 안 함): 실종센터·펫돌봄·채팅·문의·신고(사용자 화면인 것만)·회원 요약. LJS가 관리자 쿼리를 같은 XML 파일에 쓰므로 병합 충돌을 막으려고 LJS에게 미리 알린다
- 완료 명령: `./gradlew build -q`
새 의존성: 없음

## 지난 작업

SCR-ADM-022 완료 사진 열람(2026-11-05), SCR-CMU-004 펫돌봄 대기 4건(2026-11-04), SCR-CMU-002 전북 밖 좌표 거절(PR #154)·시드 게시판 코드(PR #147), SCR-CMU-005 펫돌봄 채팅(PR #139) — 모두 끝남

## 다음에 할 것
- 검증 루프 2회차 실패로 보관 중. 기획은 고치지 않는다. 루프를 버리려면 `! sh scripts/verify-loop.sh clear`. 1회차: 파기 판단에 상태 탈퇴 조건 빠짐(고침), 세 회원(파기·파기 전·정상) 결과 테스트 없음. 2회차: 첫 실패 뒤 기준 파일 `PostMapperSqlTest.java`를 고침(제가 고친 것, 기존 `doesNotContain("MBR_STTS_CD")`가 2026-11-02 결정을 굳히고 있어 새 결정과 안 맞았음), 세 회원 결과 테스트 여전히 없음(DB 테스트 환경·의존성 없음, 기획은 새 의존성 없음). 로컬 DB로 `GET /api/posts/146` 등을 사람이 확인함(2026-11-05)
- 한 일: `mapper/global/MemberDisplay.xml` 공통 조각(상태 탈퇴 `USRC0003` AND HMAC `PURGED%`), `PostMapper.xml` `summaryColumns`·`selectAuthor`, `CommentMapper.xml` `selectList`에 적용, `docs/api/community.md` 설명, `PostMapperSqlTest` 보강. 이 마일스톤은 `진행`. 끝내려면 DB 테스트 추가(H2 등 의존성, 사람 결정) 또는 사람이 이 한계를 받아들이고 루프를 `clear`한 뒤 `/end`
- 사람이 할 일: 프론트에 「작성자를 눌러 이동하는 곳이 있나, 탈퇴 회원을 막아야 하나」를 묻는다(필요하면 후속으로 그 응답에만 탈퇴 여부 필드. 상세·댓글 응답에는 `mbrSn`이 그대로 나간다). LJS에게 `PostMapper.xml`·`CommentMapper.xml` 관리자 쿼리 병합 충돌을 미리 알린다
- 다음 작업: 실종센터·펫돌봄·채팅·문의·신고(사용자 화면)·회원 요약에 같은 조각 적용(후속 절)
- 연결 남음: SCR-ADM-022 관리자 매칭 상세 완료 사진 (프론트 연결: LJS `CareMatchDetailModal.jsx`의 `publicFileUrl`을 `useFileImage` + `GET /api/admin/care-matches/{petPrtcRcrtSn}/images/{atchFileSn}`로 교체, 프론트 저장소에서 /start)
- 사람이 할 일: 시드 완료 사진 78장의 이미지 파일을 `uploads/seed/care/{ATCH_FILE_SN}.jpg`로 채울지 정한다(진짜 JPG, `uploads/`는 git 제외). 저장소 루트의 추적하지 않는 `3293.jpg`는 커밋하지 말고 지우거나 옮긴다
- 연결 남음: FN-CMU-004-05 상세의 [채팅하기]·「채팅 중」, 매칭 돌보미 표시 (프론트 연결: 상세 응답 `chatting`·`mtchSitterNcnm`, 프론트 저장소에서 /start)
- 연결 남음: FN-CMU-004-08 등록·수정 화면의 시급 안내 (프론트 연결: `GET /api/care-recruitments/minimum-wage`, 프론트 저장소에서 /start)
- 연결 남음: FN-CMU-002-03·FN-CMU-004-02 지역 콤보박스 (프론트 연결: `GET /regions`가 전북만 주므로 시도 콤보박스는 전북특별자치도 1곳, 자동 선택하거나 숨김, 시군구는 `?upRgnSn=`로 14곳, 프론트 저장소에서 /start)
- 사람이 할 일: PR 병합 뒤 팀에 "공용 DB 펫돌봄·실종 시드 글 지역을 전북 읍면동으로 바꿨고, `GET /regions`가 전북만 준다"고 알린다. 로컬 `app.region.client`는 `fake`로 되돌린다
- 연결 남음: SCR-CMU-005 채팅 모달 목록·채팅방·거래 카드와 STOMP 연결 (프론트 저장소에서 /start)
- 연결 남음: FN-CMU-005-05 주소 공유 모달 장소 검색 (YAY `GET /places`, 프론트 저장소에서 /start)
- 연결 남음: FN-COM-002-11·FN-COM-009-17 헤더 채팅 아이콘·안 읽은 수 배지 (LJS 대기, 프론트 저장소에서 /start)
- 다음 작업: 14일 무응답 자동 취소·종료 배치(`NO_RESPONSE` `PCAC0018`)와 로그아웃 때 소켓 끊기(공통 모듈 변경이라 담당 확인). 알림 `mvmnPath`는 프론트 채팅 모달 경로가 정해진 뒤 채운다
- 연결 남음: FN-CMU-004-10 주소 찾기·장소 선택 (글쓰기 장소 모달에서 YAY의 `GET /places` 결과 좌표를 등록 요청에 넣음, 프론트 저장소에서 /start)
- 연결 남음: SCR-CMU-004 리스트·상세·글쓰기·[채팅하기] 버튼 (프론트 연결, 지역 필터는 `GET /regions`, 프론트 저장소에서 /start)
- 확인 남음: 공용 DB 시드에서 회원 1의 사진 25는 공개 주소가 404일 수 있다(시드 이미지 묶음 문제). 응답에 번호가 오면 백엔드는 맞다
- 이번 작업 메모: 상세 응답 `mbrSn` 추가로 프론트 `CommentSection`의 `authorSn={post.mbrSn}`에 값이 들어가 글쓴이 댓글에 「작성자」 배지가 붙는다. 프론트 변경 없음
- SCR-CMU-003 주제 무관 기사 거르기(PR #104 병합): 제외 키워드는 `KWRD_DICT` `AIMC0040` 분류별 4행, 공용 DB 실행 완료. 키워드를 늘릴 때는 수집 로그의 `주제 무관 건너뜀`을 보고 정상 기사가 걸리는지 먼저 본다. 쌓인 무관 기사는 2026-11-02 확인에서 0건이라 정리할 것 없음
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
