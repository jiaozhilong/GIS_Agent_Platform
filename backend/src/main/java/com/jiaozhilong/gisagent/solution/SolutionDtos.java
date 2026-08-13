package com.jiaozhilong.gisagent.solution;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SolutionDtos {
    private SolutionDtos() {}

    public record GenerationRequest(@NotEmpty List<String> knowledgeBaseIds, String ragflowAssistantId,
                                    @NotNull SolutionEnums.GroundingPolicy groundingPolicy,
                                    boolean allowModelSupplement, @NotEmpty List<String> outputFormats) {}
    public record EvidenceResponse(String evidenceId, String chunkId, String datasetId, String documentId,
                                   String documentName, String content, Double score, Integer pageNumber,
                                   String assetId, Integer slideNumber) {}
    public record SectionResponse(String id, String sectionKey, String title, String purpose, String content,
                                  SolutionEnums.SourceType sourceType, double evidenceCoverage,
                                  List<String> retrievalQueries, List<String> requiredKnowledgeTypes,
                                  String status, boolean locked, List<EvidenceResponse> evidence,
                                  String confirmationReason) {}
    public record GenerationRunResponse(UUID id, String projectId, SolutionEnums.TaskStatus status,
                                        SolutionEnums.Stage stage, String ragflowSessionId, String modelName,
                                        double evidenceCoverage, List<SectionResponse> sections,
                                        OffsetDateTime createdAt, OffsetDateTime updatedAt, String errorMessage) {}
    public record UpdateSectionRequest(@NotBlank @Size(max = 300) String title,
                                       @NotNull @Size(max = 200000) String content) {}
    public record LockSectionRequest(boolean locked) {}
    public record RegenerateSectionRequest(String additionalInstruction) {}
}
