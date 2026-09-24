package com.cary.multiavatar.render;

import java.nio.charset.StandardCharsets;

/**
 * SVG 渲染策略：原样返回 SVG 文本（UTF-8 字节）。
 */
public final class SvgAvatarRenderer implements AvatarRenderer {

    @Override
    public byte[] render(String svg, int size) {
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
