package com.fly.model;

import lombok.Builder;
import lombok.Data;

/**
 * 列定义模型
 */
@Data
@Builder
public class ColumnDefinition {

    /** 列名 */
    private String name;

    /** 数据类型 */
    private String dataType;

    /** 是否可为空 */
    private boolean nullable;

    /** 是否为主键 */
    private boolean primaryKey;

    /** 默认值 */
    private String defaultValue;

    /** 注释 */
    private String comment;

    /** 精度（如 DECIMAL(10,2) 中的 10） */
    private Integer precision;

    /** 小数位数（如 DECIMAL(10,2) 中的 2） */
    private Integer scale;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(dataType);
        if (precision != null) {
            sb.append("(").append(precision);
            if (scale != null) sb.append(",").append(scale);
            sb.append(")");
        }
        if (primaryKey) sb.append(" PRIMARY KEY");
        if (!nullable) sb.append(" NOT NULL");
        if (defaultValue != null) sb.append(" DEFAULT ").append(defaultValue);
        if (comment != null) sb.append(" COMMENT '").append(comment).append("'");
        return sb.toString();
    }
}
