package com.cary.multiavatar.render;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * JPEG 编码器：BufferedImage → JPEG 字节（JDK ImageIO，零依赖）。
 *
 * <p>JPEG 不支持透明通道，带 alpha 的图像会先铺到白底再转 RGB 编码；
 * 若要控制底色，请先在光栅化时指定背景色（{@link SvgRasterizer#rasterize(String, int, Color)}）。</p>
 */
public final class JpgWriter {

    private JpgWriter() {
    }

    /**
     * 编码为 JPEG 字节。
     */
    public static byte[] toJpg(BufferedImage image) {
        BufferedImage rgb = image;
        if (image.getColorModel().hasAlpha()) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(rgb.getWidth() * rgb.getHeight() * 3);
        try {
            if (!ImageIO.write(rgb, "jpg", out)) {
                throw new IllegalStateException("当前 JVM 缺少 JPEG 编码器");
            }
        } catch (IOException e) {
            throw new IllegalStateException("JPEG 编码失败", e);
        }
        return out.toByteArray();
    }
}
