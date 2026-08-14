package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-assets")
public class KnowledgeAssetController {
    private final KnowledgeAssetService service;
    public KnowledgeAssetController(KnowledgeAssetService service) { this.service = service; }

    @GetMapping("/summary") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<KnowledgeAssetDtos.Summary> summary() { return ApiResponse.ok(service.summary()); }

    @GetMapping @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<KnowledgeAssetDtos.Asset>> list(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) KnowledgeAssetDtos.AssetType type,
                                                            @RequestParam(required = false) KnowledgeAssetDtos.AssetStatus status) {
        return ApiResponse.ok(service.list(keyword, type, status));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<KnowledgeAssetDtos.Detail> detail(@PathVariable String id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeAssetDtos.Detail> upload(Principal principal,
                                                         @RequestParam String datasetId,
                                                         @RequestParam(required = false) String title,
                                                         @RequestParam(required = false) String description,
                                                         @RequestParam(required = false) String knowledgeType,
                                                         @RequestParam(required = false) String documentType,
                                                         @RequestParam(required = false) String industry,
                                                         @RequestParam(required = false) String gisDomain,
                                                         @RequestParam(required = false) String product,
                                                         @RequestParam(required = false) String productVersion,
                                                         @RequestParam(required = false) String projectType,
                                                         @RequestParam(required = false) String region,
                                                         @RequestParam(required = false) Integer year,
                                                         @RequestParam(required = false) String tags,
                                                         @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.created(service.upload(principal.getName(), datasetId, title, description, knowledgeType,
                documentType, industry, gisDomain, product, productVersion, projectType, region, year, tags, file));
    }

    @PostMapping("/{id}/sync") @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeAssetDtos.Detail> retrySync(@PathVariable String id) { return ApiResponse.ok(service.retrySync(id)); }

    @PostMapping("/{id}/reparse") @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgeAssetDtos.Detail> reparse(@PathVariable String id,
            @RequestBody(required = false) KnowledgeAssetDtos.ReparseRequest request) {
        return ApiResponse.ok(service.reparse(id, request == null ? new KnowledgeAssetDtos.ReparseRequest(null, Map.of()) : request));
    }

    @GetMapping("/{id}/parse-status") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<KnowledgeAssetDtos.ParseTask> parseStatus(@PathVariable String id) {
        return ApiResponse.ok(service.parseStatus(id));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('knowledge:manage')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/media") @PreAuthorize("hasAuthority('knowledge:view')")
    public ApiResponse<List<KnowledgeAssetDtos.Media>> mediaList(@PathVariable String id) {
        return ApiResponse.ok(service.detail(id).media());
    }

    @GetMapping("/{id}/download") @PreAuthorize("hasAuthority('knowledge:view')")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws IOException {
        return file(service.original(id), true);
    }

    @GetMapping("/media/{id}") @PreAuthorize("hasAuthority('knowledge:view')")
    public ResponseEntity<InputStreamResource> media(@PathVariable String id) throws IOException {
        return file(service.media(id), false);
    }

    @GetMapping("/{id}/pages/{pageNumber}/preview") @PreAuthorize("hasAuthority('knowledge:view')")
    public ResponseEntity<InputStreamResource> preview(@PathVariable String id, @PathVariable int pageNumber) throws IOException {
        return file(service.preview(id, pageNumber), false);
    }

    private ResponseEntity<InputStreamResource> file(KnowledgeAssetDtos.StoredFile file, boolean attachment) throws IOException {
        InputStream stream = Files.newInputStream(file.path());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentLength(Files.size(file.path()))
                .contentType(MediaType.parseMediaType(file.mediaType()));
        if (attachment) response.header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString());
        return response.body(new InputStreamResource(stream));
    }
}
