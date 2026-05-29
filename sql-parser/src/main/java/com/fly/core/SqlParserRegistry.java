package com.fly.core;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.fly.exception.UnsupportedSqlTypeException;
import com.fly.model.ParseResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 解析器注册表
 * 采用责任链模式：遍历已注册解析器，找到第一个支持当前语句的解析器执行解析
 */
@Slf4j
public class SqlParserRegistry {

    private final List<SqlParser<? extends ParseResult>> parsers = new ArrayList<>();

    /**
     * 注册一个解析器（顺序即优先级）
     */
    public SqlParserRegistry register(SqlParser<? extends ParseResult> parser) {
        parsers.add(parser);
        log.debug("Registered parser: {}", parser.getClass().getSimpleName());
        return this;
    }

    /**
     * 根据 AST 节点找到合适的解析器并执行解析
     */
    @SuppressWarnings("unchecked")
    public <T extends ParseResult> T dispatch(SQLStatement statement, String originalSql) {
        for (SqlParser<? extends ParseResult> parser : parsers) {
            if (parser.supports(statement)) {
                log.debug("Dispatching to parser: {}", parser.getClass().getSimpleName());
                return (T) parser.parse(statement, originalSql);
            }
        }
        throw new UnsupportedSqlTypeException(
                statement.getClass().getSimpleName(), originalSql);
    }

    public List<SqlParser<? extends ParseResult>> getParsers() {
        return List.copyOf(parsers);
    }
}
