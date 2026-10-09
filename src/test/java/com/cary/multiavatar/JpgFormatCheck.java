package com.cary.multiavatar;

import com.cary.multiavatar.render.AvatarFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * JPG 输出专项验证（零依赖，main + 断言；任一断言失败即抛异常并退出码非 0）。
 *
 * <p>覆盖：可解码/尺寸/白底/无 alpha/角像素、多尺寸、多输入、空输入、
 * {@link Avatar#jpg()} 与通用 {@link Multiavatar#toJpg(input)} 入口、
 * 以及与 PNG（透明底铺白）的形状一致性（JPEG 有损，形状差异阈值放宽）。</p>
 *
 * <p>运行：<pre>java -cp target/classes;target/test-classes com.cary.multiavatar.JpgFormatCheck</pre></p>
 */
public final class JpgFormatCheck {

    private static final String[] SAMPLES = {"Binx Bond", "test", "张三", "user@example.com", "123456789"};

    public static void main(String[] args) throws Exception {
        // 1. 基本渲染：非空、可解码、尺寸 256×256、无 alpha
        byte[] jpg = Multiavatar.toJpg("Binx Bond");
        check(jpg.length > 1000, "toJpg 字节非空");
        BufferedImage img = decode(jpg);
        check(img.getWidth() == 256 && img.getHeight() == 256, "默认尺寸 256×256");
        check(!img.getColorModel().hasAlpha(), "JPG 不应有 alpha 通道");

        // 2. 白底：四角为白色
        int[] corners = {img.getRGB(0, 0), img.getRGB(255, 0),
                img.getRGB(0, 255), img.getRGB(255, 255)};
        for (int i = 0; i < corners.length; i++) {
            check((corners[i] & 0xFFFFFF) == 0xFFFFFF, "角像素 #" + i + " 为白色");
        }

        // 3. 文件头魔数
        check((jpg[0] & 0xFF) == 0xFF && (jpg[1] & 0xFF) == 0xD8 && (jpg[2] & 0xFF) == 0xFF,
                "JPEG 文件头魔数 FFD8FF");

        // 4. 多尺寸
        for (int size : new int[]{16, 128, 512}) {
            BufferedImage s = decode(Multiavatar.toJpg("Binx Bond", size));
            check(s.getWidth() == size, "指定尺寸 " + size);
        }

        // 5. 多输入
        File dir = new File("demo");
        dir.mkdirs();
        for (String sample : SAMPLES) {
            byte[] bytes = Multiavatar.toJpg(sample);
            BufferedImage s = decode(bytes);
            //把BufferedImage转为jpg图片
            File f = new File(dir, "avatar_" + sample + ".jpg");
            try (FileOutputStream os = new FileOutputStream(f)) {
                os.write(bytes);
            }
            System.out.println(f.getAbsolutePath());
            check(s.getWidth() == 256, "输入 '" + sample + "' 渲染成功");
        }

        // 6. 空输入返回空数组
        check(Multiavatar.toJpg("").length == 0, "空输入返回空数组");

        // 7. Avatar.jpg() 与 toJpg 等价（字节一致）
        Avatar avatar = Multiavatar.avatar("Binx Bond");
        check(Arrays.equals(avatar.jpg(), jpg), "Avatar.jpg() 与 toJpg 字节一致");

        // 8. render(JPG) 通用入口与 toJpg 等价
        byte[] viaRender = Multiavatar.render("Binx Bond", AvatarFormat.JPG,
                AvatarOptions.defaults());
        check(Arrays.equals(viaRender, jpg), "render(JPG) 与 toJpg 字节一致");

        // 9. 与 PNG 形状一致性：JPG（白底）与 PNG 铺白后二值化形状对比
        for (String sample : SAMPLES) {
            BufferedImage png = Multiavatar.toImage(sample, 256);
            BufferedImage jpgImg = decode(Multiavatar.toJpg(sample, 256));
            double[] m = shapeMetrics1(png, jpgImg);
            System.out.println("  metrics(" + sample + ") recall=" + String.format("%.4f", m[0])
                    + " extra=" + String.format("%.4f", m[1]));
            check(m[0] >= 0.97, "输入 '" + sample + "' JPG 召回率 " + m[0] + " ≥ 0.97");
            check(m[1] <= 0.10, "输入 '" + sample + "' JPG 扩散率 " + m[1] + " ≤ 10%");
        }

        System.out.println("OK: JPG 专项验证全部通过（" + (9 + SAMPLES.length) + " 项断言）");
    }

    private static BufferedImage decode(byte[] bytes) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        check(img != null, "JPG 可被 ImageIO 解码");
        return img;
    }

    /**
     * 形状对比指标（JPEG 有损，故用两个指标）：
     * <ul>
     *   <li>recall = |JPG ∩ PNG| / |PNG|：JPG 是否覆盖了 PNG 的几乎全部形状（无缺失）；</li>
     *   <li>extra = |JPG \ PNG| / |PNG|：JPG 因压缩振铃/边缘扩散多出的形状占比（有限）。</li>
     * </ul>
     */
    private static double[] shapeMetrics1(BufferedImage png, BufferedImage jpg) {
        int w = png.getWidth(), h = png.getHeight();
        int pngShape = 0, inter = 0, jpgOnly = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = png.getRGB(x, y);
                int a = (p >>> 24) & 0xFF;
                int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
                boolean isPng = a > 127 && !isNearWhite(r, g, b);
                int q = jpg.getRGB(x, y);
                boolean isJpg = !isNearWhite((q >>> 16) & 0xFF, (q >>> 8) & 0xFF, q & 0xFF);
                if (isPng) {
                    pngShape++;
                    if (isJpg) {
                        inter++;
                    }
                } else if (isJpg) {
                    jpgOnly++;
                }
            }
        }
        if (pngShape == 0) {
            return new double[]{1.0, 0.0};
        }
        return new double[]{inter / (double) pngShape, jpgOnly / (double) pngShape};
    }

    private static boolean isNearWhite(int r, int g, int b) {
        return r > 248 && g > 248 && b > 248;
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("FAIL: " + msg);
        }
        System.out.println("PASS: " + msg);
    }
}
