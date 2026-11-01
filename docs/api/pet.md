# 반려동물(pet) API·DTO (초안)

공통 규칙(경로 접두어, 응답 껍데기, `Yn` 필드, 정본)은 `docs/api/README.md`에 있다. 이 파일은 이 도메인의 API와 그 요청·응답 DTO만 담는다.

## API 목록

| 메서드 | 경로 | 설명 | 기능 ID | 담당 | 요청 DTO | 응답 DTO |
| --- | --- | --- | --- | --- | --- | --- |
| GET | /breeds | 반려동물 구분별 품종 마스터 목록(`?petSeCd=`) | CMU-002-13, CMU-004-06·13, MYP-003-02 (기능 분배 3.1절 10번) | KSJ | - | List<BreedSummary> |
| GET | /caregiver-invitations | 내가 보낸 공동양육 초대 목록(계정 전체) | MYP-005-03 | SJM | - | List<CaregiverInvitationSummary> |
| GET | /caregiver-invitations/{jntBrupInvtnSn} | 받은 초대 상세. 로그인 회원 누구나. 대기중이고 로그인 이메일이 초대 이메일과 같을 때만 초대한 사람·대상 반려동물 카드, 아니면 상태와 가린 이메일만(만료·처리 여부를 이메일보다 먼저 판단) | MYP-005-05·09·10 | SJM | - | CaregiverInvitationResponse |
| GET | /items/{itemSn}/image | 아이템 이미지(로그인 회원, 판매 중 아이템) | MYP-010-14, MYP-011-02 | LJS | - | 이미지 파일 |
| GET | /members/me/points | 보유 포인트, 이번 달 적립·사용 합계와 건수, 오늘 미션 요약(주양육 반려동물 전체) | MYP-001-03·04, MYP-011-05, MYP-020-01·09 | SJM | - | PointResponse |
| GET | /pets | 내 반려동물 목록(주양육·공동양육, 역할·대표 여부·공동양육 인원 포함) | MYP-001-01, MYP-002-01~03, MYP-005-02, MYP-006-04, MYP-020-03 | SJM | - | List<PetSummary> |
| GET | /pets/{petSn} | 반려동물 프로필 상세(수정 모달 채우기, 양육자 목록). 체중 잠금은 화면이 건강기록 API로 판단(`issues.md` API의 마이페이지 2번) | MYP-002-04, MYP-003-01 | SJM | - | PetResponse |
| GET | /pets/{petSn}/caregivers | 공동양육자 목록. 주양육자가 첫 줄. 주양육자에게는 전원, 공동양육자에게는 주양육자와 본인 | MYP-004-01·03·04, MYP-006-04 | SJM | - | List<CaregiverSummary> |
| GET | /pets/{petSn}/character-image | 캐릭터 이미지 `?part=full`(전신, 기본)·`face`(얼굴). 미생성·오류 확인 중이면 종별 기본 캐릭터 | MYP-001-01, MYP-002-02, MYP-010-12·23 | LJS | - | 이미지 파일 |
| GET | /pets/{petSn}/daily-missions | 그날의 케어미션 8개와 진행 상태. 쿼리 `msnCrtrYmd` | MYP-020-03·04 | SJM | - | List<DailyMissionSummary> |
| GET | /pixel-characters | 내 캐릭터·공동양육 캐릭터 목록(캐릭터 없는 반려동물 포함, 생성 상태·오류 확인 중 표시) | MYP-010-02·03·07·12·23 | LJS | - | List<PixelCharacterSummary> |
| GET | /pixel-characters/{pixelCharSn} | 꾸미기 상세. 결과 이미지·보유/착용 아이템·오류 문의 접수 여부 | MYP-010-10·13·14·23 | LJS | - | PixelCharacterResponse |
| GET | /pixel-characters/{pixelCharSn}/shop-items | 상점 아이템 목록(판매 중, 이 캐릭터 기준 보유중 표시, 보유 포인트). `?itemRegnCd=` | MYP-011-02·03 | LJS | - | ShopItemListResponse |
| GET | /point-ledgers | 선택한 월의 포인트 적립·사용 내역(최신순). 쿼리 `yearMonth` | MYP-020-02 | SJM | - | List<PointLedgerSummary> |
| POST | /caregiver-invitations | 이메일로 공동양육 초대 발송. 대상 반려동물 여러 마리를 한 초대로 묶음. 가입 회원은 인앱 알림, 미가입은 메일. 고른 반려동물을 모두 검사해 충돌을 모아 409 오류 data에 CaregiverInvitationConflictResponse. 이미 가족이 하나라도 있으면 `CAREGIVER_ALREADY_LINKED`, 대기 초대만 있으면 `CAREGIVER_INVITATION_DUPLICATED`. 충돌이 있으면 초대를 만들지 않음 | MYP-005-01·02 | SJM | CaregiverInvitationCreateRequest | CaregiverInvitationResponse |
| POST | /caregiver-invitations/{jntBrupInvtnSn}/accept | 초대 수락. 고른 반려동물만 연동하고 나머지는 반려동물별 거절 | MYP-005-05 | SJM | CaregiverInvitationAcceptRequest | - |
| POST | /caregiver-invitations/{jntBrupInvtnSn}/reject | 초대 거절. 초대의 반려동물 전체 | MYP-005-07·08 | SJM | - | - |
| POST | /pets | 반려동물 등록(계정당 10마리, 첫 등록은 대표 지정) | MYP-002-01, MYP-003-01·04·05·07 | SJM | PetCreateRequest | PetResponse |
| POST | /pets/{petSn}/pixel-characters | 캐릭터 생성 요청(사진 첨부, 백그라운드 생성, 반려동물당 1개) | MYP-010-04·08·09·17 | LJS | PixelCharacterCreateRequest | PixelCharacterResponse |
| POST | /pets/{petSn}/representative | 대표 반려동물 지정(주양육 반려동물만) | MYP-010-05 | SJM | - | - |
| POST | /pixel-characters/{pixelCharSn}/held-items | 아이템 일괄 구매(포인트 차감, 보관함 추가, 주양육자만). 201. 오류: `POINT_NOT_ENOUGH`(400), `PET_PRIMARY_ONLY`(403), `ITEM_NOT_FOUND`(404), `ITEM_ALREADY_HELD`(409) | MYP-011-05·06 | LJS | ItemPurchaseRequest | PixelCharacterResponse |
| PUT | /pixel-characters/{pixelCharSn}/equipped-items | 착용 상태 확정 저장(부위당 1개, 요청에 없는 보유 아이템은 착용 해제) | MYP-010-13·16 | LJS | EquippedItemSaveRequest | PixelCharacterResponse |
| PATCH | /pets/{petSn} | 반려동물 수정(양육자 전원, 체중 잠금은 화면에서) | MYP-002-04, MYP-003-01·04~07 | SJM | PetUpdateRequest | PetResponse |
| DELETE | /pets/{petSn} | 반려동물 삭제(주양육자만). 캐릭터·보유 아이템 삭제, 공동양육 연동 해제, 대표 승계 | MYP-002-07·08 | SJM | - | - |
| DELETE | /pets/{petSn}/caregivers/{brupRelSn} | 공동양육 연동 해제. 주양육자의 강제 해제와 공동양육자의 자진 해제 | MYP-004-01, MYP-006-02 | SJM | - | - |

