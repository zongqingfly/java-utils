package com.fly.util;

/**
 * SQL 预处理工具类
 */
public final class SqlNormalizer {

    private SqlNormalizer() {}

    /**
     * 规范化 SQL：去除首尾空白、统一换行符、移除末尾分号
     */
    public static String normalize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL cannot be null or blank");
        }
        return sql.strip()
                  .replaceAll("\\r\\n|\\r", "\n")
                  .replaceAll(";\\s*$", "");
    }

    /**
     * 判断是否为 INSERT OVERWRITE（兼容 Hive/Spark 语法）
     */
    public static boolean isInsertOverwrite(String sql) {
        String upper = sql.toUpperCase().replaceAll("\\s+", " ");
        return upper.contains("INSERT OVERWRITE");
    }

    /**
     * 提取表名（去掉反引号、双引号、方括号等修饰符）
     */
    public static String unquote(String name) {
        if (name == null) return null;
        return name.replaceAll("^[`\"\\[]|[`\"\\]]$", "").trim();
    }
}
