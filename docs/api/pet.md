# 반려동물(pet) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /admin/members/{mbrSn}/pets | 관리자 회원 상세 반려동물. 연동 중인 관계만, 삭제한 동물 포함, 20건씩. 건강 정보 없음. 일반 회원 403 ADMIN_ONLY, 없는 회원 404 MEMBER_NOT_FOUND | ADM-011-02 | LJS | - | PageResponse\<AdminPetSummary\> |
| GET | /admin/members/{mbrSn}/point-ledgers | 관리자 회원 상세 포인트 원장. 최신순 20건씩. 일반 회원 403 ADMIN_ONLY, 없는 회원 404 MEMBER_NOT_FOUND | ADM-011-04 | LJS | - | PageResponse\<AdminPointLedgerSummary\> |
| GET | /admin/points | 관리자 포인트 원장 전체. 최신순 20건씩. 쿼리는 모두 선택: `mbrSn`, `mbrNcnm`(닉네임 부분 일치), `pntDlngTypeCd`(MYPG03), `fromDt`·`toDt`(YYYY-MM-DD, 양 끝 날짜 포함). 모르는 거래 유형 코드·`fromDt`가 `toDt`보다 늦음·날짜 형식 틀림 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-072-01 | LJS | - | PageResponse\<AdminPointLedgerRow\> |
| POST | /admin/points/adjustments | 포인트 수동 조정(201, 감사 로그). 거래 유형 MYPC0009 관리자 조정, 사유는 원장 메모(회원 포인트 내역에도 보임), 보유 포인트(`MBR.HLD_PNT`)를 같이 바꾼다. 조정 화면의 현재 잔액은 `GET /admin/members/{mbrSn}`의 `hldPnt`. 형식이 틀리면 400 INVALID_INPUT_VALUE, 차감 후 잔액이 0보다 작으면 400 POINT_NOT_ENOUGH(잔액·원장 그대로), 없는 회원·탈퇴 회원 404 MEMBER_NOT_FOUND. 같은 요청을 두 번 보내면 두 건이 된다 | ADM-072-02·03 | LJS | AdminPointAdjustRequest | AdminPointLedgerRow |
| GET | /admin/processing/character-jobs?fromYmd=&toYmd=&crtJobSttsCd=&page=&size=20 | 관리자 처리 현황 캐릭터 생성 탭. 기간은 `REG_DT` 날짜 기준 양끝 포함(둘 다 선택). 상태별 건수 `sttsCnts`(CHRG01 네 상태를 순서대로 늘 주고 없는 상태는 0, 기간만 따르고 상태 필터와 무관)와 기간·상태를 건 목록(`REG_DT` 최신순, 같으면 `CHAR_CRT_JOB_SN` 내림차순). 결과 이미지·원본 사진은 주지 않는다(결정 26). 연결된 1:1 문의는 `GET /admin/inquiries/{otoInqrySn}`로 연다. 일반 회원 403 ADMIN_ONLY. 모르는 상태 코드·날짜 형식 오류·`fromYmd > toYmd` 400 INVALID_INPUT_VALUE | ADM-082-02 | LJS | AdminCharacterJobSearchCond | AdminCharacterJobListResponse |
| GET | /admin/breeds | 관리자 품종 목록. 사용 안 함 포함, 종·정렬 순서·번호순, 페이지 없음. 쿼리 `petSeCd`(PETG01) 없거나 공백이면 전체. 모르는 종 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-101-01 | LJS | - | List<AdminBreedSummary> |
| POST | /admin/breeds | 품종 추가(201, 감사 로그). 품종 코드는 서버가 매긴다(강아지 `DOGV`·고양이 `CATV` + 그 접두 최대 번호 + 1, 네 자리). 이름 1~50자(앞뒤 공백 지운 뒤)가 아니거나 종 코드가 틀리면 400 INVALID_INPUT_VALUE, 같은 종에 같은 이름(공백 무시, 사용 안 함 포함) 409 DUPLICATE_VRTY_NM | ADM-101-01 | LJS | AdminBreedCreateRequest | AdminBreedSummary |
| PATCH | /admin/breeds/{vrtySn} | 품종 수정(보낸 칸만, 종·코드는 안 바꿈, 감사 로그). 사용을 끄면 공용 `GET /breeds`와 새 반려동물·실종제보·펫돌봄 등록 선택에서 빠지고, 이미 등록된 반려동물의 품종 이름은 그대로 보인다. 아무 칸도 없거나 형식이 틀리면 400 INVALID_INPUT_VALUE, 없는 품종 404 BREED_NOT_FOUND, 자기 말고 같은 종 같은 이름 409 DUPLICATE_VRTY_NM | ADM-101-01 | LJS | AdminBreedUpdateRequest | AdminBreedSummary |
| GET | /admin/care-missions | 관리자 케어미션 목록. 사용 안 함 포함, 번호순, 페이지 없음. 쿼리 `msnPoolCd`(MYPG04: MYPC0010 일상케어·MYPC0011 활동기반) 없거나 공백이면 전체. 모르는 풀 코드 400 INVALID_INPUT_VALUE, 일반 회원 403 ADMIN_ONLY | ADM-071-01 | LJS | - | List<AdminCareMissionSummary> |
| PATCH | /admin/care-missions/{careMsnSn} | 케어미션 수정(보상 포인트·달성 기준 횟수·사용 여부 중 보낸 칸만, 미션명·풀·이동 경로는 안 바꿈, 감사 로그). 추가·삭제 API 없음(미션은 코드의 활동과 짝). 바꾼 값은 다음 날 00:00 출제부터 반영하고, 이미 출제된 미션은 출제 때 복사한 배점(`DAY_MSN.SCOR_PNT`)·달성 기준(`DAY_MSN.ACHV_CRTR_NMTM`)으로 진행·정산한다. 사용을 꺼도 이미 출제된 미션은 진행된다. 아무 칸도 없거나 범위 밖이면 400 INVALID_INPUT_VALUE, 없는 미션 404 CARE_MISSION_NOT_FOUND | ADM-071-02·03 | LJS | AdminCareMissionUpdateRequest | AdminCareMissionSummary |
| GET | /breeds | 반려동물 구분별 품종 마스터 목록(`?petSeCd=`) | CMU-002-13, CMU-004-06·13, MYP-003-02 (기능 분배 3.1절 10번) | KSJ | - | List<BreedSummary> |
| GET | /caregiver-invitations | 내가 보낸 공동양육 초대 목록(계정 전체) | MYP-005-03 | SJM | - | List<CaregiverInvitationSummary> |
| GET | /caregiver-invitations/{jntBrupInvtnSn} | 받은 초대 상세. 로그인 회원 누구나. 대기중이고 로그인 이메일이 초대 이메일과 같을 때만 초대한 사람·대상 반려동물 카드, 아니면 상태와 가린 이메일만(만료·처리 여부를 이메일보다 먼저 판단) | MYP-005-05·09·10 | SJM | - | CaregiverInvitationResponse |
| GET | /items/{itemSn}/image | 아이템 이미지(로그인 회원, 판매 중 아이템) | MYP-010-14, MYP-011-02 | LJS | - | 이미지 파일 |
| GET | /members/me/points | 보유 포인트, 이번 달 적립·사용 합계와 건수, 오늘 미션 요약(주양육 반려동물 전체) | MYP-001-03·04, MYP-011-05, MYP-020-01·09 | SJM | - | PointResponse |
| GET | /pets | 내 반려동물 목록(주양육·공동양육, 역할·대표 여부·공동양육 인원 포함) | MYP-001-01, MYP-002-01~03, MYP-005-02, MYP-006-04, MYP-020-03 | SJM | - | List<PetSummary> |
| GET | /pets/main-shortcuts | 메인 반려동물별 바로가기. 양육 중인 반려동물(연동 중, 삭제 안 함) 전부를 양육관계 순서로, 코드(MANG02)는 보일 순서대로. 저장한 적 없거나 전부 숨겼으면 `[]`. 사용 안 하는 코드는 빠짐 | MAN-008 | LJS | - | List<PetShortcutSummary> |
| GET | /pets/{petSn} | 반려동물 프로필 상세(수정 모달 채우기, 양육자 목록). 체중 잠금은 화면이 건강기록 API로 판단(`issues.md` API의 마이페이지 2번) | MYP-002-04, MYP-003-01 | SJM | - | PetResponse |
| GET | /pets/{petSn}/caregivers | 공동양육자 목록. 주양육자가 첫 줄. 주양육자에게는 전원, 공동양육자에게는 주양육자와 본인 | MYP-004-01·03·04, MYP-006-04 | SJM | - | List<CaregiverSummary> |
| GET | /pets/{petSn}/character-image | 캐릭터 이미지 `?part=full`(전신, 기본)·`face`(얼굴). 미생성·오류 확인 중이면 종별 기본 캐릭터. 착용 모습 대표 이미지가 있으면 그것, 없으면 맨몸 결과. `face`는 (96, 32)부터 320×320을 256×256으로 줄인 PNG | MYP-001-01, MYP-002-02, MYP-010-12·23 | LJS | - | 이미지 파일 |
| GET | /pets/{petSn}/daily-missions | 그날의 케어미션 8개와 진행 상태. 쿼리 `msnCrtrYmd` | MYP-020-03·04 | SJM | - | List<DailyMissionSummary> |
| GET | /pixel-characters | 내 캐릭터·공동양육 캐릭터 목록(캐릭터 없는 반려동물 포함, 생성 상태·오류 확인 중 표시) | MYP-010-02·03·07·12·23 | LJS | - | List<PixelCharacterSummary> |
| GET | /pixel-characters/{pixelCharSn} | 꾸미기 상세. 결과 이미지·보유/착용 아이템·오류 문의 접수 여부 | MYP-010-10·13·14·23 | LJS | - | PixelCharacterResponse |
| GET | /pixel-characters/{pixelCharSn}/shop-items | 상점 아이템 목록(판매 중, 사용 안 하는 부위(신발 MYPC0006) 제외, 이 캐릭터 기준 보유중 표시, 보유 포인트). `?itemRegnCd=` | MYP-011-02·03 | LJS | - | ShopItemListResponse |
| GET | /point-ledgers | 선택한 월의 포인트 적립·사용 내역(최신순). 쿼리 `yearMonth` | MYP-020-02 | SJM | - | List<PointLedgerSummary> |
| POST | /caregiver-invitations | 이메일로 공동양육 초대 발송. 대상 반려동물 여러 마리를 한 초대로 묶음. 가입 회원은 인앱 알림, 미가입은 메일. 고른 반려동물을 모두 검사해 충돌을 모아 409 오류 data에 CaregiverInvitationConflictResponse. 이미 가족이 하나라도 있으면 `CAREGIVER_ALREADY_LINKED`, 대기 초대만 있으면 `CAREGIVER_INVITATION_DUPLICATED`. 충돌이 있으면 초대를 만들지 않음 | MYP-005-01·02 | SJM | CaregiverInvitationCreateRequest | CaregiverInvitationResponse |
| POST | /caregiver-invitations/{jntBrupInvtnSn}/accept | 초대 수락. 고른 반려동물만 연동하고 나머지는 반려동물별 거절 | MYP-005-05 | SJM | CaregiverInvitationAcceptRequest | - |
| POST | /caregiver-invitations/{jntBrupInvtnSn}/reject | 초대 거절. 초대의 반려동물 전체 | MYP-005-07·08 | SJM | - | - |
| POST | /pets | 반려동물 등록(계정당 10마리, 첫 등록은 대표 지정) | MYP-002-01, MYP-003-01·04·05·07 | SJM | PetCreateRequest | PetResponse |
| POST | /pets/{petSn}/representative | 대표 반려동물 지정(주양육 반려동물만) | MYP-010-05 | SJM | - | - |
| POST | /pixel-characters/{pixelCharSn}/held-items | 아이템 일괄 구매(포인트 차감, 보관함 추가, 주양육자만). 201. 오류: `POINT_NOT_ENOUGH`(400), `PET_PRIMARY_ONLY`(403), `ITEM_NOT_FOUND`(404), `ITEM_ALREADY_HELD`(409) | MYP-011-05·06 | LJS | ItemPurchaseRequest | PixelCharacterResponse |
| PUT | /pixel-characters/{pixelCharSn}/equipped-items | 착용 상태 확정 저장(부위당 1개, 요청에 없는 보유 아이템은 착용 해제). 착용 모습 대표 이미지를 지운다 | MYP-010-13·16 | LJS | EquippedItemSaveRequest | PixelCharacterResponse |
| PUT | /pets/main-shortcuts | 메인 바로가기 저장. 본문은 배열 `[{ petSn, shrtcCds }]`, 반려동물마다 보일 코드를 순서대로(`[]`면 전부 숨김). 본문에 없는 반려동물은 그대로. 400 `INVALID_INPUT_VALUE`(`data[].field`가 `[i].petSn`·`[i].shrtcCds`·`[i].shrtcCds[j]`): 반려동물 번호 없음·중복, 코드 목록 없음, 같은 코드 두 번, MANG02에 없거나 사용 안 하는 코드. 남의 반려동물 403 `PET_ACCESS_DENIED`, 없는 번호 404 `PET_NOT_FOUND`(하나라도 틀리면 아무것도 저장 안 함). 응답은 조회와 같음 | MAN-008 | LJS | List<PetShortcutSaveRequest> | List<PetShortcutSummary> |
| PATCH | /pets/{petSn} | 반려동물 수정(양육자 전원, 체중 잠금은 화면에서) | MYP-002-04, MYP-003-01·04~07 | SJM | PetUpdateRequest | PetResponse |
| DELETE | /pets/{petSn} | 반려동물 삭제(주양육자만). 캐릭터·보유 아이템 삭제, 공동양육 연동 해제, 대표 승계 | MYP-002-07·08 | SJM | - | - |
| DELETE | /pets/{petSn}/caregivers/{brupRelSn} | 공동양육 연동 해제. 주양육자의 강제 해제와 공동양육자의 자진 해제 | MYP-004-01, MYP-006-02 | SJM | - | - |
| GET | /admin/items?itemRegnCd= | 관리자 아이템 목록(판매 중지 포함, 부위·정렬 순서·번호순, 페이지 없음). 부위 코드가 MYPG01이 아니거나 신발(MYPC0006, 사용 안 함)이면 400 `INVALID_INPUT_VALUE`. 전체 목록에서도 사용 안 하는 부위(`COM_CD.USE_YN='N'`) 아이템은 뺀다 | ADM-070-01 | LJS | - | List<AdminItemSummary> |
| GET | /admin/items/{itemSn}/image | 관리자 아이템 이미지(판매 중지도, ApiResponse 아님). 없으면 404 `ITEM_NOT_FOUND` | ADM-070-01 | LJS | - | 이미지 파일 |
| POST | /admin/items | 아이템 등록(`multipart/form-data`). 정렬은 그 부위 마지막 다음. 201. 오류: `INVALID_INPUT_VALUE`(400, 신발 MYPC0006은 사용 안 하는 부위), `FILE_TYPE_NOT_ALLOWED`·`FILE_TOO_LARGE`(400), `ITEM_NAME_DUPLICATED`(409) | ADM-070-02 | LJS | AdminItemCreateRequest | AdminItemCreateResponse |
| PATCH | /admin/items/{itemSn} | 아이템 수정·판매 중지(`multipart/form-data`, 보낸 칸만). 판매 중지해도 이미 산 회원의 보유는 그대로. 부위를 바꾸면 새 부위 마지막 다음 순서. 굽기 지시가 바뀌면 그 아이템의 착용 시트를 지워 다시 굽기 대상이 된다. 오류: `ITEM_NOT_FOUND`(404), `ITEM_REGION_LOCKED`(409, 보유 회원이 있는데 부위 변경), `ITEM_NAME_DUPLICATED`(409) | ADM-070-03 | LJS | AdminItemUpdateRequest | - |
| PUT | /admin/items/order | 부위 안 상점 순서 저장(1부터 다시 매김). 그 부위 아이템 전부(판매 중지 포함)를 빠짐·중복 없이 보내야 하고 아니면 400 `INVALID_INPUT_VALUE` | ADM-070-04 | LJS | AdminItemOrderRequest | - |

