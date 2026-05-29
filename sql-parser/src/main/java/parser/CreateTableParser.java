package parser;

import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.hive.stmt.HiveCreateTableStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import core.SqlParser;
import model.ColumnDefinition;
import model.CreateTableResult;
import model.SqlType;
import util.SqlNormalizer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * CREATE TABLE 语句解析器
 * 支持 MySQL、Hive、Spark SQL 等方言
 */
@Slf4j
public class CreateTableParser implements SqlParser<CreateTableResult> {

    @Override
    public boolean supports(SQLStatement statement) {
        return statement instanceof SQLCreateTableStatement;
    }

    @Override
    public CreateTableResult parse(SQLStatement statement, String originalSql) {
        SQLCreateTableStatement createStmt = (SQLCreateTableStatement) statement;

        String tableName = extractTableName(createStmt);
        String schemaName = extractSchemaName(createStmt);

        List<ColumnDefinition> columns = extractColumns(createStmt);
        List<String> primaryKeys = extractPrimaryKeys(createStmt);
        List<ColumnDefinition> partitionColumns = extractPartitionColumns(createStmt);
        String tableComment = extractTableComment(createStmt);
        Map<String, String> properties = extractTableProperties(createStmt);
        String storedAs = extractStoredAs(createStmt);

        return CreateTableResult
                .builder()
                .sqlType(SqlType.CREATE_TABLE)
                .originalSql(originalSql)
                .tableName(tableName)
                .schemaName(schemaName)
                .ifNotExists(createStmt.isIfNotExists())
                .columns(columns).primaryKeys(primaryKeys).partitionColumns(partitionColumns).tableComment(tableComment).tableProperties(properties).storedAs(storedAs).build();
    }

    // ── 私有提取方法 ──────────────────────────────────────────────────────────

    private String extractTableName(SQLCreateTableStatement stmt) {
        return SqlNormalizer.unquote(stmt.getTableSource().getName().getSimpleName());
    }

    private String extractSchemaName(SQLCreateTableStatement stmt) {
        final String[] schema = {null};

        // 创建 Schema 访问器
        SchemaStatVisitor visitor = new SchemaStatVisitor(stmt.getDbType());
        stmt.accept(visitor);

        // 获取原始表名列表，其中可能包含 schema 信息
        for (SQLName table : visitor.getOriginalTables()) {
            if (table instanceof SQLPropertyExpr) {
                SQLPropertyExpr propertyExpr = (SQLPropertyExpr) table;
                SQLExpr owner = propertyExpr.getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    schema[0] = ((SQLIdentifierExpr) owner).getName();
                    break;
                }
            }
            // 如果是简单表名，则 schema 为 null
            else if (table instanceof SQLIdentifierExpr) {
                schema[0] = null;
                break;
            }
        }

        if (schema[0] != null) {
            return SqlNormalizer.unquote(schema[0]);
        }
        return null;
    }

    private List<ColumnDefinition> extractColumns(SQLCreateTableStatement stmt) {
        List<ColumnDefinition> columns = new ArrayList<>();
        for (SQLTableElement element : stmt.getTableElementList()) {
            if (element instanceof SQLColumnDefinition colDef) {
                columns.add(buildColumnDefinition(colDef));
            }
        }
        return columns;
    }

    private ColumnDefinition buildColumnDefinition(SQLColumnDefinition colDef) {
        String name = SqlNormalizer.unquote(colDef.getNameAsString());
        String dataType = resolveDataType(colDef.getDataType());
        boolean nullable = !isNotNull(colDef);
        boolean primaryKey = isPrimaryKey(colDef);
        String defaultValue = colDef.getDefaultExpr() != null ? colDef.getDefaultExpr().toString() : null;
        String comment = extractColumnComment(colDef);

        Integer precision = null;
        Integer scale = null;
        if (colDef.getDataType() != null && !colDef.getDataType().getArguments().isEmpty()) {
            List<SQLExpr> args = colDef.getDataType().getArguments();
            try {
                precision = Integer.parseInt(args.get(0).toString());
                if (args.size() > 1) {
                    scale = Integer.parseInt(args.get(1).toString());
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return ColumnDefinition.builder().name(name).dataType(dataType).nullable(nullable).primaryKey(primaryKey).defaultValue(defaultValue).comment(comment).precision(precision).scale(scale).build();
    }

    private String resolveDataType(SQLDataType dataType) {
        if (dataType == null) return "UNKNOWN";
        // 只返回类型名，不含参数（参数已单独提取）
        return dataType.getName().toUpperCase();
    }

    private boolean isNotNull(SQLColumnDefinition colDef) {
        return colDef.getConstraints().stream().anyMatch(c -> c instanceof SQLNotNullConstraint);
    }

    private boolean isPrimaryKey(SQLColumnDefinition colDef) {
        return colDef.getConstraints().stream().anyMatch(c -> c instanceof SQLColumnPrimaryKey);
    }

    private String extractColumnComment(SQLColumnDefinition colDef) {
        if (colDef.getComment() instanceof SQLCharExpr charExpr) {
            return charExpr.getText();
        }
        return null;
    }

    private List<String> extractPrimaryKeys(SQLCreateTableStatement stmt) {
        List<String> pks = new ArrayList<>();
        for (SQLTableElement element : stmt.getTableElementList()) {
            if (element instanceof SQLPrimaryKey pk) {
                pk.getColumns().forEach(col -> pks.add(SqlNormalizer.unquote(col.getExpr().toString())));
            }
        }
        return pks;
    }

    private List<ColumnDefinition> extractPartitionColumns(SQLCreateTableStatement stmt) {
        List<ColumnDefinition> partitions = new ArrayList<>();

        if (stmt instanceof HiveCreateTableStatement hiveStmt) {
            for (SQLColumnDefinition colDef : hiveStmt.getPartitionColumns()) {
                partitions.add(buildColumnDefinition(colDef));
            }
        }
        return partitions;
    }

    private String extractTableComment(SQLCreateTableStatement stmt) {
        SQLExpr comment = stmt.getComment();
        if (comment instanceof SQLCharExpr charExpr) {
            return charExpr.getText();
        }
        return comment != null ? comment.toString() : null;
    }

    private Map<String, String> extractTableProperties(SQLCreateTableStatement stmt) {
        Map<String, String> props = new LinkedHashMap<>();
        if (stmt.getTableOptions() != null) {
            stmt.getTableOptions().forEach((item) -> {
                String k = item.getTarget().toString();
                String v = item.getValue().toString();
                props.put(k, v);
            });
        }
        return props.isEmpty() ? null : props;
    }


    private String extractStoredAs(SQLCreateTableStatement stmt) {
        if (stmt instanceof HiveCreateTableStatement hiveStmt) {
            if (hiveStmt.getStoredAs() != null) {
                return hiveStmt.getStoredAs().toString();
            }
        }
        return null;
    }
}
