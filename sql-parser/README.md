# SQL Parser Toolkit

基于 **Druid SQL Parser** 实现的优雅 SQL 解析工具包，支持 `CREATE TABLE`、`INSERT INTO`、`INSERT OVERWRITE` 等语句解析，兼容 MySQL、Hive、Spark SQL 等多种方言。

---

## 架构设计

```
sql-parser/
├── core/
│   ├── SqlParser.java          # 解析器接口（泛型 + 单一职责）
│   ├── SqlParserRegistry.java  # 责任链注册表（策略模式）
│   └── SqlParserEngine.java    # 对外统一入口（Builder 模式）
├── model/
│   ├── ParseResult.java        # 解析结果基类
│   ├── CreateTableResult.java  # CREATE TABLE 结果
│   ├── InsertResult.java       # INSERT / INSERT OVERWRITE 结果
│   ├── ColumnDefinition.java   # 列定义
│   └── SqlType.java            # SQL 类型枚举
├── parser/
│   ├── CreateTableParser.java  # CREATE TABLE 解析器实现
│   └── InsertParser.java       # INSERT 解析器实现
├── util/
│   ├── SqlNormalizer.java      # SQL 预处理工具
│   └── DbTypeUtil.java         # 方言工具
└── exception/
    ├── SqlParseException.java
    └── UnsupportedSqlTypeException.java
```

### 设计亮点

| 设计原则 | 实现方式 |
|---------|---------|
| **单一职责** | 每个 `SqlParser<T>` 只负责一种 SQL 类型 |
| **开闭原则** | 新增 SQL 类型只需实现接口 + 注册，无需修改核心代码 |
| **责任链模式** | `SqlParserRegistry` 按顺序委托，自动找到匹配的解析器 |
| **Builder 模式** | `SqlParserEngine.builder()` 提供流式配置 API |
| **里氏替换** | 所有解析结果统一继承 `ParseResult`，可安全向上转型 |

---

## 快速开始

### 依赖（Maven）

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.20</version>
</dependency>
```

### 创建引擎

```java
// MySQL 方言（默认）
SqlParserEngine engine = SqlParserEngine.defaultEngine();

// Hive / Spark SQL 方言
SqlParserEngine hiveEngine = SqlParserEngine.hiveEngine();

// 自定义配置
SqlParserEngine custom = SqlParserEngine.builder()
    .dbType("postgresql")
    .registerParser(new MyCustomParser())   // 注册扩展解析器
    .build();
```

---

## 使用示例

### CREATE TABLE

```java
String sql = """
    CREATE TABLE IF NOT EXISTS mydb.orders (
        order_id BIGINT PRIMARY KEY,
        user_id  BIGINT NOT NULL,
        amount   DECIMAL(10,2) DEFAULT 0.00 COMMENT '金额',
        status   VARCHAR(20)
    ) COMMENT='订单表';
    """;

CreateTableResult result = engine.parseCreateTable(sql);

result.getTableName();          // "orders"
result.getSchemaName();         // "mydb"
result.getFullTableName();      // "mydb.orders"
result.isIfNotExists();         // true
result.getTableComment();       // "订单表"

result.getColumns();            // List<ColumnDefinition>
result.findColumn("amount")
      .map(ColumnDefinition::getPrecision);  // Optional[10]

// Hive 分区表
CreateTableResult hive = hiveEngine.parseCreateTable("""
    CREATE TABLE dw.events (user_id BIGINT, action STRING)
    PARTITIONED BY (dt STRING)
    STORED AS PARQUET;
    """);

hive.getPartitionColumns();    // [ColumnDefinition(name=dt, dataType=STRING)]
hive.getStoredAs();            // "PARQUET"
hive.getAllColumns();           // 普通列 + 分区列
```

### INSERT INTO VALUES

```java
InsertResult result = engine.parseInsert("""
    INSERT INTO users (id, name, email)
    VALUES (1, 'Alice', 'alice@example.com');
    """);

result.getSqlType();            // SqlType.INSERT
result.isOverwrite();           // false
result.getTargetColumns();      // ["id", "name", "email"]
result.getSourceType();         // InsertSourceType.VALUES
result.getValuesList();         // [["1", "'Alice'", "'alice@example.com'"]]
```

### INSERT INTO ... SELECT

```java
InsertResult result = engine.parseInsert("""
    INSERT INTO order_summary (user_id, total)
    SELECT user_id, SUM(amount)
    FROM orders
    WHERE status = 'completed'
    GROUP BY user_id;
    """);

result.getSourceType();         // InsertSourceType.SELECT_FROM
result.getSourceTables();       // ["orders"]
result.getSourceQuery();        // 完整的 SELECT 子句字符串
```

### INSERT OVERWRITE（Hive/Spark）

```java
InsertResult result = hiveEngine.parseInsert("""
    INSERT OVERWRITE TABLE dw.user_events
    PARTITION (dt='2024-01-15', hour='08')
    SELECT user_id, event, ts
    FROM ods.raw_events
    WHERE dt='2024-01-15';
    """);

result.getSqlType();            // SqlType.INSERT_OVERWRITE
result.isOverwrite();           // true
result.getFullTableName();      // "dw.user_events"
result.getPartitions();         // {"dt": "'2024-01-15'", "hour": "'08'"}
result.getSourceTables();       // ["raw_events"]
```

### 泛型自动派发

```java
// parse() 根据 SQL 类型自动返回正确的子类型
ParseResult r = engine.parse(anySql);

if (r instanceof CreateTableResult ct) {
    // 处理建表
} else if (r instanceof InsertResult ins) {
    // 处理插入
}
```

### 扩展自定义解析器

```java
public class DropTableParser implements SqlParser<DropTableResult> {

    @Override
    public boolean supports(SQLStatement stmt) {
        return stmt instanceof SQLDropTableStatement;
    }

    @Override
    public DropTableResult parse(SQLStatement stmt, String originalSql) {
        SQLDropTableStatement drop = (SQLDropTableStatement) stmt;
        // ... 自定义解析逻辑
        return DropTableResult.builder()
            .sqlType(SqlType.DROP_TABLE)
            .originalSql(originalSql)
            .tableName(drop.getTableSources().get(0).getName().getSimpleName())
            .build();
    }
}

// 注册到引擎
SqlParserEngine engine = SqlParserEngine.builder()
    .registerParser(new DropTableParser())
    .build();
```

---

## 方言支持

| 方言 | dbType 字符串 |
|------|-------------|
| MySQL | `mysql`（默认）|
| Hive | `hive` |
| Spark SQL | `spark`（等同 hive）|
| PostgreSQL | `postgresql` / `postgres` |
| Oracle | `oracle` |
| SQL Server | `sqlserver` / `mssql` |
| ClickHouse | `clickhouse` |
| Presto/Trino | `presto` / `trino` |