## 내부 API

Python AI 서버가 사용자 쿠키를 그대로 넘겨 부른다. 경로에 `/api`가 없고 nginx 밖에서만 부른다. 캐릭터 생성의 공개 입구는 Python `POST /ai/characters`다. 계약 정본은 ai 저장소 `docs/api/internal.md` "Python → Java"다.

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| POST | /internal/ai/characters/jobs | 생성 작업을 만든다. 주양육자만, 201. 이미 있음 `PIXEL_CHARACTER_ALREADY_EXISTS`, 생성 중 `PIXEL_CHARACTER_CREATING`(409). 실패로 끝나 결과가 없으면 다시 요청할 수 있다 | MYP-010-08·09 | LJS | CharacterJobCreateRequest | CharacterJobResponse |
| POST | /internal/ai/characters/jobs/{charCrtJobSn}/success | 생성 성공 보고. 작업을 만든 회원만. 이미 완료면 같은 번호, 실패로 끝났으면 409 `ALREADY_PROCESSED`, 규격 밖 400 `FILE_TYPE_NOT_ALLOWED` | MYP-010-09·17 | LJS | CharacterSuccessRequest | CharacterSuccessResponse |
| POST | /internal/ai/characters/jobs/{charCrtJobSn}/failure | 생성 실패 보고. 작업을 만든 회원만. 끝난 작업이면 바꾸지 않고 200. 시작 뒤 10분이 넘은 작업은 `CharacterJobTimeoutJob`이 `CHARACTER_TIMEOUT`으로 실패 처리한다 | MYP-010-12 | LJS | CharacterFailureRequest | - |
| GET | /internal/ai/characters/bake-items | 굽기 대상(판매 중이고 굽기 지시 `BAKE_HINT`가 있거나 그림 참고 굽기 `BAKE_IMG_REF_YN`='Y', 신발은 사용 안 하는 부위). `bakeImgRefYn`이 true면 `bake-image`로 그림을 받아 참고로 굽는다 | MYP-011-01, ADM-070-02 | LJS | - | List<BakeItemSummary> |
| GET | /internal/ai/items/{itemSn}/bake-targets?page= | 그 아이템을 아직 안 구운 캐릭터(맨몸 시트가 있고 그 아이템의 착용 시트·굽기 실패 표시가 없음). 관리자 쿠키만, 아니면 403 `ADMIN_ONLY`. `page`는 0부터(기본 0), 한 페이지 100개, 캐릭터 번호순. 굽기 대상 아이템이 아니면 404 `ITEM_NOT_FOUND`, `page` 음수 400 | ADM-070-02 | LJS | - | BakeTargetPageResponse |
| GET | /internal/ai/items/{itemSn}/bake-image | 굽기 참고용 아이템 그림(파일 자체 응답, `Content-Type`은 저장된 형식 jpg·png·webp). 회원·관리자 쿠키 모두. 굽기 대상 아이템이 아니면 404 `ITEM_NOT_FOUND`, 파일이 없으면 404 `FILE_NOT_FOUND` | MYP-011-01, ADM-070-02 | LJS | - | 파일 |
| PUT | /internal/ai/characters/{pixelCharSn}/item-sheets/{itemSn} | 착용 시트 저장(주양육자만, 관리자 쿠키면 남의 캐릭터도(전체 굽기 FN-ADM-070-02), 다시 받으면 교체). 굽기 대상이 아니면 404 `ITEM_NOT_FOUND`, 1024 PNG가 아니면 400 | MYP-011-01 | LJS | ItemSheetSaveRequest | - |
| POST | /internal/ai/characters/{pixelCharSn}/item-sheets/{itemSn}/failure | 굽기 실패 표시(주양육자만, 관리자 쿠키면 남의 캐릭터도(전체 굽기 FN-ADM-070-02)). 구운 시트가 있으면 유지 | MYP-011-01 | LJS | - | - |
| GET | /internal/ai/characters/{pixelCharSn}/sheets?itemSns= | 합성용 시트 정보(주양육자만, 관리자 쿠키면 남의 캐릭터도(전체 굽기 FN-ADM-070-02), 안 산 아이템도 조회). 맨몸 시트가 없으면 404 `PIXEL_CHARACTER_NOT_FOUND` | MYP-010-13, MYP-011-01·06 | LJS | - | CharacterSheetsResponse |
| GET | /internal/ai/characters/{pixelCharSn}/sheet-files/{atchFileSn} | 시트 파일 내려받기(주양육자만, 관리자 쿠키면 남의 캐릭터도(전체 굽기 FN-ADM-070-02), `image/png`, ApiResponse 아님). 그 캐릭터의 시트가 아니면 404 `FILE_NOT_FOUND` | MYP-010-13, MYP-011-01 | LJS | - | 이미지 파일 |
| PUT | /internal/ai/characters/{pixelCharSn}/equipped-image | 착용 모습 대표 이미지 저장(주양육자만). Python이 `PUT equipped-items` 뒤 합성해 보낸다. `itemSns`가 지금 착용 아이템과 같을 때만 저장하고 다르면 409 `EQUIPPED_ITEMS_CHANGED`(늦게 끝난 옛 합성 차단), 512×512 PNG가 아니면 400 `FILE_TYPE_NOT_ALLOWED`. 착용을 바꾸거나 캐릭터를 다시 만들면 지운다 | MYP-010-16, MYP-010-13 | LJS | EquippedImageSaveRequest | - |

