package com.jiaozhilong.gisagent.solution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowGateway;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import com.jiaozhilong.gisagent.project.ProjectService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SolutionGenerationWorker {
    private final SolutionRunRepository repository;
    private final RagflowGateway ragflowGateway;
    private final RagflowManagementClient ragflowManagement;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public SolutionGenerationWorker(SolutionRunRepository repository, RagflowGateway ragflowGateway,
                                    RagflowManagementClient ragflowManagement, ProjectService projectService,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.ragflowGateway = ragflowGateway;
        this.ragflowManagement = ragflowManagement;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void generate(SolutionGenerationRequested event) {
        SolutionRunEntity run = repository.findDetailedById(event.runId()).orElse(null);
        if (run == null) return;
        try {
            run.setStatus(SolutionEnums.TaskStatus.RUNNING);
            run.setStage(SolutionEnums.Stage.GENERATING);
            repository.save(run);

            String projectContext = projectService.generationContext(event.projectId());
            RagflowManagementClient.RetrievalResult retrieval = ragflowManagement.retrieve(
                    "请检索能够支撑以下 GIS 项目的产品能力、技术架构、建设内容、实施计划和案例依据：\n" + projectContext,
                    event.knowledgeBaseIds(), 10, 0.2);
            String prompt = prompt(event, projectContext, evidence(retrieval.chunks()));
            RagflowGateway.RagflowGenerationResult result = ragflowGateway.generate(new RagflowGateway.RagflowGenerationCommand(
                    event.assistantId(), null, event.userId().toString(), prompt, event.knowledgeBaseIds(), Map.of()));

            List<RagflowGateway.RagflowCitation> citations = new ArrayList<>(result.citations());
            for (RagflowManagementClient.RetrievalChunk item : retrieval.chunks()) {
                citations.add(new RagflowGateway.RagflowCitation(item.id(), item.datasetId(), item.documentId(), item.documentName(),
                        item.content(), item.score(), item.pageNumber(), Map.copyOf(item.metadata())));
            }

            SolutionSectionEntity section = new SolutionSectionEntity();
            section.setRun(run);
            section.setSectionKey("generated-solution-draft");
            section.setTitle("GIS 项目建设方案初稿");
            section.setContent(result.answer() == null ? "" : result.answer());
            double coverage = Math.min(1d, citations.size() / 8d);
            SolutionEnums.SourceType sourceType = citations.isEmpty()
                    ? (event.allowModelSupplement() ? SolutionEnums.SourceType.MODEL_GENERATED : SolutionEnums.SourceType.PENDING_CONFIRMATION)
                    : (event.allowModelSupplement() ? SolutionEnums.SourceType.HYBRID : SolutionEnums.SourceType.KNOWLEDGE_BASE);
            section.setSourceType(sourceType);
            section.setEvidenceCoverage(BigDecimal.valueOf(coverage));
            section.setSortOrder(1);
            if (sourceType == SolutionEnums.SourceType.PENDING_CONFIRMATION) {
                section.setConfirmationReason("知识库未返回可引用证据，严格模式下禁止模型补写事实内容");
            }
            for (RagflowGateway.RagflowCitation item : citations) {
                SolutionCitationEntity citation = new SolutionCitationEntity();
                citation.setSection(section);
                citation.setRagflowChunkId(item.chunkId());
                citation.setDatasetId(item.datasetId());
                citation.setDocumentId(item.documentId());
                citation.setDocumentName(item.documentName());
                citation.setContentSnapshot(item.content());
                if (item.score() != null) citation.setSimilarityScore(BigDecimal.valueOf(item.score()));
                citation.setPageNumber(item.pageNumber());
                citation.setMetadataJson(objectMapper.writeValueAsString(item.metadata()));
                section.getCitations().add(citation);
            }
            run.getSections().add(section);
            run.setRagflowSessionId(result.sessionId());
            String modelName = result.modelName();
            if (modelName == null || modelName.isBlank()) {
                RagflowManagementClient.ChatInfo assistant = ragflowManagement.assistant();
                modelName = assistant == null ? "RAGFlow Assistant" : assistant.modelName();
            }
            run.setModelName(modelName);
            run.setEvidenceCoverage(BigDecimal.valueOf(coverage));
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setStatus(SolutionEnums.TaskStatus.SUCCEEDED);
            repository.save(run);
            projectService.markGenerated(event.projectId());
        } catch (Exception exception) {
            run.setStatus(SolutionEnums.TaskStatus.FAILED);
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setErrorMessage(exception.getMessage() == null ? "RAGFlow 联合生成失败" : exception.getMessage());
            repository.save(run);
        }
    }

    private String prompt(SolutionGenerationRequested event, String projectContext, String evidence) {
        return """
                你是 GIS 行业解决方案编制智能体。请结合下面的真实项目上下文、RAGFlow 知识库检索证据和 Assistant 内部的 DeepSeek 模型，生成可用于评审的中文方案初稿。

                项目上下文：
                %s

                已选知识库 ID：%s
                证据策略：%s

                RAGFlow 检索证据：
                %s

                规则：
                1. 产品型号、产品参数、案例数据、政策规范和客户现状等事实必须来自知识库证据。
                2. 知识库没有覆盖的总体思路、章节衔接、实施建议和保障措施，可以由大模型合理补全。
                3. 无证据的事实性内容不得编造，使用【待确认】明确标识。
                4. 输出结构完整的中文方案正文，包含项目理解、需求分析、总体设计、建设内容、实施计划、保障措施和预期成效。
                5. 在正文适当位置使用 [知识证据1] 这样的标记，便于回溯引用。
                """.formatted(projectContext, event.knowledgeBaseIds(), event.groundingPolicy(), evidence);
    }

    private String evidence(List<RagflowManagementClient.RetrievalChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RagflowManagementClient.RetrievalChunk chunk : chunks) {
            builder.append("[知识证据").append(index++).append("] 来源：").append(chunk.documentName())
                    .append("，相似度：").append(String.format("%.2f", chunk.score())).append("\n")
                    .append(chunk.content()).append("\n\n");
        }
        return builder.toString();
    }
}
