# KSJ 핸드오프

## 지금 하는 것

SCR-CSC-003 1:1 문의 · 삭제 API — 끝남 (승인 2026-10-29 13:29, 시작 2026-10-29 13:26, 검증 통과, 사람 확인 2026-10-29)
기준: 프론트 요청(2026-10-29, "본인 문의만, 미답변만, 숨김 컬럼이나 상태로 처리"), AGENTS.md 5번(삭제는 `DEL_YN = 'Y'` UPDATE), `OTO_INQRY`(db/20261029_create_tables_v0.10.sql, 지금 `DEL_YN` 없음), db/migration/README.md, docs/convention/shared-files.md. 명세 기능 ID 추가 없음(걸치는 기능 FN-CSC-003-01·12·15)
방식: `OTO_INQRY`에 `DEL_YN`을 추가해 다른 테이블과 같은 방식으로 지운다. 상태 코드(CSCG03)에 "삭제"를 넣는 안은 목록 필터·답변 경합 조건이 다 바뀌어 쓰지 않는다
- 삭제 API: `DELETE /api/inquiries/{otoInqrySn}` → 200 `ApiResponse<Void>` (`DELETE /posts/{pstSn}`과 같은 형식)
  - DB: 기준 파일 `db/20261029_create_tables_v0.10.sql`의 `OTO_INQRY`에 `DEL_YN char(1) NOT NULL DEFAULT 'N'`과 `CK_OTO_INQRY_DEL_YN` CHECK를 직접 넣는다(추가-1로 마이그레이션 파일 대신 바꿈). 이미 만들어진 DB에는 같은 컬럼을 사람이 ALTER로 한 번 더한다
  - 바꿀 파일: `support/domain/Inquiry.java`에 `delYn`. `mapper/support/InquiryMapper.xml`: `selectList`·`selectInquiry`에 `DEL_YN = 'N'`, `updateInquiry`·`updateAnswer` WHERE에 `AND DEL_YN = 'N'`, 새 `updateInquiryDeleted(otoInqrySn, receivedCd)`(`SET DEL_YN = 'Y', MDFCN_DT = NOW() WHERE OTO_INQRY_SN = ? AND INQRY_PRCS_STTS_CD = 접수 AND DEL_YN = 'N'`), `inquiryResultMap`에 `DEL_YN`. `support/mapper/InquiryMapper.java`에 메서드. `support/service/InquiryService.java`에 `delete(mbrSn, otoInqrySn)`(`@Transactional`, `getOwnedOrThrow` → 접수 상태가 아니면 `INQUIRY_NOT_DELETABLE` → 캐릭터 오류 문의(`pixelCharSn != null`)면 `INQUIRY_NOT_DELETABLE` → `updateInquiryDeleted`가 0이면 `INQUIRY_NOT_DELETABLE` → 남은 첨부를 `updatePhotosDeleted`로 `DEL_YN = 'Y'`). `support/controller/InquiryController.java`에 `delete`
  - `global/error/ErrorCode.java` 문의 구획에 `INQUIRY_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "접수 상태의 문의만 삭제할 수 있습니다.")`
  - `src/test/resources/del-yn-tables.txt`에 `OTO_INQRY` 추가(`MapperXmlRuleTest`가 이 목록으로 `DEL_YN = 'N'` 조건을 검사한다)
  - 오류: 없는 문의·이미 삭제한 문의 404 `INQUIRY_NOT_FOUND`, 남의 문의 403 `FORBIDDEN`, 답변 완료·캐릭터 오류 문의 400 `INQUIRY_NOT_DELETABLE`. 삭제된 문의는 상세·수정·첨부 이미지 조회도 404
  - 질문: 캐릭터 오류 문의(픽셀 캐릭터 오류로 접수한 문의) 삭제를 어떻게 할까요? 추천: 막는다(400). 이유: 접수할 때 캐릭터 생성 작업에 문의 번호가 연결되고 기본 캐릭터로 바뀌어(`receiveErrorInquiry`) 지우면 관리자가 처리할 길이 없다. 허용하려면 pet에 접수 취소 기능이 따로 필요하다(SJM·LJS 쪽 코드)
    결정: 캐릭터 오류 문의 삭제는 막는다(400 `INQUIRY_NOT_DELETABLE`). 사람이 답함(2026-10-29)
  - 문서(같은 PR): `docs/api/support.md` 표에 `| DELETE | /inquiries/{otoInqrySn} | 1:1 문의 삭제(본인, 접수 상태만, 캐릭터 오류 문의 제외, DEL_YN) | 명세 없음(프론트 요청) | KSJ | - | - |`
  - 테스트: `InquiryServiceTest`에 본인 접수 문의 삭제 → `updateInquiryDeleted`·`updatePhotosDeleted` 호출, 남의 문의 403, 없는 문의 404, 답변 완료 400, 캐릭터 오류 문의 400, `updateInquiryDeleted`가 0(답변과 경합)이면 400. `InquiryControllerTest`에 DELETE 200
  - 완료 조건 명령: `./gradlew build -q` 통과(위 테스트와 `MapperXmlRuleTest` 포함)
  - 완료 조건 화면(Postman, dev-token 1번·2번 회원, 로컬 DB에 마이그레이션 실행 뒤): 1번이 문의 작성 → 2번이 DELETE 403 → 1번이 DELETE 200 → 1번 목록에서 빠지고 상세 404 → 같은 DELETE 다시 404 → 답변 완료 문의 DELETE 400
  - 사람이 할 일: 병합 직후 공용 DB에 `ALTER TABLE OTO_INQRY ADD COLUMN DEL_YN char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부' AFTER ANS_DT, ADD CONSTRAINT CK_OTO_INQRY_DEL_YN CHECK (DEL_YN IN ('Y','N'))` 실행, PR 본문에 "DB 변경 있음"과 파일 이름, ERD에 `OTO_INQRY.DEL_YN` 추가
  - 프론트 연결: SCR-CSC-003 문의 상세의 삭제 버튼(미답변일 때만)과 삭제 뒤 목록 이동 (프론트 저장소에서 /start)
