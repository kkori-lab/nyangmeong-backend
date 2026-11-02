package com.nyangmeong.global.external;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * [캐릭터 이미지 규격]
 * - 아이템을 고정 좌표로 겹치기 위한 캔버스·부위 좌표 고정 (FN-MYP-010-17)
 * - 얼굴 영역 = 마이페이지 원형 표시용 (FN-MYP-001-01, FN-MYP-002-02)
 */
public final class CharacterSpec {

    /** 캔버스 한 변(px) */
    public static final int CANVAS_SIZE = 512;

    /** 시트 한 변(px) : 2×2 시트 */
    public static final int SHEET_SIZE = 1024;

    /** 얼굴 영역 */
    public static final Rectangle FACE = new Rectangle(128, 64, 256, 256);

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private CharacterSpec() {
    }

    /**
     * 규격 일치 여부
     */
    public static boolean matches(byte[] image) {
        BufferedImage buffered = read(image);
        return buffered != null && buffered.getWidth() == CANVAS_SIZE && buffered.getHeight() == CANVAS_SIZE;
    }

    /**
     * 시트 규격 일치 여부
     */
    public static boolean matchesSheet(byte[] sheet) {
        if (sheet == null || sheet.length < PNG_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(sheet, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            return false;
        }
        BufferedImage buffered = read(sheet);
        return buffered != null && buffered.getWidth() == SHEET_SIZE && buffered.getHeight() == SHEET_SIZE;
    }

    /**
     * 얼굴 영역 PNG
     */
    public static byte[] cropFace(byte[] image) {
        BufferedImage buffered = read(image);
        if (buffered == null || buffered.getWidth() < FACE.x + FACE.width || buffered.getHeight() < FACE.y + FACE.height) {
            throw new IllegalArgumentException("캐릭터 규격이 아닌 이미지");
        }
        return toPng(buffered.getSubimage(FACE.x, FACE.y, FACE.width, FACE.height));
    }

    /**
     * PNG 인코딩
     */
    public static byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 이미지 디코딩
     */
    private static BufferedImage read(byte[] image) {
        if (image == null || image.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
