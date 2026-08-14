package com.jiaozhilong.gisagent.project;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }

    @GetMapping("/projects") @PreAuthorize("hasAuthority('project:view')")
    public ApiResponse<List<ProjectDtos.ProjectSummary>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) ProjectDtos.Stage stage) {
        return ApiResponse.ok(service.list(keyword, stage));
    }

    @PostMapping("/projects") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<ProjectDtos.ProjectDetail> create(Authentication authentication, @Valid @RequestBody ProjectDtos.UpsertRequest request) {
        return ApiResponse.created(service.create(authentication.getName(), request));
    }

    @GetMapping("/projects/{id}") @PreAuthorize("hasAuthority('project:view')")
    public ApiResponse<ProjectDtos.ProjectDetail> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }

    @PutMapping("/projects/{id}") @PreAuthorize("hasAuthority('project:manage')")
    public ApiResponse<ProjectDtos.ProjectDetail> update(@PathVariable String id, @Valid @RequestBody ProjectDtos.UpsertRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/projects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('project:manage')")
    public void delete(@PathVariable String id) { service.delete(id); }

    @PostMapping("/projects/{id}/requirement-analysis") @PreAuthorize("hasAuthority('agent:run')")
    public ApiResponse<ProjectDtos.RequirementAnalysis> analyze(@PathVariable String id) { return ApiResponse.ok(service.analyze(id)); }

    @PostMapping("/projects/{id}/product-matches") @PreAuthorize("hasAuthority('agent:run')")
    public ApiResponse<List<ProjectDtos.ProductMatch>> products(@PathVariable String id) { return ApiResponse.ok(service.matchProducts(id)); }

    @PostMapping("/projects/{id}/retrievals") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<ProjectDtos.RetrievalResult> retrieve(@PathVariable String id, @Valid @RequestBody ProjectDtos.RetrievalRequest request) {
        return ApiResponse.ok(service.retrieve(id, request));
    }
}