### CharacterJobCreateRequest
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |

### CharacterJobResponse
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| charCrtJobSn | Long | Y | | CHAR_CRT_JOB.CHAR_CRT_JOB_SN |
| petSeCd | String | Y | | PET.PET_SE_CD |
| vrtyNm | String | N | | VRTY.VRTY_NM 또는 직접 입력 |
| colrNm | String | N | | 공통코드 또는 직접 입력 |
| chrcCn | String | N | | PET.CHRC_CN |

### CharacterSuccessRequest
`multipart/form-data`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| image | MultipartFile | Y | 512×512 | CHAR_PHOTO (결과) |
| sheet | MultipartFile | Y | 맨몸 2×2 시트, 1024 PNG | PIXEL_CHAR.BODY_SHEET_FILE_SN |
| scale | Double | Y | | PIXEL_CHAR.SHEET_SCALE |
| originX | Double | Y | | PIXEL_CHAR.SHEET_ORIGIN_X |
| originY | Double | Y | | PIXEL_CHAR.SHEET_ORIGIN_Y |

### CharacterSuccessResponse
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pixelCharSn | Long | Y | | PIXEL_CHAR.PIXEL_CHAR_SN |

### CharacterFailureRequest
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| failRsn | String | Y | `CHARACTER_API_ERROR`·`CHARACTER_SPEC_MISMATCH` | CHAR_CRT_JOB.FAIL_RSN |

### BakeItemSummary
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemSn | Long | Y | | ITEM.ITEM_SN |
| regnCd | String | Y | 부위 MYPG01 | ITEM.ITEM_REGN_CD |
| bakeHint | String | N | 영어 한 문장 굽기 지시. 그림 참고 아이템은 null일 수 있다 | ITEM.BAKE_HINT |
| bakeImgRefYn | boolean | Y | true면 아이템 그림을 참고로 굽는다 | ITEM.BAKE_IMG_REF_YN |
| atchFileSn | Long | Y | 아이템 그림 파일 번호(그림이 바뀌면 번호가 바뀐다). 그림은 `/internal/ai/items/{itemSn}/bake-image` | ITEM.ATCH_FILE_SN |

### BakeTargetPageResponse
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pixelCharSns | List<Long> | Y | 없으면 빈 목록 | PIXEL_CHAR.PIXEL_CHAR_SN |
| last | boolean | Y | 마지막 페이지면 true | |

### ItemSheetSaveRequest
`multipart/form-data`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| sheet | MultipartFile | Y | 1024 PNG | CHAR_ITEM_SHEET.ATCH_FILE_SN |

### EquippedImageSaveRequest
`multipart/form-data`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| image | MultipartFile | Y | 512×512 PNG. 착용 아이템을 합성한 전신 | PIXEL_CHAR.WEAR_IMG_FILE_SN |
| itemSns | List\<Long\> | N | 합성에 쓴 아이템 번호(여러 번). 맨몸이면 없음. 지금 착용 아이템 집합과 같아야 저장 | HLD_ITEM.ITEM_SN |

### CharacterSheetsResponse
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | | PET.PET_SE_CD |
| sheetFileSn | Long | Y | 맨몸 시트 | PIXEL_CHAR.BODY_SHEET_FILE_SN |
| scale | Double | Y | | PIXEL_CHAR.SHEET_SCALE |
| originX | Double | Y | | PIXEL_CHAR.SHEET_ORIGIN_X |
| originY | Double | Y | | PIXEL_CHAR.SHEET_ORIGIN_Y |
| items | List<CharacterSheetItem> | Y | 요청한 아이템 순서. 없으면 빈 목록 | CHAR_ITEM_SHEET |
| items[].itemSn | Long | Y | | CHAR_ITEM_SHEET.ITEM_SN |
| items[].atchFileSn | Long | N | 안 구웠거나 실패면 null | CHAR_ITEM_SHEET.ATCH_FILE_SN |
| items[].bakeFailed | boolean | Y | | CHAR_ITEM_SHEET.BAKE_FAIL_YN |

