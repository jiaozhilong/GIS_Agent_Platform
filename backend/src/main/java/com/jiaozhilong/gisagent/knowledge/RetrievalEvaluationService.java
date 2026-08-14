package com.jiaozhilong.gisagent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.project.ProjectDtos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RetrievalEvaluationService {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final KnowledgeRetrievalService retrieval;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher publisher;

    public RetrievalEvaluationService(JdbcTemplate jdbc, KnowledgeRetrievalService retrieval,
                                      ObjectMapper mapper, ApplicationEventPublisher publisher) {
        this.jdbc = jdbc; this.retrieval = retrieval; this.mapper = mapper; this.publisher = publisher;
    }

    public List<RetrievalEvaluationDtos.Case> cases() {
        return jdbc.query("select * from retrieval_evaluation_cases order by created_at,id", this::caseRow);
    }

    public List<RetrievalEvaluationDtos.Run> runs() {
        return jdbc.query("select * from retrieval_evaluation_runs order by created_at desc limit 50", this::runRow)
                .stream().map(item -> withResults(item, false)).toList();
    }

    public RetrievalEvaluationDtos.Run run(UUID id) {
        List<RetrievalEvaluationDtos.Run> found = jdbc.query("select * from retrieval_evaluation_runs where id=?", this::runRow, id);
        if (found.isEmpty()) throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "检索评测任务不存在");
        return withResults(found.get(0), true);
    }

    @Transactional
    public RetrievalEvaluationDtos.Run start() {
        int total = jdbc.queryForObject("select count(*) from retrieval_evaluation_cases where active", Integer.class);
        if (total == 0) throw new BusinessException(HttpStatus.CONFLICT, "NO_EVALUATION_CASES", "没有可执行的检索评测题");
        UUID id = UUID.randomUUID();
        jdbc.update("insert into retrieval_evaluation_runs(id,status,total_cases,metrics) values (?,'PENDING',?,'{}'::jsonb)", id, total);
        publisher.publishEvent(new RetrievalEvaluationRequested(id));
        return run(id);
    }

    @Async("knowledgeAssetExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void execute(RetrievalEvaluationRequested event) {
        UUID runId = event.runId();
        jdbc.update("update retrieval_evaluation_runs set status='RUNNING',started_at=current_timestamp,updated_at=current_timestamp where id=?", runId);
        List<RetrievalEvaluationDtos.Case> cases = cases().stream().filter(RetrievalEvaluationDtos.Case::active).toList();
        int completed = 0;
        for (RetrievalEvaluationDtos.Case item : cases) {
            long started = System.nanoTime();
            try {
                Map<String, Object> filters = item.expectedKnowledgeType() == null ? Map.of() : Map.of("knowledge_type", List.of(item.expectedKnowledgeType()));
                ProjectDtos.RetrievalResult result = retrieval.search(new ProjectDtos.RetrievalRequest(item.query(), List.of(), 10, .2, filters));
                List<ProjectDtos.RetrievalHit> hits = result.hits();
                Integer datasetRank = rank(hits, hit -> contains(hit.knowledgeBaseName(), item.expectedDatasetKeyword()));
                Integer documentRank = item.expectedDocumentKeyword() == null || item.expectedDocumentKeyword().isBlank()
                        ? null : rank(hits, hit -> contains(hit.documentName(), item.expectedDocumentKeyword()));
                Integer slideRank = item.expectedSlide() == null ? null : rank(hits,
                        hit -> hit.assetContext() != null && item.expectedSlide().equals(hit.assetContext().pptPage()));
                jdbc.update("""
                        insert into retrieval_evaluation_results(run_id,case_id,duration_ms,dataset_hit_rank,document_hit_rank,slide_hit_rank,top_hits)
                        values (?,?,?,?,?,?,?::jsonb)
                        """, runId, item.id(), result.durationMs(), datasetRank, documentRank, slideRank, json(hits));
            } catch (Exception exception) {
                jdbc.update("insert into retrieval_evaluation_results(run_id,case_id,duration_ms,error_message) values (?,?,?,?)",
                        runId, item.id(), (System.nanoTime() - started) / 1_000_000, truncate(exception.getMessage(), 2000));
            }
            completed++;
            jdbc.update("update retrieval_evaluation_runs set completed_cases=?,updated_at=current_timestamp where id=?", completed, runId);
        }
        Map<String, Object> metrics = metrics(runId);
        jdbc.update("update retrieval_evaluation_runs set status='SUCCEEDED',metrics=?::jsonb,finished_at=current_timestamp,updated_at=current_timestamp where id=?",
                json(metrics), runId);
    }

    private Map<String, Object> metrics(UUID runId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select r.dataset_hit_rank,r.document_hit_rank,r.slide_hit_rank,r.error_message,
                       c.expected_document_keyword,c.expected_slide
                from retrieval_evaluation_results r
                join retrieval_evaluation_cases c on c.id=r.case_id where r.run_id=?
                """, runId);
        int total = rows.size(), success = 0, docExpected = 0, docHits = 0, slideExpected = 0, slideHits = 0;
        double reciprocal = 0; int recall5 = 0, recall10 = 0;
        for (Map<String, Object> row : rows) {
            if (row.get("error_message") == null) success++;
            Integer rank = number(row.get("document_hit_rank"));
            if (rank == null) rank = number(row.get("dataset_hit_rank"));
            if (rank != null) { reciprocal += 1d / rank; if (rank <= 5) recall5++; if (rank <= 10) recall10++; }
            if (row.get("expected_document_keyword") != null && !String.valueOf(row.get("expected_document_keyword")).isBlank()) {
                docExpected++; if (row.get("document_hit_rank") != null) docHits++;
            }
            if (row.get("expected_slide") != null) { slideExpected++; if (row.get("slide_hit_rank") != null) slideHits++; }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Recall@5", ratio(recall5, total)); result.put("Recall@10", ratio(recall10, total));
        result.put("MRR", total == 0 ? 0 : round(reciprocal / total)); result.put("successfulQueryRate", ratio(success, total));
        result.put("expectedDocumentHitRate", docExpected == 0 ? null : ratio(docHits, docExpected));
        result.put("expectedPptSlideHitRate", slideExpected == 0 ? null : ratio(slideHits, slideExpected));
        result.put("documentGoldSamples", docExpected); result.put("pptSlideGoldSamples", slideExpected);
        result.put("note", "未标注文档/页码的题目以期望知识库命中作为相关性判定；精确 Precision 需业务人员完成 gold label 后计算");
        return result;
    }

    private RetrievalEvaluationDtos.Run withResults(RetrievalEvaluationDtos.Run run, boolean include) {
        if (!include) return run;
        List<RetrievalEvaluationDtos.Result> results = jdbc.query("""
                select r.*,c.query from retrieval_evaluation_results r join retrieval_evaluation_cases c on c.id=r.case_id
                where r.run_id=? order by r.created_at
                """, (rs,row) -> new RetrievalEvaluationDtos.Result((UUID) rs.getObject("case_id"), rs.getString("query"),
                rs.getLong("duration_ms"), integer(rs,"dataset_hit_rank"), integer(rs,"document_hit_rank"),
                integer(rs,"slide_hit_rank"), rs.getString("error_message")), run.id());
        return new RetrievalEvaluationDtos.Run(run.id(), run.status(), run.totalCases(), run.completedCases(), run.metrics(),
                run.errorMessage(), run.startedAt(), run.finishedAt(), run.createdAt(), results);
    }

    private RetrievalEvaluationDtos.Case caseRow(ResultSet rs, int row) throws SQLException {
        return new RetrievalEvaluationDtos.Case((UUID) rs.getObject("id"), rs.getString("query"), rs.getString("expected_dataset_keyword"),
                rs.getString("expected_document_keyword"), rs.getString("expected_section"), integer(rs,"expected_page"),
                integer(rs,"expected_slide"), rs.getString("expected_knowledge_type"), rs.getBoolean("active"));
    }
    private RetrievalEvaluationDtos.Run runRow(ResultSet rs, int row) throws SQLException {
        return new RetrievalEvaluationDtos.Run((UUID) rs.getObject("id"), rs.getString("status"), rs.getInt("total_cases"),
                rs.getInt("completed_cases"), map(rs.getString("metrics")), rs.getString("error_message"),
                rs.getObject("started_at", OffsetDateTime.class), rs.getObject("finished_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class), List.of());
    }
    private Integer rank(List<ProjectDtos.RetrievalHit> hits, java.util.function.Predicate<ProjectDtos.RetrievalHit> predicate) {
        for (int i=0;i<hits.size();i++) if (predicate.test(hits.get(i))) return i+1; return null;
    }
    private boolean contains(String actual, String expected) { return expected == null || expected.isBlank() || (actual != null && actual.toLowerCase().contains(expected.toLowerCase())); }
    private Integer integer(ResultSet rs,String name) throws SQLException { return rs.getObject(name)==null?null:rs.getInt(name); }
    private Integer number(Object value) { return value instanceof Number number ? number.intValue() : null; }
    private double ratio(int a,int b) { return b==0?0:round((double)a/b); }
    private double round(double v) { return Math.round(v*10000d)/10000d; }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch(Exception e){ return "{}"; } }
    private Map<String,Object> map(String value) { try { return mapper.readValue(value,MAP); } catch(Exception e){ return new LinkedHashMap<>(); } }
    private String truncate(String value,int max){ if(value==null)return null; return value.length()<=max?value:value.substring(0,max); }
}
