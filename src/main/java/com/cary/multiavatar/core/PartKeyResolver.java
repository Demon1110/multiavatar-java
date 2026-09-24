package com.cary.multiavatar.core;

import com.cary.multiavatar.util.Strings;

/**
 * 部件键解析器：把 0-47 的编号换算为「初始角色(00-15) + 颜色主题(A/B/C)」的部件键。
 *
 * <p>对应 multiavatar.js：
 * {@code nr>31 → nr-32+'C'；nr>15 → nr-16+'B'；否则 nr+'A'}，其中角色号补零到两位。</p>
 */
public final class PartKeyResolver {

    private PartKeyResolver() {
    }

    /**
     * 编号 → 部件键（如 16 → "00B"，35 → "03C"，7 → "07A"）。
     */
    public static String resolve(int nr) {
        int base;
        String theme;
        if (nr > 31) {
            base = nr - 32;
            theme = "C";
        } else if (nr > 15) {
            base = nr - 16;
            theme = "B";
        } else {
            base = nr;
            theme = "A";
        }
        return Strings.two(base) + theme;
    }
}
