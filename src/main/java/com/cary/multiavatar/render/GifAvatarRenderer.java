package com.cary.multiavatar.render;

import com.cary.multiavatar.svg.SvgDocument;
import com.cary.multiavatar.svg.SvgParser;
import com.cary.multiavatar.svg.SvgShape;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GIF 渲染策略：把 SVG 拆成「部件逐帧淡入」动画并编码为 GIF 字节。
 *
 * <p>形状按文档顺序即部件组装顺序（env → head → clo → top → eyes → mouth），
 * 对第 k 个形状依次生成「半透明淡入帧 + 全显帧」，每出现一个部件就定格一拍，
 * 形成头像逐步「长出来」的循环动画。帧透明与元素自身 opacity 相乘，颜色语义与静态渲染一致。</p>
 *
 * <p><b>首帧固定为完整头像</b>（全部形状全显）：资源管理器等外壳预览只取 GIF 第一帧
 * 做缩略图，若首帧是几乎透明的淡入帧会被渲染成全黑，因此首帧用完整头像保证
 * 「不打开也能看到完整图像」。</p>
 */
public final class GifAvatarRenderer implements AvatarRenderer {

    /**
     * 新部件淡入的起始不透明度。
     */
    static final double FADE_IN_ALPHA = 0.45;

    /**
     * 每帧停留毫秒。
     */
    static final int FRAME_DELAY_MS = 100;

    @Override
    public byte[] render(String svg, int size) {
        SvgDocument doc = SvgParser.parse(svg);
        List<SvgShape> shapes = doc.shapes();
        if (shapes.isEmpty()) {
            return new byte[0];
        }
        // 首帧 = 完整头像（供外壳缩略图预览），随后是部件逐帧淡入动画
        List<BufferedImage> frames = new ArrayList<>(shapes.size() * 2 + 1);
        frames.add(SvgRasterizer.rasterize(doc, size, shapes.size(), 1.0));
        for (int k = 1; k <= shapes.size(); k++) {
            frames.add(SvgRasterizer.rasterize(doc, size, k, FADE_IN_ALPHA));
            frames.add(SvgRasterizer.rasterize(doc, size, k, 1.0));
        }
        return GifWriter.toGif(frames, FRAME_DELAY_MS);
    }
}
