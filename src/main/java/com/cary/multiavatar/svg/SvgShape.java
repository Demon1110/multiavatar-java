package com.cary.multiavatar.svg;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * SVG 形状基类（组合模式叶子节点）。
 *
 * <p>封装样式与可选变换，以模板方法统一「填充 + 描边 + 不透明度」的绘制流程，
 * 具体几何由子类 {@link #shape()} 提供（path/polygon/line/rect）。</p>
 */
public abstract class SvgShape {

    /**
     * 描边 miter 上限（SVG 默认 4）。
     */
    private static final float MITER_LIMIT = 4f;

    protected final SvgStyle style;
    protected final AffineTransform transform;

    protected SvgShape(SvgStyle style, AffineTransform transform) {
        this.style = style;
        this.transform = transform;
    }

    public SvgStyle style() {
        return style;
    }

    public AffineTransform transform() {
        return transform;
    }

    /**
     * 绘制本形状（模板方法）：先填充后描边，应用元素级不透明度与矩阵变换。
     */
    public final void paint(Graphics2D g) {
        Shape s = shape();
        if (s == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (transform != null) {
                g2.transform(transform);
            }
            if (style.opacity() < 1.0) {
                g2.setComposite(AlphaComposite.SrcOver.derive((float) style.opacity()));
            }

            // SVG 语义：先填充，再描边
            if (!style.fill().isNone()) {
                g2.setColor(style.fill().toAwt());
                g2.fill(s);
            }
            if (!style.stroke().isNone() && style.strokeWidth() > 0) {
                g2.setColor(style.stroke().toAwt());
                g2.setStroke(new BasicStroke(
                        (float) style.strokeWidth(),
                        style.lineCap(),
                        style.lineJoin(),
                        MITER_LIMIT));
                g2.draw(s);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 本形状的几何（可为 null 表示无几何可绘）。
     */
    protected abstract Shape shape();
}
