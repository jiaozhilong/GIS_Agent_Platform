package com.jiaozhilong.gisagent.solution;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "solution_citations")
public class SolutionCitationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "section_id", nullable = false) private SolutionSectionEntity section;
    @Column(name = "ragflow_chunk_id", length = 160) private String ragflowChunkId;
    @Column(name = "dataset_id", length = 160) private String datasetId;
    @Column(name = "document_id", length = 160) private String documentId;
    @Column(name = "document_name", length = 500) private String documentName;
    @Column(name = "content_snapshot", columnDefinition = "text") private String contentSnapshot;
    @Column(name = "similarity_score", precision = 8, scale = 6) private BigDecimal similarityScore;
    @Column(name = "page_number") private Integer pageNumber;
    @Column(name = "asset_id") private UUID assetId;
    @Column(name = "slide_number") private Integer slideNumber;
    @Column(name = "evidence_id", length = 100) private String evidenceId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb") private String metadataJson = "{}";
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @PrePersist void create() { createdAt = OffsetDateTime.now(); }
    public UUID getId() { return id; }
    public String getRagflowChunkId() { return ragflowChunkId; }
    public String getDocumentName() { return documentName; }
    public String getDatasetId() { return datasetId; }
    public String getDocumentId() { return documentId; }
    public String getContentSnapshot() { return contentSnapshot; }
    public BigDecimal getSimilarityScore() { return similarityScore; }
    public Integer getPageNumber() { return pageNumber; }
    public UUID getAssetId() { return assetId; }
    public Integer getSlideNumber() { return slideNumber; }
    public String getEvidenceId() { return evidenceId; }
    public void setSection(SolutionSectionEntity value) { section = value; }
    public void setRagflowChunkId(String value) { ragflowChunkId = value; }
    public void setDatasetId(String value) { datasetId = value; }
    public void setDocumentId(String value) { documentId = value; }
    public void setDocumentName(String value) { documentName = value; }
    public void setContentSnapshot(String value) { contentSnapshot = value; }
    public void setSimilarityScore(BigDecimal value) { similarityScore = value; }
    public void setPageNumber(Integer value) { pageNumber = value; }
    public void setMetadataJson(String value) { metadataJson = value; }
    public void setAssetId(UUID value) { assetId = value; }
    public void setSlideNumber(Integer value) { slideNumber = value; }
    public void setEvidenceId(String value) { evidenceId = value; }
}
