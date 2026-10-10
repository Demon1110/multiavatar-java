package com.cary.multiavatar.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * favicon 打包器：把同一头像按多个边长渲染为 PNG 并打成 zip / 单文件 ICO（JDK 内置，零依赖）。
 *
 * <p>典型用途：网站 favicon / 应用图标包。viewBox 为正方形时各尺寸均为等比方图，
 * 非正方形视口会等比缩放并居中（边缘留透明边）。</p>
 *
 * <p>ICO 格式（Vista+ PNG 内嵌）由本类手写容器：ICONDIR 头 + 每尺寸 ICONDIRENTRY
 * + 原始 PNG 数据，不依赖任何第三方库。</p>
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

    /**
     * 打包多尺寸 PNG 为单文件 ICO 字节（默认尺寸 {@link #DEFAULT_SIZES}）。
     *
     * <p>结构与 PNG 内嵌 ICO（Vista+）一致：ICONDIR + ICONDIRENTRY×N + PNG 数据，
     * 浏览器与 Windows 均直接支持。</p>
     *
     * @param svg 完整 SVG 文本（非空）
     */
    public static byte[] toIco(String svg) {
        return toIco(svg, DEFAULT_SIZES);
    }

    /**
     * 打包多尺寸 PNG 为单文件 ICO 字节（指定尺寸；null/空时用默认尺寸）。
     *
     * @param svg   完整 SVG 文本（非空）
     * @param sizes 目标边长（像素），如 16, 32, 48…；大于 255 的尺寸按规范以 0 表示
     */
    public static byte[] toIco(String svg, int... sizes) {
        if (sizes == null || sizes.length == 0) {
            sizes = DEFAULT_SIZES;
        }
        // 先渲染全部 PNG，确定每个条目的字节数与偏移
        byte[][] pngs = new byte[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            BufferedImage image = SvgRasterizer.rasterize(svg, sizes[i]);
            pngs[i] = PngWriter.toPng(image);
        }
        int offset = 6 + 16 * sizes.length; // ICONDIR(6B) + ICONDIRENTRY×N(16B)
        ByteArrayOutputStream bos = new ByteArrayOutputStream(offset + 64 * 1024);
        // ICONDIR：reserved=0, type=1(icon), count
        writeLE16(bos, 0);
        writeLE16(bos, 1);
        writeLE16(bos, sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i];
            bos.write(s >= 256 ? 0 : s);      // width（0 表示 256）
            bos.write(s >= 256 ? 0 : s);      // height（0 表示 256）
            bos.write(0);                     // colorCount
            bos.write(0);                     // reserved
            writeLE16(bos, 1);                // planes
            writeLE16(bos, 32);               // bitCount
            writeLE32(bos, pngs[i].length);   // bytesInRes
            writeLE32(bos, offset);           // imageOffset
            offset += pngs[i].length;
        }
        for (byte[] png : pngs) {
            bos.write(png, 0, png.length);
        }
        return bos.toByteArray();
    }

    private static void writeLE16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static void writeLE32(ByteArrayOutputStream out, long v) {
        out.write((int) (v & 0xFF));
        out.write((int) ((v >>> 8) & 0xFF));
        out.write((int) ((v >>> 16) & 0xFF));
        out.write((int) ((v >>> 24) & 0xFF));
    }
}
