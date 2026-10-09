package com.cary.multiavatar.render;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * GIF 编码器：多帧 BufferedImage → GIF 字节（JDK ImageIO 内置 GIF writer，零依赖）。
 *
 * <p>支持每帧延迟与 Netscape 无限循环；帧图可带透明通道（GIF 自动处理为透明色）。
 * 用于部件逐帧淡入的动画头像（{@link GifAvatarRenderer}）。
 * 注意：JDK GIF writer 的 Netscape 循环扩展放在「每帧 image metadata」的
 * ApplicationExtensions 节点（stream 级 native 树不支持），故仅首帧携带循环标记；
 * 且 ImageOutputStream 需在 writer.dispose()/close 后才完成 flush，字节在全部
 * 资源释放后再取。</p>
 */
public final class GifWriter {

    private GifWriter() {
    }

    /**
     * 编码为 GIF 字节（无限循环）。
     *
     * @param frames  帧序列（非空）
     * @param delayMs 每帧停留毫秒（约 10ms 粒度，最小 1ms）
     */
    public static byte[] toGif(List<BufferedImage> frames, int delayMs) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("frames 不能为空");
        }
        ImageWriter writer = null;
        ImageOutputStream ios = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(frames.size() * 64 * 1024);
        RuntimeException failure = null;
        try {
            writer = ImageIO.getImageWritersBySuffix("gif").next();
            ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = frames.get(i);
                IIOMetadata meta = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(frame), param);
                meta = withDelay(meta, delayMs);
                if (i == 0) {
                    meta = withLoop(meta);
                }
                writer.writeToSequence(new IIOImage(frame, null, meta), param);
            }
            writer.endWriteSequence();
        } catch (IOException | IllegalArgumentException e) {
            failure = new IllegalStateException("GIF 编码失败: " + e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.dispose(); // 触发 flush 到 out
                } catch (RuntimeException ignored) {
                    // 忽略释放异常
                }
            }
            if (ios != null) {
                try {
                    ios.close();
                } catch (IOException ignored) {
                    // 忽略关闭异常
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return out.toByteArray();
    }

    /**
     * 在 image metadata 中设置 Netscape 无限循环（首帧）。
     */
    private static IIOMetadata withLoop(IIOMetadata meta) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_gif_image_1.0");
            IIOMetadataNode appExts = (IIOMetadataNode) root.getElementsByTagName("ApplicationExtensions").item(0);
            if (appExts == null) {
                appExts = new IIOMetadataNode("ApplicationExtensions");
                root.appendChild(appExts);
            }
            IIOMetadataNode appExt = new IIOMetadataNode("ApplicationExtension");
            appExt.setAttribute("applicationID", "NETSCAPE");
            appExt.setAttribute("authenticationCode", "2.0");
            appExt.setUserObject(new byte[]{1, 0, 0}); // loop count 0 = 无限循环
            appExts.appendChild(appExt);
            meta.mergeTree("javax_imageio_gif_image_1.0", root);
            return meta;
        } catch (javax.imageio.metadata.IIOInvalidTreeException e) {
            throw new IllegalStateException("GIF 循环元数据写入失败", e);
        }
    }

    /**
     * 设置单帧延迟（image metadata 的 GraphicControlExtension）。
     */
    private static IIOMetadata withDelay(IIOMetadata meta, int delayMs) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_gif_image_1.0");
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            if (gce != null) {
                gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "FALSE");
                gce.setAttribute("delayTime", Integer.toString(Math.max(1, delayMs / 10)));
            }
            meta.mergeTree("javax_imageio_gif_image_1.0", root);
            return meta;
        } catch (javax.imageio.metadata.IIOInvalidTreeException e) {
            throw new IllegalStateException("GIF 帧延迟写入失败", e);
        }
    }
}
