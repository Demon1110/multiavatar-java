package com.cary.multiavatar.svg;

import java.util.HashMap;
import java.util.Map;

/**
 * SVG 展示样式（不可变）：fill、stroke、stroke-width、线帽、线连接与元素级不透明度。
 *
 * <p>解析自元素的 {@code style} 属性（如
 * {@code fill:#fff;stroke:#000;stroke-width:3px;stroke-linecap:round;stroke-linejoin:round}）。
 * 颜色替换后可能出现内嵌的 {@code opacity:0.96} 键，同样按键值对解析。</p>
 */
public final class SvgStyle {

    /**
     * 未指定时的默认描边宽度（SVG 默认 1）。
     */
    private static final double DEFAULT_STROKE_WIDTH = 1.0;

    private final SvgColor fill;
    private final SvgColor stroke;
    private final double strokeWidth;
    private final int lineCap;      // BasicStroke.CAP_*
    private final int lineJoin;     // BasicStroke.JOIN_*
    private final double opacity;   // 0.0 - 1.0

    private SvgStyle(SvgColor fill, SvgColor stroke, double strokeWidth,
                     int lineCap, int lineJoin, double opacity) {
        this.fill = fill;
        this.stroke = stroke;
        this.strokeWidth = strokeWidth;
        this.lineCap = lineCap;
        this.lineJoin = lineJoin;
        this.opacity = opacity;
    }

    /**
     * 空样式（fill 黑、无 stroke、不透明）。
     */
    public static SvgStyle empty() {
        return new SvgStyle(SvgColor.parse("#000"), SvgColor.NONE,
                DEFAULT_STROKE_WIDTH, java.awt.BasicStroke.CAP_BUTT,
                java.awt.BasicStroke.JOIN_MITER, 1.0);
    }

    /**
     * 解析 style 属性字符串（可为 null/空，此时返回 {@link #empty()}）。
     */
    public static SvgStyle parse(String style) {
        if (style == null || style.trim().isEmpty()) {
            return empty();
        }

        Map<String, String> kv = new HashMap<>();
        for (String pair : style.split(";")) {
            int idx = pair.indexOf(':');
            if (idx < 0) {
                continue;
            }
            kv.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
        }

        SvgColor fill = value(kv, "fill", SvgColor.parse("#000"));
        SvgColor stroke = value(kv, "stroke", SvgColor.NONE);
        double width = parseLength(kv.get("stroke-width"), DEFAULT_STROKE_WIDTH);
        int cap = "round".equals(kv.get("stroke-linecap"))
                ? java.awt.BasicStroke.CAP_ROUND : java.awt.BasicStroke.CAP_BUTT;
        int join = "round".equals(kv.get("stroke-linejoin"))
                ? java.awt.BasicStroke.JOIN_ROUND : java.awt.BasicStroke.JOIN_MITER;
        double opacity = parseOpacity(kv.get("opacity"));

        return new SvgStyle(fill, stroke, width, cap, join, opacity);
    }

    private static SvgColor value(Map<String, String> kv, String key, SvgColor dft) {
        String v = kv.get(key);
        return v == null ? dft : SvgColor.parse(v);
    }

    /**
     * 解析长度（去掉 px 等单位后缀）。
     */
    private static double parseLength(String raw, double dft) {
        if (raw == null) {
            return dft;
        }
        String s = raw.trim();
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i))
                || s.charAt(i) == '.' || s.charAt(i) == '-' || s.charAt(i) == '+')) {
            i++;
        }
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return dft;
        }
    }

    /**
     * 解析 0-1 的不透明度。
     */
    private static double parseOpacity(String raw) {
        if (raw == null) {
            return 1.0;
        }
        try {
            double v = Double.parseDouble(raw.trim());
            return v < 0 ? 0 : (v > 1 ? 1 : v);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    public SvgColor fill() {
        return fill;
    }

    public SvgColor stroke() {
        return stroke;
    }

    public double strokeWidth() {
        return strokeWidth;
    }

    public int lineCap() {
        return lineCap;
    }

    public int lineJoin() {
        return lineJoin;
    }

    public double opacity() {
        return opacity;
    }
}
