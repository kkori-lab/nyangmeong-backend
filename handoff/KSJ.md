# KSJ 핸드오프

## 지금 하는 것

SCR-CMU-002 실종센터 — 끝남 (승인 2026-10-29 16:45, 시작 2026-10-29 16:38, 검증 통과 2회차 루프 1회차, 사람 확인 2026-10-30)
기준: 커뮤니티_기능명세서_v0.7_20261019(FN-CMU-002-01~13), 커뮤니티_화면설계서_v0.7_20261019(SCR-CMU-002-01~06), `docs/api/community.md`·`docs/api/pet.md` 초안, `docs/api/issues.md` 12·14·18, `LOST_TPOF`·`LOST_TPOF_STTS_HSTRY`·`VRTY`·`RGN`(db/20261029_create_tables_v0.10.sql)
공통 결정:
- 게시판 코드: `CMUG01`에 실종센터 코드가 없다. `db/migration/20261029_02_add_cmug01_lost.sql`로 `CMUC0024 실종센터`를 넣고 `BoardType.LOST`를 더한다. `BoardType`에 `isPetStory()`를 두고 `PostService`의 게시판 검사·`GET /posts`(bbsCd 없을 때 `BBS_CD IN (펫정보, 펫일상)`)·`GET/PATCH/DELETE /posts/{pstSn}`은 펫이야기 게시판만 받는다(실종 글이 펫이야기 목록·상세로 새지 않게, 없으면 404 `POST_NOT_FOUND`). 좋아요·댓글·신고는 `/posts/{pstSn}/...`를 같이 쓴다
- 실종지역(`RGN_SN`)은 서버가 좌표로 계산한다(명세 "역지오코딩 자동 계산", 글수정 "서버가 재실행"). `global/external/RegionResolver`(좌표 → 행정구역코드) 인터페이스와 `FakeRegionResolver`(`app.region.client=fake`, 고정 `5211000000` 전주시)를 둔다. 카카오 `coord2regioncode` 실제 구현은 YAY의 카카오 로컬 클라이언트가 생기면 연결. 행정구역코드 → `RGN_SN`은 `mapper/global/RegionMapper.xml`. 전북(`ADMDST_CD` 52로 시작)이 아니면 400 `LOST_REPORT_REGION_NOT_ALLOWED`("전북 지역의 장소를 선택해 주세요.")
- 만료: 배치 없이 조회 조건으로 처리한다. `실종` 상태이고 `EXPRY_CRTR_DT < NOW()`면 목록에서 빼고, 상세는 작성자만 본다(다른 회원 404). 7일·1일 전 알림은 이번에 하지 않는다
- 찾았어요 글: 작성자가 아니면 목록·상세에서 제목·상태·전환 시각만(나머지 null), 댓글 목록은 빈 목록, 댓글 등록은 400 `LOST_REPORT_CLOSED`
- 신고 누적으로 `PST_STTS_CD`가 정상이 아니면 실종 글도 목록·상세에서 뺀다
- 새 파일: `community/controller/LostReportController.java`, `community/service/LostReportService.java`, `community/mapper/LostReportMapper.java`, `mapper/community/LostReportMapper.xml`, `community/domain/LostReport.java`·`LostReportStatus.java`(CMUC0006 실종·0007 찾았어요·0008 만료)·`LostReportStatusHistory.java`, `community/dto/LostReportCreateRequest.java`·`LostReportResponse.java`·`LostReportSummary.java`·`LostReportSearchCond.java`
- `global/error/ErrorCode.java` 커뮤니티 구획에 `LOST_REPORT_NOT_FOUND`(404)·`LOST_REPORT_ALREADY_FOUND`(400)·`LOST_REPORT_NOT_RENEWABLE`(400)·`LOST_REPORT_CLOSED`(400)·`LOST_REPORT_REGION_NOT_ALLOWED`(400)·`LOST_REPORT_NOT_EDITABLE`(400)·`LOST_REPORT_NOT_RENEWABLE`(400)·`LOST_REPORT_CLOSED`(400), 기존 `LOST_REPORT_ALREADY_FOUND`는 409에서 400으로 바꿈, `BREED_NOT_FOUND`는 기존 404 그대로 씀
- FN-CMU-002-01·02·03·04 목록: `GET /api/lost-reports` → `ApiResponse<PageResponse<LostReportSummary>>`, 12건. 조건 `lostTpofSttsCd`(없으면 전체)·`rgnSn`(그 지역과 하위 지역 전부, `WITH RECURSIVE`)·`petSeCd`·`wtnsPrd`(`TODAY`·`WEEK`·`MONTH`·`OVER_MONTH`, `LAST_WTNS_DT` 기준)·`keyword`(지역명·품종명·직접입력 품종·특징 LIKE). 정렬: 실종 먼저·찾았어요 뒤, 그 안에서 `REG_DT DESC`. 초안 DTO에 대표 이미지가 없어 `LostReportSummary`에 `thumbAtchFileSn`(첫 사진, 찾았어요는 작성자만), `vrtyNm`, `petSeCd`, `regDt`를 더하고 같은 PR에서 초안을 고친다(카드에 이미지·품종·"2시간 전"이 나온다)
  - 완료 조건 화면(Postman): 실종 2건·찾았어요 1건 등록 뒤 `GET /api/lost-reports` → 실종 2건이 앞, 찾았어요가 뒤. `lostTpofSttsCd=CMUC0007`이면 1건. `petSeCd`·`wtnsPrd=TODAY`·`keyword=말티즈` AND로 걸러진다
