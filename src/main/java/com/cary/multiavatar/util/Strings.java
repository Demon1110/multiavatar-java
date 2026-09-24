package com.cary.multiavatar.util;

/**
 * 字符串小工具。
 */
public final class Strings {

    private Strings() {
    }

    /**
     * 两位补零格式化（与 JS 中 nr.length==1 时补 '0' 的语义一致）。
     */
    public static String two(int x) {
        return x < 10 ? "0" + x : Integer.toString(x);
    }

    /**
     * 是否为空字符串（null 视为空）。
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
