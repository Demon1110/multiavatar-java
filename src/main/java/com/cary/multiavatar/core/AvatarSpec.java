package com.cary.multiavatar.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 头像部件规格（不可变）：记录 6 个部件各自选中的初始角色与颜色主题。
 *
 * <p>由 {@link AvatarSpec#resolve(String, AvatarIdHasher, String, String)} 从输入推导，
 * 也可通过强制指定 {@code part}/{@code theme}（对应 JS 的 ver 参数）覆盖任意部件的选择。</p>
 */
public final class AvatarSpec {

    /**
     * 输出拼装顺序（与 multiavatar.js 一致）：env→head→clo→top→eyes→mouth。
     */
    public static final String[] PART_ORDER = {"env", "head", "clo", "top", "eyes", "mouth"};

    private final Map<String, String> partKeys;

    private AvatarSpec(Map<String, String> partKeys) {
        this.partKeys = Collections.unmodifiableMap(new LinkedHashMap<>(partKeys));
    }

    /**
     * 从输入推导规格。
     *
     * @param input       非空输入字符串（调用方需保证非空）
     * @param hasher      哈希策略
     * @param forcedPart  强制角色编号（对应 JS 的 ver.part），null 表示自动
     * @param forcedTheme 强制主题（对应 JS 的 ver.theme），null 表示自动
     */
    public static AvatarSpec resolve(String input, AvatarIdHasher hasher,
                                     String forcedPart, String forcedTheme) {
        Map<String, Integer> numbers = PartNumberMapper.map(hasher.hash12(input));

        Map<String, String> keys = new LinkedHashMap<>(6);
        for (Map.Entry<String, Integer> e : numbers.entrySet()) {
            keys.put(e.getKey(), PartKeyResolver.resolve(e.getValue()));
        }

        // 强制指定：所有部件统一使用（JS: if (ver) partV=ver.part; theme=ver.theme）
        if (forcedPart != null || forcedTheme != null) {
            for (String part : keys.keySet()) {
                String k = keys.get(part);
                keys.put(part, (forcedPart != null ? forcedPart : k.substring(0, 2))
                        + (forcedTheme != null ? forcedTheme : k.substring(2, 3)));
            }
        }
        return new AvatarSpec(keys);
    }

    /**
     * 部件名（env/clo/head/mouth/eyes/top）→ 部件键（如 "00A"）。
     */
    public String partKeyOf(String partName) {
        return partKeys.get(partName);
    }

    /**
     * 部件名 → 初始角色编号（2 位，如 "00"）。
     */
    public String partVOf(String partName) {
        String key = partKeys.get(partName);
        return key.substring(0, 2);
    }

    /**
     * 部件名 → 颜色主题（"A"/"B"/"C"）。
     */
    public String themeOf(String partName) {
        String key = partKeys.get(partName);
        return key.substring(2, 3);
    }
}
