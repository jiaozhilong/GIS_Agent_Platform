package com.jiaozhilong.gisagent.settings;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/model-configs")
public class ModelConfigController {
    private final ModelConfigService service;
    public ModelConfigController(ModelConfigService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('model:view')")
    public ApiResponse<List<ModelConfigService.Config>> list() { return ApiResponse.ok(service.list()); }

    @PostMapping("/{provider}/test") @PreAuthorize("hasAuthority('model:manage')")
    public ApiResponse<ModelConfigService.Config> test(@PathVariable ModelConfigService.Provider provider,
                                                        @RequestBody(required = false) ModelConfigService.UpdateRequest request) {
        return ApiResponse.ok(service.test(provider, request));
    }

    @PutMapping("/{provider}") @PreAuthorize("hasAuthority('model:manage')")
    public ApiResponse<ModelConfigService.Config> save(@PathVariable ModelConfigService.Provider provider,
                                                        @RequestBody ModelConfigService.UpdateRequest request) {
        return ApiResponse.ok(service.save(provider, request));
    }
}
