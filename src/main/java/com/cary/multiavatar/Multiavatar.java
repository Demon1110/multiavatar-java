package com.cary.multiavatar;

import com.cary.multiavatar.core.SvgComposer;
import com.cary.multiavatar.core.SvgOptimizer;
import com.cary.multiavatar.render.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Multiavatar —— 多文化头像生成器门面（Facade）。
 *
 * <p>对外统一入口：隐藏 生成核心(core)、数据(data)、SVG 领域模型(svg)、渲染(render) 的细节。
 * 保持与 multiavatar.js 一致的确定性算法 —— 同一输入永远产出同一头像
 * （共 16^6 = 12,230,590,464 个唯一头像）。</p>
 *
 * <p><b>用法：</b></p>
 * <pre>
 * // SVG 文本（旧 API，兼容）
 * String svg = Multiavatar.multiavatar("Binx Bond");
 *
 * // PNG / JPEG / GIF（新 API，纯 JDK 渲染，无第三方依赖）
 * byte[] png = Multiavatar.toPng("Binx Bond");
 * byte[] png512 = Multiavatar.toPng("Binx Bond", AvatarOptions.builder().size(512).build());
 * byte[] jpg = Multiavatar.toJpg("Binx Bond");      // 白底
 * byte[] gif = Multiavatar.toGif("Binx Bond");      // 部件逐帧淡入动画
 *
 * // 头像对象（SVG + 按需图像；toImage 直接返回 BufferedImage 供继续加工）
 * Avatar avatar = Multiavatar.avatar("Binx Bond");
 * String s = avatar.svg();
 * byte[] p = avatar.png(1024);
 * BufferedImage img = avatar.toImage(256);
 *
 * // favicon 多尺寸打包
 * byte[] zip = Multiavatar.toFaviconZip("Binx Bond");          // 16/32/48/64/128/256
 * byte[] zip2 = Multiavatar.toFaviconZip("Binx Bond", 32, 64); // 自定义尺寸
 *
 * // 直接写文件
 * Multiavatar.writePng("Binx Bond", new File("avatar.png"));
 * Multiavatar.writeFaviconZip("Binx Bond", new File("favicon.zip"));
 * </pre>
 */
public final class Multiavatar {

    /**
     * 组装器（SHA-256 + 默认数据源）。
     *
     * <p>viewBox 取 0 0 231 231：与官方 multiavatar.js 完全一致，环境部件（背景圆）的
     * 几何边界恰为 0~231（圆心 115.5、半径 115.5），头像在所有输出中正好充满画布。</p>
     */
    private static final SvgComposer COMPOSER = SvgComposer.withViewBox("0 0 231 231");

    /**
     * 全局 LRU 缓存：同参数输入重复生成时复用已组装的 {@link Avatar}。
     * 键覆盖 input/sansEnv/part/theme/svgWidth/svgHeight；容量满自动淘汰最久未使用项。
     */
    private static final AvatarCache CACHE = AvatarCache.defaults();

    private Multiavatar() {
    }

    // ==================== 旧 API（与 multiavatar.js / 早期版本兼容） ====================

    /**
     * 生成头像 SVG（含背景圆）。
     *
     * @param input 输入字符串（头像标识）
     * @return SVG 代码；输入为空字符串时返回空串（与 JS 一致）
     */
    public static String multiavatar(String input) {
        return avatar(input).svg();
    }

    /**
     * 生成头像 SVG。
     *
     * @param input   输入字符串
     * @param sansEnv 为 true 时输出不含背景圆（环境部件）
     */
    public static String multiavatar(String input, boolean sansEnv) {
        return avatar(input, AvatarOptions.builder().sansEnv(sansEnv).build()).svg();
    }

    /**
     * 生成头像 SVG。
     *
     * @param input   输入字符串
     * @param sansEnv 是否去掉背景圆
     * @param part    强制指定初始角色编号 "00"~"15"（对应 JS 的 ver.part）；null 表示自动
     * @param theme   强制指定颜色主题 "A"/"B"/"C"（对应 JS 的 ver.theme）；null 表示自动
     */
    public static String multiavatar(String input, boolean sansEnv, String part, String theme) {
        return avatar(input, AvatarOptions.builder().sansEnv(sansEnv).part(part).theme(theme).build()).svg();
    }

    // ==================== 新 API（面向对象） ====================

    /**
     * 生成头像对象（默认选项，SVG 格式）。同参数重复调用命中全局 LRU 缓存。
     */
    public static Avatar avatar(String input) {
        return avatar(input, AvatarOptions.defaults());
    }

