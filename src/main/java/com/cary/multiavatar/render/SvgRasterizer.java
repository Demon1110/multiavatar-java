package com.cary.multiavatar.render;

import com.cary.multiavatar.svg.SvgDocument;
import com.cary.multiavatar.svg.SvgParser;
import com.cary.multiavatar.svg.SvgShape;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * SVG 光栅化器：把 SVG 文本绘制到 {@link BufferedImage}（Java2D，JDK 内置）。
 *
 * <p>流程：解析为 {@link SvgDocument} → 按 viewBox 等比缩放并居中映射到画布 →
 * 按文档顺序绘制每个形状（先填充后描边，支持元素级不透明度与矩阵变换）。
 * 画布为 ARGB（透明背景），与浏览器渲染语义一致。</p>
 */
public final class SvgRasterizer {

    private SvgRasterizer() {
    }

    /**
     * 光栅化。
     *
     * @param svg  完整 SVG 文本（非空，由调用方保证）
     * @param size 画布边长（像素，>0）
     */
    public static BufferedImage rasterize(String svg, int size) {
        SvgDocument doc = SvgParser.parse(svg);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double vw = doc.viewWidth();
            double vh = doc.viewHeight();
            double scale = (vw > 0 && vh > 0)
                    ? Math.min(size / vw, size / vh)
                    : 1.0;
            double tx = (size - vw * scale) / 2 - doc.viewX() * scale;
            double ty = (size - vh * scale) / 2 - doc.viewY() * scale;

            g.translate(tx, ty);
            g.scale(scale, scale);
            for (SvgShape shape : doc.shapes()) {
                shape.paint(g);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
