package model;

/**
 * SQL 语句类型枚举
 */
public enum SqlType {
    CREATE_TABLE("CREATE TABLE"),
    INSERT("INSERT INTO"),
    INSERT_OVERWRITE("INSERT OVERWRITE"),
    SELECT("SELECT"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    DROP_TABLE("DROP TABLE"),
    ALTER_TABLE("ALTER TABLE"),
    UNKNOWN("UNKNOWN");

    private final String description;

    SqlType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
