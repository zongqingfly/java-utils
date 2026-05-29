package model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CREATE TABLE 解析结果
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CreateTableResult extends ParseResult {

    /** 表名 */
    private String tableName;

    /** schema/数据库名 */
    private String schemaName;

    /** 是否含 IF NOT EXISTS */
    private boolean ifNotExists;

    /** 是否为外部表（EXTERNAL TABLE） */
    private boolean external;

    /** 列定义列表 */
    private List<ColumnDefinition> columns;

    /** 主键列名列表 */
    private List<String> primaryKeys;

    /** 分区列列表 */
    private List<ColumnDefinition> partitionColumns;

    /** 表注释 */
    private String tableComment;

    /** 表属性（如 TBLPROPERTIES） */
    private Map<String, String> tableProperties;

    /** 存储格式（如 ORC、PARQUET） */
    private String storedAs;

    /** 建表来源 SQL（如 CREATE TABLE AS SELECT） */
    private String createAsSelect;

    public String getFullTableName() {
        return schemaName != null ? schemaName + "." + tableName : tableName;
    }

    public Optional<ColumnDefinition> findColumn(String columnName) {
        if (columns == null) return Optional.empty();
        return columns.stream()
                .filter(c -> c.getName().equalsIgnoreCase(columnName))
                .findFirst();
    }

    public List<ColumnDefinition> getAllColumns() {
        List<ColumnDefinition> all = new ArrayList<>();
        if (columns != null) all.addAll(columns);
        if (partitionColumns != null) all.addAll(partitionColumns);
        return all;
    }
}
