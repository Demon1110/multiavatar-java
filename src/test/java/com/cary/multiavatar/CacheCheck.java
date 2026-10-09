package com.cary.multiavatar;

/**
 * 全局 LRU 缓存行为验证（零依赖，main + 断言）：
 * 同参数命中、键区分（size 不入键 / part 入键）、容量淘汰（LRU 顺序）、清空与尺寸接口。
 * 运行：java -cp target/classes;target/test-classes com.cary.multiavatar.CacheCheck
 */
public final class CacheCheck {

    public static void main(String[] args) {
        Multiavatar.clearCache();
        check(Multiavatar.cacheSize() == 0, "初始缓存为空");

        // 1. 同参数命中：返回同一实例（避免重复 compose）
        Avatar a1 = Multiavatar.avatar("Binx Bond");
        Avatar a2 = Multiavatar.avatar("Binx Bond");
        check(a1 == a2, "同输入命中缓存（同一实例）");
        check(Multiavatar.cacheSize() == 1, "缓存条目数 1");

        // 2. 键区分：size 不影响 SVG，不入键 → 仍命中；part/theme/sansEnv 入键 → 未命中
        //    part 为角色编号（2 位数字，对应 JS ver.part）、theme 为主题字母（A/B/C，对应 JS ver.theme）
        check(Multiavatar.avatar("Binx Bond", AvatarOptions.builder().size(512).build()) == a1,
                "size 不入键（512 与 256 同实例）");
        check(Multiavatar.avatar("Binx Bond", AvatarOptions.builder().svgSize(512, 512).build()) != a1,
                "svgSize 入键（不同实例）");
        check(Multiavatar.avatar("Binx Bond", AvatarOptions.builder().part("00").build()) != a1,
                "part 入键（不同实例）");
        check(Multiavatar.avatar("Binx Bond", AvatarOptions.builder().theme("A").build()) != a1,
                "theme 入键（不同实例）");
        check(Multiavatar.avatar("Binx Bond", AvatarOptions.builder().sansEnv(true).build()) != a1,
                "sansEnv 入键（不同实例）");
        check(Multiavatar.avatar("other") != a1, "不同输入不同实例");

        // 3. 容量淘汰：容量 2 时第 3 个不同键淘汰最久未使用项
        AvatarCache small = new AvatarCache(2);
        AvatarCache.Key k1 = new AvatarCache.Key("a", false, null, null, 256, 256);
        AvatarCache.Key k2 = new AvatarCache.Key("b", false, null, null, 256, 256);
        AvatarCache.Key k3 = new AvatarCache.Key("c", false, null, null, 256, 256);
        Avatar v1 = Multiavatar.avatar("a");
        Avatar v2 = Multiavatar.avatar("b");
        Avatar v3 = Multiavatar.avatar("c");
        small.put(k1, v1);
        small.put(k2, v2);
        small.get(k1); // k1 最近使用
        small.put(k3, v3); // 淘汰 k2
        check(small.size() == 2, "容量 2 缓存条目数 2");
        check(small.get(k1) == v1, "k1 仍在（最近使用）");
        check(small.get(k2) == null, "k2 被淘汰（最久未使用）");
        check(small.get(k3) == v3, "k3 已入缓存");

        // 4. 清空
        Multiavatar.clearCache();
        check(Multiavatar.cacheSize() == 0, "clearCache 后为空");
        check(Multiavatar.avatar("Binx Bond") != a1, "清空后重新组装（新实例）");

        System.out.println("OK: LRU 缓存验证全部通过");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException("FAIL: " + msg);
        }
        System.out.println("PASS: " + msg);
    }
}
