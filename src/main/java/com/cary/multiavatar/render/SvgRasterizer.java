package com.cary.multiavatar.render;

import com.cary.multiavatar.svg.SvgDocument;
import com.cary.multiavatar.svg.SvgParser;
import com.cary.multiavatar.svg.SvgShape;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * SVG 光栅化器：把 SVG 文本绘制到 {@link BufferedImage}（Java2D，JDK 内置）。
 *
 * <p>流程：解析为 {@link SvgDocument} → 按 viewBox 等比缩放并居中映射到画布 →
 * 按文档顺序绘制每个形状（先填充后描边，支持元素级不透明度与矩阵变换）。
 * 画布默认为 ARGB（透明背景），与浏览器渲染语义一致；也可指定背景色（如 JPG 白底）。</p>
 *
 * <p>另提供「部分形状」绘制重载（GIF 帧动画用）：只绘制文档中前 N 个形状，
 * 最后一个可应用帧级不透明度实现淡入，形状与颜色语义与完整渲染一致。</p>
 */
public final class SvgRasterizer {

    private SvgRasterizer() {
    }

    /**
     * 光栅化（透明背景）。
     *
     * @param svg  完整 SVG 文本（非空，由调用方保证）
     * @param size 画布边长（像素，>0）
     */
    public static BufferedImage rasterize(String svg, int size) {
        return rasterize(svg, size, null);
    }

    /**
     * 光栅化（可指定背景色；null 表示透明背景）。
     *
     * @param svg        完整 SVG 文本（非空）
     * @param size       画布边长（像素，>0）
     * @param background 背景色，null 表示透明（ARGB）
     */
    public static BufferedImage rasterize(String svg, int size, Color background) {
        SvgDocument doc = SvgParser.parse(svg);
        return rasterize(doc, size, doc.shapes().size(), 1.0, background);
    }

    /**
     * 光栅化部分形状（GIF 帧动画）：只绘制文档前 {@code shapeCount} 个形状，
     * 其中最新的一个（最后一个）应用帧级不透明度 {@code newestAlpha}（0-1，1 为全量显示）。
     *
     * @param doc         已解析的 SVG 文档
     * @param size        画布边长（像素，>0）
     * @param shapeCount  绘制的形状个数（0=空帧，>总数时取总数）
     * @param newestAlpha 最后一个形状的淡入不透明度
     */
    public static BufferedImage rasterize(SvgDocument doc, int size, int shapeCount, double newestAlpha) {
        return rasterize(doc, size, shapeCount, newestAlpha, null);
    }

    private static BufferedImage rasterize(SvgDocument doc, int size, int shapeCount,
                                           double newestAlpha, Color background) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            if (background != null) {
                g.setColor(background);
                g.fillRect(0, 0, size, size);
            }
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

            List<SvgShape> shapes = doc.shapes();
            int n = Math.min(shapeCount, shapes.size());
            for (int i = 0; i < n; i++) {
                SvgShape shape = shapes.get(i);
                if (i == n - 1 && newestAlpha < 1.0) {
                    shape = shape.withAlpha(newestAlpha);
                }
                shape.paint(g);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