- FN-CMU-002-05·06 상세: `GET /api/lost-reports/{lostTpofSn}` → `LostReportResponse`(초안 그대로 + 작성자 닉네임 `mbrNcnm`·`regDt`, 화면 "토리맘 · 등록일시"). 조회수는 펫이야기와 같은 1일 1회(`insertViewIgnore`). 연락처는 `FieldCryptoService`로 복호화해 준다(로그인 필수 경로라 따로 분기 없음). 좌표는 저장값 그대로(반경 표시는 프론트)
  - 완료 조건 화면: 1번이 등록 → 2번 상세 200(연락처 평문, `owner=false`), 1번 상세 `owner=true`
- FN-CMU-002-10·13 작성: `POST /api/lost-reports`(multipart, 펫이야기 `PostCreateRequest`와 같은 `images`) → `LostReportResponse`. 초안과 다른 점(같은 PR에서 초안 수정): `atchFileSns` → `images: List<MultipartFile>`(펫이야기와 같은 방식, 최대 10장), `rgnSn` 삭제(서버 계산), 길이를 명세대로 `petNm` 20·`colrNm` 20·`vrtyDrctInptNm` 20·`chrcCn` 40·`pstTtl` 50·`pstCn` 2000, `cnpl`은 숫자·하이픈 9~13자, `lastWtnsDt`는 미래 불가. `vrtySn`·`vrtyDrctInptNm` 중 하나만 필수, `vrtySn`은 `petSeCd`의 품종이어야 한다(`BreedService`, 아니면 400 `BREED_NOT_FOUND`). `prvcRlsAgreYn`가 true가 아니면 400. 처리: PST(`BBS_CD`=실종, 정상) → 상태 이력 → LOST_TPOF(실종, `EXPRY_CRTR_DT = NOW()+30일`, 연락처 암호화) → 실종 이력 → 이미지
  - 완료 조건 화면: 등록 → 응답 `lostTpofSttsCd=CMUC0006`, 목록 첫 건. `GET /api/posts`(펫이야기 목록)에 안 나오고 `GET /api/posts/{pstSn}`는 404
- FN-CMU-002-09 찾았어요·연장: `POST /api/lost-reports/{lostTpofSn}/found`, `POST /api/lost-reports/{lostTpofSn}/renew` → `ApiResponse<Void>`. 작성자만(403 `FORBIDDEN`). found: `UPDATE ... WHERE 상태 = 실종`이 0이면 400 `LOST_REPORT_ALREADY_FOUND`, `DSCV_SWTC_DT = NOW()`, 실종 이력, 자동 댓글(`SYS_AUTO_CRT_YN = 'Y'`, 작성자 = 글쓴이, 고정 문구 "🎉 여러분의 제보와 관심 덕분에 {펫이름}가 무사히 가족의 품으로 돌아갔어요."). 댓글 목록 정렬은 자동 댓글 스레드를 맨 앞으로(`CommentMapper.xml`). renew: `UPDATE SET EXPRY_CRTR_DT = EXPRY_CRTR_DT + 30일, PSTG_PRD_UPDT_NMTM + 1 WHERE 상태 = 실종 AND NOW() BETWEEN EXPRY_CRTR_DT - 7일 AND EXPRY_CRTR_DT`가 0이면 400 `LOST_REPORT_NOT_RENEWABLE`(연장 뒤 다시 누르면 7일 창 밖이라 한 번만 늘어난다)
  - 완료 조건 화면: 2번이 found 403 → 1번 found 200 → 댓글 목록 첫 건이 자동 댓글 → 2번 상세는 제목·상태·시각만, 2번 댓글 목록 빈 배열 → 1번 found 다시 400. renew는 DB에서 `EXPRY_CRTR_DT`를 5일 뒤로 바꾼 글로 200, 바로 다시 400
