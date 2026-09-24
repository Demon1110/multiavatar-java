package com.cary.multiavatar.core;

import com.cary.multiavatar.data.DataTables;

/**
 * SVG 组装器：固定「推导规格 → 逐部件着色 → 按序拼装」的组装流程（模板方法）。
 *
 * <p>流程骨架固定，可替换的步骤（哈希策略、着色器、数据源）通过构造注入：
 * <ol>
 *   <li>由输入推导 {@link AvatarSpec}（哪一部件用哪个角色/主题）；</li>
 *   <li>逐部件调用 {@link SvgFragmentPainter} 着色；</li>
 *   <li>按 {@link AvatarSpec#PART_ORDER} 拼装为完整 SVG。</li>
 * </ol>
 * </p>
 */
public final class SvgComposer {

    /**
     * 默认 SVG 宽高（像素）。
     */
    public static final int DEFAULT_SVG_SIZE = 200;
    private static final String SVG_END = "</svg>";
    /**
     * SVG 根开标签（width/height 与 viewBox 取自构建配置）。
     */
    private static final String SVG_START =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"%s\">";

    private final AvatarIdHasher hasher;
    private final SvgFragmentPainter painter;
    private final String viewBox;
    private final int svgWidth;
    private final int svgHeight;

    /**
     * @param hasher    哈希策略（默认 {@link Sha256AvatarIdHasher}）
     * @param painter   着色器
     * @param viewBox   SVG viewBox，如 "0 0 256 256"
     * @param svgWidth  SVG 根元素默认宽度（像素）
     * @param svgHeight SVG 根元素默认高度（像素）
     */
    public SvgComposer(AvatarIdHasher hasher, SvgFragmentPainter painter, String viewBox,
                       int svgWidth, int svgHeight) {
        this.hasher = hasher;
        this.painter = painter;
        this.viewBox = viewBox;
        this.svgWidth = svgWidth;
        this.svgHeight = svgHeight;
    }

    /**
     * 便捷构造：SHA-256 + 默认数据源 + 指定 viewBox，宽高取默认 200×200。
     */
    public static SvgComposer withViewBox(String viewBox) {
        return new SvgComposer(
                new Sha256AvatarIdHasher(),
                new SvgFragmentPainter(DataTables.INSTANCE),
                viewBox, DEFAULT_SVG_SIZE, DEFAULT_SVG_SIZE);
    }

    /**
     * 便捷构造：SHA-256 + 默认数据源 + 指定 viewBox 与默认宽高。
     */
    public static SvgComposer withViewBox(String viewBox, int width, int height) {
        return new SvgComposer(
                new Sha256AvatarIdHasher(),
                new SvgFragmentPainter(DataTables.INSTANCE),
                viewBox, width, height);
    }

    /**
     * 组装完整 SVG（使用本实例默认宽高）。
     *
     * @param input       输入字符串（null 视为空串）
     * @param sansEnv     是否去掉环境部件（背景圆）
     * @param forcedPart  强制角色编号，null 表示自动
     * @param forcedTheme 强制主题，null 表示自动
     * @return 完整 SVG；输入为空串时返回空串（与 JS 一致）
     */
    public String compose(String input, boolean sansEnv, String forcedPart, String forcedTheme) {
        return compose(input, sansEnv, forcedPart, forcedTheme, svgWidth, svgHeight);
    }

    /**
     * 组装完整 SVG（自定义根元素宽高）。
     *
     * @param input       输入字符串（null 视为空串）
     * @param sansEnv     是否去掉环境部件（背景圆）
     * @param forcedPart  强制角色编号，null 表示自动
     * @param forcedTheme 强制主题，null 表示自动
     * @param width       SVG 根元素宽度（像素）
     * @param height      SVG 根元素高度（像素）
     * @return 完整 SVG；输入为空串时返回空串（与 JS 一致）
     */
    public String compose(String input, boolean sansEnv, String forcedPart, String forcedTheme,
                          int width, int height) {
        if (input == null) {
            input = "";
        }
        if (input.isEmpty()) {
            return "";
        }

        AvatarSpec spec = AvatarSpec.resolve(input, hasher, forcedPart, forcedTheme);

        StringBuilder sb = new StringBuilder(4096);
        sb.append(String.format(SVG_START, width, height, viewBox));
        for (String partName : AvatarSpec.PART_ORDER) {
            if (sansEnv && "env".equals(partName)) {
                continue; // JS: final['env'] = ''
            }
            sb.append(painter.paint(partName, spec.partVOf(partName), spec.themeOf(partName)));
        }
        sb.append(SVG_END);
        return sb.toString();
    }
}
