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
     * SVG 根开标签（viewBox 取自构建配置）。
     */
    private static final String SVG_START =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"%s\">";
    private static final String SVG_END = "</svg>";

    private final AvatarIdHasher hasher;
    private final SvgFragmentPainter painter;
    private final String viewBox;

    /**
     * @param hasher  哈希策略（默认 {@link Sha256AvatarIdHasher}）
     * @param painter 着色器
     * @param viewBox SVG viewBox，如 "0 0 256 256"
     */
    public SvgComposer(AvatarIdHasher hasher, SvgFragmentPainter painter, String viewBox) {
        this.hasher = hasher;
        this.painter = painter;
        this.viewBox = viewBox;
    }

    /**
     * 便捷构造：SHA-256 + 默认数据源 + 指定 viewBox。
     */
    public static SvgComposer withViewBox(String viewBox) {
        return new SvgComposer(
                new Sha256AvatarIdHasher(),
                new SvgFragmentPainter(DataTables.INSTANCE),
                viewBox);
    }

    /**
     * 组装完整 SVG。
     *
     * @param input       输入字符串（null 视为空串）
     * @param sansEnv     是否去掉环境部件（背景圆）
     * @param forcedPart  强制角色编号，null 表示自动
     * @param forcedTheme 强制主题，null 表示自动
     * @return 完整 SVG；输入为空串时返回空串（与 JS 一致）
     */
    public String compose(String input, boolean sansEnv, String forcedPart, String forcedTheme) {
        if (input == null) {
            input = "";
        }
        if (input.isEmpty()) {
            return "";
        }

        AvatarSpec spec = AvatarSpec.resolve(input, hasher, forcedPart, forcedTheme);

        StringBuilder sb = new StringBuilder(4096);
        sb.append(String.format(SVG_START, viewBox));
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