    /**
     * 生成头像对象（经全局 LRU 缓存）。
     *
     * @param input   输入字符串
     * @param options 生成选项（sansEnv/part/theme/size/svgSize/format）
     */
    public static Avatar avatar(String input, AvatarOptions options) {
        if (options == null) {
            options = AvatarOptions.defaults();
        }
        String norm = input == null ? "" : input;
        AvatarCache.Key key = new AvatarCache.Key(norm, options.sansEnv(), options.part(),
                options.theme(), options.svgWidth(), options.svgHeight(), options.optimizeSvg());
        Avatar cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String svg = COMPOSER.compose(norm, options.sansEnv(), options.part(), options.theme(),
                options.svgWidth(), options.svgHeight());
        if (options.optimizeSvg() && !svg.isEmpty()) {
            svg = SvgOptimizer.optimize(svg);
        }
        Avatar avatar = new Avatar(norm, options, svg);
        CACHE.put(key, avatar);
        return avatar;
    }

    /**
     * 全局 LRU 缓存当前条目数。
     */
    public static int cacheSize() {
        return CACHE.size();
    }

    /**
     * 清空全局 LRU 缓存。
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * 直接渲染为 PNG 字节（默认尺寸 256）。
     */
    public static byte[] toPng(String input) {
        return toPng(input, AvatarOptions.defaults());
    }

    /**
     * 直接渲染为 PNG 字节（指定边长）。
     */
    public static byte[] toPng(String input, int size) {
        return toPng(input, AvatarOptions.builder().size(size).build());
    }

    /**
     * 直接渲染为 PNG 字节。
     */
    public static byte[] toPng(String input, AvatarOptions options) {
        Avatar avatar = avatar(input, options);
        return avatar.png(options.size());
    }

    /**
     * 光栅化图像（默认尺寸 256，透明背景）；空输入返回 null。
     */
    public static BufferedImage toImage(String input) {
        return toImage(input, AvatarOptions.defaults());
    }

    /**
     * 光栅化图像（指定边长，透明背景）；空输入返回 null。
     */
    public static BufferedImage toImage(String input, int size) {
        return toImage(input, AvatarOptions.builder().size(size).build());
    }

    /**
     * 光栅化图像（透明背景）；空输入返回 null。
     */
    public static BufferedImage toImage(String input, AvatarOptions options) {
        return avatar(input, options).toImage();
    }

    /**
     * PNG data URI（默认尺寸 256）：{@code data:image/png;base64,...}，可直接用于
     * {@code <img src>} / CSS background，免上传即内嵌展示。空输入返回空串。
     */
    public static String toDataUri(String input) {
        return toDataUri(input, AvatarOptions.defaults());
    }

    /**
     * PNG data URI（指定边长）；空输入返回空串。
     */
    public static String toDataUri(String input, int size) {
        return toDataUri(input, AvatarOptions.builder().size(size).build());
    }

    /**
     * PNG data URI；空输入返回空串。
     */
    public static String toDataUri(String input, AvatarOptions options) {
        return avatar(input, options).dataUri();
    }

    /**
     * 直接渲染为 JPEG 字节（默认尺寸 256，白底）。
     */
    public static byte[] toJpg(String input) {
        return toJpg(input, AvatarOptions.defaults());
    }

    /**
     * 直接渲染为 JPEG 字节（指定边长，白底）。
     */
    public static byte[] toJpg(String input, int size) {
        return toJpg(input, AvatarOptions.builder().size(size).build());
    }

    /**
     * 直接渲染为 JPEG 字节（白底）。
     */
    public static byte[] toJpg(String input, AvatarOptions options) {
        return render(input, AvatarFormat.JPG, options);
    }

    /**
     * 直接渲染为 GIF 动画字节（部件逐帧淡入，无限循环，默认尺寸 256）。
     */
    public static byte[] toGif(String input) {
        return toGif(input, AvatarOptions.defaults());
    }

    /**
     * 直接渲染为 GIF 动画字节（指定边长）。
     */
    public static byte[] toGif(String input, int size) {
        return toGif(input, AvatarOptions.builder().size(size).build());
    }

    /**
     * 直接渲染为 GIF 动画字节。
     */
    public static byte[] toGif(String input, AvatarOptions options) {
        return render(input, AvatarFormat.GIF, options);
    }

