package com.jiaozhilong.gisagent.solution;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SolutionDtos {
    private SolutionDtos() {}

    public record GenerationRequest(@NotEmpty List<String> knowledgeBaseIds, String ragflowAssistantId,
                                    @NotNull SolutionEnums.GroundingPolicy groundingPolicy,
                                    boolean allowModelSupplement, @NotEmpty List<String> outputFormats) {}
    public record SectionResponse(String id, String title, String content, SolutionEnums.SourceType sourceType,
                                  double evidenceCoverage, List<String> citationIds, String confirmationReason) {}
    public record GenerationRunResponse(UUID id, String projectId, SolutionEnums.TaskStatus status,
                                        SolutionEnums.Stage stage, String ragflowSessionId, String modelName,
                                        double evidenceCoverage, List<SectionResponse> sections,
                                        OffsetDateTime createdAt, OffsetDateTime updatedAt, String errorMessage) {}
}
