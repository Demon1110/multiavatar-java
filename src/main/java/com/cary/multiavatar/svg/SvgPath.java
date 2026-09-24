package com.cary.multiavatar.svg;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

/**
 * SVG {@code <path>} 元素：几何为已解析的 {@link Path2D}（含全部 path 指令，弧线已转贝塞尔）。
 */
public final class SvgPath extends SvgShape {

    private final Path2D path;

    public SvgPath(Path2D path, SvgStyle style, AffineTransform transform) {
        super(style, transform);
        this.path = path;
    }

    @Override
    protected Shape shape() {
        return path;
    }
}