## DTO

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
| achvCrtrNmtm | Integer | Y | 달성 기준 횟수(진행률 분모) | CARE_MSN.ACHV_CRTR_NMTM, MYP-020-05 |
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

### PixelCharacterCreateRequest
쓰는 API: POST /pets/{petSn}/pixel-characters
`multipart/form-data`
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| photo | MultipartFile | Y | 사진 첨부 필수. 형식·용량 규칙은 명세에 없음(`issues.md` API의 마이페이지 18번) | MYP-010-08, CHAR_PHOTO.ATCH_FILE_SN |

### PixelCharacterResponse
쓰는 API: GET /pixel-characters/{pixelCharSn}; POST /pets/{petSn}/pixel-characters; PUT /pixel-characters/{pixelCharSn}/equipped-items; POST /pixel-characters/{pixelCharSn}/held-items
결과 이미지는 `GET /pets/{petSn}/character-image`로 받는다. POST 응답과 GET 응답의 필드를 합쳤다(issues 11번).
| 필드 | 타입 | 필수 | 검증·설명 | 근거 |
| --- | --- | --- | --- | --- |
| pixelCharSn | Long | Y | | PIXEL_CHAR.PIXEL_CHAR_SN |
| petSn | Long | Y | | PIXEL_CHAR.PET_SN |
| charCrtJobSn | Long | Y | 최근 생성 작업 | CHAR_CRT_JOB.CHAR_CRT_JOB_SN |
| crtJobSttsCd | String | Y | 생성 작업 상태(CHRG01) | CHAR_CRT_JOB.CRT_JOB_STTS_CD |
| bscCharSbstYn | boolean | Y | 오류 확인 중(기본 캐릭터로 대체) | PIXEL_CHAR.BSC_CHAR_SBST_YN |
| inqryRcptYn | boolean | Y | 현재 결과에 오류 문의 접수됨(이름은 issues 3번) | MYP-010-24, CHAR_CRT_JOB.OTO_INQRY_SN 유무 |
| heldItems | List<HeldItemSummary> | Y | 보관함. 생성 직후는 빈 목록 | MYP-010-14, HLD_ITEM |
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
