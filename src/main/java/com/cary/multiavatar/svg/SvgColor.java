package com.cary.multiavatar.svg;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SVG 颜色（不可变）。
 *
 * <p>支持 {@code #RGB}、{@code #RRGGBB} 十六进制与 CSS 命名色；{@code none} 表示不绘制
 * （对应 {@link #NONE} 单例）。未知颜色按黑色处理并在解析时忽略。</p>
 */
public final class SvgColor {

    /**
     * 表示 SVG 的 none（不填充/不描边）。
     */
    public static final SvgColor NONE = new SvgColor(null, true);

    /**
     * CSS 命名色表（本工程 SVG 数据实际用到的命名色）。
     */
    private static final Map<String, Color> NAMED = new HashMap<>();

    static {
        NAMED.put("black", Color.BLACK);
        NAMED.put("white", Color.WHITE);
        NAMED.put("red", Color.RED);
        NAMED.put("aqua", new Color(0x00FFFF));
        NAMED.put("magenta", new Color(0xFF00FF));
        NAMED.put("yellow", Color.YELLOW);
        NAMED.put("cyan", new Color(0x00FFFF));
        NAMED.put("gray", new Color(0x808080));
        NAMED.put("grey", new Color(0x808080));
        NAMED.put("silver", new Color(0xC0C0C0));
        NAMED.put("blue", Color.BLUE);
        NAMED.put("green", Color.GREEN);
        NAMED.put("orange", new Color(0xFFA500));
        NAMED.put("purple", new Color(0x800080));
    }

    private final Color awtColor;
    private final boolean none;

    private SvgColor(Color awtColor) {
        this(awtColor, false);
    }

    private SvgColor(Color awtColor, boolean none) {
        this.awtColor = awtColor;
        this.none = none;
    }

    /**
     * 解析颜色字符串。
     *
     * @param value 如 "#fff"、"#884f00"、"black"、"none"（null/空 按 none 处理）
     */
    public static SvgColor parse(String value) {
        if (value == null) {
            return NONE;
        }
        String v = value.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("none") || v.equalsIgnoreCase("transparent")) {
            return NONE;
        }
        if (v.startsWith("#")) {
            String hex = v.substring(1);
            try {
                if (hex.length() == 3) {
                    int r = Integer.parseInt(hex.substring(0, 1), 16);
                    int g = Integer.parseInt(hex.substring(1, 2), 16);
                    int b = Integer.parseInt(hex.substring(2, 3), 16);
                    return new SvgColor(new Color(r * 17, g * 17, b * 17));
                }
                if (hex.length() == 6) {
                    return new SvgColor(new Color(Integer.parseInt(hex, 16)));
                }
                if (hex.length() == 8) {
                    // 非标准 8 位写法：取前 6 位作为 RGB
                    return new SvgColor(new Color(Integer.parseInt(hex.substring(0, 6), 16)));
                }
            } catch (NumberFormatException ignored) {
                // 非法 hex 按黑色处理
            }
            return new SvgColor(Color.BLACK);
        }
        Color named = NAMED.get(v.toLowerCase());
        return named != null ? new SvgColor(named) : new SvgColor(Color.BLACK);
    }

    /**
     * 是否为 none（不绘制）。
     */
    public boolean isNone() {
        return none;
    }

    /**
     * 对应的 AWT 颜色（none 时返回 null）。
     */
    public Color toAwt() {
        return awtColor;
    }
}
