package com.cary.multiavatar;

import com.cary.multiavatar.render.PngWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 批量头像生成器（测试类 / 可复用工具）。
 *
 * <p>从「每行一个名字」的文本文件批量生成头像（SVG/PNG/JPG/GIF/favicon zip），
 * 可选输出一张横排网格预览图；生成后逐项断言验证产物：非空、可解码、尺寸正确、
 * 同输入两次生成字节一致（确定性）。任一断言失败抛异常并以非 0 退出码结束。</p>
 *
 * <p>用法：</p>
 * <pre>
 *   java -cp target/classes;target/test-classes com.cary.multiavatar.BatchGenerator names.txt outDir [--size 256] [--format svg|png|jpg|gif|favicon] [--no-grid] [--no-optimize]
 * </pre>
 *
 * <p>默认：size=256、format=png、输出网格预览、启用 SVG 体积优化。</p>
 */
public final class BatchGenerator {

    private static int size = 256;
    private static boolean grid = true;
    private static boolean optimize = true;
    private static String format = "png";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法: BatchGenerator <namesFile> <outDir> [--size N] "
                    + "[--format svg|png|jpg|gif|favicon|ico] [--no-grid] [--no-optimize]");
            System.exit(2);
            return;
        }
        File namesFile = new File(args[0]);
        File outDir = new File(args[1]);
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if ("--size".equals(a) && i + 1 < args.length) {
                size = Integer.parseInt(args[++i]);
            } else if ("--format".equals(a) && i + 1 < args.length) {
                format = args[++i].toLowerCase();
            } else if ("--no-grid".equals(a)) {
                grid = false;
            } else if ("--no-optimize".equals(a)) {
                optimize = false;
            }
        }
        if (!namesFile.isFile()) {
            throw new IllegalArgumentException("名字文件不存在: " + namesFile.getAbsolutePath());
        }
        outDir.mkdirs();

        List<String> names = readNames(namesFile);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("名字文件为空: " + namesFile.getAbsolutePath());
        }
        System.out.println("读取 " + names.size() + " 个名字，输出目录: " + outDir.getAbsolutePath()
                + "（size=" + size + ", format=" + format + ", optimize=" + optimize + "）");

        AvatarOptions options = AvatarOptions.builder()
                .size(size)
                .svgSize(size, size)
                .optimizeSvg(optimize)
                .build();

        long t0 = System.nanoTime();
        List<BufferedImage> images = new ArrayList<BufferedImage>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String safe = String.format("%04d_%s", i + 1,
                    name.replaceAll("[^A-Za-z0-9_-]", "_"));
            File out = writeOne(name, safe, outDir, options);
            checkDeterministic(name, safe, outDir, options);
            if (grid) {
                images.add(Multiavatar.avatar(name, options).toImage(size));
            }
            System.out.println("  [" + (i + 1) + "/" + names.size() + "] " + name
                    + " -> " + out.getName());
        }

        if (grid) {
            File gridFile = new File(outDir, "grid.png");
            writeGrid(images, size, gridFile);
            System.out.println("网格预览: " + gridFile.getAbsolutePath());
        }

        long ms = (System.nanoTime() - t0) / 1_000_000L;
        System.out.println("OK: 批量生成 " + names.size() + " 个头像全部验证通过，耗时 "
                + ms + " ms（缓存命中数=" + Multiavatar.cacheSize() + "）");
    }

    /**
     * 读取 UTF-8 名字文件（跳过空行与首尾空白）。
     */
    private static List<String> readNames(File f) throws IOException {
        List<String> names = new ArrayList<String>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (!s.isEmpty()) {
                    names.add(s);
                }
            }
        }
        return names;
    }

    /**
     * 按格式生成一个头像文件，并断言产物合法。
     */
    private static File writeOne(String name, String safe, File outDir, AvatarOptions options)
            throws IOException {
        Avatar avatar = Multiavatar.avatar(name, options);
        String ext = "favicon".equals(format) ? "zip" : format;
        File out = new File(outDir, safe + "." + ext);
        if ("svg".equals(format)) {
            try (Writer w = new OutputStreamWriter(
                    new FileOutputStream(out), StandardCharsets.UTF_8)) {
                w.write(avatar.svg());
            }
            check(!avatar.svg().isEmpty(), name + ": SVG 非空");
        } else if ("png".equals(format)) {
            byte[] bytes = avatar.png(size);
            writeBytes(out, bytes);
            check(bytes.length > 8, name + ": PNG 非空");
            checkImage(bytes, name);
        } else if ("jpg".equals(format)) {
            byte[] bytes = avatar.jpg(size);
            writeBytes(out, bytes);
            check(bytes.length > 8, name + ": JPG 非空");
            checkImage(bytes, name);
        } else if ("gif".equals(format)) {
            byte[] bytes = Multiavatar.toGif(name, options);
            writeBytes(out, bytes);
            check(bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F',
                    name + ": GIF 头合法");
        } else if ("favicon".equals(format)) {
            byte[] zip = Multiavatar.toFaviconZip(name);
            writeBytes(out, zip);
            checkZip(zip, name);
        } else if ("ico".equals(format)) {
            byte[] ico = Multiavatar.toFaviconIco(name);
            writeBytes(out, ico);
            checkIco(ico, name);
        } else {
            throw new IllegalArgumentException("不支持的格式: " + format);
        }
        return out;
    }

    /**
     * ICO 断言：头合法、默认 6 条目、每条目数据区可解码为 PNG。
     */
    private static void checkIco(byte[] ico, String name) throws IOException {
        check(ico.length > 6 && ico[0] == 0 && ico[1] == 0
                && ico[2] == 1 && ico[3] == 0, name + ": ICO 头合法");
        int count = (ico[4] & 0xFF) | ((ico[5] & 0xFF) << 8);
        check(count >= 2, name + ": ICO 应含至少 2 个尺寸，实际 " + count);
        for (int i = 0; i < count; i++) {
            int base = 6 + 16 * i;
            int len = (ico[base + 8] & 0xFF) | ((ico[base + 9] & 0xFF) << 8)
                    | ((ico[base + 10] & 0xFF) << 16) | ((ico[base + 11] & 0xFF) << 24);
            int off = (ico[base + 12] & 0xFF) | ((ico[base + 13] & 0xFF) << 8)
                    | ((ico[base + 14] & 0xFF) << 16) | ((ico[base + 15] & 0xFF) << 24);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(ico, off, len));
            check(img != null, name + ": ICO 条目 " + i + " 数据可解码");
        }
    }

    /**
     * 确定性断言：同输入重新生成字节完全一致。
     */
    private static void checkDeterministic(String name, String safe, File outDir,
                                           AvatarOptions options) throws IOException {
        AvatarOptions raw = AvatarOptions.builder()
                .size(size)
                .svgSize(size, size)
                .optimizeSvg(false)
                .build();
        if ("svg".equals(format)) {
            byte[] a = Multiavatar.avatar(name, raw).svg().getBytes(StandardCharsets.UTF_8);
            byte[] b = Multiavatar.avatar(name, raw).svg().getBytes(StandardCharsets.UTF_8);
            check(Arrays.equals(a, b), name + ": SVG 确定性一致");
        } else if ("png".equals(format)) {
            check(Arrays.equals(Multiavatar.toPng(name, raw),
                    Multiavatar.toPng(name, raw)), name + ": PNG 确定性一致");
        } else if ("jpg".equals(format)) {
            check(Arrays.equals(Multiavatar.toJpg(name, raw),
                    Multiavatar.toJpg(name, raw)), name + ": JPG 确定性一致");
        }
    }

    /**
     * 网格预览：N 张 size×size 透明底横排拼一张。
     */
    private static void writeGrid(List<BufferedImage> images, int cell, File out)
            throws IOException {
        BufferedImage gridImg = new BufferedImage(cell * images.size(), cell,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gridImg.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, gridImg.getWidth(), gridImg.getHeight());
        for (int i = 0; i < images.size(); i++) {
            BufferedImage img = images.get(i);
            if (img != null) {
                g.drawImage(img, i * cell, 0, null);
            }
        }
        g.dispose();
        writeBytes(out, PngWriter.toPng(gridImg));
    }

    private static void checkImage(byte[] bytes, String name) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        check(img != null, name + ": 图像可解码");
        check(img.getWidth() == size && img.getHeight() == size,
                name + ": 尺寸 " + img.getWidth() + "x" + img.getHeight() + " != " + size);
    }

    private static void checkZip(byte[] bytes, String name) throws IOException {
        int entries = 0;
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                entries++;
                check(e.getName().matches(".*\\.(png|ico)$"), name + ": zip 条目为图片");
            }
        }
        check(entries >= 2, name + ": favicon zip 含 " + entries + " 个尺寸");
    }

    private static void writeBytes(File out, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(bytes);
        }
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("断言失败: " + msg);
        }
    }
}
