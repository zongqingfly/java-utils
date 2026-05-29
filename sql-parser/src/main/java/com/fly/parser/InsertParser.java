package com.fly.parser;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.hive.ast.HiveInsertStatement;
import com.fly.core.SqlParser;
import com.fly.model.InsertResult;
import com.fly.model.InsertResult.InsertSourceType;
import com.fly.model.SqlType;
import com.fly.util.SqlNormalizer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * INSERT / INSERT OVERWRITE 语句解析器
 * 兼容标准 SQL、Hive、Spark SQL 语法
 */
@Slf4j
public class InsertParser implements SqlParser<InsertResult> {

    @Override
    public boolean supports(SQLStatement statement) {
        return statement instanceof SQLInsertStatement
                || statement instanceof HiveInsertStatement;
    }

    @Override
    public InsertResult parse(SQLStatement statement, String originalSql) {
        return parseStandardInsert((SQLInsertStatement) statement, originalSql);
    }

    // ── 标准 INSERT 解析 ──────────────────────────────────────────────────────

    private InsertResult parseStandardInsert(SQLInsertStatement stmt, String originalSql) {
        boolean overwrite = isOverwrite(stmt, originalSql);
        String tableName = extractTableName(stmt.getTableSource());
        String schemaName = extractSchemaName(stmt.getTableSource());
        List<String> targetColumns = extractTargetColumns(stmt.getColumns());
        Map<String, String> partitions = extractPartitions(stmt.getPartitions());

        InsertSourceType sourceType;
        List<List<String>> valuesList = null;
        String sourceQuery = null;
        List<String> sourceTables = null;

        if (stmt.getQuery() != null) {
            // INSERT INTO ... SELECT ...
            sourceQuery = stmt.getQuery().toString();
            sourceTables = extractSourceTables(stmt.getQuery());
            sourceType = sourceTables.isEmpty()
                    ? InsertSourceType.SELECT
                    : InsertSourceType.SELECT_FROM;
        } else {
            // INSERT INTO ... VALUES (...)
            valuesList = extractValues(stmt.getValuesList());
            sourceType = InsertSourceType.VALUES;
        }

        return InsertResult.builder()
                .sqlType(overwrite ? SqlType.INSERT_OVERWRITE : SqlType.INSERT)
                .originalSql(originalSql)
                .tableName(tableName)
                .schemaName(schemaName)
                .overwrite(overwrite)
                .targetColumns(targetColumns)
                .sourceType(sourceType)
                .valuesList(valuesList)
                .sourceQuery(sourceQuery)
                .sourceTables(sourceTables)
                .partitions(partitions)
                .build();
    }

    // ── 私有提取方法 ──────────────────────────────────────────────────────────

    private boolean isOverwrite(SQLInsertStatement stmt, String originalSql) {
        // 部分方言通过 overwrite 标志判断，部分需要从原始 SQL 判断
        return stmt.isOverwrite() || SqlNormalizer.isInsertOverwrite(originalSql);
    }

    private String extractTableName(SQLExprTableSource tableSource) {
        if (tableSource == null) return null;
        return SqlNormalizer.unquote(tableSource.getName().getSimpleName());
    }

    private String extractSchemaName(SQLExprTableSource tableSource) {
        if (tableSource == null) return null;
        SQLName sqlName = tableSource.getName();

        // SQLPropertyExpr 包含库名和表名，如 "db.user"
        if (sqlName instanceof SQLPropertyExpr) {
            SQLExpr owner = ((SQLPropertyExpr) sqlName).getOwner();
            if (owner instanceof SQLIdentifierExpr) {
                return SqlNormalizer.unquote(((SQLIdentifierExpr) owner).getName());
            }
        }

        // SQLIdentifierExpr 只包含表名，没有库名
        if (sqlName instanceof SQLIdentifierExpr) {
            // 返回默认库名或 null
            return null;
        }

        return null;
    }

    private List<String> extractTargetColumns(List<SQLExpr> columns) {
        if (columns == null || columns.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (SQLExpr col : columns) {
            result.add(SqlNormalizer.unquote(col.toString()));
        }
        return result;
    }

    private List<List<String>> extractValues(List<SQLInsertStatement.ValuesClause> valuesClauses) {
        if (valuesClauses == null) return Collections.emptyList();
        List<List<String>> result = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : valuesClauses) {
            List<String> row = new ArrayList<>();
            for (SQLExpr val : clause.getValues()) {
                row.add(formatValue(val));
            }
            result.add(row);
        }
        return result;
    }

    private String formatValue(SQLExpr val) {
        if (val instanceof SQLNullExpr) return "NULL";
        if (val instanceof SQLCharExpr charExpr) return "'" + charExpr.getText() + "'";
        return val.toString();
    }

    private List<String> extractSourceTables(SQLSelect query) {
        List<String> tables = new ArrayList<>();
        if (query == null) return tables;
        collectTablesFromSelect(query.getQuery(), tables);
        return tables;
    }

    private void collectTablesFromSelect(SQLSelectQuery query, List<String> tables) {
        if (query instanceof SQLSelectQueryBlock block) {
            SQLTableSource from = block.getFrom();
            if (from != null) collectTablesFromSource(from, tables);
        } else if (query instanceof SQLUnionQuery union) {
            collectTablesFromSelect(union.getLeft(), tables);
            collectTablesFromSelect(union.getRight(), tables);
        }
    }

    private void collectTablesFromSource(SQLTableSource source, List<String> tables) {
        if (source instanceof SQLExprTableSource exprSource) {
            String name = exprSource.getName().getSimpleName();
            tables.add(SqlNormalizer.unquote(name));
        } else if (source instanceof SQLJoinTableSource join) {
            collectTablesFromSource(join.getLeft(), tables);
            collectTablesFromSource(join.getRight(), tables);
        } else if (source instanceof SQLSubqueryTableSource) {
            // 子查询来源暂不深入递归
            tables.add("<subquery>");
        }
    }

    private Map<String, String> extractPartitions(List<SQLAssignItem> partitionItems) {
        if (partitionItems == null || partitionItems.isEmpty()) return null;
        Map<String, String> partitions = new LinkedHashMap<>();
        for (SQLAssignItem item : partitionItems) {
            String key = SqlNormalizer.unquote(item.getTarget().toString());
            String value = item.getValue() != null ? item.getValue().toString() : null;
            partitions.put(key, value);
        }
        return partitions;
    }
}
