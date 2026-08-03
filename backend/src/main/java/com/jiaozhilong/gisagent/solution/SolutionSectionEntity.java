package com.jiaozhilong.gisagent.solution;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true) private Set<SolutionCitationEntity> citations = new LinkedHashSet<>();
    @PrePersist void create() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public void setRun(SolutionRunEntity run) { this.run = run; }
    public void setSectionKey(String value) { sectionKey = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getContent() { return content; } public void setContent(String value) { content = value; }
    public SolutionEnums.SourceType getSourceType() { return sourceType; } public void setSourceType(SolutionEnums.SourceType value) { sourceType = value; }
    public BigDecimal getEvidenceCoverage() { return evidenceCoverage; } public void setEvidenceCoverage(BigDecimal value) { evidenceCoverage = value; }
    public String getConfirmationReason() { return confirmationReason; } public void setConfirmationReason(String value) { confirmationReason = value; }
    public void setSortOrder(int value) { sortOrder = value; }
    public Set<SolutionCitationEntity> getCitations() { return citations; }
}
