package com.jiaozhilong.gisagent.dashboard;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import com.jiaozhilong.gisagent.project.ProjectDtos;
import com.jiaozhilong.gisagent.project.ProjectService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;
    private final ProjectService projects;
    private final RagflowManagementClient ragflow;

    public DashboardController(JdbcTemplate jdbc, ProjectService projects, RagflowManagementClient ragflow) {
        this.jdbc = jdbc;
        this.projects = projects;
        this.ragflow = ragflow;
    }

    @GetMapping("/summary") @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<DashboardSummary> summary() {
        boolean healthy = true;
        int chunks = 0;
        try { chunks = ragflow.datasets().stream().mapToInt(RagflowManagementClient.Dataset::chunkCount).sum(); }
        catch (RuntimeException exception) { healthy = false; }
        long projectCount = count("select count(*) from platform_projects");
        long requirementCount = count("select count(*) from requirement_analysis_runs");
        long proposalCount = count("select count(*) from solution_generation_runs where status='SUCCEEDED'");
        long running = count("select count(*) from solution_generation_runs where status in ('PENDING','RUNNING')");
        List<ProjectDtos.ProjectSummary> recent = projects.list(null, null).stream().limit(5).toList();
        return ApiResponse.ok(new DashboardSummary(projectCount, requirementCount, proposalCount, chunks, running,
                healthy ? "HEALTHY" : "DEGRADED", recent));
    }

    private long count(String sql) { Long value = jdbc.queryForObject(sql, Long.class); return value == null ? 0 : value; }

    public record DashboardSummary(long projectCount, long requirementTaskCount, long proposalCount,
                                   int knowledgeChunkCount, long runningAgentCount, String systemStatus,
                                   List<ProjectDtos.ProjectSummary> projects) {}
}
