package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import com.jiaozhilong.gisagent.project.ProjectDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeRetrievalService {
    private final RagflowManagementClient ragflow;
    private final KnowledgeAssetService assets;
    private final int candidateTopK;
    private final double vectorWeight;
    private final boolean keywordEnabled;

    public KnowledgeRetrievalService(RagflowManagementClient ragflow, KnowledgeAssetService assets,
                                     @Value("${platform.knowledge.retrieval.candidate-top-k:40}") int candidateTopK,
                                     @Value("${platform.knowledge.retrieval.vector-weight:0.3}") double vectorWeight,
                                     @Value("${platform.knowledge.retrieval.keyword-enabled:true}") boolean keywordEnabled) {
        this.ragflow = ragflow;
        this.assets = assets;
        this.candidateTopK = candidateTopK;
        this.vectorWeight = vectorWeight;
        this.keywordEnabled = keywordEnabled;
    }

    public ProjectDtos.RetrievalResult search(ProjectDtos.RetrievalRequest request) {
        long started = System.nanoTime();
        Map<String, String> datasetNames = new LinkedHashMap<>();
        ragflow.datasets().forEach(item -> datasetNames.put(item.id(), item.name()));
        List<String> datasetIds = routeDatasets(request.knowledgeBaseIds(), datasetNames, request.metadataFilters());
        List<String> queries = expand(request.query());
        Map<String, ScoredChunk> unique = new LinkedHashMap<>();
        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            String query = queries.get(queryIndex);
            var upstream = ragflow.retrieve(new RagflowManagementClient.RetrievalRequest(query, datasetIds, List.of(), 1,
                    Math.min(candidateTopK, 100), request.similarityThreshold(), vectorWeight, 1024,
                    keywordEnabled, metadataCondition(request.metadataFilters())));
            for (RagflowManagementClient.RetrievalChunk chunk : upstream.chunks()) {
                String key = chunk.datasetId() + ":" + chunk.documentId() + ":" + chunk.id();
                double adjusted = chunk.score() + (queryIndex == 0 ? .02 : 0) + exactEntityBoost(request.query(), chunk.content());
                ScoredChunk existing = unique.get(key);
                if (existing == null || adjusted > existing.score()) unique.put(key, new ScoredChunk(chunk, adjusted));
            }
        }
        // Existing production chunks predate platform metadata.  Keep them
        // searchable while all newly ingested assets use strict meta_fields.
        if (unique.isEmpty() && request.metadataFilters() != null && !request.metadataFilters().isEmpty()) {
            return search(new ProjectDtos.RetrievalRequest(request.query(), datasetIds, request.topK(),
                    request.similarityThreshold(), Map.of()));
        }
        List<ProjectDtos.RetrievalHit> hits = unique.values().stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()).limit(request.topK())
                .map(item -> hit(item.chunk(), item.score(), datasetNames)).toList();
        long duration = (System.nanoTime() - started) / 1_000_000;
        return new ProjectDtos.RetrievalResult(UUID.randomUUID(), "SUCCEEDED", request.query(), duration, hits);
    }

    public EvidencePackage evidence(String section, List<String> preferredDatasets, List<String> knowledgeTypes,
                                    String query, int topK) {
        Map<String, String> datasetNames = new LinkedHashMap<>();
        ragflow.datasets().forEach(item -> datasetNames.put(item.id(), item.name()));
        List<String> routedDatasets = routeByKnowledgeTypes(preferredDatasets, knowledgeTypes, datasetNames);
        Map<String, Object> filters = knowledgeTypes == null || knowledgeTypes.isEmpty() ? Map.of()
                : Map.of("knowledge_type", knowledgeTypes);
        ProjectDtos.RetrievalResult result = search(new ProjectDtos.RetrievalRequest(query,
                routedDatasets, Math.max(1, Math.min(20, topK)), .2, filters));
        List<KnowledgeFragment> fragments = result.hits().stream().map(hit -> new KnowledgeFragment(
                "EV-" + hit.chunkId(), hit.metadata().get("knowledge_type"), hit.knowledgeBaseId(), hit.documentId(),
                hit.assetContext() == null ? null : hit.assetContext().assetId(), hit.documentName(),
                hit.metadata().get("document_type"), hit.assetContext() == null ? null : hit.assetContext().pageTitle(),
                hit.pageNumber(), hit.assetContext() == null ? null : hit.assetContext().pptPage(), hit.content(), hit.score(),
                hit.metadata(), hit.assetContext())).toList();
        return new EvidencePackage(section, fragments, citations(fragments), result.durationMs());
    }

    private ProjectDtos.RetrievalHit hit(RagflowManagementClient.RetrievalChunk item, double score, Map<String, String> names) {
        return new ProjectDtos.RetrievalHit(item.id(), item.datasetId(), names.getOrDefault(item.datasetId(), item.datasetId()),
                item.documentId(), item.documentName(), item.id(), item.content(), Math.min(1, score), item.pageNumber(),
                item.metadata(), assets.contextFor(item.documentId(), item.content(), item.pageNumber()));
    }

    private List<String> expand(String query) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        queries.add(query.trim());
        if (query.contains("一张图")) { queries.add(query + " 总体架构 数据底座"); queries.add(query + " 二三维一体化 空间分析"); }
        if (query.contains("总体") || query.contains("架构")) { queries.add(query + " 技术架构"); queries.add(query + " 数据架构 业务架构"); }
        if (query.contains("产品") || query.matches("(?i).*iServer|iPortal|iDesktop|iObjects.*")) queries.add(query + " 产品能力 功能特性");
        if (query.contains("案例") || query.contains("项目")) queries.add(query + " 历史项目 建设成效");
        return new ArrayList<>(queries).subList(0, Math.min(5, queries.size()));
    }

    private List<String> routeDatasets(List<String> requested, Map<String, String> names, Map<String, Object> filters) {
        if (requested != null && !requested.isEmpty()) return requested;
        Object knowledge = filters == null ? null : filters.get("knowledge_type");
        String value = String.valueOf(knowledge == null ? "" : knowledge).toUpperCase(Locale.ROOT);
        List<String> routed = names.entrySet().stream().filter(entry ->
                value.contains("PRODUCT") ? entry.getValue().contains("产品技术") :
                value.contains("CASE") ? entry.getValue().contains("历史项目") :
                value.contains("TEMPLATE") ? entry.getValue().contains("模板") :
                value.contains("TROUBLESHOOTING") ? entry.getValue().contains("故障") : true)
                .map(Map.Entry::getKey).toList();
        return routed.isEmpty() ? new ArrayList<>(names.keySet()) : routed;
    }

    private List<String> routeByKnowledgeTypes(List<String> preferred, List<String> knowledgeTypes, Map<String, String> names) {
        String value = String.join(",", knowledgeTypes == null ? List.of() : knowledgeTypes).toUpperCase(Locale.ROOT);
        LinkedHashSet<String> routed = new LinkedHashSet<>();
        names.forEach((id, name) -> {
            if (value.contains("PRODUCT") && name.contains("产品技术")) routed.add(id);
            if (value.contains("SOLUTION") && name.contains("行业解决方案")) routed.add(id);
            if (value.contains("CASE") && name.contains("历史项目")) routed.add(id);
            if (value.contains("TEMPLATE") && name.contains("模板")) routed.add(id);
            if (value.contains("TROUBLESHOOTING") && name.contains("故障")) routed.add(id);
        });
        if (routed.isEmpty()) return preferred == null || preferred.isEmpty() ? new ArrayList<>(names.keySet()) : preferred;
        if (preferred != null && !preferred.isEmpty()) routed.removeIf(id -> !preferred.contains(id));
        return routed.isEmpty() ? (preferred == null ? List.of() : preferred) : new ArrayList<>(routed);
    }

    private Map<String, Object> metadataCondition(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return Map.of();
        List<Map<String, Object>> conditions = new ArrayList<>();
        filters.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).isBlank()) conditions.add(Map.of("name", key, "comparison_operator", "in", "value", value));
        });
        return conditions.isEmpty() ? Map.of() : Map.of("logic", "and", "conditions", conditions);
    }
    private double exactEntityBoost(String query, String content) {
        String joined = (query + " " + content).toLowerCase(Locale.ROOT);
        return List.of("iserver", "iportal", "idesktopx", "iobjects", "rest", "地图服务", "三维服务", "空间分析")
                .stream().filter(term -> query.toLowerCase(Locale.ROOT).contains(term) && joined.contains(term)).count() * .01;
    }
    private List<Citation> citations(List<KnowledgeFragment> fragments) {
        return fragments.stream().map(item -> new Citation(item.evidenceId(), item.documentName(), item.pageNumber(), item.slideNumber())).toList();
    }

    private record ScoredChunk(RagflowManagementClient.RetrievalChunk chunk, double score) {}
    public record KnowledgeFragment(String evidenceId, String knowledgeType, String datasetId, String documentId,
                                    UUID assetId, String documentName, String documentType, String sectionTitle,
                                    Integer pageNumber, Integer slideNumber, String text, double similarityScore,
                                    Map<String, String> metadata, KnowledgeAssetDtos.AssetContext assetContext) {}
    public record Citation(String evidenceId, String documentName, Integer pageNumber, Integer slideNumber) {}
    public record EvidencePackage(String section, List<KnowledgeFragment> fragments, List<Citation> citations, long durationMs) {}
}
