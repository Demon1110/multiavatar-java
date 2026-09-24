package com.cary.multiavatar;

import com.cary.multiavatar.core.SvgComposer;
import com.cary.multiavatar.render.AvatarFormat;
import com.cary.multiavatar.render.AvatarRenderer;
import com.cary.multiavatar.render.Renderers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * // PNG 字节（新 API，纯 JDK 渲染，无第三方依赖）
 * byte[] png = Multiavatar.toPng("Binx Bond");
 * byte[] png512 = Multiavatar.toPng("Binx Bond", AvatarOptions.builder().size(512).build());
 *
 * // 头像对象（SVG + 按需 PNG）
 * Avatar avatar = Multiavatar.avatar("Binx Bond");
 * String s = avatar.svg();
 * byte[] p = avatar.png(1024);
 *
 * // 直接写文件
 * Multiavatar.writePng("Binx Bond", new File("avatar.png"));
 * </pre>
 */
public final class Multiavatar {

    /**
     * 组装器（SHA-256 + 默认数据源），viewBox 与现行实现保持一致。
     */
    private static final SvgComposer COMPOSER = SvgComposer.withViewBox("0 0 256 256");

    private Multiavatar() {
    }

    // ==================== 旧 API（与 multiavatar.js / 早期版本兼容） ====================

    /**
     * 生成头像 SVG（含背景圆）。
     *
     * @param string 输入字符串（头像标识）
     * @return SVG 代码；输入为空字符串时返回空串（与 JS 一致）
     */
    public static String multiavatar(String string) {
        return COMPOSER.compose(string, false, null, null);
    }

    /**
     * 生成头像 SVG。
     *
     * @param string  输入字符串
     * @param sansEnv 为 true 时输出不含背景圆（环境部件）
     */
    public static String multiavatar(String string, boolean sansEnv) {
        return COMPOSER.compose(string, sansEnv, null, null);
    }

    /**
     * 生成头像 SVG。
     *
     * @param string  输入字符串
     * @param sansEnv 是否去掉背景圆
     * @param part    强制指定初始角色编号 "00"~"15"（对应 JS 的 ver.part）；null 表示自动
     * @param theme   强制指定颜色主题 "A"/"B"/"C"（对应 JS 的 ver.theme）；null 表示自动
     */
    public static String multiavatar(String string, boolean sansEnv, String part, String theme) {
        return COMPOSER.compose(string, sansEnv, part, theme);
    }

    // ==================== 新 API（面向对象） ====================

    /**
     * 生成头像对象（默认选项，SVG 格式）。
     */
    public static Avatar avatar(String string) {
        return avatar(string, AvatarOptions.defaults());
    }

    /**
     * 生成头像对象。
     *
     * @param string  输入字符串
     * @param options 生成选项（sansEnv/part/theme/size/svgSize/format）
     */
    public static Avatar avatar(String string, AvatarOptions options) {
        if (options == null) {
            options = AvatarOptions.defaults();
        }
        String svg = COMPOSER.compose(string, options.sansEnv(), options.part(), options.theme(),
                options.svgWidth(), options.svgHeight());
        return new Avatar(string == null ? "" : string, options, svg);
    }

    /**
     * 直接渲染为 PNG 字节（默认尺寸 256）。
     */
    public static byte[] toPng(String string) {
        return toPng(string, AvatarOptions.defaults());
    }

    /**
     * 直接渲染为 PNG 字节（指定边长）。
     */
    public static byte[] toPng(String string, int size) {
        return toPng(string, AvatarOptions.builder().size(size).build());
    }

    /**
     * 直接渲染为 PNG 字节。
     */
    public static byte[] toPng(String string, AvatarOptions options) {
        Avatar avatar = avatar(string, options);
        return avatar.png(options.size());
    }

    /**
     * 通用渲染入口：按格式与选项渲染。
     *
     * @param format SVG 返回文本 UTF-8 字节；PNG 返回 PNG 图片字节
     */
    public static byte[] render(String string, AvatarFormat format, AvatarOptions options) {
        Avatar avatar = avatar(string, options);
        if (avatar.isEmpty()) {
            return new byte[0];
        }
        AvatarRenderer renderer = Renderers.create(format);
        return renderer.render(avatar.svg(), options.size());
    }

    /**
     * 将头像 PNG 写入文件（默认尺寸 256）。
     */
    public static void writePng(String string, File file) throws IOException {
        writePng(string, AvatarOptions.defaults(), file);
    }

    /**
     * 将头像 PNG 写入文件。
     */
    public static void writePng(String string, AvatarOptions options, File file) throws IOException {
        byte[] png = toPng(string, options);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(png);
        }
    }

    // ==================== 演示入口 ====================

    /**
     * 演示入口：
     * <ul>
     *   <li>无参数：为若干示例字符串生成 SVG 与 PNG 写入 ./demo/ 目录；</li>
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
        for (String s : samples) {
            String name = s.replaceAll("[^A-Za-z0-9_-]", "_");
            try (java.io.Writer w = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(new File(dir, "avatar_" + name + ".svg")),
                    StandardCharsets.UTF_8)) {
                w.write(multiavatar(s));
            }
            writePng(s, AvatarOptions.builder().size(256).build(),
                    new File(dir, "avatar_" + name + ".png"));
            System.out.println("已生成: " + dir.getAbsolutePath() + "\\avatar_" + name + ".{svg,png}");
        }
    }
}
