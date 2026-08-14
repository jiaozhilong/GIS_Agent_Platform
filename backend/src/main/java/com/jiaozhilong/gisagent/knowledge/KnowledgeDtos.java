package com.jiaozhilong.gisagent.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class KnowledgeDtos {
    private KnowledgeDtos() {}
    public enum KnowledgeType { PRODUCT_TECH, INDUSTRY_SOLUTION, PROJECT_CASE, TROUBLESHOOTING, TEMPLATE }
    public record KnowledgeBase(String id, String name, KnowledgeType knowledgeType, String description,
                                int documentCount, int chunkCount, String embeddingModel, String chunkMethod,
                                String status, String updatedAt) {}
    public record Document(String id, String name, int chunkCount, double progress, String status, String createdAt) {}
    public record CreateRequest(@NotBlank @Size(max = 128) String name, @Size(max = 1000) String description,
                                KnowledgeType knowledgeType, @Size(max = 40) String chunkMethod) {}
    public record SyncResult(int knowledgeBaseCount, int documentCount, int chunkCount, List<KnowledgeBase> knowledgeBases) {}
}
