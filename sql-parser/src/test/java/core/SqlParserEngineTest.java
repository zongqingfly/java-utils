package core;

import core.SqlParserEngine;
import exception.SqlParseException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * SQL 解析引擎完整测试套件
 */
class SqlParserEngineTest {

    private SqlParserEngine mysqlEngine;
    private SqlParserEngine hiveEngine;

    @BeforeEach
    void setUp() {
        mysqlEngine = SqlParserEngine.defaultEngine();
        hiveEngine  = SqlParserEngine.hiveEngine();
    }

    // ════════════════════════════════════════════════════════════════════
    //  CREATE TABLE
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CREATE TABLE 解析")
    class CreateTableTests {

        @Test
        @DisplayName("基础 MySQL CREATE TABLE")
        void testBasicCreateTable() {
            String sql = """
                CREATE TABLE users (
                    id      BIGINT NOT NULL AUTO_INCREMENT,
                    name    VARCHAR(100) NOT NULL COMMENT '用户名',
                    email   VARCHAR(255),
                    age     INT DEFAULT 0,
                    PRIMARY KEY (id)
                ) COMMENT='用户表';
                """;

            CreateTableResult result = mysqlEngine.parseCreateTable(sql);

            assertThat(result.getSqlType()).isEqualTo(SqlType.CREATE_TABLE);
            assertThat(result.getTableName()).isEqualTo("users");
            assertThat(result.getSchemaName()).isNull();
            assertThat(result.isIfNotExists()).isFalse();
            assertThat(result.getColumns()).hasSize(4);

            ColumnDefinition idCol = result.findColumn("id").orElseThrow();
            assertThat(idCol.getDataType()).isEqualTo("BIGINT");
            assertThat(idCol.isNullable()).isFalse();

            ColumnDefinition nameCol = result.findColumn("name").orElseThrow();
            assertThat(nameCol.getComment()).isEqualTo("用户名");

            ColumnDefinition ageCol = result.findColumn("age").orElseThrow();
            assertThat(ageCol.getDefaultValue()).isEqualTo("0");
        }

