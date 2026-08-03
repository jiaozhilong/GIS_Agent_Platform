package com.jiaozhilong.gisagent.solution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowGateway;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class SolutionGenerationWorker {
    private final SolutionRunRepository repository;
    private final RagflowGateway ragflowGateway;
    private final ObjectMapper objectMapper;

    public SolutionGenerationWorker(SolutionRunRepository repository, RagflowGateway ragflowGateway, ObjectMapper objectMapper) {
        this.repository = repository;
        this.ragflowGateway = ragflowGateway;
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

            String prompt = prompt(event);
            RagflowGateway.RagflowGenerationResult result = ragflowGateway.generate(new RagflowGateway.RagflowGenerationCommand(
                    event.assistantId(), null, event.userId().toString(), prompt, event.knowledgeBaseIds(), Map.of()));

            SolutionSectionEntity section = new SolutionSectionEntity();
            section.setRun(run);
            section.setSectionKey("generated-solution-draft");
            section.setTitle("方案初稿");
            section.setContent(result.answer() == null ? "" : result.answer());
            double coverage = Math.min(1d, result.citations().size() / 4d);
            SolutionEnums.SourceType sourceType = result.citations().isEmpty()
                    ? (event.allowModelSupplement() ? SolutionEnums.SourceType.MODEL_GENERATED : SolutionEnums.SourceType.PENDING_CONFIRMATION)
                    : (event.allowModelSupplement() ? SolutionEnums.SourceType.HYBRID : SolutionEnums.SourceType.KNOWLEDGE_BASE);
            section.setSourceType(sourceType);
            section.setEvidenceCoverage(BigDecimal.valueOf(coverage));
            section.setSortOrder(1);
            if (sourceType == SolutionEnums.SourceType.PENDING_CONFIRMATION) section.setConfirmationReason("知识库未返回可引用证据，严格模式下禁止模型补写事实内容");
            for (RagflowGateway.RagflowCitation item : result.citations()) {
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
            run.setModelName(result.modelName());
            run.setEvidenceCoverage(BigDecimal.valueOf(coverage));
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setStatus(SolutionEnums.TaskStatus.SUCCEEDED);
            repository.save(run);
        } catch (Exception exception) {
            run.setStatus(SolutionEnums.TaskStatus.FAILED);
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setErrorMessage(exception.getMessage() == null ? "RAGFlow 联合生成失败" : exception.getMessage());
            repository.save(run);
        }
    }

    private String prompt(SolutionGenerationRequested event) {
        return """
                你是 GIS 行业解决方案编制智能体。请基于 RAGFlow 当前 Assistant 绑定的知识库和其内部大模型，生成项目方案初稿。

                项目ID：%s
                允许使用的知识库ID：%s
                证据策略：%s

                规则：
                1. 产品型号、产品参数、案例数据、政策规范、客户现状等事实内容必须来自知识库并保留引用。
                2. 知识库没有的建设思路、章节衔接、实施建议和保障措施，可以由大模型合理补全。
                3. 如果事实性内容没有证据，不得编造，使用“待确认”明确标识。
                4. 输出结构完整的中文方案正文，包含项目理解、需求分析、总体设计、建设内容、实施计划、保障措施和预期成效。
                5. 优先复用已有资料的准确表达，同时进行必要的整合、推演和专业化改写，不要只罗列检索结果。
                """.formatted(event.projectId(), event.knowledgeBaseIds(), event.groundingPolicy());
    }
}
