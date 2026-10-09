package com.cary.multiavatar.render;

/**
 * 头像输出格式。
 */
public enum AvatarFormat {
    /**
     * SVG 文本。
     */
    SVG,
    /**
     * PNG 图片字节（透明背景）。
     */
    PNG,
    /**
     * JPEG 图片字节（白色背景，不支持透明）。
     */
    JPG,
    /**
     * GIF 动画字节：部件逐帧淡入，无限循环。
     */
    GIF
}
