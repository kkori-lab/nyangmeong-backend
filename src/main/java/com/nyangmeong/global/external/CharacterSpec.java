package com.nyangmeong.global.external;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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

    /** 얼굴 영역 (2026-11-02 결정, AI app/character/schemas.py와 같은 값) */
    public static final Rectangle FACE = new Rectangle(96, 32, 320, 320);

    /** 얼굴 이미지 한 변(px) : 잘라낸 얼굴 영역을 이 크기로 줄인다 */
    public static final int FACE_OUTPUT_SIZE = 256;

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
     * 얼굴 영역 PNG : FACE 영역을 잘라 FACE_OUTPUT_SIZE 정사각형으로 줄인다
     */
    public static byte[] cropFace(byte[] image) {
        BufferedImage buffered = read(image);
        if (buffered == null || buffered.getWidth() < FACE.x + FACE.width || buffered.getHeight() < FACE.y + FACE.height) {
            throw new IllegalArgumentException("캐릭터 규격이 아닌 이미지");
        }
        BufferedImage face = new BufferedImage(FACE_OUTPUT_SIZE, FACE_OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = face.createGraphics();
        try {
            /** 픽셀 아트 : 흐려지지 않게 최근접 보간 */
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(buffered.getSubimage(FACE.x, FACE.y, FACE.width, FACE.height), 0, 0, FACE_OUTPUT_SIZE,
                    FACE_OUTPUT_SIZE, null);
        } finally {
            g.dispose();
        }
        return toPng(face);
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
