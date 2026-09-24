package com.cary.multiavatar.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 部件编号映射器：把 12 位数字 hash 转换为 6 个部件各自的编号（0-47）。
 *
 * <p>对应 multiavatar.js：
 * 每 2 位数字经 {@code Math.round((47/100) * 两位)} 映射为 0-47。</p>
 */
public final class PartNumberMapper {

    /**
     * 六个部件的固定定义顺序（与 JS 的 p 对象一致）。
     */
    public static final String[] PARTS = {"env", "clo", "head", "mouth", "eyes", "top"};

    private static final double SCALE = 47 / 100.0;

    private PartNumberMapper() {
    }

    /**
     * 计算各部件编号。
     *
     * @param hash12 12 位数字字符串（来自 {@link AvatarIdHasher}）
     * @return 部件名 -> 0-47 编号（按 {@link #PARTS} 顺序插入）
     */
    public static Map<String, Integer> map(String hash12) {
        Map<String, Integer> result = new LinkedHashMap<>(6);
        for (int i = 0; i < PARTS.length; i++) {
            result.put(PARTS[i], mapTo47(twoDigits(hash12, i * 2)));
        }
        return result;
    }

    /**
     * 对应 JS：Math.round((47/100) * 两位数字)，返回 0-47。
     */
    private static int mapTo47(int twoDigits) {
        return (int) Math.round(SCALE * twoDigits);
    }

    /**
     * 从 hash 中取从 idx 开始的两位数字。
     */
    private static int twoDigits(String hash, int idx) {
        if (idx + 2 > hash.length()) {
            return 0;
        }
        return (hash.charAt(idx) - '0') * 10 + (hash.charAt(idx + 1) - '0');
    }
}
