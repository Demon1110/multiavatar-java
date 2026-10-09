package com.cary.multiavatar;

import com.cary.multiavatar.render.SvgRasterizer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 临时工具：把 <dir> 下 tN.svg（原文）光栅化为 tN.raw.png（256）。
 */
public final class RawPngTool {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        for (File f : dir.listFiles()) {
            if (!f.getName().matches("t\\d+\\.svg")) {
                continue;
            }
            String svg = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            BufferedImage img = SvgRasterizer.rasterize(svg, 256);
            File out = new File(dir, f.getName().replace(".svg", ".raw.png"));
            javax.imageio.ImageIO.write(img, "png", out);
        }
        System.out.println("raw pngs done");
    }
}
