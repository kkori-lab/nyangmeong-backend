package com.nyangmeong.global.error;

/**
 * [비즈니스 예외]
 * - 사용 예: throw new CustomException(ErrorCode.PET_NOT_FOUND);
 * - 틀린 칸이 있는 입력 검사: throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "careCycleCd");
 */
public class CustomException extends BaseException {

    public CustomException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CustomException(ErrorCode errorCode, String... fields) {
        super(errorCode, fields);
    }
}