## DTO

### AdminBreedSummary
쓰는 API: GET·POST /admin/breeds, PATCH /admin/breeds/{vrtySn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| vrtySn | Long | Y | | VRTY.VRTY_SN, ADM-101-01 |
| vrtyCd | String | Y | 서버가 매긴다 | VRTY.VRTY_CD, ADM-101-01 |
| vrtyNm | String | Y | | VRTY.VRTY_NM, ADM-101-01 |
| petSeCd | String | Y | 공통코드 PETG01 | VRTY.PET_SE_CD, ADM-101-01 |
| sortSeq | Integer | Y | 종 안 정렬 순서 | VRTY.SORT_SEQ, ADM-101-01 |
| useYn | boolean | Y | 사용 중이면 true | VRTY.USE_YN, ADM-101-01 |

### AdminBreedCreateRequest
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSeCd | String | Y | PETG01 코드(PETC0001 강아지·PETC0002 고양이) | VRTY.PET_SE_CD |
| vrtyNm | String | Y | 앞뒤 공백 지운 뒤 1~50자. 같은 종에 같은 이름(공백 무시) 409 | VRTY.VRTY_NM |
| sortSeq | Integer | N | 0 이상 정수. 없으면 그 종 최대 + 10 | VRTY.SORT_SEQ |
| useYn | Boolean | N | 없으면 true | VRTY.USE_YN |

### AdminBreedUpdateRequest
보낸 칸만 바뀐다. 종·코드는 바꾸지 않는다
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| vrtyNm | String | N | 앞뒤 공백 지운 뒤 1~50자 | VRTY.VRTY_NM |
| sortSeq | Integer | N | 0 이상 정수 | VRTY.SORT_SEQ |
| useYn | Boolean | N | false면 사용 안 함 | VRTY.USE_YN |

### AdminCareMissionSummary
쓰는 API: GET /admin/care-missions, PATCH /admin/care-missions/{careMsnSn}
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| careMsnSn | Long | Y | | CARE_MSN.CARE_MSN_SN, ADM-071-01 |
| msnPoolCd | String | Y | 공통코드 MYPG04 | CARE_MSN.MSN_POOL_CD, ADM-071-01 |
| msnNm | String | Y | | CARE_MSN.MSN_NM, ADM-071-01 |
| cmpnPnt | Integer | Y | 보상 포인트 | CARE_MSN.CMPN_PNT, ADM-071-01 |
| achvCrtrNmtm | Integer | Y | 달성 기준 횟수 | CARE_MSN.ACHV_CRTR_NMTM, ADM-071-01 |
| mvmnPath | String | N | 이동 경로 | CARE_MSN.MVMN_PATH, ADM-071-01 |
| useYn | boolean | Y | 사용 중이면 true | CARE_MSN.USE_YN, ADM-071-01 |

### AdminCareMissionUpdateRequest
보낸 칸만 바뀐다. 미션명·풀·이동 경로는 바꾸지 않는다
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| cmpnPnt | Integer | N | 1~1000 정수 | CARE_MSN.CMPN_PNT, ADM-071-02 |
| achvCrtrNmtm | Integer | N | 1~10 정수 | CARE_MSN.ACHV_CRTR_NMTM, ADM-071-02 |
| useYn | Boolean | N | false면 다음 출제부터 내지 않음 | CARE_MSN.USE_YN, ADM-071-02 |

### AdminCharacterJobListResponse
쓰는 API: GET /admin/processing/character-jobs. `{ sttsCnts, content, page, size, totalElements, totalPages, hasNext }`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| sttsCnts | List<AdminCharacterJobStatusCount> | Y | 네 줄, CHRG01 순서(CHRC0001 요청·CHRC0002 생성 중·CHRC0003 생성 완료·CHRC0004 생성 실패). 상태 필터와 무관, 기간은 따른다. 실패율은 화면이 계산한다 | ADM-082-02 |
| content | List<AdminCharacterJobRow> | Y | 아래 | |
| page·size·totalElements·totalPages·hasNext | | Y | 공통 목록 응답과 같다 | |

#### AdminCharacterJobStatusCount (AdminCharacterJobListResponse.sttsCnts)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| crtJobSttsCd | String | Y | CHRG01 | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| cnt | long | Y | 그 상태 건수(삭제 제외, 기간 안), 없으면 0 | CHAR_CRT_JOB |

#### AdminCharacterJobRow (AdminCharacterJobListResponse.content)
결과 이미지·원본 사진 없음(결정 26)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| charCrtJobSn | Long | Y | | CHAR_CRT_JOB.CHAR_CRT_JOB_SN |
| mbrSn | Long | Y | 요청한 회원 | CHAR_CRT_JOB.MBR_SN |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| crtJobSttsCd | String | Y | CHRG01 | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| failRsn | String | N | 실패 사유 | CHAR_CRT_JOB.FAIL_RSN |
| otoInqrySn | Long | N | 연결된 1:1 문의(→ SCR-ADM-042 문의 상세) | CHAR_CRT_JOB.OTO_INQRY_SN |
| regDt | LocalDateTime | Y | 요청 일시 | CHAR_CRT_JOB.REG_DT |

### AdminCharacterJobSearchCond
쓰는 API: GET /admin/processing/character-jobs (쿼리 파라미터, 모두 선택)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| fromYmd | LocalDate | N | 시작일 `YYYY-MM-DD`, `REG_DT` 날짜 기준 포함. 목록·sttsCnts 모두 건다. 형식 오류 400 | CHAR_CRT_JOB.REG_DT |
| toYmd | LocalDate | N | 종료일 `YYYY-MM-DD`, 그날 포함. `fromYmd`보다 이르면 400 | CHAR_CRT_JOB.REG_DT |
| crtJobSttsCd | String | N | CHRG01(CHRC0001~0004), 목록에만 건다(sttsCnts 무관). 그 밖은 400. 빈 값은 전체 | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| page·size | Integer | N | 1부터, 기본 20·최대 50 | |

### AdminItemSummary
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemSn | Long | Y | | ITEM.ITEM_SN |
| itemNm | String | Y | | ITEM.ITEM_NM |
| itemRegnCd | String | Y | 부위 MYPG01 | ITEM.ITEM_REGN_CD |
| itemRegnCdNm | String | Y | 부위 이름 | COM_CD.COM_CD_NM |
| needPnt | Long | Y | | ITEM.NEED_PNT |
| ntslYn | boolean | Y | 판매 중이면 true | ITEM.NTSL_YN |
| sortSeq | Long | Y | 부위 안 상점 순서 | ITEM.SORT_SEQ |
| atchFileSn | Long | Y | 이미지는 `/admin/items/{itemSn}/image`로 연다 | ITEM.ATCH_FILE_SN |
| bakeHint | String | N | 굽기 지시, 없으면 null | ITEM.BAKE_HINT |
| bakeImgRefYn | boolean | Y | 그림 참고 굽기면 true | ITEM.BAKE_IMG_REF_YN |
| hldMbrCnt | Long | Y | 보유 회원 수(그 아이템을 보유한 캐릭터 수, 캐릭터는 반려동물당 1개) | HLD_ITEM |

### AdminItemCreateRequest
`multipart/form-data`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemRegnCd | String | Y | MYPG01 코드. 신발(MYPC0006)은 사용 안 함, 400 | ITEM.ITEM_REGN_CD |
| itemNm | String | Y | 1~50자, 앞뒤 공백 제거. 같은 이름 409 | ITEM.ITEM_NM |
| needPnt | Long | Y | 0~1,000,000 | ITEM.NEED_PNT |
| ntslYn | Boolean | Y | | ITEM.NTSL_YN |
| bakeHint | String | N | 300자 이하, 비우고 `bakeImgRefYn`도 false면 굽지 않음. 신발(MYPC0006)은 사용 안 하는 부위라 등록 400(부위 검사). 초안은 AI `/ai/items/bake-hints` | ITEM.BAKE_HINT |
| bakeImgRefYn | Boolean | N | true면 굽기 지시 없이도 굽기 대상이고 아이템 그림을 참고로 굽는다. 없으면 false | ITEM.BAKE_IMG_REF_YN |
| image | MultipartFile | Y | 이미지 파일(형식·용량은 공통 파일 규칙) | ITEM.ATCH_FILE_SN |

### AdminItemCreateResponse
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemSn | Long | Y | | ITEM.ITEM_SN |

### AdminItemUpdateRequest
`multipart/form-data`, 보낸 칸만 바뀐다
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemRegnCd | String | N | MYPG01 코드. 보유 회원이 있으면 바꿀 수 없다. 신발(MYPC0006)로는 바꿀 수 없다(400) | ITEM.ITEM_REGN_CD |
| itemNm | String | N | 1~50자 | ITEM.ITEM_NM |
| needPnt | Long | N | 0~1,000,000 | ITEM.NEED_PNT |
| ntslYn | Boolean | N | false면 판매 중지 | ITEM.NTSL_YN |
| bakeHint | String | N | 300자 이하. 빈 문자열이면 지운다 | ITEM.BAKE_HINT |
| bakeImgRefYn | Boolean | N | 그림 참고 굽기 여부. 바꿔도 이미 구운 착용 시트는 그대로다 | ITEM.BAKE_IMG_REF_YN |
| image | MultipartFile | N | 바꿀 때만 | ITEM.ATCH_FILE_SN |

### AdminItemOrderRequest
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemRegnCd | String | Y | MYPG01 코드. 신발(MYPC0006)은 사용 안 함, 400 | ITEM.ITEM_REGN_CD |
| itemSns | List<Long> | Y | 그 부위 아이템 전부를 상점 순서대로 | ITEM.SORT_SEQ |

### AdminPetSummary
쓰는 API: GET /admin/members/{mbrSn}/pets

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN, ADM-011-02 |
| petNm | String | Y | | PET.PET_NM, ADM-011-02 |
| petSeCd | String | Y | 종 코드 | PET.PET_SE_CD, ADM-011-02 |
| vrtyNm | String | N | 품종명 | VRTY.VRTY_NM, ADM-011-02 |
| vrtyDrctInptNm | String | N | 품종 직접 입력 | PET.VRTY_DRCT_INPT_NM, ADM-011-02 |
| brupRoleCd | String | Y | 이 회원의 양육 역할 | BRUP_REL.BRUP_ROLE_CD, ADM-011-02 |
| caregiverCount | Integer | Y | 연동 중 공동양육자 수. 주양육자 제외 | BRUP_REL, ADM-011-02 |
| delYn | boolean | Y | 삭제 여부 | PET.DEL_YN, ADM-011-02 |

### AdminPointLedgerSummary
쓰는 API: GET /admin/members/{mbrSn}/point-ledgers

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pntGnlgrSn | Long | Y | | PNT_GNLGR.PNT_GNLGR_SN, ADM-011-04 |
| pntDlngTypeCd | String | Y | 거래 유형 코드 | PNT_GNLGR.PNT_DLNG_TYPE_CD, ADM-011-04 |
| pntDlngTypeCdNm | String | Y | 거래 유형 이름 | COM_CD.COM_CD_NM, ADM-011-04 |
| dlngPnt | Integer | Y | 증감. 부호로 적립·사용 구분 | PNT_GNLGR.DLNG_PNT, ADM-011-04 |
| dlngAftrBlncPnt | Integer | Y | 거래 후 잔액 | PNT_GNLGR.DLNG_AFTR_BLNC_PNT, ADM-011-04 |
| memoCn | String | N | | PNT_GNLGR.MEMO_CN, ADM-011-04 |
| regDt | LocalDateTime | Y | 일시 | PNT_GNLGR.REG_DT, ADM-011-04 |

### AdminPointLedgerRow
쓰는 API: GET /admin/points, POST /admin/points/adjustments

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pntGnlgrSn | Long | Y | | PNT_GNLGR.PNT_GNLGR_SN, ADM-072-01 |
| mbrSn | Long | Y | 회원 번호 | PNT_GNLGR.MBR_SN, ADM-072-01 |
| mbrNcnm | String | Y | 회원 닉네임 | MBR.MBR_NCNM, ADM-072-01 |
| pntDlngTypeCd | String | Y | 거래 유형 코드 | PNT_GNLGR.PNT_DLNG_TYPE_CD, ADM-072-01 |
| pntDlngTypeCdNm | String | Y | 거래 유형 이름 | COM_CD.COM_CD_NM, ADM-072-01 |
| dlngPnt | Integer | Y | 증감. 부호로 적립·사용 구분 | PNT_GNLGR.DLNG_PNT, ADM-072-01 |
| dlngAftrBlncPnt | Integer | Y | 거래 후 잔액 | PNT_GNLGR.DLNG_AFTR_BLNC_PNT, ADM-072-01 |
| memoCn | String | N | | PNT_GNLGR.MEMO_CN, ADM-072-01 |
| regDt | LocalDateTime | Y | 일시 | PNT_GNLGR.REG_DT, ADM-072-01 |

### AdminPointAdjustRequest
쓰는 API: POST /admin/points/adjustments

| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| mbrSn | Long | Y | 조정할 회원 | PNT_GNLGR.MBR_SN, ADM-072-02 |
| dlngPnt | Integer | Y | 지급 +, 차감 -. 0 아님, 절댓값 100,000 이하 | PNT_GNLGR.DLNG_PNT, ADM-072-02 |
| memoCn | String | Y | 사유. 앞뒤 공백 지운 뒤 1~200자 | PNT_GNLGR.MEMO_CN, ADM-072-02 |

### BreedSummary
쓰는 API: GET /breeds
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| vrtySn | Long | Y | 초안 그대로 | VRTY.VRTY_SN |
| vrtyCd | String | Y | 초안 그대로 | VRTY.VRTY_CD |
| vrtyNm | String | Y | 초안 그대로 | VRTY.VRTY_NM |
| petSeCd | String | Y | 초안 그대로. 공통코드 PETG01 | VRTY.PET_SE_CD |
| sortSeq | Integer | Y | 초안 그대로 | VRTY.SORT_SEQ |

### CaregiverInvitationAcceptRequest
쓰는 API: POST /caregiver-invitations/{jntBrupInvtnSn}/accept
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSns | List<Long> | Y | 1개 이상. 초대에 든 반려동물만(아니면 400 `INVALID_INPUT_VALUE`). 고르지 않은 반려동물은 거절 | INVTN_TRGT_PET.PET_SN, INVTN_TRGT_PET.ACPT_YN, MYP-005-05 |

### CaregiverInvitationConflictResponse
쓰는 API: POST /caregiver-invitations의 오류 응답 data(409 `CAREGIVER_ALREADY_LINKED`·`CAREGIVER_INVITATION_DUPLICATED`)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| linkedPets | List<CaregiverInvitationLinkedPet> | Y | 초대받는 사람이 이미 가족으로 연동된 동물, "이미 ○○의 가족이에요". 없으면 빈 목록 | BRUP_REL, MYP-005-01 |
| pendingPets | List<CaregiverInvitationPendingPet> | Y | 같은 사람에게 대기 중인 초대가 있는 동물, "이미 초대를 보냈어요 (MM-DD에 만료돼요)". 없으면 빈 목록 | JNT_BRUP_INVTN, INVTN_TRGT_PET, MYP-005-01 |

#### CaregiverInvitationLinkedPet (CaregiverInvitationConflictResponse.linkedPets)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |
| petNm | String | Y | | PET.PET_NM |

#### CaregiverInvitationPendingPet (CaregiverInvitationConflictResponse.pendingPets)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |
| petNm | String | Y | | PET.PET_NM |
| invtnExpryDt | LocalDateTime | Y | 그 대기 초대의 만료일시 | JNT_BRUP_INVTN.INVTN_EXPRY_DT |

응답 예시(둘 다 섞임)
```json
{"status":"error","httpCode":409,"message":"이미 가족으로 연동된 반려동물이에요.","path":"/api/caregiver-invitations","code":"CAREGIVER_ALREADY_LINKED",
 "data":{"linkedPets":[{"petSn":12,"petNm":"뭉치"}],"pendingPets":[{"petSn":14,"petNm":"콩이","invtnExpryDt":"2026-11-06T14:20:00"}]}}
```

### CaregiverInvitationCreateRequest
쓰는 API: POST /caregiver-invitations
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| invtnTrgtEml | String | Y | 초대할 상대의 이메일 주소. 회원가입 이메일과 같은 검증(이메일 형식, 최대 254자). 본인 이메일은 400 `CAREGIVER_INVITATION_SELF` | JNT_BRUP_INVTN.INVTN_TRGT_EML_ENC, MYP-005-01 |
| petSns | List<Long> | Y | 1개 이상. 내가 주양육자(PCG)인 반려동물만(아니면 403 `PET_PRIMARY_ONLY`). 공동양육 5명이 찬 반려동물은 불가(400 `CAREGIVER_LIMIT_EXCEEDED`) | INVTN_TRGT_PET.PET_SN, MYP-005-02, MYP-004-01 |

### CaregiverInvitationResponse
쓰는 API: GET /caregiver-invitations/{jntBrupInvtnSn}; POST /caregiver-invitations
초대 링크(메일·인앱 알림)는 프론트 `/mypage/invitations/{jntBrupInvtnSn}`이고, 로그인한 뒤 이 API를 부른다. 미가입자는 가입·로그인 뒤 같은 링크로 수락하며 `RCVR_MBR_SN`은 수락·거절 때 채운다. 수락 권한은 로그인 계정 이메일 일치로 판단하므로 `INVTN_TKN_HASH`는 저장만 하고 쓰지 않는다. 5-3 수락·5-5 이메일 불일치·5-6 만료·종료를 이 응답 하나로 가른다. `sndrMbrNcnm`·`pets`는 대기중이고 `emailMatched`가 true일 때만 있고, 아니면 JSON에서 빠진다(다른 계정에 남의 반려동물 정보 노출 방지, MYP-005-09·10).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| jntBrupInvtnSn | Long | Y | | JNT_BRUP_INVTN.JNT_BRUP_INVTN_SN |
| sndrMbrNcnm | String | N | | JNT_BRUP_INVTN.SNDR_MBR_SN → MBR.MBR_NCNM |
| invtnSttsCd | String | Y | 초대 상태(PETG06). 대기중이어도 만료일시가 지나면 만료됨 | JNT_BRUP_INVTN.INVTN_STTS_CD, MYP-005-04 |
| invtnExpryDt | LocalDateTime | Y | | JNT_BRUP_INVTN.INVTN_EXPRY_DT |
| invtnTrgtEmlMask | String | Y | 아이디 첫 글자와 도메인만 남긴 초대 이메일("s***@example.com") | JNT_BRUP_INVTN.INVTN_TRGT_EML_ENC, MYP-005-09 |
| emailMatched | boolean | Y | 로그인 계정 이메일이 초대 이메일과 같은지 | JNT_BRUP_INVTN.INVTN_TRGT_EML_HMAC, MBR.ACNT_EML_HMAC, MYP-005-09 |
| pets | List<CaregiverInvitationPetSummary> | N | 초대 대상 반려동물 카드(삭제된 반려동물은 빠짐) | INVTN_TRGT_PET.PET_SN, MYP-005-05 |

### CaregiverInvitationSummary
쓰는 API: GET /caregiver-invitations
최신 발송순. 한 초대의 반려동물은 `pets`에 반려동물별 상태와 함께 담는다(화면은 반려동물마다 한 줄).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| jntBrupInvtnSn | Long | Y | | JNT_BRUP_INVTN.JNT_BRUP_INVTN_SN |
| invtnTrgtEml | String | Y | 복호화한 대상 이메일 | JNT_BRUP_INVTN.INVTN_TRGT_EML_ENC, MYP-005-03 |
| rcvrJoined | boolean | Y | 발송 때 가입 회원이었는지. true "가입 회원(인앱 알림)", false "미가입 이메일" | JNT_BRUP_INVTN.RCVR_MBR_SN, MBR.REG_DT, MYP-005-03 |
| pets | List<CaregiverInvitationPetSummary> | Y | 반려동물명·반려동물별 상태 표시 | INVTN_TRGT_PET.PET_SN, MYP-005-03 |
| regDt | LocalDateTime | Y | 발송일 | JNT_BRUP_INVTN.REG_DT, MYP-005-03 |
| invtnSttsCd | String | Y | 초대 단위 상태, 대기중·수락·거절·만료됨(PETG06) | JNT_BRUP_INVTN.INVTN_STTS_CD, MYP-005-03 |
| invtnExpryDt | LocalDateTime | Y | 대기중 항목의 만료 예정일 | JNT_BRUP_INVTN.INVTN_EXPRY_DT, MYP-005-03·04 |

#### CaregiverInvitationPetSummary (CaregiverInvitationSummary.pets, CaregiverInvitationResponse.pets)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | INVTN_TRGT_PET.PET_SN |
| petNm | String | Y | | PET.PET_NM, MYP-005-03 |
| petSeCd | String | Y | | PET.PET_SE_CD, MYP-005-05 |
| vrtyNm | String | N | 목록에서 고른 품종명. 품종을 고르지 않았으면 null | PET.VRTY_SN → VRTY.VRTY_NM, MYP-005-05 |
| vrtyDrctInptNm | String | N | 직접 입력한 품종명. 없으면 null. 둘 다 있으면 직접 입력을 먼저 보인다(합치지 않고 따로 내림) | PET.VRTY_DRCT_INPT_NM, MYP-005-05 |
| invtnSttsCd | String | Y | 반려동물별 상태(PETG06). 일부 수락이면 고른 반려동물은 수락, 뺀 반려동물은 거절. 응답 전이면 초대 상태(만료 포함) | INVTN_TRGT_PET.ACPT_YN, JNT_BRUP_INVTN.INVTN_STTS_CD, MYP-005-03 |

### CaregiverSummary
쓰는 API: GET /pets/{petSn}/caregivers
`PetResponse.caregivers[]`와 같은 모양(GET /pets/{petSn}). 주양육자가 첫 줄, 공동양육자는 연동한 순서. 주양육자에게는 연동 중인 전원, 공동양육자에게는 주양육자와 본인(다른 공동양육자는 빼고, MYP-004-03·04). 권한("조회+수정")은 역할로 정해져 필드를 두지 않는다.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| brupRelSn | Long | Y | 연동 해제 경로 변수 | BRUP_REL.BRUP_REL_SN, MYP-004-01, MYP-006-02 |
| mbrSn | Long | Y | | BRUP_REL.MBR_SN |
| mbrNcnm | String | Y | | MBR.MBR_NCNM |
| brupRoleCd | String | Y | 주양육자 줄 구분(PETG05) | BRUP_REL.BRUP_ROLE_CD, MYP-004-03 |
| linkYmd | LocalDate | Y | 연동일. 마지막 초대 수락 연동(PETC0024) 이력의 등록일, 이력이 없으면(주양육자) 양육관계 등록일 | BRUP_LINK_STTS_HSTRY.REG_DT, BRUP_REL.REG_DT, MYP-004-01 |

### DailyMissionSummary
쓰는 API: GET /pets/{petSn}/daily-missions
출제 순서대로 정렬. 상단의 완료 개수·달성률·획득 포인트·남은 포인트는 프론트가 이 목록으로 계산한다(MYP-020-04).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| dayMsnSn | Long | Y | | DAY_MSN.DAY_MSN_SN |
| careMsnSn | Long | Y | | DAY_MSN.CARE_MSN_SN |
| msnNm | String | Y | | CARE_MSN.MSN_NM, MYP-020-04 |
| scorPnt | Integer | Y | 배점 | DAY_MSN.SCOR_PNT, MYP-020-04·05 |
| prgrsNmtm | Integer | Y | 진행 횟수 | DAY_MSN.PRGRS_NMTM, MYP-020-04 |
| achvCrtrNmtm | Integer | Y | 달성 기준 횟수(진행률 분모) | DAY_MSN.ACHV_CRTR_NMTM(출제 때 CARE_MSN 값 복사, ADM-071-03), MYP-020-05 |
| givePnt | Integer | N | 지급 포인트. 완료도 자정 정산도 안 됐으면 null, 그 밖에는 값(0 포함) | DAY_MSN.GIVE_PNT, MYP-020-04·05 |
| cmptnYn | boolean | Y | 완료/미완료 | DAY_MSN.CMPTN_YN, MYP-020-04 |

### EquippedItemSaveRequest
쓰는 API: PUT /pixel-characters/{pixelCharSn}/equipped-items
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| hldItemSns | List<Long> | Y | 이 캐릭터의 보유 아이템만. 부위당 1개. 빈 목록이면 전부 착용 해제(요청에 없는 보유 아이템은 해제) | HLD_ITEM.HLD_ITEM_SN, MYP-010-13·16 |

### ItemPurchaseRequest
쓰는 API: POST /pixel-characters/{pixelCharSn}/held-items
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| idmptDmndId | String | Y | 최대 100자. 포인트 원장에 넘겨 중복 차감을 막는다 | PNT_GNLGR.IDMPT_DMND_ID, MYP-011-05 |
| itemSns | List<Long> | Y | 1개 이상. 이 캐릭터가 이미 보유한 아이템은 불가. 총 필요 포인트가 보유 포인트를 넘으면 불가 | ITEM.ITEM_SN, MYP-011-03·05 |

### ShopItemListResponse
쓰는 API: GET /pixel-characters/{pixelCharSn}/shop-items
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| hldPnt | Long | Y | 보유 포인트 | MBR.HLD_PNT, MYP-011-03 |
| items | List<ShopItemSummary> | Y | 판매 중 아이템. 없으면 빈 목록 | ITEM, MYP-011-02 |

#### ShopItemSummary (ShopItemListResponse.items)
아이템 이미지는 `GET /items/{itemSn}/image`로 받는다.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| itemSn | Long | Y | | ITEM.ITEM_SN |
| itemNm | String | Y | | ITEM.ITEM_NM |
| itemRegnCd | String | Y | 부위(MYPG01) | ITEM.ITEM_REGN_CD |
| needPnt | Long | Y | 가격 | ITEM.NEED_PNT |
| hldYn | boolean | Y | 이 캐릭터가 이미 보유했는지 | HLD_ITEM 유무, MYP-011-03 |

### PetCreateRequest
쓰는 API: POST /pets
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petNm | String | Y | 최대 20자 | PET.PET_NM, MYP-003-01 |
| petSeCd | String | Y | 강아지/고양이(PETG01) | PET.PET_SE_CD, MYP-003-01·02 |
| vrtySn | Long | N | 종에 맞는 품종 목록에서 선택. `vrtyDrctInptNm`과 둘 중 하나 필수 | PET.VRTY_SN, MYP-003-02 |
| vrtyDrctInptNm | String | N | [직접 입력] 선택 시. 최대 40자 | PET.VRTY_DRCT_INPT_NM, MYP-003-02 |
| petBrdt | LocalDate | Y | | PET.PET_BRDT, MYP-003-01 |
| chipNo | String | N | 숫자만, 최대 20자리 | PET.CHIP_NO, MYP-003-05 |
| petGndrCd | String | Y | 성별(PETG02) | PET.PET_GNDR_CD, MYP-003-01 |
| nutrYn | Boolean | Y | 중성화 여부 | PET.NUTR_YN, MYP-003-01·06 |
| petBdwg | BigDecimal | Y | 0보다 큼, 소수 셋째 자리까지 | PET.PET_BDWG, MYP-003-01 |
| goalBdwg | BigDecimal | N | 0보다 큼, 소수 셋째 자리까지 | PET.GOAL_BDWG, MYP-003-04 |
| minBdwg | BigDecimal | Y | 0보다 큼, 소수 셋째 자리까지, `maxBdwg`보다 작음 | PET.MIN_BDWG, MYP-003-04 |
| maxBdwg | BigDecimal | Y | 0보다 큼, 소수 셋째 자리까지 | PET.MAX_BDWG, MYP-003-04 |
| colrCd | String | Y | 종별 선택지(PETG03) | PET.COLR_CD, MYP-003-03 |
| colrDrctInptNm | String | N | [직접 입력] 선택 시. 최대 20자 | PET.COLR_DRCT_INPT_NM, MYP-003-03 |
| chrcCn | String | N | 최대 1,000자 | PET.CHRC_CN, MYP-003-01 |
| algyCn | String | N | 최대 1,000자. 쉼표 구분, 저장 시 앞뒤 공백 제거·빈 값 버림 | PET.ALGY_CN, MYP-003-01·07 |
| dssCn | String | N | 최대 1,000자. 쉼표 구분, 저장 시 앞뒤 공백 제거·빈 값 버림 | PET.DSS_CN, MYP-003-01·07 |

### PetResponse
쓰는 API: GET /pets/{petSn}; PATCH /pets/{petSn}; POST /pets
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |
| petNm | String | Y | | PET.PET_NM |
| petSeCd | String | Y | | PET.PET_SE_CD |
| vrtySn | Long | N | | PET.VRTY_SN |
| vrtyNm | String | N | | VRTY.VRTY_NM |
| vrtyDrctInptNm | String | N | | PET.VRTY_DRCT_INPT_NM |
| petGndrCd | String | Y | | PET.PET_GNDR_CD |
| petBrdt | LocalDate | Y | | PET.PET_BRDT |
| petBdwg | BigDecimal | Y | | PET.PET_BDWG |
| colrCd | String | Y | | PET.COLR_CD |
| colrDrctInptNm | String | N | | PET.COLR_DRCT_INPT_NM |
| chipNo | String | N | | PET.CHIP_NO |
| chrcCn | String | N | | PET.CHRC_CN |
| algyCn | String | N | | PET.ALGY_CN |
| dssCn | String | N | | PET.DSS_CN |
| brupRoleCd | String | Y | | BRUP_REL.BRUP_ROLE_CD |
| rprsPetYn | boolean | Y | | BRUP_REL.RPRS_PET_YN |
| nutrYn | boolean | Y | | PET.NUTR_YN |
| goalBdwg | BigDecimal | N | | PET.GOAL_BDWG |
| minBdwg | BigDecimal | Y | | PET.MIN_BDWG |
| maxBdwg | BigDecimal | Y | | PET.MAX_BDWG |
| caregivers | List<CaregiverSummary> | Y | 양육자 목록 | D절 6번, BRUP_REL |
| caregivers[].brupRelSn | Long | Y | | BRUP_REL.BRUP_REL_SN |
| caregivers[].mbrSn | Long | Y | | BRUP_REL.MBR_SN |
| caregivers[].mbrNcnm | String | Y | | MBR.MBR_NCNM |
| caregivers[].brupRoleCd | String | Y | | BRUP_REL.BRUP_ROLE_CD |
| caregivers[].linkYmd | LocalDate | Y | 연동일(CaregiverSummary와 같음) | BRUP_LINK_STTS_HSTRY.REG_DT, BRUP_REL.REG_DT |

### PetShortcutSaveRequest
쓰는 API: PUT /pets/main-shortcuts (본문 배열의 한 줄)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | 양육 중인 반려동물. 본문 안에서 한 번씩 | PET_MAIN_SHRTC.PET_SN, MAN-008 |
| shrtcCds | List<String> | Y | 보일 순서대로 늘어놓은 MANG02 바로가기 코드(MANC0008 종합 건강기록·MANC0009 건강 일지·MANC0010 케어 캘린더). 한 번씩, 빈 목록이면 전부 숨김 | PET_MAIN_SHRTC.SHRTC_CD·INDCT_YN·SORT_SEQ, MAN-008 |

### PetShortcutSummary
쓰는 API: GET /pets/main-shortcuts; PUT /pets/main-shortcuts (응답 행)
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | 양육 중인 반려동물 | BRUP_REL.PET_SN |
| shrtcCds | List<String> | Y | 보일 순서대로 MANG02 코드. 이름은 `GET /common-codes?prntComCd=MANG02`, 이동 경로는 프론트가 코드로 고름 | PET_MAIN_SHRTC.SHRTC_CD, MAN-008 |

### PetSummary
쓰는 API: GET /pets
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |
| petNm | String | Y | | PET.PET_NM |
| petSeCd | String | Y | | PET.PET_SE_CD |
| vrtySn | Long | N | | PET.VRTY_SN |
| vrtyNm | String | N | | VRTY.VRTY_NM |
| vrtyDrctInptNm | String | N | | PET.VRTY_DRCT_INPT_NM |
| petGndrCd | String | Y | | PET.PET_GNDR_CD |
| petBrdt | LocalDate | Y | | PET.PET_BRDT |
| petBdwg | BigDecimal | Y | | PET.PET_BDWG |
| nutrYn | boolean | Y | 중성화 여부. 슬라이드의 "성별(중성화)" | PET.NUTR_YN, MYP-002-02 |
| minBdwg | BigDecimal | Y | 정상 체중 최소. 슬라이드의 "정상 a~bkg" | PET.MIN_BDWG, MYP-002-02 |
| maxBdwg | BigDecimal | Y | 정상 체중 최대 | PET.MAX_BDWG, MYP-002-02 |
| colrCd | String | Y | | PET.COLR_CD |
| colrDrctInptNm | String | N | | PET.COLR_DRCT_INPT_NM |
| chipNo | String | N | | PET.CHIP_NO |
| chrcCn | String | N | | PET.CHRC_CN |
| algyCn | String | N | | PET.ALGY_CN |
| dssCn | String | N | | PET.DSS_CN |
| brupRoleCd | String | Y | | BRUP_REL.BRUP_ROLE_CD |
| rprsPetYn | boolean | Y | | BRUP_REL.RPRS_PET_YN |
| caregiverCount | Integer | Y | | MYP-005-02 (BRUP_REL 건수) |
| bscCharSbstYn | boolean | Y | 캐릭터 오류 확인 중. 캐릭터가 없으면 false | PIXEL_CHAR.BSC_CHAR_SBST_YN, MYP-001-01 |

### PetUpdateRequest
쓰는 API: PATCH /pets/{petSn}
보낸 필드만 바뀐다. 문자열을 비우려면 `""`(A절). 목표체중은 `goalBdwgReset`으로 비운다. 품종은 `vrtySn`·`vrtyDrctInptNm` 중 하나만 보내면 다른 쪽이 비워진다(issues 4번).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petNm | String | N | 최대 20자 | PET.PET_NM, MYP-003-01 |
| petSeCd | String | N | 강아지/고양이(PETG01) | PET.PET_SE_CD, MYP-002-04 |
| vrtySn | Long | N | 종에 맞는 품종 목록에서 선택 | PET.VRTY_SN, MYP-003-02 |
| vrtyDrctInptNm | String | N | 최대 40자 | PET.VRTY_DRCT_INPT_NM, MYP-003-02 |
| petBrdt | LocalDate | N | | PET.PET_BRDT, MYP-003-01 |
| chipNo | String | N | 숫자만, 최대 20자리 | PET.CHIP_NO, MYP-003-05 |
| petGndrCd | String | N | 성별(PETG02) | PET.PET_GNDR_CD, MYP-003-01 |
| nutrYn | Boolean | N | | PET.NUTR_YN, MYP-003-06 |
| petBdwg | BigDecimal | N | 0보다 큼, 소수 셋째 자리까지. 체중 잠금은 화면에서만(서버는 막지 않음) | PET.PET_BDWG, MYP-002-04, MYP-003-01 |
| goalBdwg | BigDecimal | N | 0보다 큼, 소수 셋째 자리까지 | PET.GOAL_BDWG, MYP-003-04 |
| minBdwg | BigDecimal | N | 0보다 큼, 소수 셋째 자리까지, 최대보다 작음(저장 후 값 기준) | PET.MIN_BDWG, MYP-003-04 |
| maxBdwg | BigDecimal | N | 0보다 큼, 소수 셋째 자리까지 | PET.MAX_BDWG, MYP-003-04 |
| colrCd | String | N | 종별 선택지(PETG03) | PET.COLR_CD, MYP-003-03 |
| colrDrctInptNm | String | N | 최대 20자 | PET.COLR_DRCT_INPT_NM, MYP-003-03 |
| chrcCn | String | N | 최대 1,000자 | PET.CHRC_CN, MYP-003-01 |
| algyCn | String | N | 최대 1,000자. 쉼표 구분, 앞뒤 공백 제거·빈 값 버림 | PET.ALGY_CN, MYP-003-07 |
| dssCn | String | N | 최대 1,000자. 쉼표 구분, 앞뒤 공백 제거·빈 값 버림 | PET.DSS_CN, MYP-003-07 |
| goalBdwgReset | Boolean | N | `true`면 목표체중을 비운다. `goalBdwg`와 함께 보내면 400 BAD_REQUEST | PET.GOAL_BDWG, MYP-003-04 |

### PixelCharacterResponse
쓰는 API: GET /pixel-characters/{pixelCharSn}; PUT /pixel-characters/{pixelCharSn}/equipped-items; POST /pixel-characters/{pixelCharSn}/held-items
결과 이미지는 `GET /pets/{petSn}/character-image`로 받는다. 여러 응답의 필드를 합쳤다(issues 11번).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pixelCharSn | Long | Y | | PIXEL_CHAR.PIXEL_CHAR_SN |
| petSn | Long | Y | | PIXEL_CHAR.PET_SN |
| charCrtJobSn | Long | Y | 최근 생성 작업 | CHAR_CRT_JOB.CHAR_CRT_JOB_SN |
| crtJobSttsCd | String | Y | 생성 작업 상태(CHRG01) | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| bscCharSbstYn | boolean | Y | 오류 확인 중(기본 캐릭터로 대체) | PIXEL_CHAR.BSC_CHAR_SBST_YN |
| inqryRcptYn | boolean | Y | 현재 결과에 오류 문의 접수됨(이름은 issues 3번) | MYP-010-24, CHAR_CRT_JOB.OTO_INQRY_SN 유무 |
| heldItems | List<HeldItemSummary> | Y | 보관함. 생성 직후는 빈 목록. 사용 안 하는 부위(신발 MYPC0006) 보유 아이템은 빼고, 착용 저장에 보내면 `ITEM_NOT_HELD` | MYP-010-14, HLD_ITEM |
| heldItems[].hldItemSn | Long | Y | | HLD_ITEM.HLD_ITEM_SN |
| heldItems[].itemSn | Long | Y | | HLD_ITEM.ITEM_SN |
| heldItems[].itemNm | String | Y | | ITEM.ITEM_NM |
| heldItems[].itemRegnCd | String | Y | | ITEM.ITEM_REGN_CD |
| heldItems[].atchFileSn | Long | Y | | ITEM.ATCH_FILE_SN |
| heldItems[].equipYn | Boolean | Y | | HLD_ITEM.EQUIP_YN |

### PixelCharacterSummary
쓰는 API: GET /pixel-characters
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| petSn | Long | Y | | PET.PET_SN |
| petNm | String | Y | | PET.PET_NM |
| petSeCd | String | Y | | PET.PET_SE_CD (종별 기본 캐릭터) |
| brupRoleCd | String | Y | | BRUP_REL.BRUP_ROLE_CD (탭 구분) |
| rprsPetYn | boolean | Y | | BRUP_REL.RPRS_PET_YN |
| pixelCharSn | Long | N | | PIXEL_CHAR.PIXEL_CHAR_SN (없으면 미생성) |
| crtJobSttsCd | String | N | | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| bscCharSbstYn | boolean | N | | PIXEL_CHAR.BSC_CHAR_SBST_YN (오류 확인 중) |

### PointLedgerSummary
쓰는 API: GET /point-ledgers
최신순. 선택한 월의 적립·사용 합계는 프론트가 합산한다(`issues.md` API의 마이페이지 19번).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pntGnlgrSn | Long | Y | | PNT_GNLGR.PNT_GNLGR_SN |
| pntDlngTypeCd | String | Y | 거래 유형(MYPG03). 적립·사용 구분 | PNT_GNLGR.PNT_DLNG_TYPE_CD, MYP-020-02 |
| pntDlngTypeCdNm | String | Y | 내역 표시 이름(예: "검진기록 등록") | COM_CD.COM_CD_NM, MYP-020-08 |
| dlngPnt | Integer | Y | 거래 포인트. 적립은 양수, 사용은 음수 | PNT_GNLGR.DLNG_PNT, MYP-020-02 |
| memoCn | String | N | 내용 보조 문구. 미션은 "{반려동물명} · {미션명}", 검진은 반려동물명, 아이템 구매는 구매 문구 | PNT_GNLGR.MEMO_CN, MYP-020-02 |
| regDt | LocalDateTime | Y | 거래 일시(행의 MM-DD) | PNT_GNLGR.REG_DT, MYP-020-02 |

### PointResponse
쓰는 API: GET /members/me/points
달성률은 프론트가 `todayCmptnMsnCnt / todayMsnCnt`로 계산한다.
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| hldPnt | Integer | Y | 보유 포인트 | MBR.HLD_PNT, MYP-020-01 |
| monthEarnPnt | Integer | Y | 이번 달 적립 합계 | MYP-020-09 |
| monthEarnCnt | Integer | Y | 이번 달 적립 건수 | MYP-020-09 |
| monthUsePnt | Integer | Y | 이번 달 사용 합계 | MYP-020-09 |
| monthUseCnt | Integer | Y | 이번 달 사용 건수 | MYP-020-09 |
| todayCmptnMsnCnt | Integer | Y | 주양육 반려동물 전체의 오늘 완료 미션 수 | MYP-020-09, MYP-001-04 |
| todayMsnCnt | Integer | Y | 주양육 반려동물 전체의 오늘 미션 수 | MYP-020-09, MYP-001-04 |
| todayMsnPetCnt | Integer | Y | 오늘 미션이 있는 주양육 반려동물 수 | MYP-020-09 |
