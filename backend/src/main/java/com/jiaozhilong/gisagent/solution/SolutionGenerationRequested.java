package com.jiaozhilong.gisagent.solution;

import java.util.List;
import java.util.UUID;

public record SolutionGenerationRequested(UUID runId, String projectId, UUID userId, String assistantId,
                                          List<String> knowledgeBaseIds, SolutionEnums.GroundingPolicy groundingPolicy,
                                          boolean allowModelSupplement) {}
