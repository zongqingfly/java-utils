package model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * INSERT / INSERT OVERWRITE 解析结果
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InsertResult extends ParseResult {

    /** 目标表名 */
    private String tableName;

    /** schema/数据库名 */
    private String schemaName;

    /** 是否为 INSERT OVERWRITE */
    private boolean overwrite;

    /** 显式指定的目标列名列表 */
    private List<String> targetColumns;

    /** 插入来源类型 */
    private InsertSourceType sourceType;

    /** VALUES 列表（每行为一个 value 列表） */
    private List<List<String>> valuesList;

    /** 来源 SELECT 语句（INSERT INTO ... SELECT） */
    private String sourceQuery;

    /** 来源表列表（从 SELECT 子句中解析） */
    private List<String> sourceTables;

    /** 分区信息（INSERT INTO t PARTITION(dt='2024-01-01')） */
    private Map<String, String> partitions;

    public String getFullTableName() {
        return schemaName != null ? schemaName + "." + tableName : tableName;
    }

    public boolean hasExplicitColumns() {
        return targetColumns != null && !targetColumns.isEmpty();
    }

    public boolean hasPartitions() {
        return partitions != null && !partitions.isEmpty();
    }

    /**
     * INSERT 数据来源类型
     */
    public enum InsertSourceType {
        /** INSERT INTO t VALUES (...) */
        VALUES,
        /** INSERT INTO t SELECT ... */
        SELECT,
        /** INSERT INTO t SELECT ... FROM ... */
        SELECT_FROM
    }
}
