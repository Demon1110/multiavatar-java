package com.cary.multiavatar.svg;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * SVG {@code <rect>} 元素：支持圆角（rx/ry，缺省时互为相等）。
 */
public final class SvgRect extends SvgShape {

    private final RoundRectangle2D rect;

    public SvgRect(double x, double y, double width, double height,
                   double rx, double ry, SvgStyle style, AffineTransform transform) {
        super(style, transform);
        // SVG：仅给 ry 时 rx=ry，仅给 rx 时 ry=rx
        double arc = (rx > 0 ? rx : ry) > 0 ? (rx > 0 ? rx : ry) : 0;
        this.rect = new RoundRectangle2D.Double(x, y, width, height, arc * 2, arc * 2);
    }

    @Override
    protected Shape shape() {
        return rect;
    }
}
