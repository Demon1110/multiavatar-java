package com.cary.multiavatar;

import javax.imageio.ImageIO;
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

        // 2. JPG：白底、JPEG 可解码
        byte[] jpg = Multiavatar.toJpg("Binx Bond", 128);
        File jpgFile = new File(dir, "out.jpg");
        write(jpgFile, jpg);
        BufferedImage jpgImg = ImageIO.read(jpgFile);
        check(jpgImg != null && jpgImg.getWidth() == 128, "JPG 可解码且 128 宽");
        check(jpgImg.getRGB(0, 0) == 0xFFFFFFFF, "JPG 左上角应为白底");
        check(!jpgImg.getColorModel().hasAlpha(), "JPG 不应有 alpha 通道");

        // 3. GIF：可解码、帧数 = 形状数×2
        byte[] gif = Multiavatar.toGif("Binx Bond", 128);
        File gifFile = new File(dir, "out.gif");
        write(gifFile, gif);
        try (javax.imageio.stream.FileImageInputStream in = new javax.imageio.stream.FileImageInputStream(gifFile)) {
            javax.imageio.ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
            reader.setInput(in);
            int frames = reader.getNumImages(true);
            int svgShapes = com.cary.multiavatar.svg.SvgParser.parse(Multiavatar.multiavatar("Binx Bond")).shapes()
                    .size();
            System.out.println("GIF 帧数=" + frames + ", 形状数=" + svgShapes);
            check(frames == svgShapes * 2, "GIF 帧数应为 形状数×2");
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

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("FAIL: " + msg);
        }
        System.out.println("PASS: " + msg);
    }
}
