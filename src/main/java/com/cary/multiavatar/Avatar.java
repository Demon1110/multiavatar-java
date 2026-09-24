package com.cary.multiavatar;

import com.cary.multiavatar.render.AvatarFormat;
import com.cary.multiavatar.render.AvatarRenderer;
import com.cary.multiavatar.render.Renderers;

/**
 * 头像产物（不可变）：封装输入字符串对应的 SVG 文本，并按需提供 PNG 字节。
 *
 * <p>通过 {@link Multiavatar#avatar(String)} 或 {@link Multiavatar#avatar(String, AvatarOptions)} 获取。
 * SVG 生成后缓存，PNG 按请求尺寸惰性渲染（渲染器策略 {@link AvatarRenderer}）。</p>
 */
public final class Avatar {

    private final String input;
    private final AvatarOptions options;
    private final String svg;

    Avatar(String input, AvatarOptions options, String svg) {
        this.input = input;
        this.options = options;
        this.svg = svg;
    }

    /**
     * 输入字符串。
     */
    public String input() {
        return input;
    }

    /**
     * 生成时使用的选项。
     */
    public AvatarOptions options() {
        return options;
    }

    /**
     * SVG 文本（空输入时为空串）。
     */
    public String svg() {
        return svg;
    }

    /**
     * 是否有内容（输入为空时无内容）。
     */
    public boolean isEmpty() {
        return svg.isEmpty();
    }

    /**
     * PNG 字节（默认尺寸 256）；空输入返回空数组。
     */
    public byte[] png() {
        return png(options.size());
    }

    /**
     * PNG 字节（指定边长）；空输入返回空数组。
     */
    public byte[] png(int size) {
        if (isEmpty()) {
            return new byte[0];
        }
        AvatarRenderer renderer = Renderers.create(AvatarFormat.PNG);
        return renderer.render(svg, size);
    }
}
