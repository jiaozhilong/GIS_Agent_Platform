package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeController {
    private final KnowledgeService service;
    public KnowledgeController(KnowledgeService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<KnowledgeDtos.KnowledgeBase>> list() { return ApiResponse.ok(service.list()); }

    @PostMapping("/sync") @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeDtos.SyncResult> sync() { return ApiResponse.ok(service.sync()); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeDtos.KnowledgeBase> create(@Valid @RequestBody KnowledgeDtos.CreateRequest request) {
        return ApiResponse.created(service.create(request));
    }

    @GetMapping("/{id}/documents") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<KnowledgeDtos.Document>> documents(@PathVariable String id) { return ApiResponse.ok(service.documents(id)); }

    @PostMapping("/{id}/documents") @ResponseStatus(HttpStatus.ACCEPTED) @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<List<KnowledgeDtos.Document>> upload(@PathVariable String id, @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(service.upload(id, file.getOriginalFilename() == null ? "document" : file.getOriginalFilename(), file.getBytes()));
    }
}
