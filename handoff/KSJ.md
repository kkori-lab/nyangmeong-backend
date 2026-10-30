# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-002 자주 묻는 질문(FAQ) — 끝남 (승인 2026-10-30 16:59, 시작 2026-10-30 16:34, 검증 통과 1회차, 사람 확인 2026-10-30)
기준: 고객센터 기능명세서 v0.6(2026-10-30)·화면설계서 v0.4(2026-10-25). `docs/api/support.md` `GET /faqs` 행, `FaqSummary`
결정: 제목의 「질문」은 FAQ 이름이라 질문이 아니다(추천안대로)
- 공통 파일: `support/domain/Faq.java`(FAQ VO), `support/domain/FaqCategory.java`(CSCG01 enum: `CSCC0001` 이용방법·`CSCC0002` 계정·`CSCC0003` 신고/제재·`CSCC0004` 기타, `exists()`는 `InquiryType`과 같은 모양), `support/mapper/FaqMapper.java`, `mapper/support/FaqMapper.xml`(`selectList`·`selectCount`), `support/service/FaqService.java`(`@Transactional(readOnly = true)`), `support/controller/FaqController.java`, `support/dto/FaqSummary.java`·`FaqSearchCond.java`. 테스트 `FaqServiceTest`·`FaqControllerTest`·`FaqMapperSqlTest`. 로그인 필요(공지와 같다). 조회 조건 `USE_YN = 'Y'`(이 테이블엔 `DEL_YN` 없음). 쓰기·이벤트 없음
- FN-CSC-002-01 목록 조회: `GET /api/faqs` → `PageResponse<FaqSummary>`(10건, `PageQuery` `sizeOr(10)`). 카테고리 배지 `faqClsfCdNm`은 `LEFT JOIN COM_CD C ON C.COM_CD = F.FAQ_CLSF_CD AND C.COM_CD_PRNT_SN IS NOT NULL`. 정렬 `SORT_SEQ ASC, FAQ_SN ASC`(명세에 정렬 기준이 없어 관리자가 정하는 정렬 순서 컬럼을 따른다). 완료 조건 명령: `./gradlew build -q`, `FaqControllerTest` 목록 200·`FaqSummary` 다섯 필드·페이지 바인딩. 화면: Postman `GET /api/faqs` → 10건·total·`faqClsfCdNm` 채워짐
  - "최대 10페이지"는 공지와 같이 페이지 번호 10개 묶음(FN-CSC-002-05)으로 보고 서버에서 자르지 않는다
  - FN-CSC-002-04 답변 아코디언은 목록 응답의 `ansCn`으로 끝난다(별도 API 없음, 마일스톤 기능 목록에 없는 이유)
- FN-CSC-002-02 분류 필터: 쿼리 `faqClsfCd`(`FaqSearchCond`, 없거나 공백이면 전체) → `F.FAQ_CLSF_CD = #{faqClsfCd}`(인덱스 `IDX_FAQ_1`). CSCG01 하위 코드가 아니면 400 `INVALID_INPUT_VALUE`(`FaqCategory.exists()`). 완료 조건 명령: `FaqServiceTest`에 없는 코드 → 400, 공백 코드 → 조건 없음. 화면: `?faqClsfCd=CSCC0002` → 계정만, `?faqClsfCd=XXXX` → 400
- FN-CSC-002-03 키워드 검색: 쿼리 `keyword`(앞뒤 공백 제거, 빈 값은 조건 없음) → `(F.QSTN_TTL LIKE CONCAT('%', #{keyword}, '%') OR F.ANS_CN LIKE CONCAT('%', #{keyword}, '%'))`. 분류와 AND. 완료 조건 명령: `FaqMapperSqlTest`에 분류·키워드를 함께 주면 `FAQ_CLSF_CD = ?`와 괄호로 묶인 OR이 AND로 붙는 케이스, `FaqServiceTest`에 공백 keyword → null. 화면: `?faqClsfCd=CSCC0002&keyword=비밀번호` → 계정 FAQ 중 질문·답변에 "비밀번호"가 든 것만, 답변에만 있는 단어도 걸림, 0건이면 빈 목록·total 0
  결정: 추천안대로
- FN-CSC-002-05 페이지네이션: `page`·`size`(PageQuery), `selectCount`는 목록과 같은 조건. 완료 조건 화면: `?page=2` → 11번째부터
- `docs/api/support.md`: 경로·DTO는 초안 그대로. 구현 전에 `GET /faqs` 행 설명에 쿼리 이름(`faqClsfCd`·`keyword`·`page`·`size`)을 적는다
- 프론트 연결: SCR-CSC-002 FAQ 리스트(카테고리 칩 → `faqClsfCd`, 검색, 배지, 아코디언 Q/A, 0건 「찾는 질문이 없어요」·[전체 질문 보기], 페이지네이션) (프론트 저장소에서 /start)
  결정: 추천안대로. 화면 변경(사람 2026-10-30): 목록 항목 안의 카테고리 칩은 없애고 카테고리 탭으로 바꿨다. 필터는 그대로 `faqClsfCd`를 쓰고 응답 DTO는 초안 그대로 둔다(`faqClsfCdNm`은 프론트가 안 써도 된다)
이번에 하지 않는 것: 관리자 FAQ 작성(명세에 없음), 비로그인 열람(공지와 같은 이유)
새 의존성: 없음

## 다음에 할 것
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