        @Test
        @DisplayName("带 schema 的 CREATE TABLE")
        void testCreateTableWithSchema() {
            String sql = """
                CREATE TABLE IF NOT EXISTS mydb.orders (
                    order_id BIGINT PRIMARY KEY,
                    amount   DECIMAL(10,2)
                );
                """;

            CreateTableResult result = mysqlEngine.parseCreateTable(sql);

            assertThat(result.getTableName()).isEqualTo("orders");
            assertThat(result.getSchemaName()).isEqualTo("mydb");
            assertThat(result.getFullTableName()).isEqualTo("mydb.orders");
            assertThat(result.isIfNotExists()).isTrue();

            ColumnDefinition amountCol = result.findColumn("amount").orElseThrow();
            assertThat(amountCol.getPrecision()).isEqualTo(10);
            assertThat(amountCol.getScale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Hive 分区表 CREATE TABLE")
        void testHiveCreateTableWithPartitions() {
            String sql = """
                CREATE TABLE IF NOT EXISTS dw.user_events (
                    user_id   BIGINT,
                    event     STRING,
                    ts        TIMESTAMP
                )
                PARTITIONED BY (dt STRING, hour STRING)
                STORED AS PARQUET;
                """;

            CreateTableResult result = hiveEngine.parseCreateTable(sql);

            assertThat(result.getTableName()).isEqualTo("user_events");
            assertThat(result.getSchemaName()).isEqualTo("dw");
            assertThat(result.getColumns()).hasSize(3);
            assertThat(result.getPartitionColumns()).hasSize(2);
            assertThat(result.getPartitionColumns())
                    .extracting(ColumnDefinition::getName)
                    .containsExactly("dt", "hour");
            assertThat(result.getStoredAs()).isEqualToIgnoringCase("PARQUET");

            // getAllColumns 应包含普通列 + 分区列
            assertThat(result.getAllColumns()).hasSize(5);
        }

        @Test
        @DisplayName("外部表 CREATE EXTERNAL TABLE")
        void testCreateExternalTable() {
            String sql = """
                CREATE EXTERNAL TABLE raw.logs (
                    log_id  STRING,
                    content STRING
                )
                STORED AS ORC;
                """;

            CreateTableResult result = hiveEngine.parseCreateTable(sql);

            assertThat(result.getStoredAs()).isEqualToIgnoringCase("ORC");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  INSERT INTO
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("INSERT INTO 解析")
    class InsertTests {

        @Test
        @DisplayName("INSERT VALUES 单行")
        void testInsertValues() {
            String sql = """
                INSERT INTO users (id, name, email)
                VALUES (1, 'Alice', 'alice@example.com');
                """;

            InsertResult result = mysqlEngine.parseInsert(sql);

            assertThat(result.getSqlType()).isEqualTo(SqlType.INSERT);
            assertThat(result.isOverwrite()).isFalse();
            assertThat(result.getTableName()).isEqualTo("users");
            assertThat(result.getTargetColumns()).containsExactly("id", "name", "email");
            assertThat(result.getSourceType()).isEqualTo(InsertResult.InsertSourceType.VALUES);
            assertThat(result.getValuesList()).hasSize(1);
            assertThat(result.getValuesList().get(0)).containsExactly("1", "'Alice'", "'alice@example.com'");
        }

        @Test
        @DisplayName("INSERT VALUES 多行")
        void testInsertMultipleValues() {
            String sql = """
                INSERT INTO products (id, name, price) VALUES
                    (1, 'Apple',  1.50),
                    (2, 'Banana', 0.80),
                    (3, 'Cherry', 3.00);
                """;

            InsertResult result = mysqlEngine.parseInsert(sql);

            assertThat(result.getValuesList()).hasSize(3);
        }

        @Test
        @DisplayName("INSERT INTO ... SELECT")
        void testInsertSelect() {
            String sql = """
                INSERT INTO order_summary (user_id, total)
                SELECT user_id, SUM(amount) AS total
                FROM orders
                WHERE status = 'completed'
                GROUP BY user_id;
                """;

            InsertResult result = mysqlEngine.parseInsert(sql);

            assertThat(result.getSourceType()).isEqualTo(InsertResult.InsertSourceType.SELECT_FROM);
            assertThat(result.getSourceQuery()).isNotBlank();
            assertThat(result.getSourceTables()).contains("orders");
            assertThat(result.getValuesList()).isNull();
        }

        @Test
        @DisplayName("INSERT INTO ... SELECT JOIN")
        void testInsertSelectJoin() {
            String sql = """
                INSERT INTO report (order_id, user_name, amount)
                SELECT o.order_id, u.name, o.amount
                FROM orders o
                JOIN users u ON o.user_id = u.id;
                """;

            InsertResult result = mysqlEngine.parseInsert(sql);

            assertThat(result.getSourceTables()).containsExactlyInAnyOrder("orders", "users");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  INSERT OVERWRITE
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("INSERT OVERWRITE 解析")
    class InsertOverwriteTests {

        @Test
        @DisplayName("Hive INSERT OVERWRITE TABLE")
        void testHiveInsertOverwrite() {
            String sql = """
                INSERT OVERWRITE TABLE dw.user_events
                PARTITION (dt='2024-01-15', hour='08')
                SELECT user_id, event, ts
                FROM ods.raw_events
                WHERE dt='2024-01-15' AND hour='08';
                """;

            InsertResult result = hiveEngine.parseInsert(sql);

            assertThat(result.getSqlType()).isEqualTo(SqlType.INSERT_OVERWRITE);
            assertThat(result.isOverwrite()).isTrue();
            assertThat(result.getTableName()).isEqualTo("user_events");
            assertThat(result.getSchemaName()).isEqualTo("dw");
            assertThat(result.getFullTableName()).isEqualTo("dw.user_events");
            assertThat(result.hasPartitions()).isTrue();
            assertThat(result.getPartitions())
                    .containsEntry("dt", "'2024-01-15'")
                    .containsEntry("hour", "'08'");
            assertThat(result.getSourceTables()).contains("raw_events");
        }

        @Test
        @DisplayName("INSERT OVERWRITE 无分区")
        void testInsertOverwriteNoPartition() {
            String sql = """
                INSERT OVERWRITE TABLE dw.dim_users
                SELECT id, name, email, NOW() AS updated_at
                FROM ods.users;
                """;

            InsertResult result = hiveEngine.parseInsert(sql);

            assertThat(result.isOverwrite()).isTrue();
            assertThat(result.hasPartitions()).isFalse();
        }

        @Test
        @DisplayName("INSERT OVERWRITE 动态分区")
        void testInsertOverwriteDynamicPartition() {
            String sql = """
                INSERT OVERWRITE TABLE dw.events
                PARTITION (dt)
                SELECT user_id, action, dt
                FROM ods.raw_events;
                """;

            InsertResult result = hiveEngine.parseInsert(sql);

            assertThat(result.isOverwrite()).isTrue();
            assertThat(result.getPartitions()).containsKey("dt");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  边界与错误处理
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("边界与错误处理")
    class EdgeCaseTests {

        @Test
        @DisplayName("SQL 末尾分号自动去除")
        void testTrailingSemicolon() {
            String sql = "INSERT INTO t VALUES (1);;;";
            InsertResult result = mysqlEngine.parseInsert(sql);
            assertThat(result.getTableName()).isEqualTo("t");
        }

        @Test
        @DisplayName("NULL 值解析")
        void testNullValue() {
            String sql = "INSERT INTO t (a, b) VALUES (1, NULL);";
            InsertResult result = mysqlEngine.parseInsert(sql);
            assertThat(result.getValuesList().get(0)).containsExactly("1", "NULL");
        }

        @Test
        @DisplayName("非法 SQL 抛出 SqlParseException")
        void testInvalidSql() {
            assertThatThrownBy(() -> mysqlEngine.parse("THIS IS NOT SQL"))
                    .isInstanceOf(SqlParseException.class);
        }

        @Test
        @DisplayName("空 SQL 抛出 IllegalArgumentException")
        void testBlankSql() {
            assertThatThrownBy(() -> mysqlEngine.parse("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("反引号列名解析")
        void testBacktickColumnNames() {
            String sql = """
                CREATE TABLE `my_table` (
                    `user_id` BIGINT NOT NULL,
                    `user_name` VARCHAR(50)
                );
                """;

            CreateTableResult result = mysqlEngine.parseCreateTable(sql);
            assertThat(result.getTableName()).isEqualTo("my_table");
            assertThat(result.findColumn("user_id")).isPresent();
        }
    }
}
