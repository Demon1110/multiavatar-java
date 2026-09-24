package com.cary.multiavatar.core;

/**
 * 头像标识哈希策略。
 *
 * <p>把任意输入字符串转换为 12 位数字 hash（后续每 2 位映射一个部件编号）。
 * 设计为策略接口，便于扩展不同的哈希算法；默认实现为 {@link Sha256AvatarIdHasher}。</p>
 */
public interface AvatarIdHasher {

    /**
     * 计算 12 位数字 hash。
     *
     * @param input 输入字符串（已保证非 null；为空串时调用方在进入本策略前已短路返回）
     * @return 12 位数字字符串（全为 '0'-'9'）
     */
    String hash12(String input);
}
