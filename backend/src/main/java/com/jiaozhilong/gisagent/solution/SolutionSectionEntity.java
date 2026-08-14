package com.jiaozhilong.gisagent.solution;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solution_sections")
public class SolutionSectionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "run_id", nullable = false) private SolutionRunEntity run;
    @Column(name = "section_key", nullable = false, length = 80) private String sectionKey;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 30) private SolutionEnums.SourceType sourceType;
    @Column(name = "evidence_coverage", nullable = false, precision = 5, scale = 4) private BigDecimal evidenceCoverage;
    @Column(name = "confirmation_reason", length = 1000) private String confirmationReason;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, columnDefinition = "text") private String purpose = "";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "required_knowledge_types", nullable = false, columnDefinition = "jsonb") private String requiredKnowledgeTypes = "[]";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "preferred_datasets", nullable = false, columnDefinition = "jsonb") private String preferredDatasets = "[]";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "retrieval_queries", nullable = false, columnDefinition = "jsonb") private String retrievalQueries = "[]";
    @Column(name = "generation_requirements", nullable = false, columnDefinition = "text") private String generationRequirements = "";
    @Column(name = "section_status", nullable = false, length = 30) private String sectionStatus = "PLANNED";
    @Column(nullable = false) private boolean locked;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true) private Set<SolutionCitationEntity> citations = new LinkedHashSet<>();
    @PrePersist void create() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public SolutionRunEntity getRun() { return run; }
    public String getSectionKey() { return sectionKey; }
    public void setRun(SolutionRunEntity run) { this.run = run; }
    public void setSectionKey(String value) { sectionKey = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getContent() { return content; } public void setContent(String value) { content = value; }
    public SolutionEnums.SourceType getSourceType() { return sourceType; } public void setSourceType(SolutionEnums.SourceType value) { sourceType = value; }
    public BigDecimal getEvidenceCoverage() { return evidenceCoverage; } public void setEvidenceCoverage(BigDecimal value) { evidenceCoverage = value; }
    public String getConfirmationReason() { return confirmationReason; } public void setConfirmationReason(String value) { confirmationReason = value; }
    public void setSortOrder(int value) { sortOrder = value; }
    public int getSortOrder() { return sortOrder; }
    public String getPurpose() { return purpose; } public void setPurpose(String value) { purpose = value; }
    public String getRequiredKnowledgeTypes() { return requiredKnowledgeTypes; } public void setRequiredKnowledgeTypes(String value) { requiredKnowledgeTypes = value; }
    public String getPreferredDatasets() { return preferredDatasets; } public void setPreferredDatasets(String value) { preferredDatasets = value; }
    public String getRetrievalQueries() { return retrievalQueries; } public void setRetrievalQueries(String value) { retrievalQueries = value; }
    public String getGenerationRequirements() { return generationRequirements; } public void setGenerationRequirements(String value) { generationRequirements = value; }
    public String getSectionStatus() { return sectionStatus; } public void setSectionStatus(String value) { sectionStatus = value; }
    public boolean isLocked() { return locked; } public void setLocked(boolean value) { locked = value; }
    public Set<SolutionCitationEntity> getCitations() { return citations; }
}
