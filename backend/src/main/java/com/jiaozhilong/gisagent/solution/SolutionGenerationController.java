package com.jiaozhilong.gisagent.solution;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SolutionGenerationController {
    private final SolutionGenerationService service;
    public SolutionGenerationController(SolutionGenerationService service) { this.service = service; }

    @PostMapping("/projects/{projectId}/solution-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('proposal:generate')")
    public ApiResponse<SolutionDtos.GenerationRunResponse> start(@PathVariable String projectId, Authentication authentication,
                                                                 @Valid @RequestBody SolutionDtos.GenerationRequest request) {
        return ApiResponse.ok(service.start(projectId, authentication.getName(), request));
    }

    @GetMapping("/solution-runs/{runId}")
    @PreAuthorize("hasAuthority('proposal:view')")
    public ApiResponse<SolutionDtos.GenerationRunResponse> get(@PathVariable UUID runId) { return ApiResponse.ok(service.get(runId)); }

    @GetMapping("/solution-runs")
    @PreAuthorize("hasAuthority('proposal:view')")
    public ApiResponse<List<SolutionDtos.GenerationRunResponse>> list(@RequestParam(required = false) String projectId) {
        return ApiResponse.ok(service.list(projectId));
    }

    @GetMapping("/solution-sections/{sectionId}")
    @PreAuthorize("hasAuthority('proposal:view')")
    public ApiResponse<SolutionDtos.SectionResponse> section(@PathVariable UUID sectionId) {
        return ApiResponse.ok(service.getSection(sectionId));
    }

    @PatchMapping("/solution-sections/{sectionId}")
    @PreAuthorize("hasAuthority('proposal:generate')")
    public ApiResponse<SolutionDtos.SectionResponse> updateSection(@PathVariable UUID sectionId,
            @Valid @RequestBody SolutionDtos.UpdateSectionRequest request) {
        return ApiResponse.ok(service.updateSection(sectionId, request));
    }

    @PostMapping("/solution-sections/{sectionId}/lock")
    @PreAuthorize("hasAuthority('proposal:generate')")
    public ApiResponse<SolutionDtos.SectionResponse> lockSection(@PathVariable UUID sectionId,
            @Valid @RequestBody SolutionDtos.LockSectionRequest request) {
        return ApiResponse.ok(service.lockSection(sectionId, request.locked()));
    }

    @PostMapping("/solution-sections/{sectionId}/regenerate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('proposal:generate')")
    public ApiResponse<SolutionDtos.SectionResponse> regenerateSection(@PathVariable UUID sectionId,
            @Valid @RequestBody SolutionDtos.RegenerateSectionRequest request) {
        return ApiResponse.ok(service.regenerateSection(sectionId, request));
    }
}
