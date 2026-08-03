package com.jiaozhilong.gisagent.common.web;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Integer database = jdbcTemplate.queryForObject("select 1", Integer.class);
        return ApiResponse.ok(Map.of("status", "UP", "database", database != null && database == 1 ? "UP" : "DOWN"));
    }
}
