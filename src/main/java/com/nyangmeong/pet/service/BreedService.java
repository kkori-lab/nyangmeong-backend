package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.Breed;
import com.nyangmeong.pet.domain.PetSe;
import com.nyangmeong.pet.dto.BreedSummary;
import com.nyangmeong.pet.mapper.BreedMapper;

import lombok.RequiredArgsConstructor;

/**
 * [품종 서비스]
 * - 품종 마스터 조회, 실종센터·펫돌봄·반려동물 등록이 같이 사용 (FN-CMU-002-13)
 */
@Service
@RequiredArgsConstructor
public class BreedService {

    private final BreedMapper breedMapper;

    /**
     * 구분별 품종 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BreedSummary> getBreeds(String petSeCd) {
        checkPetSe(petSeCd);
        return breedMapper.selectList(petSeCd).stream().map(BreedSummary::from).toList();
    }

    /**
     * 해당 구분의 품종인지 확인 (아니면 BREED_NOT_FOUND 404)
     */
    @Transactional(readOnly = true)
    public Breed getBreedOrThrow(Long vrtySn, String petSeCd) {
        Breed breed = breedMapper.selectBreed(vrtySn);
        if (breed == null || !breed.getPetSeCd().equals(petSeCd)) {
            throw new CustomException(ErrorCode.BREED_NOT_FOUND);
        }
        return breed;
    }

    /**
     * 반려동물 구분 코드 확인 (아니면 400)
     */
    @Transactional(readOnly = true)
    public void checkPetSe(String petSeCd) {
        if (!PetSe.DOG.is(petSeCd) && !PetSe.CAT.is(petSeCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