이번에 하지 않는 것: 실제 파일(디스크) 삭제, 관리자 화면에서 삭제된 문의 보기, 캐릭터 오류 문의 접수 취소
새 의존성: 없음

### 추가 요청
| 번호 | 바꿀 파일 | 완료 조건 | 요청 |
| --- | --- | --- | --- |
| 추가-1 | `db/20261029_create_tables_v0.10.sql`, `db/migration/20261029_01_add_oto_inqry_del_yn.sql`(삭제) | 명령: `./gradlew build -q` 통과. 화면: 새 DB를 기준 파일로 만들면 `OTO_INQRY.DEL_YN`이 있다 | 요청 2026-10-29 "마이그레이션 파일은 따로 안 만들고 create_table 파일에 포함시키기로 했다" |

## 다음에 할 것
- 연결 남음: SCR-CSC-003 1:1 문의 삭제 (프론트 연결 문의 상세의 삭제 버튼(미답변일 때만)과 삭제 뒤 목록 이동, 프론트 저장소에서 /start)
- 사람이 할 일: 삭제 API PR 병합 직후 공용 DB에 `OTO_INQRY.DEL_YN` ALTER 실행(이미 실행했으면 PR에 완료 댓글만), ERD에 `OTO_INQRY.DEL_YN` 추가. 기준 파일 `db/20261029_create_tables_v0.10.sql`에 직접 반영했으므로 PR 본문에 "DB 변경 있음"을 적는다
- 사람이 할 일: 디스코드에 공유한 `application.properties`와 팀원 각자 설정의 `spring.servlet.multipart.max-request-size`를 100MB로 바꾼다(배포 서버 설정이 있으면 같이)
- 확인 남음: 펫이야기 글 사진 중 `PostService`가 `storePublic`으로 바꾸기 전에 `store`로 올린 것(`public/` 아님)은 `GET /api/files/public/{atchFileSn}`가 404일 수 있다
- 정리: 검증·확인용으로 만든 문의 223·225·226·227·229·203(203은 삭제 확인 중 DEL_YN='Y'로 이미 지움, 공용 DB, 첨부 파일 4082~4085·4088~4097 포함)와 `uploads/`의 테스트 이미지. 문의 삭제 API가 없어 직접 정한다
- 연결 남음: FN-CMU-001-14·09·11 작성·좋아요·댓글 뒤 케어미션·포인트 반영 (`pet` service에 메서드가 생기면 붙인다, SJM)
- 연결 남음: FN-CMU-001-01~11 비로그인 열람 (`SecurityPathProperties`가 메서드를 구분하지 않아 `GET /api/posts/**`만 열 수 없음, 담당 미정)
- 연결 남음: FN-CMU-001-05 오늘의 이야기 자정 배치, FN-CMU-001-07 건강팁 관리자 선정 (관리자·배치 마일스톤, 담당 미정). FN-CMU-001-12 공유하기는 화면만
- 연결 남음: FN-CSC-003-14 관리자 답변 등록 API가 `InquiryService.answer()`를 부르도록 연결 (관리자 화면, 담당 미정)
