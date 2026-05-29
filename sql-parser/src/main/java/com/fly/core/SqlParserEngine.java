package com.fly.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.DbType;
import com.fly.exception.SqlParseException;
import com.fly.model.CreateTableResult;
import com.fly.model.InsertResult;
import com.fly.model.ParseResult;
import com.fly.parser.CreateTableParser;
import com.fly.parser.InsertParser;
import com.fly.util.DbTypeUtil;
import com.fly.util.SqlNormalizer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * SQL 解析引擎 —— 对外唯一入口
 *
 * <p>使用示例：
 * <pre>{@code
 * SqlParserEngine engine = SqlParserEngine.builder()
 *     .dbType("hive")
 *     .build();
 *
 * CreateTableResult result = engine.parseCreateTable(sql);
 * InsertResult insert = engine.parseInsert(sql);
 *
 * // 泛型方式（自动派发）
 * ParseResult r = engine.parse(sql);
 * }</pre>
 */
@Slf4j
public class SqlParserEngine {

    private final DbType dbType;
    private final SqlParserRegistry registry;

    private SqlParserEngine(Builder builder) {
        this.dbType = builder.dbType;
        this.registry = new SqlParserRegistry()
                .register(new CreateTableParser())
                .register(new InsertParser());

        // 扩展点：注册自定义解析器
        builder.extraParsers.forEach(registry::register);
    }

    // ── 强类型便捷方法 ────────────────────────────────────────────────────────

    /**
     * 解析 CREATE TABLE 语句
     */
    public CreateTableResult parseCreateTable(String sql) {
        return parse(sql);
    }

    /**
     * 解析 INSERT / INSERT OVERWRITE 语句
     */
    public InsertResult parseInsert(String sql) {
        return parse(sql);
    }

    /**
     * 解析任意 SQL（自动派发到对应解析器）
     *
     * @param sql 原始 SQL 字符串
     * @param <T> 期望的解析结果类型
     * @return 解析结果
     * @throws SqlParseException 解析失败时抛出
     */
    public <T extends ParseResult> T parse(String sql) {
        String normalized = SqlNormalizer.normalize(sql);
        log.debug("Parsing SQL [dbType={}]: {}", dbType, normalized);

        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(normalized, dbType);
            if (statements.isEmpty()) {
                throw new SqlParseException("No SQL statement found", sql);
            }
            if (statements.size() > 1) {
                log.warn("Multiple statements found; only the first will be parsed");
            }
            return registry.dispatch(statements.get(0), normalized);
        } catch (ParserException e) {
            throw new SqlParseException("Failed to parse SQL: " + e.getMessage(), sql, e);
        }
    }

    /**
     * 解析多条 SQL（以分号分隔）
     */
    public List<ParseResult> parseMultiple(String sql) {
        String normalized = sql.strip().replaceAll("\\r\\n|\\r", "\n");
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(normalized, dbType);
            return statements.stream()
                    .<ParseResult>map(stmt -> registry.dispatch(stmt, stmt.toString()))
                    .toList();
        } catch (ParserException e) {
            throw new SqlParseException("Failed to parse SQL: " + e.getMessage(), sql, e);
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DbType dbType = DbType.mysql;
        private final List<SqlParser<? extends ParseResult>> extraParsers = new java.util.ArrayList<>();

        /** 设置数据库方言 */
        public Builder dbType(String dbType) {
            this.dbType = DbTypeUtil.from(dbType);
            return this;
        }

        /** 设置数据库方言（枚举） */
        public Builder dbType(DbType dbType) {
            this.dbType = dbType;
            return this;
        }

        /** 注册自定义解析器 */
        public Builder registerParser(SqlParser<? extends ParseResult> parser) {
            extraParsers.add(parser);
            return this;
        }

        public SqlParserEngine build() {
            return new SqlParserEngine(this);
        }
    }

    // ── 静态工厂（快速创建） ───────────────────────────────────────────────────

    /** 默认 MySQL 方言引擎 */
    public static SqlParserEngine defaultEngine() {
        return builder().build();
    }

    /** Hive / Spark SQL 方言引擎 */
    public static SqlParserEngine hiveEngine() {
        return builder().dbType("hive").build();
    }
}
