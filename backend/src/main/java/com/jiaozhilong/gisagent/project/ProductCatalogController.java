package com.jiaozhilong.gisagent.project;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-catalog")
public class ProductCatalogController {
    private final ProductCatalogService service;
    public ProductCatalogController(ProductCatalogService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('project:view')")
    public ApiResponse<List<ProductCatalogService.CatalogItem>> list() { return ApiResponse.ok(service.list()); }
}
