package com.cary.multiavatar;

import com.cary.multiavatar.render.AvatarFormat;
import com.cary.multiavatar.render.AvatarRenderer;
import com.cary.multiavatar.render.Renderers;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.cary.multiavatar.Multiavatar.multiavatar;

public class MultiavatarTest {
    /**
     * 演示入口：
     * <ul>
     *   <li>无参数：为若干示例字符串生成 SVG 并写入 ./demo/ 目录；</li>
     *   <li>一个参数：把该参数作为输入字符串，将 SVG 打印到标准输出。</li>
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        testFirst(args);
//        testMultiavatar();
    }

    private static void testFirst(String[] args) throws IOException {
        if (args.length == 1) {
            System.out.print(multiavatar(args[0]));
            return;
        }

        String[] samples = {"Binx Bond", "test", "张三", "user@example.com", "123456789"};
        File dir = new File("demo");
        dir.mkdirs();
        for (String s : samples) {
            String name = s.replaceAll("[^A-Za-z0-9_-]", "_");
            File f = new File(dir, "avatar_" + name + ".svg");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write(multiavatar(s));
            }
            System.out.println("已生成: " + f.getAbsolutePath());
        }
    }

    public static void testMultiavatar() {
        // 不可变产物：SVG 文本 + 惰性 PNG 渲染
        Avatar avatar = Multiavatar.avatar("Binx Bond");
        String svg = avatar.svg();
        byte[] png = avatar.png();            // 首次调用时渲染并缓存
        byte[] pngLarge = avatar.png(512);    // 指定尺寸重新渲染

// 直接获取渲染器（策略模式），可自行扩展新格式
        AvatarRenderer renderer = Renderers.create(AvatarFormat.PNG);
        byte[] out = renderer.render(avatar.svg(), 256);
    }
}