    /**
     * 打包多尺寸 PNG 为 favicon zip 字节（默认 16/32/48/64/128/256）。
     */
    public static byte[] toFaviconZip(String input) {
        return toFaviconZip(input, (int[]) null);
    }

    /**
     * 打包多尺寸 PNG 为 favicon zip 字节（自定义尺寸，如 16, 32, 48…）。
     */
    public static byte[] toFaviconZip(String input, int... sizes) {
        String svg = multiavatar(input);
        return svg.isEmpty() ? new byte[0] : Favicons.toZip(svg, sizes);
    }

    /**
     * 通用渲染入口：按格式与选项渲染。
     *
     * @param format SVG 返回文本 UTF-8 字节；PNG/JPEG/GIF 返回对应图片字节
     */
    public static byte[] render(String input, AvatarFormat format, AvatarOptions options) {
        Avatar avatar = avatar(input, options);
        if (avatar.isEmpty()) {
            return new byte[0];
        }
        AvatarRenderer renderer = Renderers.create(format);
        return renderer.render(avatar.svg(), options.size());
    }

    /**
     * 将头像 PNG 写入文件（默认尺寸 256）。
     */
    public static void writePng(String input, File file) throws IOException {
        writePng(input, AvatarOptions.defaults(), file);
    }

    /**
     * 将头像 PNG 写入文件。
     */
    public static void writePng(String input, AvatarOptions options, File file) throws IOException {
        byte[] png = toPng(input, options);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(png);
        }
    }

    /**
     * 将 favicon zip 写入文件（默认尺寸）。
     */
    public static void writeFaviconZip(String input, File file) throws IOException {
        writeFaviconZip(input, file, (int[]) null);
    }

    /**
     * 将 favicon zip 写入文件（自定义尺寸）。
     */
    public static void writeFaviconZip(String input, File file, int... sizes) throws IOException {
        byte[] zip = toFaviconZip(input, sizes);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(zip);
        }
    }

    // ==================== 演示入口 ====================

    /**
     * 演示入口：
     * <ul>
     *   <li>无参数：为若干示例字符串生成 SVG/PNG（GIF 与 favicon zip 各一个）写入 ./demo/ 目录，
     *       并拼接一张横排预览图 demo/preview_grid.png（兼作批量渲染的视觉回归图）；</li>
     *   <li>一个参数：把该参数作为输入字符串，将 SVG 打印到标准输出。</li>
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            System.out.print(multiavatar(args[0]));
            return;
        }

        String[] samples = {"Binx Bond", "test", "张三", "user@example.com", "123456789"};
        File dir = new File("demo");
        dir.mkdirs();
        int size = 256;
        BufferedImage[] imgs = new BufferedImage[samples.length];
        for (int i = 0; i < samples.length; i++) {
            String s = samples[i];
            String name = s.replaceAll("[^A-Za-z0-9_-]", "_");
            Avatar avatar = avatar(s); // 默认启用 SVG 体积优化
            try (Writer w = new OutputStreamWriter(
                    new FileOutputStream(new File(dir, "avatar_" + name + ".svg")),
                    StandardCharsets.UTF_8)) {
                w.write(avatar.svg());
            }
            writePng(s, AvatarOptions.builder().size(size).build(),
                    new File(dir, "avatar_" + name + ".png"));
            imgs[i] = avatar.toImage(size);
            System.out.println("已生成: " + dir.getAbsolutePath() + "\\avatar_" + name + ".{svg,png}");
        }

        // 横排预览图：N 张 256×256 依次拼接为一张（透明底，兼作批量回归图）
        BufferedImage grid = new BufferedImage(size * samples.length, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = grid.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, grid.getWidth(), grid.getHeight());
        for (int i = 0; i < imgs.length; i++) {
            if (imgs[i] != null) {
                g.drawImage(imgs[i], i * size, 0, null);
            }
        }
        g.dispose();
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "preview_grid.png"))) {
            fos.write(PngWriter.toPng(grid));
        }
        System.out.println("已生成: " + dir.getAbsolutePath() + "\\preview_grid.png");

        // GIF 动画 + favicon 打包演示（取第一个样例）
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "avatar_Binx_Bond.gif"))) {
            fos.write(toGif("Binx Bond", 256));
        }
        writeFaviconZip("Binx Bond", new File(dir, "avatar_Binx_Bond_favicon.zip"));
        System.out.println("已生成: " + dir.getAbsolutePath() + "\\avatar_Binx_Bond.{gif,favicon.zip}");
    }
}
