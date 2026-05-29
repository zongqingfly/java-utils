package com.fly.exception;

/**
 * SQL 解析异常
 */
public class SqlParseException extends RuntimeException {

    private final String sql;

    public SqlParseException(String message, String sql) {
        super(message);
        this.sql = sql;
    }

    public SqlParseException(String message, String sql, Throwable cause) {
        super(message, cause);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " | SQL: [" + sql + "]";
    }
}
