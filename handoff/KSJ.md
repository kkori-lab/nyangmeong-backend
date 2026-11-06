# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-002 실종센터 · 만료 7일·1일 전 알림 — 끝남 (승인 2026-11-06 14:39, 시작 2026-11-06 13:59, 검증 2회차 통과, 사람 확인 2026-11-06)
- 한 일: 새 배치 `LostReportExpiryAlarmJob`(`SYSC0032`, 매일 09:00)이 정상 게시글의 실종(`CMUC0006`) 제보 중 종료일이 7일 뒤·1일 뒤인 글의 작성자에게 실종 만료 알림(`AIMC0012`, 이동 경로 `/community/lost/{lostTpofSn}`)을 보낸다. 보내기 직전 다시 확인, 글마다 따로 트랜잭션. 같은 날 다시 돌리면 다시 보낸다(사람 결정 2026-11-06). `PST`·`LOST_TPOF`에 `DEL_YN`이 없어 삭제는 게시글 상태로 거른다. FN-CMU-002-01 설정 지역 우선 정렬은 회원 지역 설정이 명세에 없어 제외(사람 확인 2026-11-06). `LostReportService`(생성자에 `EventPublisher` 추가)·`LostReportMapper`·`BatchType`, db v0.13 (4), 테스트

## 지난 작업

