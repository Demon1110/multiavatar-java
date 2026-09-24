package com.cary.multiavatar.render;

import java.awt.image.BufferedImage;

/**
 * PNG 渲染策略：把 SVG 光栅化后编码为 PNG 字节。
 */
public final class PngAvatarRenderer implements AvatarRenderer {

    @Override
    public byte[] render(String svg, int size) {
        BufferedImage image = SvgRasterizer.rasterize(svg, size);
        return PngWriter.toPng(image);
    }
}
