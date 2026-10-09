package com.cary.multiavatar.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG 体积优化器（纯文本级处理，不解析 DOM，保持其余部分逐字符不变）。
 *
 * <p>优化项（均近无损，渲染几何等价）：</p>
 * <ul>
 *   <li><b>坐标精度裁剪</b>：{@code d="..."} 内数字四舍五入——普通填充形状到 1 位小数，
 *       含描边（{@code stroke:}）的细线条形状到 2 位小数（线条对亚像素偏移更敏感，2 位误差 ≤0.006px）；
 *       {@code transform="..."} 矩阵与 {@code points="..."} 多边形坐标统一 1 位；
 *       尾零去除、整数不带小数点、负零归一。
 *       官方数据坐标普遍 2~5 位小数，头像 231 坐标系下 0.05 单位误差
 *       ≈ 256px 输出的 0.06px，视觉不可察。</li>
 *   <li><b>style 尾分号删除</b>：{@code fill:#333;} → {@code fill:#333}（CSS 允许无尾分号）。</li>
 * </ul>
 *
 * <p>注：官方模板中不存在「{@code fill:none} 且无 {@code stroke}」的真隐形形状——
 * 48 个 {@code fill:none} 形状全部带描边（可见线条），故无形状可删。</p>
 */
public final class SvgOptimizer {

    /**
     * 形状标签：<名 前缀… d="…" 中间… style="…" 后缀…>；替换只动 d 与 style 值，保留全部其他属性。
     */
    private static final Pattern SHAPE_TAG = Pattern.compile(
            "((?:path|polygon|rect|line)[^>]*?)\\bd=\"([^\"]*)\"([^>]*?)\\bstyle=\"([^\"]*)\"");

    /**
     * transform 属性值。
     */
    private static final Pattern TRANSFORM_VALUE = Pattern.compile("\\btransform=\"([^\"]*)\"");

    /**
     * points 属性值（多边形坐标）。
     */
    private static final Pattern POINTS_VALUE = Pattern.compile("\\bpoints=\"([^\"]*)\"");

    /**
     * 数字 token：整数 / 小数 / 科学计数法。
     */
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    private SvgOptimizer() {
    }

    /**
     * 优化 SVG 文本（近无损）；空串原样返回。
     */
    public static String optimize(String svg) {
        if (svg == null || svg.isEmpty()) {
            return svg;
        }
        StringBuffer sb = new StringBuffer(svg.length());

        // 1. 形状标签：d 按是否描边取整（1 位 / 描边 2 位）
        Matcher m = SHAPE_TAG.matcher(svg);
        while (m.find()) {
            String d = roundAttributeValue(m.group(2), m.group(4).contains("stroke") ? 2 : 1);
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + " d=\"" + d
                    + "\"" + m.group(3) + " style=\"" + m.group(4) + "\""));
        }
        m.appendTail(sb);

        // 2. transform / points 数值 1 位
        String mid = sb.toString();
        sb = new StringBuffer(mid.length());
        Matcher t = TRANSFORM_VALUE.matcher(mid);
        while (t.find()) {
            t.appendReplacement(sb, Matcher.quoteReplacement("transform=\""
                    + roundAttributeValue(t.group(1), 1) + "\""));
        }
        t.appendTail(sb);
        mid = sb.toString();
        sb = new StringBuffer(mid.length());
        Matcher p = POINTS_VALUE.matcher(mid);
        while (p.find()) {
            p.appendReplacement(sb, Matcher.quoteReplacement("points=\""
                    + roundAttributeValue(p.group(1), 1) + "\""));
        }
        p.appendTail(sb);

        // 3. style 尾分号删除
        return sb.toString().replace(";\"", "\"");
    }

    private static String roundAttributeValue(String value, int decimals) {
        Matcher n = NUMBER.matcher(value);
        StringBuffer sb = new StringBuffer(value.length());
        while (n.find()) {
            n.appendReplacement(sb, Matcher.quoteReplacement(roundToken(n.group(), decimals)));
        }
        n.appendTail(sb);
        return sb.toString();
    }

    /**
     * 数字 token 四舍五入到指定小数位；整数/负零归一。
     *
     * @param decimals 保留小数位数（0/1/2）
     */
    public static String roundToken(String token, int decimals) {
        double v;
        try {
            v = Double.parseDouble(token);
        } catch (NumberFormatException e) {
            return token;
        }
        double mul = 1;
        for (int i = 0; i < decimals; i++) {
            mul *= 10;
        }
        double r = Math.round(v * mul) / mul;
        if (r == 0.0) {
            // 负数取整为 0 时必须保留负号：原文紧凑负号（如 "2.707-0.0428"）中
            // 负号是唯一的 token 分隔符，输出 "0" 会与前一数字粘连成 "2.70"（坐标丢失）；
            // "-0" 是合法 SVG 数字且永远保持分隔。
            return token.startsWith("-") ? "-0" : "0";
        }
        // 固定 decimals 位小数，再剥离尾零与尾点
        String s = String.format(java.util.Locale.ROOT, "%." + decimals + "f", r);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    /**
     * 数字 token 四舍五入到 1 位小数（兼容旧调用）。
     */
    public static String roundToken(String token) {
        return roundToken(token, 1);
    }
}
