package com.cary.multiavatar.render;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * JPEG 渲染策略：把 SVG 光栅化（白色背景）后编码为 JPEG 字节。
 *
 * <p>JPEG 不支持透明通道，因此以白底填充，适合博客/OA 等需要不透明图片的场景。</p>
 */
public final class JpgAvatarRenderer implements AvatarRenderer {

    @Override
    public byte[] render(String svg, int size) {
        BufferedImage image = SvgRasterizer.rasterize(svg, size, Color.WHITE);
        return JpgWriter.toJpg(image);
    }
}
