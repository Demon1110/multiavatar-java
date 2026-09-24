package com.cary.multiavatar.core;

import com.cary.multiavatar.util.Hashes;

import java.nio.charset.StandardCharsets;

/**
 * 默认哈希策略：SHA-256 取前 12 位数字。
 *
 * <p>与 multiavatar.js 行为逐位一致：
 * {@code sha256Hex = SHA256(utf8(input))} → 去除非数字字符 → 取前 12 位。</p>
 */
public final class Sha256AvatarIdHasher implements AvatarIdHasher {

    @Override
    public String hash12(String input) {
        String hex = Hashes.sha256Hex(input.getBytes(StandardCharsets.UTF_8));

        // JS: hex.replace(/\D/g, '') —— 去掉所有非数字字符
        StringBuilder digits = new StringBuilder(64);
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }

        // JS: substring(0, 12)
        return digits.length() >= 12 ? digits.substring(0, 12) : digits.toString();
    }
}
