package cn.luopan.animemasterbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class DatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkTableStructure() {
        // 查看user_anime_status表的完整结构
        List<Map<String, Object>> createTableResult = jdbcTemplate.queryForList("SHOW CREATE TABLE user_anime_status");
        System.out.println("Table Structure:");
        for (Map<String, Object> row : createTableResult) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        // 查看所有检查约束
        List<Map<String, Object>> constraintsResult = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, CHECK_CLAUSE " +
                        "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS " +
                        "USING (CONSTRAINT_SCHEMA, CONSTRAINT_NAME) " +
                        "WHERE TABLE_SCHEMA = 'anime_master' AND TABLE_NAME = 'user_anime_status'");
        
        System.out.println("\nCheck Constraints:");
        if (constraintsResult.isEmpty()) {
            System.out.println("No check constraints found for user_anime_status table");
        } else {
            for (Map<String, Object> row : constraintsResult) {
                System.out.println("Constraint Name: " + row.get("CONSTRAINT_NAME"));
                System.out.println("Constraint Type: " + row.get("CONSTRAINT_TYPE"));
                System.out.println("Check Clause: " + row.get("CHECK_CLAUSE"));
                System.out.println();
            }
        }

        // 查看status字段的枚举值
        List<Map<String, Object>> columnsResult = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM user_anime_status WHERE Field = 'status'");
        
        System.out.println("\nStatus Column Definition:");
        for (Map<String, Object> row : columnsResult) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}