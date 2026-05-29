package core;

import model.ParseResult;

/**
 * SQL 解析器接口
 * 每种 SQL 类型对应一个实现，遵循单一职责原则
 *
 * @param <T> 解析结果类型
 */
public interface SqlParser<T extends ParseResult> {

    /**
     * 判断是否支持该 SQL 语句
     *
     * @param statement Druid 解析后的 AST 节点
     * @return 是否支持
     */
    boolean supports(com.alibaba.druid.sql.ast.SQLStatement statement);

    /**
     * 执行解析
     *
     * @param statement Druid AST 节点
     * @param originalSql 原始 SQL 字符串
     * @return 解析结果
     */
    T parse(com.alibaba.druid.sql.ast.SQLStatement statement, String originalSql);
}
