package com.cary.multiavatar;

import com.cary.multiavatar.core.SvgOptimizer;
import com.cary.multiavatar.render.SvgRasterizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 临时工具：读取 <dir> 下 tN.svg，用 SvgOptimizer 生成 tN.opt.svg，
 * 并将 tN.opt.svg 光栅化为 tN.opt.png（优化版 Chrome 对照用）。
 */
public final class OptPngTool {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        for (File f : dir.listFiles()) {
            if (!f.getName().matches("t\\d+\\.svg")) {
                continue;
            }
            String svg = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String opt = SvgOptimizer.optimize(svg);
            File osvg = new File(dir, f.getName().replace(".svg", ".opt.svg"));
            Files.write(osvg.toPath(), opt.getBytes(StandardCharsets.UTF_8));
            BufferedImage img = SvgRasterizer.rasterize(opt, 256);
            File opng = new File(dir, f.getName().replace(".svg", ".opt.png"));
            ImageIO.write(img, "png", opng);
        }
        System.out.println("opt svg+png done");
    }
}
