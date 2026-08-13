package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/retrieval-evaluations")
public class RetrievalEvaluationController {
    private final RetrievalEvaluationService service;
    public RetrievalEvaluationController(RetrievalEvaluationService service) { this.service = service; }

    @GetMapping("/cases") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<RetrievalEvaluationDtos.Case>> cases() { return ApiResponse.ok(service.cases()); }

    @GetMapping("/runs") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<RetrievalEvaluationDtos.Run>> runs() { return ApiResponse.ok(service.runs()); }

    @GetMapping("/runs/{id}") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<RetrievalEvaluationDtos.Run> run(@PathVariable UUID id) { return ApiResponse.ok(service.run(id)); }

    @PostMapping("/runs") @ResponseStatus(HttpStatus.ACCEPTED) @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<RetrievalEvaluationDtos.Run> start() { return ApiResponse.ok(service.start()); }
}
