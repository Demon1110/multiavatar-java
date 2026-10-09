package com.cary.multiavatar;

import com.cary.multiavatar.render.SvgRasterizer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 临时工具：渲染任意 SVG 文件（256×256），输出同名 .png。
 * 用法：java -cp ... SvgFileTool <dir> <tag>
 * 读取 <dir>/<tag>.svg 写出 <dir>/<tag>.png。
 */
public final class SvgFileTool {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String tag = args[1];
        String svg = new String(Files.readAllBytes(new File(dir, tag + ".svg").toPath()),
                StandardCharsets.UTF_8);
        BufferedImage img = SvgRasterizer.rasterize(svg, 256);
        javax.imageio.ImageIO.write(img, "png", new File(dir, tag + ".png"));
        System.out.println("rendered " + tag);
    }
}
