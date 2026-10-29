package com.nyangmeong.pet.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [양육자 목록 항목]
 * - PetResponse.caregivers 와 GET /pets/{petSn}/caregivers
 * - 주양육자 첫 줄, 주양육자에게 전원·공동양육자에게 주양육자와 본인
 */
@Data
public class CaregiverSummary {
    private Long brupRelSn;
    private Long mbrSn;
    private String mbrNcnm;
    private String brupRoleCd;
    private LocalDate linkYmd;
}
