package com.fly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 解析结果基类
 * 所有具体解析结果均继承此类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class ParseResult {

    /** SQL 类型 */
    private SqlType sqlType;

    /** 原始 SQL */
    private String originalSql;

    /** 解析警告信息 */
    private final List<String> warnings = new ArrayList<>();

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
