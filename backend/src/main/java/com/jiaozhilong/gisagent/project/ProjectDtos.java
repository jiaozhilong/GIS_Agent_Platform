package com.jiaozhilong.gisagent.project;

import com.jiaozhilong.gisagent.knowledge.KnowledgeAssetDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {}

    public enum Stage { DRAFT, REQUIREMENT_ANALYSIS, PRODUCT_MATCH, KNOWLEDGE_RETRIEVAL, PROPOSAL_GENERATION, REVIEW, DELIVERED }

    public record ProjectSummary(UUID id, String name, String customerName, String industry, Stage stage, int progress,
                                 String ownerName, OffsetDateTime updatedAt) {}

    public record ProjectDetail(UUID id, String name, String customerName, String industry, Stage stage, int progress,
                                String ownerName, OffsetDateTime updatedAt, String projectCode, String region,
                                String background, String rawDemand, List<String> goals, LocalDate deliveryDeadline,
                                List<String> knowledgeBaseIds, List<String> collaboratorNames) {}

    public record UpsertRequest(@NotBlank @Size(max = 240) String name,
                                @NotBlank @Size(max = 200) String customerName,
                                @NotBlank @Size(max = 100) String industry,
                                @Size(max = 120) String region,
                                @Size(max = 5000) String background,
                                @NotBlank @Size(max = 10000) String rawDemand,
                                List<String> goals,
                                LocalDate deliveryDeadline,
                                List<String> knowledgeBaseIds,
                                List<String> collaboratorNames) {}

    public record RequirementDimension(String name, int score, String description) {}
    public record RequirementAnalysis(UUID taskId, String status, int completion, List<String> demandPoints,
                                      String summary, List<RequirementDimension> dimensions,
                                      List<String> recommendedProductNames) {}

    public record ProductMatch(String productId, String productName, String productFamily, int matchScore,
                               List<String> matchedCapabilities, List<String> gaps, boolean recommended) {}

    public record RetrievalRequest(@NotBlank @Size(min = 4, max = 2000) String query,
                                   List<String> knowledgeBaseIds,
                                   @Min(1) @Max(20) int topK,
                                   @Min(0) @Max(1) double similarityThreshold,
                                   Map<String, Object> metadataFilters) {}

    public record RetrievalHit(String id, String knowledgeBaseId, String knowledgeBaseName, String documentId,
                               String documentName, String chunkId, String content, double score,
                               Integer pageNumber, Map<String, String> metadata,
                               KnowledgeAssetDtos.AssetContext assetContext) {}
    public record RetrievalResult(UUID taskId, String status, String query, long durationMs, List<RetrievalHit> hits) {}
}
