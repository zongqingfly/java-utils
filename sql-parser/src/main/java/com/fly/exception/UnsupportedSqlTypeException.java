package com.fly.exception;

/**
 * 不支持的 SQL 类型异常
 */
public class UnsupportedSqlTypeException extends SqlParseException {

    public UnsupportedSqlTypeException(String sqlType, String sql) {
        super("Unsupported SQL type: " + sqlType, sql);
    }
}
