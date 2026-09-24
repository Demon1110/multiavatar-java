package com.cary.multiavatar.svg;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

/**
 * SVG {@code <polygon>} 元素：points 点列构成闭合多边形。
 */
public final class SvgPolygon extends SvgShape {

    private final Path2D path;

    public SvgPolygon(Path2D path, SvgStyle style, AffineTransform transform) {
        super(style, transform);
        this.path = path;
    }

    @Override
    protected Shape shape() {
        return path;
    }
}
