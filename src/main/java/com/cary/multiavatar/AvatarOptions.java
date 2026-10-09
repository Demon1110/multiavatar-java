package com.cary.multiavatar;

import com.cary.multiavatar.render.AvatarFormat;

/**
 * 头像生成选项（不可变），通过 {@link #builder()} 构建。
 *
 * <pre>
 * AvatarOptions options = AvatarOptions.builder()
 *         .sansEnv(true)          // 去掉背景圆
 *         .part("00")             // 强制初始角色 00-15（对应 JS 的 ver.part）
 *         .theme("A")             // 强制颜色主题 A/B/C（对应 JS 的 ver.theme）
 *         .size(512)              // PNG 输出边长（像素）
 *         .svgSize(256, 256)      // SVG 根元素默认宽高（像素）
 *         .format(AvatarFormat.PNG)
 *         .build();
 * </pre>
 */
public final class AvatarOptions {

    /**
     * 默认 PNG 输出边长。
     */
    public static final int DEFAULT_SIZE = 256;

    /**
     * 默认 SVG 根元素宽高（像素）。
     */
    public static final int DEFAULT_SVG_SIZE = 256;

    /**
     * 默认是否启用 SVG 体积优化（坐标精度裁剪，近无损）。
     */
    public static final boolean DEFAULT_OPTIMIZE_SVG = true;

    private final boolean sansEnv;
    private final String part;
    private final String theme;
    private final int size;
    private final int svgWidth;
    private final int svgHeight;
    private final AvatarFormat format;
    private final boolean optimizeSvg;

    private AvatarOptions(Builder b) {
        this.sansEnv = b.sansEnv;
        this.part = b.part;
        this.theme = b.theme;
        this.size = b.size;
        this.svgWidth = b.svgWidth;
        this.svgHeight = b.svgHeight;
        this.format = b.format;
        this.optimizeSvg = b.optimizeSvg;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 便捷构造：默认选项。
     */
    public static AvatarOptions defaults() {
        return builder().build();
    }

    public boolean sansEnv() {
        return sansEnv;
    }

    /**
     * 强制角色编号（"00"-"15"），null 表示自动。
     */
    public String part() {
        return part;
    }

    /**
     * 强制颜色主题（"A"/"B"/"C"），null 表示自动。
     */
    public String theme() {
        return theme;
    }

    /**
     * PNG 输出边长（像素）。
     */
    public int size() {
        return size;
    }

    /**
     * SVG 根元素宽度（像素）。
     */
    public int svgWidth() {
        return svgWidth;
    }

    /**
     * SVG 根元素高度（像素）。
     */
    public int svgHeight() {
        return svgHeight;
    }

    public AvatarFormat format() {
        return format;
    }

    /**
     * 是否启用 SVG 体积优化（坐标精度裁剪，近无损，渲染几何等价）。
     */
    public boolean optimizeSvg() {
        return optimizeSvg;
    }

    /**
     * AvatarOptions 构建者（Builder 模式）。
     */
    public static final class Builder {
        private boolean sansEnv = false;
        private String part;
        private String theme;
        private int size = DEFAULT_SIZE;
        private int svgWidth = DEFAULT_SVG_SIZE;
        private int svgHeight = DEFAULT_SVG_SIZE;
        private AvatarFormat format = AvatarFormat.SVG;
        private boolean optimizeSvg = DEFAULT_OPTIMIZE_SVG;

        private Builder() {
        }

        /**
         * 是否去掉环境部件（背景圆）。
         */
        public Builder sansEnv(boolean sansEnv) {
            this.sansEnv = sansEnv;
            return this;
        }

        /**
         * 强制初始角色编号（"00"-"15"），null 表示自动。
         */
        public Builder part(String part) {
            this.part = part;
            return this;
        }

        /**
         * 强制颜色主题（"A"/"B"/"C"），null 表示自动。
         */
        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        /**
         * PNG 输出边长（像素），仅对 PNG 格式生效。
         */
        public Builder size(int size) {
            this.size = Math.max(1, size);
            return this;
        }

        /**
         * SVG 根元素宽高（像素），仅对 SVG 文本输出生效。
         */
        public Builder svgSize(int width, int height) {
            this.svgWidth = Math.max(1, width);
            this.svgHeight = Math.max(1, height);
            return this;
        }

        /**
         * 输出格式。
         */
        public Builder format(AvatarFormat format) {
            this.format = format;
            return this;
        }

        /**
         * 是否启用 SVG 体积优化（默认开）：坐标精度裁剪（四舍五入到 1 位小数）、
         * style 尾分号删除，近无损、渲染几何等价；设 {@code false} 可获得与官方 JS 逐字符一致的原文。
         */
        public Builder optimizeSvg(boolean optimizeSvg) {
            this.optimizeSvg = optimizeSvg;
            return this;
        }

        public AvatarOptions build() {
            return new AvatarOptions(this);
        }
    }
}
