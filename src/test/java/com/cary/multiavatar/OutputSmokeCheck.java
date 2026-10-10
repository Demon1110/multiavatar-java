package com.cary.multiavatar;

import com.cary.multiavatar.render.Favicons;
import com.cary.multiavatar.svg.SvgParser;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 输出层 1-4 冒烟验证：toImage / JPG / GIF / favicon zip。
 * 运行：java -cp target/classes;target/test-classes com.cary.multiavatar.OutputSmokeCheck
 */
public final class OutputSmokeCheck {

    public static void main(String[] args) throws Exception {
        File dir = new File("target", "outcheck");
        dir.mkdirs();

        // 1. toImage：直接返回 BufferedImage
        BufferedImage img = Multiavatar.toImage("Binx Bond", 128);
        check(img != null && img.getWidth() == 128 && img.getHeight() == 128,
                "toImage 尺寸应为 128×128");
        // 空输入返回 null
        check(Multiavatar.toImage("") == null, "toImage 空输入应返回 null");
        // 1b. SVG 解析缓存：同一 Avatar 多次 document() 返回同一实例（只 parse 一次）
        Avatar cachedAvatar = Multiavatar.avatar("Binx Bond");
        check(cachedAvatar.document() == cachedAvatar.document(),
                "Avatar.document() 应缓存复用同一 SvgDocument");
        check(Multiavatar.toImage("Binx Bond", 128).getRGB(0, 0) == img.getRGB(0, 0),
                "toImage 重复调用结果一致");

        // 2. JPG：白底、JPEG 可解码
        byte[] jpg = Multiavatar.toJpg("Binx Bond", 128);
        File jpgFile = new File(dir, "out.jpg");
        write(jpgFile, jpg);
        BufferedImage jpgImg = ImageIO.read(jpgFile);
        check(jpgImg != null && jpgImg.getWidth() == 128, "JPG 可解码且 128 宽");
        check(jpgImg.getRGB(0, 0) == 0xFFFFFFFF, "JPG 左上角应为白底");
        check(!jpgImg.getColorModel().hasAlpha(), "JPG 不应有 alpha 通道");

        // 3. GIF：可解码、帧数 = 形状数×2+1、首帧 = 完整头像（外壳缩略图可见完整图像）
        byte[] gif = Multiavatar.toGif("Binx Bond", 128);
        File gifFile = new File(dir, "out.gif");
        write(gifFile, gif);
        try (FileImageInputStream in = new FileImageInputStream(gifFile)) {
            ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
            reader.setInput(in);
            int frames = reader.getNumImages(true);
            int svgShapes = SvgParser.parse(Multiavatar.multiavatar("Binx Bond")).shapes()
                    .size();
            System.out.println("GIF 帧数=" + frames + ", 形状数=" + svgShapes);
            check(frames == svgShapes * 2 + 1, "GIF 帧数应为 形状数×2+1（含首帧完整头像）");
            BufferedImage first = reader.read(0);
            BufferedImage last = reader.read(frames - 1);
            check(samePixels(first, last), "GIF 首帧（完整头像）应与末帧像素一致");
            reader.dispose();
        }

        // 4. favicon zip：默认 6 个尺寸 + 自定义尺寸
        byte[] zip = Multiavatar.toFaviconZip("Binx Bond");
        File zipFile = new File(dir, "favicon.zip");
        write(zipFile, zip);
        int entries = 0;
        boolean has16 = false, has256 = false;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                entries++;
                if (e.getName().equals("avatar-16.png")) has16 = true;
                if (e.getName().equals("avatar-256.png")) has256 = true;
            }
        }
        check(entries == 6, "默认 favicon zip 应有 6 个条目，实际 " + entries);
        check(has16 && has256, "应含 avatar-16.png 与 avatar-256.png");
        byte[] zip2 = Multiavatar.toFaviconZip("Binx Bond", 32, 64);
        File zipFile2 = new File(dir, "favicon2.zip");
        write(zipFile2, zip2);
        int entries2 = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile2))) {
            while (zis.getNextEntry() != null) entries2++;
        }
        check(entries2 == 2, "自定义 favicon zip 应有 2 个条目，实际 " + entries2);

        // 4b. favicon ICO：头合法、条目数、每条目数据区可解码为 PNG
        byte[] ico = Multiavatar.toFaviconIco("Binx Bond");
        File icoFile = new File(dir, "favicon.ico");
        write(icoFile, ico);
        check(ico.length > 6 && ico[0] == 0 && ico[1] == 0
                && ico[2] == 1 && ico[3] == 0, "ICO 头合法（reserved=0, type=1）");
        int icoCount = (ico[4] & 0xFF) | ((ico[5] & 0xFF) << 8);
        check(icoCount == 6, "默认 ICO 应有 6 个尺寸，实际 " + icoCount);
        for (int i = 0; i < icoCount; i++) {
            int base = 6 + 16 * i;
            int len = (ico[base + 8] & 0xFF) | ((ico[base + 9] & 0xFF) << 8)
                    | ((ico[base + 10] & 0xFF) << 16) | ((ico[base + 11] & 0xFF) << 24);
            int off = (ico[base + 12] & 0xFF) | ((ico[base + 13] & 0xFF) << 8)
                    | ((ico[base + 14] & 0xFF) << 16) | ((ico[base + 15] & 0xFF) << 24);
            BufferedImage icoImg = ImageIO.read(new ByteArrayInputStream(ico, off, len));
            check(icoImg != null, "ICO 条目 " + i + " 数据区可解码为 PNG");
        }
        byte[] ico2 = Favicons.toIco(Multiavatar.multiavatar("Binx Bond"), 32, 64);
        check(((ico2[4] & 0xFF) | ((ico2[5] & 0xFF) << 8)) == 2,
                "底层 Favicons.toIco 自定义尺寸应有 2 个，实际 "
                        + ((ico2[4] & 0xFF) | ((ico2[5] & 0xFF) << 8)));

        // 5. data URI：前缀、Base64 可解码回 PNG 且尺寸一致；空输入返回空串
        String uri = Multiavatar.toDataUri("Binx Bond", 128);
        check(uri.startsWith("data:image/png;base64,"), "data URI 前缀正确");
        byte[] uriBytes = Base64.getDecoder().decode(
                uri.substring("data:image/png;base64,".length()));
        BufferedImage uriImg = ImageIO.read(new ByteArrayInputStream(uriBytes));
        check(uriImg != null && uriImg.getWidth() == 128, "data URI 可解码为 128 PNG");
        check(uri.equals(Multiavatar.avatar("Binx Bond").dataUri(128)), "Avatar.dataUri() 与 toDataUri 一致");
        check(Multiavatar.toDataUri("").isEmpty(), "data URI 空输入返回空串");

        System.out.println("OK: 输出层 1-5 冒烟验证全部通过");
    }

    private static void write(File f, byte[] b) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(b);
        }
    }

    /**
     * 两帧逐像素比较（尺寸与 ARGB 全同）。
     */
    private static boolean samePixels(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("FAIL: " + msg);
        }
        System.out.println("PASS: " + msg);
    }
}
