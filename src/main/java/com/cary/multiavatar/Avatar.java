package com.cary.multiavatar;

import com.cary.multiavatar.render.*;

import java.awt.image.BufferedImage;

/**
 * 头像产物（不可变）：封装输入字符串对应的 SVG 文本，并提供图像渲染能力。
 *
 * <p>通过 {@link Multiavatar#avatar(String)} 或 {@link Multiavatar#avatar(String, AvatarOptions)} 获取。
 * SVG 生成后缓存；图像按请求尺寸惰性渲染：{@link #toImage(int)} 直接返回 {@link BufferedImage}
 * 供继续加工，{@link #png(int)} / {@link #jpg(int)} 编码为对应格式字节。</p>
 */
public final class Avatar {

    private final String input;
    private final AvatarOptions options;
    private final String svg;

    Avatar(String input, AvatarOptions options, String svg) {
        this.input = input;
        this.options = options;
        this.svg = svg;
    }

    /**
     * 输入字符串。
     */
    public String input() {
        return input;
    }

    /**
     * 生成时使用的选项。
     */
    public AvatarOptions options() {
        return options;
    }

    /**
     * SVG 文本（空输入时为空串）。
     */
    public String svg() {
        return svg;
    }

    /**
     * 是否有内容（输入为空时无内容）。
     */
    public boolean isEmpty() {
        return svg.isEmpty();
    }

    /**
     * 光栅化图像（默认尺寸 256，透明背景）；空输入返回 null。
     */
    public BufferedImage toImage() {
        return toImage(options.size());
    }

    /**
     * 光栅化图像（指定边长，透明背景）；空输入返回 null。
     *
     * <p>直接返回 {@link BufferedImage} 便于调用方继续加工（缩放/合成/加水印），
     * 或自行编码为任意格式。</p>
     */
    public BufferedImage toImage(int size) {
        if (isEmpty()) {
            return null;
        }
        return SvgRasterizer.rasterize(svg, size);
    }

    /**
     * PNG 字节（默认尺寸 256）；空输入返回空数组。
     */
    public byte[] png() {
        return png(options.size());
    }

    /**
     * PNG 字节（指定边长）；空输入返回空数组。
     */
    public byte[] png(int size) {
        BufferedImage image = toImage(size);
        return image == null ? new byte[0] : PngWriter.toPng(image);
    }

    /**
     * JPEG 字节（默认尺寸 256，白底）；空输入返回空数组。
     */
    public byte[] jpg() {
        return jpg(options.size());
    }

    /**
     * JPEG 字节（指定边长，白底）；空输入返回空数组。
     */
    public byte[] jpg(int size) {
        if (isEmpty()) {
            return new byte[0];
        }
        AvatarRenderer renderer = Renderers.create(AvatarFormat.JPG);
        return renderer.render(svg, size);
    }
}
