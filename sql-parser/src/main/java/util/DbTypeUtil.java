package util;

import com.alibaba.druid.DbType;

/**
 * 数据库方言工具类
 */
public final class DbTypeUtil {

    private DbTypeUtil() {}

    public static DbType from(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return DbType.mysql;
        }
        return switch (dbType.toLowerCase().trim()) {
            case "mysql"      -> DbType.mysql;
            case "hive"       -> DbType.hive;
            case "spark"      -> DbType.hive;  // Spark SQL 兼容 Hive 方言
            case "postgresql",
                 "postgres"   -> DbType.postgresql;
            case "oracle"     -> DbType.oracle;
            case "sqlserver",
                 "mssql"      -> DbType.sqlserver;
            case "db2"        -> DbType.db2;
            case "clickhouse" -> DbType.clickhouse;
            case "presto",
                 "trino"      -> DbType.presto;
            default           -> DbType.mysql;
        };
    }
}
