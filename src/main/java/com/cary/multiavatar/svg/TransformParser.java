package com.cary.multiavatar.svg;

import java.awt.geom.AffineTransform;

/**
 * SVG transform 属性解析器。
 *
 * <p>本工程 SVG 数据仅使用 {@code matrix(a b c d e f)}（参数可能为科学计数法，
 * 如 {@code matrix(1 0 0 .99987 4e-5 -3e-5)}）。</p>
 */
public final class TransformParser {

    private TransformParser() {
    }

    /**
     * 解析 transform 属性；null/空/未知形式返回 null（表示无变换）。
     */
    public static AffineTransform parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (v.startsWith("matrix(")) {
            String inner = v.substring("matrix(".length(), v.length() - 1).trim();
            String[] parts = inner.split("[,\\s]+");
            if (parts.length >= 6) {
                try {
                    return new AffineTransform(
                            Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                            Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
