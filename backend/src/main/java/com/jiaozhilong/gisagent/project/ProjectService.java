package com.jiaozhilong.gisagent.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RagflowManagementClient ragflow;

    public ProjectService(JdbcTemplate jdbc, ObjectMapper objectMapper, RagflowManagementClient ragflow) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.ragflow = ragflow;
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectSummary> list(String keyword, ProjectDtos.Stage stage) {
        return jdbc.query("select * from platform_projects order by updated_at desc", this::detail).stream()
                .filter(item -> stage == null || item.stage() == stage)
                .filter(item -> keyword == null || keyword.isBlank() ||
                        (item.name() + item.customerName() + item.industry()).toLowerCase(Locale.ROOT)
                                .contains(keyword.trim().toLowerCase(Locale.ROOT)))
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectDetail get(String id) {
        UUID projectId = uuid(id);
        List<ProjectDtos.ProjectDetail> result = jdbc.query("select * from platform_projects where id = ?", this::detail, projectId);
        if (result.isEmpty()) throw notFound();
        return result.get(0);
    }

    @Transactional
    public ProjectDtos.ProjectDetail create(String username, ProjectDtos.UpsertRequest request) {
        UUID id = UUID.randomUUID();
        String code = "GIS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + id.toString().substring(0, 6).toUpperCase(Locale.ROOT);
        Map<String, Object> owner = owner(username);
        jdbc.update("""
                insert into platform_projects(id, project_code, name, customer_name, industry, region, background, raw_demand,
                  goals, delivery_deadline, knowledge_base_ids, collaborator_names, owner_id, owner_name)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?::jsonb, ?, ?)
                """, id, code, clean(request.name()), clean(request.customerName()), clean(request.industry()), clean(request.region()),
                clean(request.background()), clean(request.rawDemand()), json(strings(request.goals())),
                request.deliveryDeadline() == null ? null : Date.valueOf(request.deliveryDeadline()),
                json(strings(request.knowledgeBaseIds())), json(strings(request.collaboratorNames())),
                owner.get("id"), owner.get("display_name"));
        return get(id.toString());
    }

    @Transactional
    public ProjectDtos.ProjectDetail update(String id, ProjectDtos.UpsertRequest request) {
        UUID projectId = uuid(id);
        int changed = jdbc.update("""
                update platform_projects set name=?, customer_name=?, industry=?, region=?, background=?, raw_demand=?,
                  goals=?::jsonb, delivery_deadline=?, knowledge_base_ids=?::jsonb, collaborator_names=?::jsonb,
                  updated_at=current_timestamp where id=?
                """, clean(request.name()), clean(request.customerName()), clean(request.industry()), clean(request.region()),
                clean(request.background()), clean(request.rawDemand()), json(strings(request.goals())),
                request.deliveryDeadline() == null ? null : Date.valueOf(request.deliveryDeadline()),
                json(strings(request.knowledgeBaseIds())), json(strings(request.collaboratorNames())), projectId);
        if (changed == 0) throw notFound();
        return get(id);
    }

    @Transactional
    public ProjectDtos.RequirementAnalysis analyze(String id) {
        ProjectDtos.ProjectDetail project = get(id);
        String demand = project.rawDemand();
        LinkedHashSet<String> points = new LinkedHashSet<>();
        addIfContains(points, demand, "数据", "整合多源异构空间数据并建立统一标准");
        addIfContains(points, demand, "二三维", "建设二三维一体化 GIS 展示与分析能力");
        addIfContains(points, demand, "空间分析", "提供面向业务的空间分析与辅助决策能力");
        addIfContains(points, demand, "服务", "统一发布、管理和共享 GIS 服务");
        addIfContains(points, demand, "权限", "建立用户、角色、数据与服务权限体系");
        addIfContains(points, demand, "知识库", "利用知识库证据与大模型联合生成可追溯方案");
        if (points.isEmpty()) points.add("梳理客户现状、建设目标和业务边界，形成可执行的 GIS 建设范围");

        List<ProjectDtos.RequirementDimension> dimensions = List.of(
                dimension("数据治理", demand, List.of("数据", "标准", "整合"), "多源数据汇聚、标准化与质量治理"),
                dimension("平台能力", demand, List.of("平台", "服务", "共享"), "GIS 服务发布、管理与开放集成"),
                dimension("业务应用", demand, List.of("业务", "审批", "一张图"), "业务协同与一张图专题应用"),
                dimension("空间智能", demand, List.of("空间分析", "AI", "智能"), "空间分析、知识检索与智能辅助"),
                dimension("安全运维", demand, List.of("权限", "审计", "运维"), "权限、安全、监控与持续运维"));
        int completion = (int) Math.round(dimensions.stream().mapToInt(ProjectDtos.RequirementDimension::score).average().orElse(60));
        List<String> products = recommendedProducts(demand);
        ProjectDtos.RequirementAnalysis result = new ProjectDtos.RequirementAnalysis(UUID.randomUUID(), "SUCCEEDED", completion,
                new ArrayList<>(points), "项目以“" + project.name() + "”为目标，需要同步推进数据底座、GIS 平台能力、业务应用与智能方案生成，并在实施中保留知识证据与人工确认环节。",
                dimensions, products);
        jdbc.update("insert into requirement_analysis_runs(id, project_id, result_json) values (?, ?, ?::jsonb)", result.taskId(), project.id(), json(result));
        advance(project.id(), ProjectDtos.Stage.REQUIREMENT_ANALYSIS, 30);
        return result;
    }

    @Transactional
    public List<ProjectDtos.ProductMatch> matchProducts(String id) {
        ProjectDtos.ProjectDetail project = get(id);
        List<String> datasetIds = project.knowledgeBaseIds().isEmpty()
                ? ragflow.datasets().stream().map(RagflowManagementClient.Dataset::id).toList()
                : project.knowledgeBaseIds();
        RagflowManagementClient.RetrievalResult evidence = ragflow.retrieve(
                "根据以下项目需求匹配 SuperMap GIS 产品能力：" + project.rawDemand(), datasetIds, 8, 0.2);
        String joined = evidence.chunks().stream().map(RagflowManagementClient.RetrievalChunk::content)
                .reduce("", (left, right) -> left + "\n" + right);
        List<ProjectDtos.ProductMatch> result = List.of(
                product("iserver", "SuperMap iServer", "云 GIS 平台", joined, List.of("GIS 服务发布与管理", "空间分析", "分布式处理", "云原生扩展"), 94, true),
                product("iportal", "SuperMap iPortal", "GIS 门户", joined, List.of("资源整合与共享", "服务注册", "权限控制", "专题应用"), 89, true),
                product("idesktopx", "SuperMap iDesktopX", "桌面 GIS", joined, List.of("空间数据生产", "制图与分析", "数据治理"), 82, true),
                product("iobjects", "SuperMap iObjects", "组件 GIS", joined, List.of("业务系统集成", "二次开发", "空间计算"), 76, false));
        jdbc.update("insert into product_match_runs(project_id, result_json) values (?, ?::jsonb)", project.id(), json(result));
        advance(project.id(), ProjectDtos.Stage.PRODUCT_MATCH, 48);
        return result;
    }

    @Transactional
    public ProjectDtos.RetrievalResult retrieve(String id, ProjectDtos.RetrievalRequest request) {
        ProjectDtos.ProjectDetail project = get(id);
        List<RagflowManagementClient.Dataset> datasets = ragflow.datasets();
        Map<String, String> names = new LinkedHashMap<>();
        datasets.forEach(item -> names.put(item.id(), item.name()));
        RagflowManagementClient.RetrievalResult upstream = ragflow.retrieve(request.query(), request.knowledgeBaseIds(), request.topK(), request.similarityThreshold());
        List<ProjectDtos.RetrievalHit> hits = upstream.chunks().stream().limit(request.topK()).map(item ->
                new ProjectDtos.RetrievalHit(item.id(), item.datasetId(), names.getOrDefault(item.datasetId(), item.datasetId()),
                        item.documentId(), item.documentName(), item.id(), item.content(), item.score(), item.pageNumber(), item.metadata())).toList();
        ProjectDtos.RetrievalResult result = new ProjectDtos.RetrievalResult(UUID.randomUUID(), "SUCCEEDED", request.query(), upstream.durationMs(), hits);
        jdbc.update("""
                insert into retrieval_runs(id, project_id, query, knowledge_base_ids, top_k, similarity_threshold, duration_ms, result_json)
                values (?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb)
                """, result.taskId(), project.id(), request.query(), json(request.knowledgeBaseIds()), request.topK(),
                request.similarityThreshold(), result.durationMs(), json(result));
        advance(project.id(), ProjectDtos.Stage.KNOWLEDGE_RETRIEVAL, 64);
        return result;
    }

    @Transactional(readOnly = true)
    public String generationContext(String id) {
        ProjectDtos.ProjectDetail project = get(id);
        return "项目名称：" + project.name() + "\n客户：" + project.customerName() + "\n行业：" + project.industry()
                + "\n区域：" + project.region() + "\n项目背景：" + project.background() + "\n原始需求：" + project.rawDemand()
                + "\n建设目标：" + String.join("；", project.goals()) + "\n交付日期：" + project.deliveryDeadline();
    }

    @Transactional
    public void markGenerating(String id) { advance(uuid(id), ProjectDtos.Stage.PROPOSAL_GENERATION, 75); }

    @Transactional
    public void markGenerated(String id) { advance(uuid(id), ProjectDtos.Stage.REVIEW, 90); }

    private ProjectDtos.ProjectDetail detail(ResultSet rs, int rowNum) throws SQLException {
        return new ProjectDtos.ProjectDetail((UUID) rs.getObject("id"), rs.getString("name"), rs.getString("customer_name"),
                rs.getString("industry"), ProjectDtos.Stage.valueOf(rs.getString("stage")), rs.getInt("progress"),
                rs.getString("owner_name"), rs.getObject("updated_at", OffsetDateTime.class), rs.getString("project_code"),
                rs.getString("region"), rs.getString("background"), rs.getString("raw_demand"),
                strings(rs.getString("goals")), rs.getObject("delivery_deadline", LocalDate.class),
                strings(rs.getString("knowledge_base_ids")), strings(rs.getString("collaborator_names")));
    }

    private ProjectDtos.ProjectSummary summary(ProjectDtos.ProjectDetail detail) {
        return new ProjectDtos.ProjectSummary(detail.id(), detail.name(), detail.customerName(), detail.industry(),
                detail.stage(), detail.progress(), detail.ownerName(), detail.updatedAt());
    }

    private void advance(UUID projectId, ProjectDtos.Stage stage, int progress) {
        jdbc.update("update platform_projects set stage=?, progress=greatest(progress, ?), updated_at=current_timestamp where id=?",
                stage.name(), progress, projectId);
    }

    private ProjectDtos.RequirementDimension dimension(String name, String demand, List<String> terms, String description) {
        long matched = terms.stream().filter(demand::contains).count();
        return new ProjectDtos.RequirementDimension(name, (int) Math.min(96, 58 + matched * 12), description);
    }

    private List<String> recommendedProducts(String demand) {
        List<String> result = new ArrayList<>(List.of("SuperMap iServer", "SuperMap iPortal"));
        if (demand.contains("二三维") || demand.contains("三维")) result.add("SuperMap iClient3D for Cesium");
        if (demand.contains("数据") || demand.contains("治理")) result.add("SuperMap iDesktopX");
        return result;
    }

    private ProjectDtos.ProductMatch product(String id, String name, String family, String evidence,
                                              List<String> capabilities, int baseScore, boolean recommended) {
        List<String> matched = capabilities.stream().filter(capability -> evidence.contains(capability.substring(0, Math.min(2, capability.length())))).toList();
        List<String> resolved = matched.isEmpty() ? capabilities.subList(0, Math.min(2, capabilities.size())) : matched;
        int score = Math.min(98, baseScore + Math.min(4, resolved.size()));
        List<String> gaps = recommended ? List.of("授权与部署规模需结合并发量进一步确认") : List.of("仅在存在深度二次开发需求时选配");
        return new ProjectDtos.ProductMatch(id, name, family, score, resolved, gaps, recommended);
    }

    private void addIfContains(LinkedHashSet<String> target, String source, String keyword, String value) {
        if (source.contains(keyword)) target.add(value);
    }

    private Map<String, Object> owner(String username) {
        List<Map<String, Object>> result = jdbc.queryForList("select id, display_name from platform_users where lower(username)=lower(?) or lower(email)=lower(?)", username, username);
        if (result.isEmpty()) throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "当前用户不存在");
        return result.get(0);
    }

    private List<String> strings(List<String> values) { return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList(); }
    private List<String> strings(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, STRING_LIST); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("项目 JSON 字段格式错误", exception); }
    }
    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("无法序列化项目数据", exception); }
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private UUID uuid(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw notFound(); }
    }
    private BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "项目不存在"); }
}
