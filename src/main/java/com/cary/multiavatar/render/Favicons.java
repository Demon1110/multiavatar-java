package com.cary.multiavatar.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * favicon 打包器：把同一头像按多个边长渲染为 PNG 并打成 zip（JDK 内置，零依赖）。
 *
 * <p>典型用途：网站 favicon / 应用图标包。viewBox 为正方形时各尺寸均为等比方图，
 * 非正方形视口会等比缩放并居中（边缘留透明边）。</p>
 */
public final class Favicons {

    /**
     * 默认打包尺寸（favicon 常见规格）。
     */
    public static final int[] DEFAULT_SIZES = {16, 32, 48, 64, 128, 256};

    private Favicons() {
    }

    /**
     * 打包多尺寸 PNG 为 zip 字节（默认尺寸 {@link #DEFAULT_SIZES}）。
     *
     * @param svg 完整 SVG 文本（非空）
     */
    public static byte[] toZip(String svg) {
        return toZip(svg, DEFAULT_SIZES);
    }

    /**
     * 打包多尺寸 PNG 为 zip 字节（指定尺寸；null/空时用默认尺寸）。
     *
     * @param svg   完整 SVG 文本（非空）
     * @param sizes 目标边长（像素），如 16, 32, 48…
     */
    public static byte[] toZip(String svg, int... sizes) {
        if (sizes == null || sizes.length == 0) {
            sizes = DEFAULT_SIZES;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            for (int size : sizes) {
                BufferedImage image = SvgRasterizer.rasterize(svg, size);
                zip.putNextEntry(new ZipEntry("avatar-" + size + ".png"));
                zip.write(PngWriter.toPng(image));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("favicon zip 打包失败", e);
        }
        return bos.toByteArray();
    }
}
