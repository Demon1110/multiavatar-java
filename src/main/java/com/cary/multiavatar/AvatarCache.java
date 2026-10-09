package com.cary.multiavatar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 全局 LRU 缓存（线程安全，JDK8 零依赖）。
 *
 * <p>键 = 影响 SVG 组装的参数（{@code input / sansEnv / part / theme / svgWidth / svgHeight}），
 * 值 = {@link Avatar}。同一输入重复请求时直接复用已组装的 SVG，免去重复 SHA-256 与拼装；
 * 容量满时淘汰最久未使用项。{@code size / format} 不影响 SVG 文本，不入键——
 * 同一个 {@link Avatar} 可渲染任意尺寸与格式。</p>
 *
 * <p>由 {@link Multiavatar#avatar(String, AvatarOptions)} 自动接入；也可独立使用。</p>
 */
public final class AvatarCache {

    /**
     * 默认容量：128 个不同输入（键内参数不同均单独计数）。
     */
    public static final int DEFAULT_CAPACITY = 128;

    private final int capacity;
    private final Map<Key, Avatar> map;

    /**
     * @param capacity 最大缓存条目数
     */
    public AvatarCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        // accessOrder=true：LinkedHashMap 按访问序维护，配合 removeEldestEntry 即 LRU
        this.map = new LinkedHashMap<Key, Avatar>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, Avatar> eldest) {
                return size() > AvatarCache.this.capacity;
            }
        };
    }

    public static AvatarCache defaults() {
        return new AvatarCache(DEFAULT_CAPACITY);
    }

    public synchronized Avatar get(Key key) {
        return key == null ? null : map.get(key);
    }

    public synchronized void put(Key key, Avatar avatar) {
        if (key != null && avatar != null) {
            map.put(key, avatar);
        }
    }

    /**
     * 当前缓存条目数。
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * 清空缓存。
     */
    public synchronized void clear() {
        map.clear();
    }

    /**
     * 缓存键：SVG 组装相关的全部参数（值不可变）。
     */
    public static final class Key {

        private final String input;
        private final boolean sansEnv;
        private final String part;
        private final String theme;
        private final int svgWidth;
        private final int svgHeight;
        private final boolean optimizeSvg;

        public Key(String input, boolean sansEnv, String part, String theme,
                   int svgWidth, int svgHeight, boolean optimizeSvg) {
            this.input = input == null ? "" : input;
            this.sansEnv = sansEnv;
            this.part = part;
            this.theme = theme;
            this.svgWidth = svgWidth;
            this.svgHeight = svgHeight;
            this.optimizeSvg = optimizeSvg;
        }

        private static boolean eq(String a, String b) {
            return Objects.equals(a, b);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            return sansEnv == k.sansEnv
                    && svgWidth == k.svgWidth
                    && svgHeight == k.svgHeight
                    && optimizeSvg == k.optimizeSvg
                    && input.equals(k.input)
                    && eq(part, k.part)
                    && eq(theme, k.theme);
        }

        @Override
        public int hashCode() {
            int h = input.hashCode();
            h = 31 * h + (sansEnv ? 1 : 0);
            h = 31 * h + (part == null ? 0 : part.hashCode());
            h = 31 * h + (theme == null ? 0 : theme.hashCode());
            h = 31 * h + svgWidth;
            h = 31 * h + svgHeight;
            h = 31 * h + (optimizeSvg ? 1 : 0);
            return h;
        }
    }
}
