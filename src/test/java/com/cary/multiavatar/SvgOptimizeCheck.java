package com.cary.multiavatar;

import com.cary.multiavatar.core.SvgOptimizer;
import com.cary.multiavatar.render.SvgRasterizer;

import java.awt.image.BufferedImage;

/**
 * SVG 体积优化专项验证（零依赖，main + 断言）：
 * 体积减量、坐标取整正确性、优化前后渲染几何等价、
 * optimizeSvg(false) 还原逐字符一致、缓存键区分优化开关。
 * 运行：java -cp target/classes;target/test-classes com.cary.multiavatar.SvgOptimizeCheck
 */
public final class SvgOptimizeCheck {

    private static final String[] SAMPLES = {"Binx Bond", "test", "张三", "user@example.com", "123456789",
            "github", "hello", "java", "vector", "orange"};

    /**
     * 与官方 JS 一致的原文 SVG（关闭体积优化，作优化对比基线）。
     */
    private static String rawSvg(String input) {
        return Multiavatar.avatar(input, AvatarOptions.builder().optimizeSvg(false).build()).svg();
    }

    public static void main(String[] args) {
        Multiavatar.clearCache();

        // 1. 取整规则单元验证
        check(SvgOptimizer.roundToken("3.9863").equals("4"), "取整 3.9863 -> 4");
        check(SvgOptimizer.roundToken("0.59192").equals("0.6"), "取整 0.59192 -> 0.6");
        check(SvgOptimizer.roundToken("115.5").equals("115.5"), "1 位小数保持 115.5");
        check(SvgOptimizer.roundToken("-12.34").equals("-12.3"), "负数取整 -12.34 -> -12.3");
        check(SvgOptimizer.roundToken("4e-5").equals("0"), "科学计数法 4e-5 -> 0");
        check(SvgOptimizer.roundToken("0.50424").equals("0.5"), "取整 0.50424 -> 0.5");
        // 回归护栏：负数取整为 0 保留负号，避免与前一数字紧凑粘连（2.7 0 -> 2.70 坐标丢失）
        check(SvgOptimizer.roundToken("-0.0428").equals("-0"), "负数归零保留负号 -0.0428 -> -0");

        // 2. 默认开启优化：avatar().svg() 与原文（optimizeSvg=false）不同且更短
        String raw = rawSvg("Binx Bond");
        String opt = Multiavatar.avatar("Binx Bond").svg();
        check(!raw.equals(opt), "默认 avatar().svg() 为优化版（非原文）");
        double saving = 1.0 - (double) opt.length() / raw.length();
        System.out.printf("Binx Bond 体积: %d -> %d (减 %.1f%%)%n", raw.length(), opt.length(),
                saving * 100);
        check(opt.length() < raw.length(), "优化后体积减小");

        // 3. 多输入体积减量 ≥ 15%（1 位取整的理论下限）
        for (String s : SAMPLES) {
            String r = rawSvg(s);
            String o = SvgOptimizer.optimize(r);
            double save = 1.0 - (double) o.length() / r.length();
            check(save >= 0.15, "输入 '" + s + "' 体积减量 " + (save * 100) + "% ≥ 15%");
            System.out.println("  " + s + ": " + r.length() + " -> " + o.length()
                    + " (减 " + String.format("%.1f", save * 100) + "%)");
        }

        // 4. 优化前后渲染几何等价（256px 光栅化，双指标：recall 无缺失、extra 扩散有限）
        for (String s : SAMPLES) {
            String r = rawSvg(s);
            String o = SvgOptimizer.optimize(r);
            BufferedImage a = SvgRasterizer.rasterize(r, 256);
            BufferedImage b = SvgRasterizer.rasterize(o, 256);
            double[] m = shapeMetrics(a, b);
            System.out.println("  " + s + ": recall=" + String.format("%.4f", m[0])
                    + " extra=" + String.format("%.4f", m[1]));
            check(m[0] >= 0.995, "输入 '" + s + "' 优化后召回率 " + m[0] + " ≥ 0.995");
            check(m[1] <= 0.005, "输入 '" + s + "' 优化后扩散率 " + m[1] + " ≤ 0.5%");
        }

        // 5. optimizeSvg(false) 与 multiavatar() 逐字符一致
        check(Multiavatar.avatar("Binx Bond",
                        AvatarOptions.builder().optimizeSvg(false).build()).svg().equals(raw),
                "optimizeSvg(false) 还原逐字符原文");

        // 6. 优化开关入缓存键（不同实例）
        Avatar def = Multiavatar.avatar("Binx Bond");
        Avatar unopt = Multiavatar.avatar("Binx Bond", AvatarOptions.builder().optimizeSvg(false).build());
        check(def != unopt, "optimizeSvg 差异不入同一缓存条目");

        // 7. 描边形状全部保留（fill:none 且带 stroke 的是可见线条，优化器不得删除）
        String optFull = Multiavatar.avatar("Binx Bond").svg();
        check(optFull.contains("stroke:"), "描边样式保留（未误删可见线条）");
        check(optFull.contains("fill:none"), "fill:none 描边线条保留（不可删）");

        System.out.println("OK: SVG 体积优化验证全部通过");
    }

    /**
     * 形状对比指标（优化版 b 相对原始 a）：recall = |b∩a|/|a|、extra = |b\a|/|a|。
     */
    private static double[] shapeMetrics(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        int base = 0, inter = 0, extra = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean sa = isShape(a, x, y);
                boolean sb = isShape(b, x, y);
                if (sa) {
                    base++;
                    if (sb) {
                        inter++;
                    }
                } else if (sb) {
                    extra++;
                }
            }
        }
        if (base == 0) {
            return new double[]{1.0, 0.0};
        }
        return new double[]{inter / (double) base, extra / (double) base};
    }

    private static boolean isShape(BufferedImage img, int x, int y) {
        int p = img.getRGB(x, y);
        int a = (p >>> 24) & 0xFF;
        int r = (p >>> 16) & 0xFF, g = (p >>> 8) & 0xFF, b = p & 0xFF;
        return a > 127 && !(r > 248 && g > 248 && b > 248);
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("FAIL: " + msg);
        }
        System.out.println("PASS: " + msg);
    }
}
