package com.jiaozhilong.gisagent.solution;

import com.jiaozhilong.gisagent.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "solution_generation_runs")
public class SolutionRunEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false, length = 80) private String projectId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requested_by") private UserEntity requestedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SolutionEnums.TaskStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private SolutionEnums.Stage stage;
    @Enumerated(EnumType.STRING) @Column(name = "grounding_policy", nullable = false, length = 20) private SolutionEnums.GroundingPolicy groundingPolicy;
    @Column(name = "allow_model_supplement", nullable = false) private boolean allowModelSupplement;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "knowledge_base_ids", nullable = false, columnDefinition = "jsonb") private String knowledgeBaseIds;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "output_formats", nullable = false, columnDefinition = "jsonb") private String outputFormats;
    @Column(name = "ragflow_assistant_id", length = 100) private String ragflowAssistantId;
    @Column(name = "ragflow_session_id", length = 100) private String ragflowSessionId;
    @Column(name = "model_name", length = 160) private String modelName;
    @Column(name = "evidence_coverage", nullable = false, precision = 5, scale = 4) private BigDecimal evidenceCoverage = BigDecimal.ZERO;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("sortOrder asc")
    private List<SolutionSectionEntity> sections = new ArrayList<>();

    @PrePersist void create() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public String getProjectId() { return projectId; } public void setProjectId(String projectId) { this.projectId = projectId; }
    public UserEntity getRequestedBy() { return requestedBy; } public void setRequestedBy(UserEntity requestedBy) { this.requestedBy = requestedBy; }
    public SolutionEnums.TaskStatus getStatus() { return status; } public void setStatus(SolutionEnums.TaskStatus status) { this.status = status; }
    public SolutionEnums.Stage getStage() { return stage; } public void setStage(SolutionEnums.Stage stage) { this.stage = stage; }
    public SolutionEnums.GroundingPolicy getGroundingPolicy() { return groundingPolicy; } public void setGroundingPolicy(SolutionEnums.GroundingPolicy value) { this.groundingPolicy = value; }
    public boolean isAllowModelSupplement() { return allowModelSupplement; } public void setAllowModelSupplement(boolean value) { this.allowModelSupplement = value; }
    public String getKnowledgeBaseIds() { return knowledgeBaseIds; } public void setKnowledgeBaseIds(String value) { this.knowledgeBaseIds = value; }
    public String getOutputFormats() { return outputFormats; } public void setOutputFormats(String value) { this.outputFormats = value; }
    public String getRagflowAssistantId() { return ragflowAssistantId; } public void setRagflowAssistantId(String value) { this.ragflowAssistantId = value; }
    public String getRagflowSessionId() { return ragflowSessionId; } public void setRagflowSessionId(String value) { this.ragflowSessionId = value; }
    public String getModelName() { return modelName; } public void setModelName(String value) { this.modelName = value; }
    public BigDecimal getEvidenceCoverage() { return evidenceCoverage; } public void setEvidenceCoverage(BigDecimal value) { this.evidenceCoverage = value; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String value) { this.errorMessage = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; } public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<SolutionSectionEntity> getSections() { return sections; }
}
