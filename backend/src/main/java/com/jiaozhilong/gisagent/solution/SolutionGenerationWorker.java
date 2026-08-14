package com.jiaozhilong.gisagent.solution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowGateway;
import com.jiaozhilong.gisagent.knowledge.KnowledgeRetrievalService;
import com.jiaozhilong.gisagent.project.ProjectService;
import com.jiaozhilong.gisagent.settings.ModelConfigService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SolutionGenerationWorker {
    private final SolutionRunRepository repository;
    private final RagflowGateway ragflowGateway;
    private final KnowledgeRetrievalService retrieval;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final ModelConfigService modelConfigService;
    private final SolutionSectionRepository sectionRepository;

    public SolutionGenerationWorker(SolutionRunRepository repository, RagflowGateway ragflowGateway,
                                    KnowledgeRetrievalService retrieval, ProjectService projectService,
                                    ObjectMapper objectMapper, ModelConfigService modelConfigService,
                                    SolutionSectionRepository sectionRepository) {
        this.repository = repository;
        this.ragflowGateway = ragflowGateway;
        this.retrieval = retrieval;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
        this.modelConfigService = modelConfigService;
        this.sectionRepository = sectionRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void generate(SolutionGenerationRequested event) {
        SolutionRunEntity run = repository.findDetailedById(event.runId()).orElse(null);
        if (run == null) return;
        try {
            run.setStatus(SolutionEnums.TaskStatus.RUNNING);
            run.setStage(SolutionEnums.Stage.PLANNING);
            repository.save(run);
            String projectContext = projectService.generationContext(event.projectId());
            List<SectionPlan> blueprint = blueprint(event.knowledgeBaseIds(), projectContext);
            for (SectionPlan plan : blueprint) run.getSections().add(planned(run, plan));
            repository.save(run);

            run.setStage(SolutionEnums.Stage.GENERATING);
            double coverageTotal = 0;
            String modelName = null;
            String sessionId = null;
            for (int index = 0; index < blueprint.size(); index++) {
                SectionPlan plan = blueprint.get(index);
                SolutionSectionEntity section = run.getSections().get(index);
                section.setSectionStatus("RETRIEVING");
                repository.save(run);
                String query = String.join("；", plan.queries()) + "。项目上下文：" + projectContext;
                KnowledgeRetrievalService.EvidencePackage evidence = retrieval.evidence(plan.title(), plan.datasets(), plan.knowledgeTypes(), query, 8);
                section.setSectionStatus("GENERATING");
                String prompt = sectionPrompt(event, projectContext, plan, evidence);
                String answer;
                if (modelConfigService.platformGenerationEnabled()) {
                    ModelConfigService.PlatformGenerationResult result = modelConfigService.generateWithPlatformModel(prompt);
                    answer = result.answer(); modelName = result.modelName();
                } else {
                    RagflowGateway.RagflowGenerationResult result = ragflowGateway.generate(new RagflowGateway.RagflowGenerationCommand(
                            event.assistantId(), sessionId, event.userId().toString(), prompt, plan.datasets(), Map.of()));
                    answer = result.answer(); modelName = result.modelName(); sessionId = result.sessionId();
                }
                section.setContent(answer == null ? "" : answer);
                double coverage = Math.min(1, evidence.fragments().size() / 5d);
                coverageTotal += coverage;
                section.setEvidenceCoverage(BigDecimal.valueOf(coverage));
                section.setSourceType(evidence.fragments().isEmpty()
                        ? (event.allowModelSupplement() ? SolutionEnums.SourceType.MODEL_GENERATED : SolutionEnums.SourceType.PENDING_CONFIRMATION)
                        : (event.allowModelSupplement() ? SolutionEnums.SourceType.HYBRID : SolutionEnums.SourceType.KNOWLEDGE_BASE));
                if (evidence.fragments().isEmpty() && !event.allowModelSupplement()) section.setConfirmationReason("本章节未检索到充分证据，严格模式下等待人工补充资料");
                evidence.fragments().forEach(fragment -> section.getCitations().add(citation(section, fragment)));
                section.setSectionStatus("GENERATED");
                repository.save(run);
            }
            run.setRagflowSessionId(sessionId);
            run.setModelName(modelName == null || modelName.isBlank() ? "RAGFlow Assistant" : modelName);
            run.setEvidenceCoverage(BigDecimal.valueOf(coverageTotal / Math.max(1, blueprint.size())));
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setStatus(SolutionEnums.TaskStatus.SUCCEEDED);
            repository.save(run);
            projectService.markGenerated(event.projectId());
        } catch (Exception exception) {
            run.setStatus(SolutionEnums.TaskStatus.FAILED);
            run.setStage(SolutionEnums.Stage.COMPLETED);
            run.setErrorMessage(exception.getMessage() == null ? "章节级方案生成失败" : exception.getMessage());
            repository.save(run);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void regenerate(SolutionSectionRegenerationRequested event) {
        SolutionSectionEntity section = sectionRepository.findDetailedById(event.sectionId()).orElse(null);
        if (section == null || section.isLocked()) return;
        SolutionRunEntity run = section.getRun();
        try {
            section.setSectionStatus("RETRIEVING");
            sectionRepository.save(section);
            List<String> datasets = strings(section.getPreferredDatasets());
            List<String> types = strings(section.getRequiredKnowledgeTypes());
            List<String> queries = strings(section.getRetrievalQueries());
            String projectContext = projectService.generationContext(run.getProjectId());
            String query = String.join("；", queries) + "。项目上下文：" + projectContext;
            KnowledgeRetrievalService.EvidencePackage evidence = retrieval.evidence(section.getTitle(), datasets, types, query, 8);
            section.setSectionStatus("GENERATING");
            SectionPlan plan = new SectionPlan(section.getSortOrder(), section.getSectionKey(), section.getTitle(), section.getPurpose(),
                    types, datasets, queries, section.getGenerationRequirements() + (event.additionalInstruction().isBlank() ? "" : "；补充要求：" + event.additionalInstruction()));
            SolutionGenerationRequested request = new SolutionGenerationRequested(run.getId(), run.getProjectId(), run.getRequestedBy().getId(),
                    run.getRagflowAssistantId(), datasets, run.getGroundingPolicy(), run.isAllowModelSupplement());
            String prompt = sectionPrompt(request, projectContext, plan, evidence);
            String answer;
            if (modelConfigService.platformGenerationEnabled()) {
                answer = modelConfigService.generateWithPlatformModel(prompt).answer();
            } else {
                answer = ragflowGateway.generate(new RagflowGateway.RagflowGenerationCommand(run.getRagflowAssistantId(), null,
                        run.getRequestedBy().getId().toString(), prompt, datasets, Map.of())).answer();
            }
            section.getCitations().clear();
            evidence.fragments().forEach(fragment -> section.getCitations().add(citation(section, fragment)));
            section.setContent(answer == null ? "" : answer);
            section.setEvidenceCoverage(BigDecimal.valueOf(Math.min(1, evidence.fragments().size() / 5d)));
            section.setSourceType(evidence.fragments().isEmpty()
                    ? (run.isAllowModelSupplement() ? SolutionEnums.SourceType.MODEL_GENERATED : SolutionEnums.SourceType.PENDING_CONFIRMATION)
                    : (run.isAllowModelSupplement() ? SolutionEnums.SourceType.HYBRID : SolutionEnums.SourceType.KNOWLEDGE_BASE));
            section.setSectionStatus("GENERATED");
            sectionRepository.save(section);
        } catch (Exception exception) {
            section.setSectionStatus("FAILED");
            section.setConfirmationReason(exception.getMessage() == null ? "章节重新生成失败" : exception.getMessage());
            sectionRepository.save(section);
        }
    }

    private SolutionSectionEntity planned(SolutionRunEntity run, SectionPlan plan) throws Exception {
        SolutionSectionEntity section = new SolutionSectionEntity();
        section.setRun(run); section.setSectionKey(plan.key()); section.setTitle(plan.title()); section.setPurpose(plan.purpose());
        section.setContent(""); section.setSourceType(SolutionEnums.SourceType.PENDING_CONFIRMATION); section.setEvidenceCoverage(BigDecimal.ZERO);
        section.setSortOrder(plan.order()); section.setRequiredKnowledgeTypes(objectMapper.writeValueAsString(plan.knowledgeTypes()));
        section.setPreferredDatasets(objectMapper.writeValueAsString(plan.datasets())); section.setRetrievalQueries(objectMapper.writeValueAsString(plan.queries()));
        section.setGenerationRequirements(plan.requirements()); section.setSectionStatus("PLANNED");
        return section;
    }

    private SolutionCitationEntity citation(SolutionSectionEntity section, KnowledgeRetrievalService.KnowledgeFragment item) {
        SolutionCitationEntity citation = new SolutionCitationEntity();
        citation.setSection(section); citation.setEvidenceId(item.evidenceId()); citation.setRagflowChunkId(item.evidenceId().replace("EV-", ""));
        citation.setDatasetId(item.datasetId()); citation.setDocumentId(item.documentId()); citation.setDocumentName(item.documentName());
        citation.setContentSnapshot(item.text()); citation.setSimilarityScore(BigDecimal.valueOf(item.similarityScore()));
        citation.setPageNumber(item.pageNumber()); citation.setAssetId(item.assetId()); citation.setSlideNumber(item.slideNumber());
        try { citation.setMetadataJson(objectMapper.writeValueAsString(item.metadata())); } catch (Exception ignored) { citation.setMetadataJson("{}"); }
        return citation;
    }

    private String sectionPrompt(SolutionGenerationRequested event, String context, SectionPlan plan,
                                 KnowledgeRetrievalService.EvidencePackage evidence) {
        StringBuilder sources = new StringBuilder();
        int index = 1;
        for (var item : evidence.fragments()) sources.append("[证据").append(index++).append("] 来源：")
                .append(item.documentName()).append(item.slideNumber() == null ? "" : "，PPT第" + item.slideNumber() + "页")
                .append("\n").append(item.text()).append("\n\n");
        return """
                你是 GIS 行业解决方案编制智能体。当前只生成一个章节，不得带入历史客户名称、金额、服务器数量、地区和工期。
                客户需求优先级最高，其次是知识证据，最后才是模型通用知识。

                当前项目：
                %s

                当前章节：%s
                章节目的：%s
                生成要求：%s
                证据策略：%s；允许模型补充：%s

                Evidence Package：
                %s

                规则：事实、产品能力和案例必须引用证据；证据不足的事实使用【待确认】；模型可补充结构、方法和衔接。
                输出本章节正文，并使用 [证据1] 形式标注依据。不要生成其他章节。
                """.formatted(context, plan.title(), plan.purpose(), plan.requirements(), event.groundingPolicy(), event.allowModelSupplement(), sources);
    }

    private List<SectionPlan> blueprint(List<String> selected, String projectContext) {
        List<String> all = selected == null ? List.of() : selected;
        return List.of(
                plan(1, "project-understanding", "项目理解", "形成客户上下文和建设边界", List.of("SOLUTION"), all, List.of("行业现状与客户需求理解", "建设目标与范围")),
                plan(2, "requirement-analysis", "需求分析", "识别业务痛点和GIS能力", List.of("SOLUTION", "CASE"), all, List.of("行业痛点 GIS能力需求", "非功能性需求")),
                plan(3, "overall-design", "总体设计", "形成总体、业务、数据和技术架构", List.of("SOLUTION", "CASE"), all, List.of("总体架构", "数据架构 业务架构 技术架构")),
                plan(4, "data-resources", "数据资源体系", "规划多源空间数据治理与数字底板", List.of("SOLUTION", "CASE"), all, List.of("数据资源体系", "多源数据治理 数字底板")),
                plan(5, "gis-platform", "GIS基础平台", "规划地图、数据、三维、空间分析和门户能力", List.of("PRODUCT", "SOLUTION"), all, List.of("地图服务 数据服务 三维服务", "空间分析 门户共享")),
                plan(6, "business-applications", "业务应用建设", "将GIS能力落到客户业务场景", List.of("SOLUTION", "CASE"), all, List.of("行业专题应用", "一张图业务协同")),
                plan(7, "product-configuration", "产品配置", "基于知识证据给出产品组合与能力依据", List.of("PRODUCT"), all, List.of("SuperMap 产品能力匹配", "产品组合 配置依据")),
                plan(8, "deployment", "部署与实施", "形成部署、实施、运维和安全建议", List.of("PRODUCT", "SOLUTION"), all, List.of("部署架构", "实施计划 安全运维")),
                plan(9, "project-cases", "项目案例", "仅引用真实历史案例的可复用建设思路", List.of("CASE"), all, List.of("同类历史项目案例", "建设成效 可复用经验")));
    }
    private SectionPlan plan(int order, String key, String title, String purpose, List<String> types, List<String> datasets, List<String> queries) {
        return new SectionPlan(order, key, title, purpose, types, datasets, queries, "面向当前客户生成，事实可追溯，历史项目上下文必须隔离");
    }
    private record SectionPlan(int order, String key, String title, String purpose, List<String> knowledgeTypes,
                               List<String> datasets, List<String> queries, String requirements) {}

    private List<String> strings(String value) {
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception exception) {
            return List.of();
        }
    }
}
