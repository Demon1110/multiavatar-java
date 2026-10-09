package com.cary.multiavatar;

import com.cary.multiavatar.core.AvatarSpec;
import com.cary.multiavatar.core.Sha256AvatarIdHasher;
import com.cary.multiavatar.core.SvgFragmentPainter;
import com.cary.multiavatar.data.DataTables;
import com.cary.multiavatar.render.AvatarFormat;
import com.cary.multiavatar.render.Renderers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * 逐部件渲染诊断（非库代码）：把单个头像的每个部件拆成独立 SVG + PNG，
 * 便于与浏览器逐部件对照，定位渲染差异。
 *
 * <p>运行：{@code java -cp target/classes:target/test-classes com.cary.multiavatar.PartCheckTool <input> <outDir>}</p>
 */
public final class PartCheckTool {

    private PartCheckTool() {
    }

    public static void main(String[] args) throws Exception {
        String input = args.length > 0 ? args[0] : "user@example.com";
        File dir = new File(args.length > 1 ? args[1] : "target/partcheck");
        dir.mkdirs();

        String full = Multiavatar.avatar(input, AvatarOptions.builder().optimizeSvg(false).build()).svg();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, "full.svg")), StandardCharsets.UTF_8)) {
            w.write(full);
        }
        Multiavatar.writePng(input, new File(dir, "full.png"));

        // 逐部件：用 AvatarSpec 取每个部件的角色/主题，复用内部组装逻辑输出独立 SVG
        String[] parts = {"env", "head", "clo", "top", "eyes", "mouth"};
        for (String p : parts) {
            String svg = partSvg(input, p);
            if (svg == null) {
                continue;
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir,
                    "part_" + p + ".svg")), StandardCharsets.UTF_8)) {
                w.write(svg);
            }
            byte[] png = Renderers.create(AvatarFormat.PNG)
                    .render(svg, 256);
            try (FileOutputStream fos = new FileOutputStream(new File(dir, "part_" + p + ".png"))) {
                fos.write(png);
            }
        }
        System.out.println("part check files in " + dir.getAbsolutePath());
    }

    /**
     * 生成只含单个部件的 SVG。
     */
    private static String partSvg(String input, String partName) {
        AvatarSpec spec = AvatarSpec.resolve(input, new Sha256AvatarIdHasher(), null, null);
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 231 231\">"
                + new SvgFragmentPainter(DataTables.INSTANCE).paint(partName, spec.partVOf(partName), spec.themeOf(partName))
                + "</svg>";
        return svg;
    }
}
