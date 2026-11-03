# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-004 우리동네 펫돌봄 — 끝남 (승인 2026-11-03 09:46, 시작 2026-11-03 09:33, 검증 통과 1회차(새 루프), 사람 확인 2026-11-03)
기준: 커뮤니티 기능명세서·화면설계서 v0.8(20261101). 걸치는 기능 FN-CMU-004-01·02·04·05·06~11·13, FN-COM-009-02. 제외 FN-CMU-004-12 좋아요·신고(기존 `/posts/{pstSn}/likes`·`/reports`가 펫돌봄 글에도 동작)
- API: `GET·POST /care-recruitments`, `GET·PATCH·DELETE /care-recruitments/{petPrtcRcrtSn}`(등록·수정은 multipart), `POST /chat-rooms`(새 방 201, 기존 방 200). 목록·상세는 `mbrNcnm`과 `mbrAtchFileSn`을 준다
- 결정: 강아지 크기는 컬럼 추가 없이 대표 체중(5·15·30kg, 고양이 0)으로 저장하고 `petSizeCd`로 주고받는다(`issues.md` 커뮤니티 11번). 「채팅 n명」은 메시지가 1건 이상 오간 방만 센다. 수정은 모집중인 글만, 삭제는 상태 무관. 블라인드·삭제 글에는 새 채팅방을 만들지 않고 기존 방은 계속 연다. 신고 누적 처리는 펫이야기와 같다
- DB: 게시판 코드 `CMUC0028`(우리동네 펫돌봄)을 기준 SQL 본문과 변경 절 v0.12 (3)에 추가했고 공용 DB에 실행했다(`COM_CD_SN` 391)
- 1회차 검증이 `ErrorCode` 이름이 기획과 달라 실패해 사람이 루프를 버리고 새 루프로 통과했다. 기존 `CARE_WAGE_BELOW_MINIMUM`·`CHAT_SELF_NOT_ALLOWED`·`CARE_RECRUITMENT_CLOSED`는 새 코드와 뜻이 겹치나 쓰지 않는다
- 알려 둔 것: 중복 키 뒤 방을 다시 읽을 때 `FOR SHARE`를 쓴다. 단위 테스트는 mock이라 실제 격리 수준 동작은 로컬 MySQL에서 동시 요청으로 따로 봐야 한다
이번에 하지 않은 것: FN-CMU-004-12(위), 메시지·채팅 목록·매칭(SCR-CMU-005)
새 의존성: 없음

### 추가 요청
| 추가-1 | `ChatRoomMapper`(java·XML), `ChatRoomService`, `ChatRoomServiceTest`, `CareRecruitmentMapperSqlTest` | 명령: `./gradlew build -q` 통과. 중복 키 뒤 다시 조회는 `FOR SHARE` 잠금 읽기라 상대 트랜잭션이 커밋한 방을 읽고, 그래도 없으면 NPE 대신 `CustomException`을 던진다 | 요청 2026-11-03 10:21 "동시 채팅 요청에서 중복 키 뒤 다시 조회가 방을 못 보는 문제를 이번에 고침" |

## 다음에 할 것
- 연결 남음: FN-CMU-004-10 주소 찾기·장소 선택 (글쓰기 장소 모달에서 YAY의 `GET /places` 결과 좌표를 등록 요청에 넣음, 프론트 저장소에서 /start)
- 연결 남음: SCR-CMU-004 리스트·상세·글쓰기·[채팅하기] 버튼 (프론트 연결, 지역 필터는 `GET /regions`, 프론트 저장소에서 /start)
- 사람이 할 일: PR 본문에 "DB 변경 있음, `db/20261102_insert_base_data_v0.12.sql` 변경 절 v0.12 (3)"와 공용 DB 실행 완료 댓글(2026-11-03, `COM_CD_SN` 391). 확인용 글 `pstSn` 1003(공용 DB)과 채팅방 행은 정리했다
- 확인 남음: 공용 DB 시드에서 회원 1의 사진 25는 공개 주소가 404일 수 있다(시드 이미지 묶음 문제). 응답에 번호가 오면 백엔드는 맞다
- 다음 작업: SCR-CMU-002 실종센터 · 전북 밖 좌표 거절(`/start CMU-002 전북`). 우선순위 낮음. 그 뒤 SCR-CMU-005 펫돌봄 채팅(메시지·채팅 목록·매칭, 장소 검색은 YAY `GET /places`)
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
