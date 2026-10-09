package com.cary.multiavatar.svg;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

/**
 * SVG {@code <line>} 元素：两端点连线，仅描边不填充。
 */
public final class SvgLine extends SvgShape {

    private final Line2D line;

    public SvgLine(double x1, double y1, double x2, double y2,
                   SvgStyle style, AffineTransform transform) {
        super(style, transform);
        this.line = new Line2D.Double(x1, y1, x2, y2);
    }

    private SvgLine(Line2D line, SvgStyle style, AffineTransform transform, double alpha) {
        super(style, transform, alpha);
        this.line = line;
    }

    @Override
    public SvgLine withAlpha(double alpha) {
        return new SvgLine(line, style, transform, alpha);
    }

    @Override
    protected Shape shape() {
        return line;
    }
}