- 실종 글 수정·삭제(사람이 이번에 같이 하기로 함, 2026-10-29. 명세 기능 ID 없음, 화면설계서 SCR-CMU-002-03·06과 issues 18 근거):
  - 수정 `PATCH /api/lost-reports/{lostTpofSn}`(multipart) → `LostReportResponse`. 요청 `LostReportUpdateRequest`는 등록 요청과 같은 필드·같은 검증이고 `keepAtchFileSns`(남길 기존 사진, 펫이야기 `PostUpdateRequest`와 같은 방식)·`images`(새 사진)를 쓴다. 수정 범위는 전체(화면설계서)지만 상태·`EXPRY_CRTR_DT`·조회수·좋아요·댓글은 바꾸지 않는다. 좌표가 바뀌면 `RegionResolver`로 `RGN_SN`을 다시 계산하고 전북 밖이면 400 `LOST_REPORT_REGION_NOT_ALLOWED`, 안 바뀌면 기존 값 유지. `prvcRlsAgreYn`은 다시 true여야 한다(화면설계서 9번). 작성자만(403), 「실종」 상태만(찾았어요·만료 글은 400 `LOST_REPORT_NOT_EDITABLE`). `PST` 제목·내용은 `PostMapper.updatePost`를 쓰되 `bbsCd`는 null로 둬서 게시판을 바꾸지 않는다
  - 삭제 `DELETE /api/lost-reports/{lostTpofSn}` → `ApiResponse<Void>`. 작성자만(403). `PST_STTS_CD`를 정상 → 삭제(`CMUC0005`)로 바꾸고 상태 이력을 남긴다(`PostMapper.updateStatus`, 펫이야기 삭제와 같은 방식). 찾았어요·만료 글도 작성자는 삭제할 수 있다. 삭제된 글은 목록·상세·댓글·좋아요 모두 404
  - 에러코드: `LOST_REPORT_NOT_EDITABLE`(400)
  - 완료 조건 화면: 1번이 등록 → 2번 PATCH 403 → 1번 PATCH(제목·특징 변경, 사진 1장 교체) 200, 상세에 반영되고 상태·만료일 그대로 → 전북 밖 좌표 PATCH 400 → found 처리한 글 PATCH 400 → 1번 DELETE 200 → 목록에서 빠지고 상세·DELETE 다시 404
  - 문서(같은 PR): `docs/api/community.md` 표에 PATCH·DELETE 행 추가(기능 ID 칸은 `명세 없음(화면설계서 SCR-CMU-002-03·06)`), `docs/api/issues.md` 18번에 수정·삭제를 정했다고 덧붙임
- FN-CMU-002-07·08·12 댓글·좋아요·신고: SCR-CMU-001에서 만든 `/posts/{pstSn}/...` 그대로. 이번에는 찾았어요 비공개 처리(`CommentService.search`·`create`가 실종 글이면 `LostReportMapper`로 상태·작성자 확인)와 자동 댓글 정렬만 바꾼다
- FN-CMU-002-11 목격 장소 선택: 장소 검색 `GET /places`는 YAY 것. 이 저장소에서 할 것 없음(프론트가 YAY API 결과 좌표를 등록 요청에 넣는다). 전북 검사는 등록 시 위 `RegionResolver`
- FN-CMU-002-03 지역 콤보박스용 `GET /api/regions?upRgnSn=`(issues 14, 담당 없음): `global/code`에 `RegionController`·`RegionService`, `mapper/global/RegionMapper.xml`, 응답 `List<RegionSummary>`(`rgnSn`·`admdstCd`·`ctpvNm`·`sggNm`·`emdNm`·`rgnStpCd`), `upRgnSn` 없으면 시도 목록, `USE_YN = 'Y'`, `SORT_SEQ` 순. `docs/api/global.md`에 행 추가
  - 완료 조건 화면: `GET /api/regions` → 시도 17건, `upRgnSn=<전북 RGN_SN>` → 전북 시군구
- 품종 `GET /api/breeds?petSeCd=`(CMU-004·MYP-003과 공용, 초안 `docs/api/pet.md` 그대로): `pet/controller/BreedController.java`, `pet/service/BreedService.java`(`getBreeds`, `getBreedOrThrow(vrtySn, petSeCd)`), `pet/mapper/BreedMapper.java`, `mapper/pet/BreedMapper.xml`, `pet/domain/Breed.java`, `pet/dto/BreedSummary.java`. `USE_YN = 'Y'`, `SORT_SEQ, VRTY_SN` 순. `pet`은 SJM 담당 패키지라 새 파일만 더하고 기존 파일은 고치지 않는다
  - 완료 조건 화면: `GET /api/breeds?petSeCd=PETC0001` → 말티즈·푸들… 정렬순서대로
- 완료 조건 명령: `./gradlew build -q` 통과(위 테스트, `ArchitectureTest`, `MapperXmlRuleTest` 포함)
- 사람이 할 일: 병합 직후 공용 DB에 `db/migration/20261029_02_add_cmug01_lost.sql` 실행, PR 본문에 "DB 변경 있음". 공용 DB `RGN`에 전북 시군구·읍면동이 거의 없다(시드에 전북 2건). 지역 데이터 적재 담당을 정한다
- 프론트 연결: SCR-CMU-002 리스트·상세·작성자 상세·글쓰기·찾았어요/연장 모달·목격 장소 모달 (프론트 저장소에서 /start)
이번에 하지 않는 것: 만료 7일·1일 전 알림 배치, 로그인 회원 설정 지역 우선 정렬(회원 설정 지역 컬럼 확인 필요), 우측 레일 고정 문구, 카카오 역지오코딩 실제 연결
결정: 실종 글 수정·삭제를 이번에 같이 한다. 사람이 답함(2026-10-29)
새 의존성: 없음

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |

## 다음에 할 것
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
