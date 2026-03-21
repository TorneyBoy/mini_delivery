package com.delivery.controller;

import com.delivery.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台数据库控制器
 * 提供数据库查询和管理功能
 */
@Slf4j
@Tag(name = "数据库管理", description = "管理后台数据库操作接口")
@RestController
@RequestMapping("/api/admin/database")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class AdminDatabaseController {

    private final JdbcTemplate jdbcTemplate;

    // 允许查询的表（白名单）
    private static final List<String> ALLOWED_TABLES = List.of(
            "admin", "branch_manager", "shop", "driver", "product",
            "order", "order_item", "bill", "bill_order",
            "picking_list", "delivery_list", "delivery_order",
            "product_image_request", "task_log");

    // 禁止的SQL关键词（防止危险操作）
    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE",
            "SHUTDOWN", "LOAD_FILE", "INTO OUTFILE", "INTO DUMPFILE");

    @Operation(summary = "执行SQL查询", description = "执行只读SQL查询（仅限SELECT）")
    @PostMapping("/query")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> executeQuery(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");

        if (sql == null || sql.trim().isEmpty()) {
            return Result.fail("SQL语句不能为空");
        }

        String upperSql = sql.trim().toUpperCase();

        // 安全检查：禁止危险操作
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                log.warn("禁止执行的SQL操作: {}", keyword);
                return Result.fail("禁止执行包含 " + keyword + " 的SQL语句");
            }
        }

        try {
            // 判断是查询还是更新操作
            if (upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW") || upperSql.startsWith("DESC")
                    || upperSql.startsWith("EXPLAIN")) {
                // 查询操作
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
                log.info("SQL查询成功，返回 {} 条记录", results.size());
                return Result.success(results);
            } else if (upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")
                    || upperSql.startsWith("INSERT")) {
                // 更新操作（需要额外权限检查）
                // 只允许对白名单表进行操作
                boolean allowed = false;
                for (String table : ALLOWED_TABLES) {
                    if (upperSql.contains(table.toUpperCase())) {
                        allowed = true;
                        break;
                    }
                }

                if (!allowed) {
                    return Result.fail("只允许操作以下表: " + String.join(", ", ALLOWED_TABLES));
                }

                int affectedRows = jdbcTemplate.update(sql);
                log.info("SQL更新成功，影响 {} 行", affectedRows);
                return Result.success(Map.of("affectedRows", affectedRows));
            } else {
                return Result.fail("不支持的SQL语句类型");
            }
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage());
            return Result.fail("SQL执行失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有表名", description = "获取数据库中所有表名")
    @GetMapping("/tables")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getTables() {
        String sql = "SHOW TABLES";
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql);
        return Result.success(tables);
    }

    @Operation(summary = "获取表结构", description = "获取指定表的结构信息")
    @GetMapping("/tables/{tableName}/structure")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getTableStructure(@PathVariable String tableName) {
        // 检查表名是否在白名单中
        if (!ALLOWED_TABLES.contains(tableName.toLowerCase()) && !ALLOWED_TABLES.contains(tableName)) {
            return Result.fail("不允许查看该表");
        }

        String sql = "DESC " + tableName;
        List<Map<String, Object>> structure = jdbcTemplate.queryForList(sql);
        return Result.success(structure);
    }

    @Operation(summary = "获取表数据", description = "分页获取表数据")
    @GetMapping("/tables/{tableName}/data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 检查表名是否在白名单中
        if (!ALLOWED_TABLES.contains(tableName.toLowerCase()) && !ALLOWED_TABLES.contains(tableName)) {
            return Result.fail("不允许查看该表");
        }

        // 处理order表名（需要反引号）
        String actualTableName = tableName.equalsIgnoreCase("order") ? "`order`" : tableName;

        int offset = (page - 1) * size;
        String sql = String.format("SELECT * FROM %s LIMIT %d, %d", actualTableName, offset, size);
        String countSql = String.format("SELECT COUNT(*) FROM %s", actualTableName);

        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        return Result.success(Map.of(
                "records", data,
                "total", total,
                "page", page,
                "size", size));
    }
}
