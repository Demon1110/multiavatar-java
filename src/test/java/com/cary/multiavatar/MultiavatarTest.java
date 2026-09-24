package com.cary.multiavatar;

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
        if (args.length == 1) {
            System.out.print(multiavatar(args[0]));
            return;
        }

        String[] samples = {"Binx Bond", "test", "张三", "user@example.com", "123456789"};
        java.io.File dir = new java.io.File("demo");
        dir.mkdirs();
        for (String s : samples) {
            String name = s.replaceAll("[^A-Za-z0-9_-]", "_");
            java.io.File f = new java.io.File(dir, "avatar_" + name + ".svg");
            try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write(multiavatar(s));
            }
            System.out.println("已生成: " + f.getAbsolutePath());
        }
    }
}
