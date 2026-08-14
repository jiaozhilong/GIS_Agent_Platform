package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import com.jiaozhilong.gisagent.project.ProjectDtos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge-search")
public class KnowledgeSearchController {
    private final KnowledgeSearchService service;
    public KnowledgeSearchController(KnowledgeSearchService service) { this.service = service; }

    @PostMapping @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<ProjectDtos.RetrievalResult> search(@Valid @RequestBody ProjectDtos.RetrievalRequest request) {
        return ApiResponse.ok(service.search(request));
    }
}
