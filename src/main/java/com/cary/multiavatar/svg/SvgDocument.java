package com.cary.multiavatar.svg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SVG 文档模型（不可变）：viewBox 视口与按文档顺序排列的形状列表。
 */
public final class SvgDocument {

    private final double viewX;
    private final double viewY;
    private final double viewWidth;
    private final double viewHeight;
    private final List<SvgShape> shapes;

    public SvgDocument(double viewX, double viewY, double viewWidth, double viewHeight,
                       List<SvgShape> shapes) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.shapes = Collections.unmodifiableList(new ArrayList<>(shapes));
    }

    public double viewX() {
        return viewX;
    }

    public double viewY() {
        return viewY;
    }

    public double viewWidth() {
        return viewWidth;
    }

    public double viewHeight() {
        return viewHeight;
    }

    /**
     * 按文档顺序排列的形状（只读）。
     */
    public List<SvgShape> shapes() {
        return shapes;
    }
}
