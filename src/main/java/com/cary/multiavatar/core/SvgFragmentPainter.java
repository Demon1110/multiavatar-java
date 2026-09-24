package com.cary.multiavatar.core;

import com.cary.multiavatar.data.DataTables;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG 片段着色器：用主题颜色按序替换部件模板中的 {@code #xxx;} 颜色占位符。
 *
 * <p>对应 multiavatar.js 的 getFinal()。为保持与 JS 输出逐字符一致，严格复现其
 * {@code resultFinal.replace(result[i], colors[i]+';')} 语义 —— 每次只替换第一个匹配，
 * 且只替换占位符数量个颜色（多余颜色按 JS 固有行为忽略）。</p>
 */
public final class SvgFragmentPainter {

    /**
     * 对应 JS 的 /#(.*?);/g。
     */
    private static final Pattern COLOR_PLACEHOLDER = Pattern.compile("#([^;]*);");

    private final DataTables tables;

    public SvgFragmentPainter(DataTables tables) {
        this.tables = tables;
    }

    /**
     * 对指定部件的模板着色。
     *
     * @param partName 部件名（env/head/clo/mouth/eyes/top）
     * @param partV    初始角色编号（如 "00"）
     * @param theme    颜色主题（"A"/"B"/"C"）
     * @return 着色后的 SVG 片段；模板缺失时返回空串
     */
    public String paint(String partName, String partV, String theme) {
        List<String> colors = tables.colorsOf(partV + theme, partName);
        String svg = tables.templateOf(partV, partName);
        if (svg == null) {
            return "";
        }

        // 提取所有 #xxx; 占位符（顺序保持）
        Matcher m = COLOR_PLACEHOLDER.matcher(svg);
        List<String> placeholders = new ArrayList<>();
        while (m.find()) {
            placeholders.add(m.group(0));
        }

        String out = svg;
        for (int i = 0; i < placeholders.size(); i++) {
            out = out.replaceFirst(
                    Pattern.quote(placeholders.get(i)),
                    Matcher.quoteReplacement(colors.get(i) + ";"));
        }
        return out;
    }
}
