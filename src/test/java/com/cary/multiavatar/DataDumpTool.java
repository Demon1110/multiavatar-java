package com.cary.multiavatar;

import com.cary.multiavatar.data.MultiavatarData;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * 一次性导出工具：把 {@link MultiavatarData} 的静态数据（THEMES/PARTS）
 * 序列化为 gzip 资源 {@code src/main/resources/multiavatar-data.gz}。
 *
 * <p>行格式（UTF-8，每行一条）：
 * <ul>
 *   <li>主题：{@code T|<partKey(角色2位+主题字母)>|<partName>|<c1>,<c2>,...}</li>
 *   <li>部件：{@code P|<partKey(角色2位)>|<partName>|<utf8字节数>:<svg 原文>}</li>
 * </ul>
 *
 * <p>运行（需先编译）：{@code java -cp target/classes com.cary.multiavatar.DataDumpTool src/main/resources/multiavatar-data.gz}
 */
public final class DataDumpTool {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "src/main/resources/multiavatar-data.gz";
        try (Writer w = new OutputStreamWriter(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(out))), StandardCharsets.UTF_8)) {
            // THEMES: partKey(如 "00A") -> partName -> 颜色列表
            for (Map.Entry<String, Map<String, List<String>>> theme : MultiavatarData.THEMES.entrySet()) {
                for (Map.Entry<String, List<String>> e : theme.getValue().entrySet()) {
                    StringBuilder sb = new StringBuilder("T|").append(theme.getKey()).append('|')
                            .append(e.getKey()).append('|');
                    boolean first = true;
                    for (String c : e.getValue()) {
                        if (!first) {
                            sb.append(',');
                        }
                        first = false;
                        sb.append(c);
                    }
                    w.write(sb.toString());
                    w.write('\n');
                }
            }
            // PARTS: partKey(如 "00") -> partName -> svg（长度前缀，避免 base64 膨胀与分隔符冲突）
            for (Map.Entry<String, Map<String, String>> part : MultiavatarData.PARTS.entrySet()) {
                for (Map.Entry<String, String> e : part.getValue().entrySet()) {
                    w.write("P|");
                    w.write(part.getKey());
                    w.write('|');
                    w.write(e.getKey());
                    w.write('|');
                    byte[] svg = e.getValue().getBytes(StandardCharsets.UTF_8);
                    w.write(String.valueOf(svg.length));
                    w.write(':');
                    w.write(e.getValue());
                    w.write('\n');
                }
            }
        }
        System.out.println("已写出: " + out);
    }
}
