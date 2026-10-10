package com.cary.multiavatar.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * 头像静态数据（颜色主题 + SVG 部件模板）。
 *
 * <p>数据由 {@code DataDumpTool}（src/test，可复现）从 multiavatar.js 提取结果序列化
 * 为 gzip 资源 {@code /multiavatar-data.gz}（约 25KB，vs 旧源码内嵌约 74KB），
 * 本类在首次类加载时解压一次并构建只读 Map，对外 API（THEMES/PARTS）与旧版完全一致。</p>
 *
 * <p>行格式（UTF-8）：
 * <ul>
 *   <li>{@code T|<partKey>|<partName>|<c1>,<c2>,...} —— 主题颜色</li>
 *   <li>{@code P|<partKey>|<partName>|<utf8字节数>:<svg>} —— 部件模板</li>
 * </ul></p>
 */
public final class MultiavatarData {
    private static final Object[] DATA = parse();

    /**
     * 颜色主题: THEMES[partKey(角色2位+主题,如"00A")][partName] -> 颜色列表(可为"none")
     */
    public static final Map<String, Map<String, List<String>>> THEMES =
            Collections.unmodifiableMap((Map<String, Map<String, List<String>>>) DATA[0]);
    /**
     * SVG 部件模板: PARTS[partKey(角色2位,如"00")][partName] -> SVG 片段(含 #xxx; 颜色占位符)
     */
    public static final Map<String, Map<String, String>> PARTS =
            Collections.unmodifiableMap((Map<String, Map<String, String>>) DATA[1]);

    private MultiavatarData() {
    }

    /**
     * 一次性解析：返回 [主题Map, 部件Map]。
     */
    private static Object[] parse() {
        Map<String, Map<String, List<String>>> themes = new HashMap<String, Map<String, List<String>>>();
        Map<String, Map<String, String>> parts = new HashMap<String, Map<String, String>>();
        InputStream in = MultiavatarData.class.getResourceAsStream("/multiavatar-data.gz");
        if (in == null) {
            throw new IllegalStateException("缺少数据资源 /multiavatar-data.gz");
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.charAt(0) == 'T') {
                    // T|<partKey>|<partName>|<c1>,<c2>,...
                    int i = line.indexOf('|', 2);
                    int j = line.indexOf('|', i + 1);
                    String partKey = line.substring(2, i);
                    String partName = line.substring(i + 1, j);
                    List<String> colors = Arrays.asList(line.substring(j + 1).split(","));
                    Map<String, List<String>> byPart = themes.get(partKey);
                    if (byPart == null) {
                        byPart = new HashMap<String, List<String>>();
                        themes.put(partKey, byPart);
                    }
                    byPart.put(partName, colors);
                } else {
                    // P|<partKey>|<partName>|<utf8字节数>:<svg>
                    int i = line.indexOf('|', 2);
                    int j = line.indexOf('|', i + 1);
                    int k = line.indexOf(':', j + 1);
                    int len = Integer.parseInt(line.substring(j + 1, k));
                    String svg = line.substring(k + 1, k + 1 + len);
                    String partKey = line.substring(2, i);
                    String partName = line.substring(i + 1, j);
                    Map<String, String> byPart = parts.get(partKey);
                    if (byPart == null) {
                        byPart = new HashMap<String, String>();
                        parts.put(partKey, byPart);
                    }
                    byPart.put(partName, svg);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("解析 /multiavatar-data.gz 失败", e);
        }
        return new Object[]{themes, parts};
    }
}
