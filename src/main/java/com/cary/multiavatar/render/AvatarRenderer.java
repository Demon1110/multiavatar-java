package com.cary.multiavatar.render;

/**
 * 头像渲染策略：把 SVG 文本渲染为指定输出。
 *
 * <p>实现：{@link SvgAvatarRenderer}（原样返回 SVG 文本）、{@link PngAvatarRenderer}
 * （光栅化并编码为 PNG 字节）。新增输出格式只需实现本接口并由 {@link Renderers} 注册。</p>
 */
public interface AvatarRenderer {

    /**
     * 渲染。
     *
     * @param svg  完整 SVG 文本（非空）
     * @param size 目标边长（像素，SVG 渲染器可忽略）
     * @return 渲染结果字节
     */
    byte[] render(String svg, int size);
}
