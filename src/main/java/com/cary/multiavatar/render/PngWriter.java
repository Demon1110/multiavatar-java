package com.cary.multiavatar.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PNG 编码器：BufferedImage → PNG 字节（JDK ImageIO，零依赖）。
 */
public final class PngWriter {

    private PngWriter() {
    }

    /**
     * 编码为 PNG 字节。
     */
    public static byte[] toPng(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 4);
        try {
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("当前 JVM 缺少 PNG 编码器");
            }
        } catch (IOException e) {
            throw new IllegalStateException("PNG 编码失败", e);
        }
        return out.toByteArray();
    }
}
