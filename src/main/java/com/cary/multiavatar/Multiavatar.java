package com.cary.multiavatar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multiavatar —— 多文化头像生成器，纯 Java 实现。
 *
 * <p>移植自 <a href="https://github.com/multiavatar/Multiavatar">multiavatar.js</a>（Gie Katon, 2020-2021），
 * 不依赖任何第三方库，JDK 8 及以上即可运行。</p>
 *
 * <p>算法：对输入字符串做 SHA-256，取十六进制中前 12 位数字，每 2 位映射为一个 0-47 的部件编号；
 * 编号进一步换算成 16 个初始角色(00-15) + 3 个颜色主题(A/B/C)；每个角色部件对应的 SVG 模板中的
 * {@code #xxx;} 颜色占位符被该主题的颜色按序替换，最后按
 * env→head→clo→top→eyes→mouth 顺序拼装成完整 SVG。</p>
 *
 * <p>共可生成 16^6 = 12,230,590,464 个唯一头像。</p>
 */
public final class Multiavatar {

    /**
     * 输出时的部件拼装顺序（与 multiavatar.js 一致）。
     */
    private static final String[] PART_ORDER = {"env", "head", "clo", "top", "eyes", "mouth"};

    /**
     * 对应 JS 的 /#(.*?);/g —— 匹配形如 #fff; 的颜色占位符。
     */
    private static final Pattern COLOR_PH = Pattern.compile("#([^;]*);");

    private static final String SVG_START = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 256 256\">";
    private static final String SVG_END = "</svg>";

    private Multiavatar() {
    }

    /**
     * 生成头像 SVG（含环境背景圆）。
     */
    public static String multiavatar(String string) {
        return multiavatar(string, false, null, null);
    }

    /**
     * 生成头像 SVG；{@code sansEnv=true} 时去掉背景圆（环境部件）。
     */
    public static String multiavatar(String string, boolean sansEnv) {
        return multiavatar(string, sansEnv, null, null);
    }

    /**
     * 生成头像 SVG。
     *
     * @param string  输入字符串（头像标识）
     * @param sansEnv 为 true 时输出不含背景圆（环境部件）
     * @param part    强制指定初始角色编号，如 "00"~"15"（对应 JS 的 ver.part）；传 null 表示自动
     * @param theme   强制指定颜色主题 "A"/"B"/"C"（对应 JS 的 ver.theme）；传 null 表示自动
     * @return SVG 代码；当输入为空字符串时返回空串（与 JS 一致）
     */
    public static String multiavatar(String string, boolean sansEnv, String part, String theme) {
        if (string == null) {
            string = "";
        }

        // JS: if (string.length == 0) return hash;  —— 空字符串直接返回空串
        if (string.isEmpty()) {
            return "";
        }

        // ---- SHA-256（标准实现，与 JS 内置 CryptoJS 输出一致，小写 hex）----
        String hex = sha256Hex(string.getBytes(StandardCharsets.UTF_8));

        // JS: sha256Numbers = hex.replace(/\D/g,'')  —— 去掉所有非数字字符
        StringBuilder digits = new StringBuilder(64);
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        // JS: hash = sha256Numbers.substring(0,12)  —— 取前 12 位数字
        String hash = digits.length() >= 12 ? digits.substring(0, 12) : digits.toString();

        // ---- 每 2 位数字 -> 0-47 的部件编号（JS: Math.round((47/100)*两位)）----
        Map<String, Integer> parts = new LinkedHashMap<>();
        parts.put("env", mapTo47(twoDigits(hash, 0)));
        parts.put("clo", mapTo47(twoDigits(hash, 2)));
        parts.put("head", mapTo47(twoDigits(hash, 4)));
        parts.put("mouth", mapTo47(twoDigits(hash, 6)));
        parts.put("eyes", mapTo47(twoDigits(hash, 8)));
        parts.put("top", mapTo47(twoDigits(hash, 10)));

        // ---- 编号 -> 初始角色(00-15) + 主题(A/B/C)（JS 第 738-754 行）----
        Map<String, String> partKeys = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : parts.entrySet()) {
            int nr = e.getValue();
            int base;
            String th;
            if (nr > 31) {
                base = nr - 32;
                th = "C";
            } else if (nr > 15) {
                base = nr - 16;
                th = "B";
            } else {
                base = nr;
                th = "A";
            }
            partKeys.put(e.getKey(), two(base) + th);
        }

        // ---- 为每个部件取 SVG（JS: final[part] = getFinal(...)）----
        Map<String, String> finalParts = new LinkedHashMap<>();
        for (String partName : PART_ORDER) {
            String key = partKeys.get(partName);
            String partV = key.substring(0, 2); // 初始角色编号
            String th = key.substring(2, 3);    // 主题
            if (part != null) {
                partV = part;
            }
            if (theme != null) {
                th = theme;
            }
            finalParts.put(partName, getFinal(partName, partV, th));
        }

        // ---- sansEnv：去掉环境部件 ----
        if (sansEnv) {
            finalParts.put("env", "");
        }

        // ---- 按固定顺序拼装输出（JS 第 804 行）----
        StringBuilder sb = new StringBuilder(4096);
        sb.append(SVG_START);
        for (String partName : PART_ORDER) {
            sb.append(finalParts.get(partName));
        }
        sb.append(SVG_END);
        return sb.toString();
    }

    /**
     * 对应 JS getFinal：用主题颜色按序替换 SVG 模板中的颜色占位符。
     */
    private static String getFinal(String partName, String partV, String theme) {
        List<String> colors = MultiavatarData.THEMES.get(partV + theme).get(partName);
        String svg = MultiavatarData.PARTS.get(partV).get(partName);
        if (svg == null) {
            return "";
        }

        // 提取所有 #xxx; 占位符（与 JS 的 match 一致，顺序保持）
        Matcher m = COLOR_PH.matcher(svg);
        List<String> result = new ArrayList<>();
        while (m.find()) {
            result.add(m.group(0));
        }

        String out = svg;
        // JS: resultFinal.replace(result[i], colors[i]+';') —— 只替换第一个出现
        for (int i = 0; i < result.size(); i++) {
            out = out.replaceFirst(Pattern.quote(result.get(i)), Matcher.quoteReplacement(colors.get(i) + ";"));
        }
        return out;
    }

    /**
     * 对应 JS：Math.round((47/100) * 两位数字)，返回 0-47。
     */
    private static int mapTo47(int twoDigits) {
        return (int) Math.round((47 / 100.0) * twoDigits);
    }

    /**
     * 从 hash（12 位数字）中取从 idx 开始的两位数字（idx 为 0/2/4/6/8/10）。
     */
    private static int twoDigits(String hash, int idx) {
        if (idx + 2 > hash.length()) {
            return 0;
        }
        return (hash.charAt(idx) - '0') * 10 + (hash.charAt(idx + 1) - '0');
    }

    /**
     * 两位补零格式化。
     */
    private static String two(int x) {
        return x < 10 ? "0" + x : Integer.toString(x);
    }

    /**
     * 计算 SHA-256 并输出小写十六进制。
     */
    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
