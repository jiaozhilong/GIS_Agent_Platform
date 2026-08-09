package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeService {
    private final RagflowManagementClient ragflow;
    public KnowledgeService(RagflowManagementClient ragflow) { this.ragflow = ragflow; }

    public List<KnowledgeDtos.KnowledgeBase> list() { return ragflow.datasets().stream().map(this::map).toList(); }

    public KnowledgeDtos.SyncResult sync() {
        List<KnowledgeDtos.KnowledgeBase> bases = list();
        return new KnowledgeDtos.SyncResult(bases.size(), bases.stream().mapToInt(KnowledgeDtos.KnowledgeBase::documentCount).sum(),
                bases.stream().mapToInt(KnowledgeDtos.KnowledgeBase::chunkCount).sum(), bases);
    }

    public KnowledgeDtos.KnowledgeBase create(KnowledgeDtos.CreateRequest request) {
        return map(ragflow.createDataset(request.name().trim(), request.description(),
                request.chunkMethod() == null || request.chunkMethod().isBlank() ? defaultChunk(request.knowledgeType()) : request.chunkMethod()));
    }

    public List<KnowledgeDtos.Document> documents(String datasetId) {
        return ragflow.documents(datasetId).stream().map(item -> new KnowledgeDtos.Document(item.id(), item.name(), item.chunkCount(),
                item.progress(), status(item.run(), item.progress()), item.createdAt())).toList();
    }

    public List<KnowledgeDtos.Document> upload(String datasetId, String filename, byte[] content) {
        return ragflow.uploadAndParse(datasetId, filename, content).stream().map(item -> new KnowledgeDtos.Document(item.id(), item.name(),
                item.chunkCount(), item.progress(), status(item.run(), item.progress()), item.createdAt())).toList();
    }

    private KnowledgeDtos.KnowledgeBase map(RagflowManagementClient.Dataset item) {
        return new KnowledgeDtos.KnowledgeBase(item.id(), item.name(), type(item.name()), item.description(), item.documentCount(),
                item.chunkCount(), item.embeddingModel(), item.chunkMethod(), item.ready() ? "READY" : "ERROR", item.updatedAt());
    }

    private KnowledgeDtos.KnowledgeType type(String name) {
        if (name.startsWith("01-") || name.contains("产品")) return KnowledgeDtos.KnowledgeType.PRODUCT_TECH;
        if (name.startsWith("02-") || name.contains("行业")) return KnowledgeDtos.KnowledgeType.INDUSTRY_SOLUTION;
        if (name.startsWith("03-") || name.contains("案例")) return KnowledgeDtos.KnowledgeType.PROJECT_CASE;
        if (name.startsWith("04-") || name.contains("故障")) return KnowledgeDtos.KnowledgeType.TROUBLESHOOTING;
        return KnowledgeDtos.KnowledgeType.TEMPLATE;
    }

    private String defaultChunk(KnowledgeDtos.KnowledgeType type) {
        if (type == KnowledgeDtos.KnowledgeType.TROUBLESHOOTING) return "qa";
        if (type == KnowledgeDtos.KnowledgeType.TEMPLATE) return "presentation";
        return "manual";
    }

    private String status(String run, double progress) {
        if ("FAIL".equalsIgnoreCase(run)) return "ERROR";
        if (progress >= 1 || "DONE".equalsIgnoreCase(run)) return "READY";
        if ("UNSTART".equalsIgnoreCase(run)) return "PENDING";
        return "PARSING";
    }
}
