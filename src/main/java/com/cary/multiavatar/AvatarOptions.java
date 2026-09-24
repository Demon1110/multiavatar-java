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
 *         .format(AvatarFormat.PNG)
 *         .build();
 * </pre>
 */
public final class AvatarOptions {

    /**
     * 默认 PNG 输出边长。
     */
    public static final int DEFAULT_SIZE = 256;

    private final boolean sansEnv;
    private final String part;
    private final String theme;
    private final int size;
    private final AvatarFormat format;

    private AvatarOptions(Builder b) {
        this.sansEnv = b.sansEnv;
        this.part = b.part;
        this.theme = b.theme;
        this.size = b.size;
        this.format = b.format;
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

    public AvatarFormat format() {
        return format;
    }

    /**
     * AvatarOptions 构建者（Builder 模式）。
     */
    public static final class Builder {
        private boolean sansEnv = false;
        private String part;
        private String theme;
        private int size = DEFAULT_SIZE;
        private AvatarFormat format = AvatarFormat.SVG;

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
         * 输出格式。
         */
        public Builder format(AvatarFormat format) {
            this.format = format;
            return this;
        }

        public AvatarOptions build() {
            return new AvatarOptions(this);
        }
    }
}
