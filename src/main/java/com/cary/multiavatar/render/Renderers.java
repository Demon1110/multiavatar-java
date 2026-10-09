package com.cary.multiavatar.render;

/**
 * 渲染器简单工厂：按 {@link AvatarFormat} 创建对应策略实例。
 *
 * <p>新增输出格式时：实现 {@link AvatarRenderer}，并在此注册。</p>
 */
public final class Renderers {

    private Renderers() {
    }

    public static AvatarRenderer create(AvatarFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("format 不能为 null");
        }
        switch (format) {
            case SVG:
                return new SvgAvatarRenderer();
            case PNG:
                return new PngAvatarRenderer();
            case JPG:
                return new JpgAvatarRenderer();
            case GIF:
                return new GifAvatarRenderer();
            default:
                throw new IllegalArgumentException("不支持的格式: " + format);
        }
    }
}
