package com.cary.multiavatar.data;

import com.cary.multiavatar.MultiavatarData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 头像静态数据的只读访问入口（单例）。
 *
 * <p>数据来源：{@link MultiavatarData}，由脚本从 multiavatar.js 自动提取。
 * 本类将可变 Map 包装为只读视图，隔离底层数据结构，供上层按「角色+主题」与「部件名」查询。</p>
 */
public final class DataTables {

    /**
     * 单一实例（数据为静态不可变，天然线程安全）。
     */
    public static final DataTables INSTANCE = new DataTables();

    private DataTables() {
    }

    /**
     * 取指定「角色+主题」（如 "00A"）下某部件的颜色列表。
     *
     * @return 颜色列表（元素可能是 "#RRGGBB"、"#RGB"、命名色或 "none"）；未命中返回 null
     */
    public List<String> colorsOf(String partKey, String partName) {
        Map<String, List<String>> theme = MultiavatarData.THEMES.get(partKey);
        if (theme == null) {
            return null;
        }
        return theme.get(partName);
    }

    /**
     * 取指定角色（如 "00"）下某部件的 SVG 模板（含 {@code #xxx;} 颜色占位符）。
     *
     * @return SVG 片段；未命中返回 null
     */
    public String templateOf(String partV, String partName) {
        Map<String, String> parts = MultiavatarData.PARTS.get(partV);
        if (parts == null) {
            return null;
        }
        return parts.get(partName);
    }

    /**
     * 全部主题数据（只读视图）。
     */
    public Map<String, Map<String, List<String>>> themes() {
        return Collections.unmodifiableMap(MultiavatarData.THEMES);
    }

    /**
     * 全部部件模板（只读视图）。
     */
    public Map<String, Map<String, String>> parts() {
        return Collections.unmodifiableMap(MultiavatarData.PARTS);
    }
}
