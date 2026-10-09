package com.cary.multiavatar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * PNG 渲染验证工具（非库代码）：批量生成 SVG 与 PNG，供与浏览器渲染对照。
 * 覆盖常规输入与强制角色/主题（含带 opacity 半透明颜色的 eyes 部件）。
 *
 * <p>运行：{@code java -cp target/classes:target/test-classes com.cary.multiavatar.PngCheckTool <outDir>}</p>
 */
public final class PngCheckTool {

    private PngCheckTool() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "target/pngcheck");
        dir.mkdirs();

        String[] tests = {
                "Binx Bond", "test", "张三", "user@example.com", "123456789",
                "Hello World!", "a", "000000000000", "  leading space  ", "AaBbCcDd",
                "apple", "banana", "cherry", "data", "engineer", "flower", "github",
                "hello", "java", "kotlin", "lambda", "maven", "nodejs", "orange",
                "python", "query", "spring", "tuple", "unique", "vector"
        };
        // 强制角色+主题（覆盖全部 4 个带 opacity 的颜色：eyes 部件）
        String[][] forced = {
                {"05", "B"}, {"11", "B"}, {"15", "A"}, {"15", "B"}
        };

        List<String[]> cases = new ArrayList<>();
        for (String t : tests) {
            cases.add(new String[]{t, null, null});
        }
        for (String[] f : forced) {
            cases.add(new String[]{"forced-opacity-" + f[0] + f[1], f[0], f[1]});
        }

        int i = 0;
        for (String[] c : cases) {
            AvatarOptions opts = AvatarOptions.builder()
                    .part(c[1]).theme(c[2]).size(512).build();
            String svg = Multiavatar.multiavatar(c[0], false, c[1], c[2]);
            try (Writer w = new OutputStreamWriter(
                    new FileOutputStream(new File(dir, "t" + i + ".svg")), StandardCharsets.UTF_8)) {
                w.write(svg);
            }
            Multiavatar.writePng(c[0], opts, new File(dir, "t" + i + ".png"));
            i++;
        }
        System.out.println("total=" + i);
    }
}
