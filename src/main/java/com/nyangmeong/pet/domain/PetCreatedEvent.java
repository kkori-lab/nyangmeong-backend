package com.nyangmeong.pet.domain;

import java.time.LocalDate;

/**
 * [반려동물 등록 이벤트]
 * - PetService.createPet 이 같은 트랜잭션 안에서 발행한다. 등록이 커밋된 뒤 받아 그날 케어미션을 바로 출제한다
 * - 받는 쪽은 AFTER_COMMIT 으로 받는다 : 출제가 실패해도 등록은 성공해야 하고, 못 채운 미션은 다음 정각 배치가 채운다
 */
public record PetCreatedEvent(Long petSn, LocalDate msnCrtrYmd) {
}
