package com.cary.multiavatar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 与 JS 基准输出对比用的工具（非库代码）。
 * 使用与 gen_benchmark.js 完全相同的测试输入与模式，把 Java 的结果写成 JSON 文件，
 * 再由脚本与 benchmark.json 逐项比对。
 *
 * <p>运行：{@code java -cp target/classes multiavatar.CompareWithJs target/compare_java.json}</p>
 */
public final class CompareWithJs {

    private CompareWithJs() {
    }

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "target/compare_java.json";

        List<String> tests = Arrays.asList(
                "", "Binx Bond", "test", "a", "1", "123456789", "张三", "user@example.com",
                "Hello World!", repeat('x', 64), "  leading space  ", "AaBbCcDd", "000000000000");

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // basic
        sb.append("\"basic\":{");
        appendMap(sb, tests, false);
        sb.append("},\"sansEnv\":{");
        appendMap(sb, tests.subList(1, 5), true);
        sb.append("},\"ver\":{");
        appendMap(sb, tests.subList(1, 2), false, "00", "A");
        sb.append(",").append(escJson("test")).append(":");
        sb.append(escJson(rawSvg("test", false, "15", "C")));
        sb.append("}");

        sb.append("}");

        File f = new File(out);
        f.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        System.out.println("已写出: " + f.getAbsolutePath());
    }

    private static void appendMap(StringBuilder sb, List<String> keys, boolean sansEnv) {
        appendMap(sb, keys, sansEnv, null, null);
    }

    private static void appendMap(StringBuilder sb, List<String> keys, boolean sansEnv,
                                  String part, String theme) {
        boolean first = true;
        for (String t : keys) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(escJson(t)).append(":");
            sb.append(escJson(rawSvg(t, sansEnv, part, theme)));
        }
    }

    /**
     * 与官方 JS 逐字符比对的基线必须关闭 SVG 体积优化。
     */
    private static String rawSvg(String input, boolean sansEnv, String part, String theme) {
        return Multiavatar.avatar(input, AvatarOptions.builder()
                .sansEnv(sansEnv).part(part).theme(theme).optimizeSvg(false).build()).svg();
    }

    private static String escJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        char[] a = new char[n];
        Arrays.fill(a, c);
        return new String(a);
    }
}
