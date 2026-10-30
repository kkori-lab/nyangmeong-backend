# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-002 실종센터 · 내 글만 보기 — 끝남 (승인 2026-10-30 12:29, 시작 2026-10-30 12:27, 검증 통과 2회차 루프 1회차, 사람 확인 2026-10-30)
기준: 커뮤니티 기능명세서 v0.7·화면설계서 v0.7에 없음(사람 요청 2026-10-30, 명세 기능 ID 추가 없음). 걸치는 기능 FN-CMU-002-01 목록 조회, FN-CMU-002-02 상태별 필터, FN-CMU-002-09 찾았어요·연장. `GET /api/lost-reports` 행(`docs/api/community.md` 17행)
- 왜 필요한가: 만료된 실종 글(`실종` 상태 + `EXPRY_CRTR_DT < NOW()`)은 지금 목록에서 빠지고 상세는 작성자만 본다. 작성자가 자기 글(만료 글 포함)을 찾아 상세·수정·삭제로 갈 길이 없다
- 요청: `GET /api/lost-reports`에 쿼리 `mineYn`(Boolean, 기본 false)을 더한다. 새 경로는 만들지 않는다(필터·키워드·12건 페이징을 그대로 같이 쓴다)
  - 바꿀 파일: `community/dto/LostReportSearchCond.java`(`mineYn`), `community/service/LostReportService.java`(`search`), `community/mapper/LostReportMapper.java`(`selectList` 인자 `mineYn`·`expiredCd`), `mapper/community/LostReportMapper.xml`(`selectList`)
  - `mineYn=true`면 `P.MBR_SN = #{mbrSn}`만 보고, 만료 글도 넣는다(정상 게시 `PST_STTS_CD`만, 삭제된 글은 빠진다). `mineYn=false`·없음이면 지금과 똑같다
  - 응답 `lostTpofSttsCd`: `실종` 상태로 만료 시각이 지난 글은 `CMUC0008`(만료)로 내린다(DB는 배치가 없어 `실종` 그대로라 SQL `CASE`로 계산). 작성자 상세 응답이 만료를 어떻게 내리는지 구현 전에 `isExpired`·`selectDetail`을 확인해 같게 맞춘다
  - 상태 필터 `lostTpofSttsCd`: `mineYn=true`일 때만 `CMUC0008`도 받는다(만료 = `실종` + 만료 시각 지남, `CMUC0006`은 만료 안 된 실종만). `mineYn`이 아니면서 `CMUC0008`이면 지금처럼 400 `INVALID_INPUT_VALUE`
  - 정렬: 지금과 같은 규칙에 만료를 맨 뒤로(실종 → 찾았어요 → 만료, 그 안에서 `REG_DT DESC`)
  - 찾았어요 비공개(`hiddenCard`)는 작성자 본인 글이라 걸리지 않는다. 변경 없음
  - 완료 조건 명령: `./gradlew build -q` 통과. `LostReportServiceTest`에 `mineYn=false`에서 `CMUC0008` 400, `mineYn=true`면 `CMUC0008` 통과·mapper에 `mineYn`이 넘어가는 경우를 더하고, `LostReportControllerTest`에 `mineYn=true` 바인딩 케이스를 더한다
  - 완료 조건 화면(Postman): 1번이 실종 2건(그중 1건은 DB에서 `EXPRY_CRTR_DT`를 어제로), 2번이 실종 1건 등록 → 1번 `GET /api/lost-reports?mineYn=true` → 1번 글 2건만, 만료 글은 `lostTpofSttsCd=CMUC0008`로 맨 뒤. `mineYn=true&lostTpofSttsCd=CMUC0008` → 1건. `mineYn` 없이 → 만료 글 빠지고 2번 글 포함(기존 동작). `lostTpofSttsCd=CMUC0008`만 주면 400. 1번이 DELETE한 글은 `mineYn=true`에서도 빠진다
- 문서(같은 PR): `docs/api/community.md` `GET /lost-reports` 행 설명에 `mineYn`(내 글만, 만료 포함)을 더한다. `LostReportSummary`의 `lostTpofSttsCd` 설명에 만료(`CMUC0008`)가 내 글 보기에서만 나온다고 적는다
- 프론트 연결: SCR-CMU-002 리스트의 「내 글만 보기」 토글과 만료 배지 (프론트 저장소에서 /start)
이번에 하지 않는 것: 마이페이지 「내가 쓴 글」 통합 목록(펫이야기·펫돌봄과 합치기), 만료 알림 배치
새 의존성: 없음

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `community/service/LostReportService.java`(`toResponse`), `LostReportServiceTest.java` | 명령: `./gradlew build -q` 통과, 만료된 실종 글의 작성자 상세 응답 `lostTpofSttsCd`가 `CMUC0008`. 화면: 만료시킨 글의 `GET /api/lost-reports/{lostTpofSn}`(작성자) → `CMUC0008`, 만료 전 글은 `CMUC0006` 그대로 | 요청 2026-10-30 12:39 "작성자 상세의 상태도 목록처럼 만료면 CMUC0008로 맞춰 줘" |
| 추가-2 | `community/dto/LostReportSummary.java`(`owner`), `mapper/community/LostReportMapper.xml`(`selectList`, resultMap), `LostReportControllerTest.java`, `docs/api/community.md` | 명령: `./gradlew build -q` 통과. 화면: `GET /api/lost-reports`(`mineYn` 없이)에서 내가 쓴 글의 `owner=true`, 남의 글은 `false`. `mineYn=true`면 모두 `true` | 요청 2026-10-30 13:26 "목록 응답에 내 글 여부 필드(owner)를 넣어 내 글 배지를 붙일 수 있게 해 줘" |

## 다음에 할 것
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
