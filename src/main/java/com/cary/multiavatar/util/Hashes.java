package com.cary.multiavatar.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希与进制工具。
 */
public final class Hashes {

    private Hashes() {
    }

    /**
     * 计算 SHA-256，输出小写十六进制（与 JS CryptoJS SHA256 的 toString() 一致）。
     */
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }
}