SCR-CMU-005 14일 무응답 자동 종료(PR #186, 2026-11-06), SCR-CMU-001·002·004·005 탈퇴 회원 표시(PR #164·#166·#170·#172)·SCR-ADM-022 완료 사진 열람(2026-11-05), SCR-CMU-004 펫돌봄 대기 4건(2026-11-04), SCR-CMU-002 전북 밖 좌표 거절(PR #154)·시드 게시판 코드(PR #147), SCR-CMU-005 펫돌봄 채팅(PR #139) — 모두 끝남

## 다음에 할 것
- 사람이 할 일: 공용 DB에 `db/20261104_insert_base_data_v0.13.sql` 변경 절 「v0.13 (3)」 `SYSC0031`(PR #186 병합됨)·「v0.13 (4)」 `SYSC0032` INSERT가 들어갔는지 확인(`SET NAMES utf8mb4;` 먼저, PR에 완료 댓글). 팀에 "무응답 배치가 첫 실행 때 시드의 오래된 매칭(2026-11-06 확인 15건)을 정리한다"고 알린다. 사람 확인 때 생긴 실종 만료 알림(`INAPP_NOTI`, 「실종 제보 게시 종료 안내」)은 지웠는지 확인한다
- 사람이 할 일: LJS·프론트와 실종 만료 알림 이동 경로 `/community/lost/{lostTpofSn}`가 실제 상세 화면 경로와 같은지 맞춘다(다르면 `LostReportService.EXPIRY_ALARM_PATH`만 바꾼다)
- 연결 남음: FN-CMU-005-06·09 요청 취소(`PCAC0017`)·무응답 종료(`PCAC0018`) 배지(무응답 종료는 거래 완료와 다른 문구), 두 상태에서 실행 버튼 숨김, 시스템 메시지(`CHTC0009`, 보낸 회원 없음) 말풍선 (프론트 연결 SCR-CMU-005 채팅 모달, LJS, 프론트 저장소에서 /start)
- 확인 방법 메모: 배치 수동 실행은 대상 전체를 바로 처리한다. 사람 확인 전에 대상 건수부터 조회한다
- 다음 작업: SCR-CMU-002 실종센터 · 품종 선택 항목(마일스톤 `대기`, `/start CMU-002 품종`). 끝나면 만료 알림 커밋과 한 PR로 올린다
- 다음 작업: 로그아웃 때 소켓 끊기(공통 모듈 변경이라 담당 확인). 알림 `mvmnPath`는 프론트 채팅 모달 경로가 정해진 뒤 채운다. 탈퇴 회원 표시는 커뮤니티 끝남(문의·신고는 관리자 전용이라 대상 없음), 남은 후보는 공동 보호자 목록(`PetMapper.selectCaregivers`, pet 담당 확인)·세 회원 DB 테스트(새 의존성, 팀 합의)
- 사람이 할 일: 프론트에 「작성자를 눌러 이동하는 곳이 있나, 탈퇴 회원을 막아야 하나」를 묻는다(필요하면 후속으로 그 응답에만 탈퇴 여부 필드. 상세·댓글 응답에는 `mbrSn`이 그대로 나간다). LJS에게 `PostMapper.xml`·`CommentMapper.xml` 관리자 쿼리 병합 충돌을 미리 알린다
- 연결 남음: SCR-ADM-022 관리자 매칭 상세 완료 사진 (프론트 연결: LJS `CareMatchDetailModal.jsx`의 `publicFileUrl`을 `useFileImage` + `GET /api/admin/care-matches/{petPrtcRcrtSn}/images/{atchFileSn}`로 교체, 프론트 저장소에서 /start)
- 사람이 할 일: 시드 완료 사진 78장의 이미지 파일을 `uploads/seed/care/{ATCH_FILE_SN}.jpg`로 채울지 정한다(진짜 JPG, `uploads/`는 git 제외). 저장소 루트의 추적하지 않는 `3293.jpg`는 커밋하지 말고 지우거나 옮긴다
- 연결 남음: FN-CMU-004-05 상세의 [채팅하기]·「채팅 중」, 매칭 돌보미 표시 (프론트 연결: 상세 응답 `chatting`·`mtchSitterNcnm`, 프론트 저장소에서 /start)
- 연결 남음: FN-CMU-004-08 등록·수정 화면의 시급 안내 (프론트 연결: `GET /api/care-recruitments/minimum-wage`, 프론트 저장소에서 /start)
- 연결 남음: FN-CMU-002-03·FN-CMU-004-02 지역 콤보박스 (프론트 연결: `GET /regions`가 전북만 주므로 시도 콤보박스는 전북특별자치도 1곳, 자동 선택하거나 숨김, 시군구는 `?upRgnSn=`로 14곳, 프론트 저장소에서 /start)
- 사람이 할 일: PR 병합 뒤 팀에 "공용 DB 펫돌봄·실종 시드 글 지역을 전북 읍면동으로 바꿨고, `GET /regions`가 전북만 준다"고 알린다. 로컬 `app.region.client`는 `fake`로 되돌린다
- 연결 남음: SCR-CMU-005 채팅 모달 목록·채팅방·거래 카드와 STOMP 연결 (프론트 저장소에서 /start)
- 연결 남음: FN-CMU-005-05 주소 공유 모달 장소 검색 (YAY `GET /places`, 프론트 저장소에서 /start)
- 연결 남음: FN-COM-002-11·FN-COM-009-17 헤더 채팅 아이콘·안 읽은 수 배지 (LJS 대기, 프론트 저장소에서 /start)
- 연결 남음: FN-CMU-004-10 주소 찾기·장소 선택 (글쓰기 장소 모달에서 YAY의 `GET /places` 결과 좌표를 등록 요청에 넣음, 프론트 저장소에서 /start)
- 연결 남음: SCR-CMU-004 리스트·상세·글쓰기·[채팅하기] 버튼 (프론트 연결, 지역 필터는 `GET /regions`, 프론트 저장소에서 /start)
- 사람이 할 일(공용 DB, 일회성이라 `db/`에 넣지 않음): 시드와 확인용으로 쌓인 기존 뉴스를 지우고 새 중복 판정으로 다시 수집한다(사람 결정 2026-11-02). 대상은 외부 언론사(`CMUC0022`) 143건만이다. 관리자 등록(`CMUC0023`) 시드 14건은 프론트의 「관리자 등록」 배지·원문보기 미노출 확인용으로 남긴다(사람 결정 2026-11-02). 지운 뒤 로컬에서 `app.news.batch-enabled=true` 또는 `POST /api/admin/batches/SYSC0028/run`을 한 번 돌려 로그의 `중복 건너뜀 K건`과 `GET /api/news`를 확인하고, 팀에 공용 DB를 비웠다고 알린다
- 연결 남음: SCR-CMU-003 펫뉴스 화면 (프론트 연결 리스트(카테고리 칩 `newsClsfCd`, 검색 `keyword`, 언론사 `srcPressNm`·「관리자 등록」 `newsSrcCdNm` 배지, 요약 줄 `newsCn`, 썸네일 없으면 기본 이미지, 페이지네이션)와 상세(원문보기는 외부 기사만, 하단 최신 5건은 `?size=5`), 프론트 저장소에서 /start)
- 연결 남음: SCR-CSC-002 FAQ 화면 (프론트 연결 카테고리 탭 → `faqClsfCd`, 검색 `keyword`, 아코디언 Q/A는 목록 `ansCn`, 0건 「찾는 질문이 없어요」·[전체 질문 보기], 페이지네이션, 항목 안 카테고리 칩은 뺐으니 `faqClsfCdNm`은 안 써도 됨, 프론트 저장소에서 /start)
- 연결 남음: SCR-CSC-001 공지사항 화면 (프론트 연결 공지 리스트(검색·고정 배경·전체 n건·0건 안내·페이지네이션)와 상세, 상세 하단 최신 5개는 `?latestYn=true&size=5`, 조회수 표시는 뺀다, 프론트 저장소에서 /start)
- 사람이 할 일: 공지 정렬 확인으로 `UPEND_FIX_YN='Y'`로 바꾼 공지가 있으면 `UPDATE NTC_MTTR SET UPEND_FIX_YN='N' WHERE NTC_MTTR_SN=<번호>;`로 되돌린다. 공지 작성은 DB가 아닌 관리자 화면이 생기기 전까지 직접 넣는다
- 연결 남음: SCR-CMU-002 실종센터 · 내 글만 보기 (프론트 연결 리스트의 「내 글만 보기」 토글(`mineYn=true`), 만료 배지(`CMUC0008`), 목록 `owner`로 「내 글」 배지(전체 목록에서도), 프론트 저장소에서 /start)
- 연결 남음: SCR-CMU-002 실종센터 화면 전체 (프론트 연결 리스트·상세·작성자 상세·글쓰기·찾았어요/연장 모달·목격 장소 모달, 프론트 저장소에서 /start). 목격 장소 선택은 YAY의 `GET /places` 결과 좌표를 등록 요청에 넣는다
- 사람이 할 일: 병합 직후 공용 DB에 `db/migration/20261029_02_add_cmug01_lost.sql` 실행(PR에 완료 댓글), PR 본문에 "DB 변경 있음". 공용 DB `RGN`에 전북 시군구·읍면동이 거의 없다(시드에 전북 2건). 지역 데이터 적재 담당을 정한다
- 정리: 시험용 실종 글 101~104(공용 DB) 중 지우지 않은 것은 `DELETE /api/lost-reports/{lostTpofSn}`로 지운다. `app.region.client`는 `fake`로 되돌린다
- 연결 남음: SCR-CSC-003 1:1 문의 삭제 (프론트 연결 문의 상세의 삭제 버튼(미답변일 때만)과 삭제 뒤 목록 이동, 프론트 저장소에서 /start)
- 사람이 할 일: 삭제 API PR 병합 직후 공용 DB에 `OTO_INQRY.DEL_YN` ALTER 실행(이미 실행했으면 PR에 완료 댓글만), ERD에 `OTO_INQRY.DEL_YN` 추가. 기준 파일 `db/20261029_create_tables_v0.10.sql`에 직접 반영했으므로 PR 본문에 "DB 변경 있음"을 적는다
- 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `spring.servlet.multipart.max-request-size`를 100MB로 바꾼다(배포 서버 설정이 있으면 같이)
- 정리: 검증·확인용으로 만든 문의 223·225·226·227·229·203(203은 삭제 확인 중 DEL_YN='Y'로 이미 지움, 공용 DB, 첨부 파일 4082~4085·4088~4097 포함)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
