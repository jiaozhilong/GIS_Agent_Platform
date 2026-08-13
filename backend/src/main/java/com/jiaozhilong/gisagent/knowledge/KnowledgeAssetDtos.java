package com.jiaozhilong.gisagent.knowledge;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KnowledgeAssetDtos {
    private KnowledgeAssetDtos() {}

    public enum AssetType { DOCUMENT, IMAGE, VIDEO }
    public enum AssetStatus { PROCESSING, READY, ERROR }
    public enum MediaKind { IMAGE, VIDEO }
    public enum KnowledgeType { PRODUCT, SOLUTION, CASE, TROUBLESHOOTING, TEMPLATE }
    public enum DocumentType {
        PRODUCT_MANUAL, TECHNICAL_MANUAL, PRODUCT_PRESENTATION, INDUSTRY_SOLUTION,
        PROJECT_SOLUTION, PROJECT_CASE, FAQ, TEMPLATE, OTHER
    }
    public enum ParserStrategy { GENERAL, MANUAL, PRESENTATION, TABLE, QA, PICTURE }
    public enum ParseStage {
        UPLOAD, VALIDATE, STORE_ORIGINAL, MEDIA_EXTRACT, RAGFLOW_UPLOAD,
        PARSER_CONFIG, RAGFLOW_PARSE, INDEXING, METADATA_SYNC, COMPLETED
    }
    public enum ParseTaskStatus { PENDING, RUNNING, SUCCEEDED, FAILED }

    public record Summary(int total, int documents, int images, int videos, int pptPages, int ready) {}
    public record Asset(
            UUID id, String ragflowDatasetId, String ragflowDocumentId, AssetType assetType,
            String documentFormat, String title, String originalFilename, String mediaType,
            String description, String industry, List<String> tags, AssetStatus status,
            String statusMessage, long fileSize, int pageCount, int extractedImageCount,
            int extractedVideoCount, String createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            KnowledgeType knowledgeType, DocumentType documentType, String gisDomain, String product,
            String productVersion, String projectType, String region, Integer year,
            ParserStrategy parserStrategy, String parserReason, Map<String, Object> parserConfig,
            boolean parserOverride, String parseStatus, String ragflowStatus, String mediaStatus,
            int chunkCount, long tokenCount, String errorCode, String errorMessage) {}
    public record Page(UUID id, int pageNumber, String title, String textContent, int imageCount,
                       int videoCount, String previewUrl) {}
    public record Media(UUID id, Integer pageNumber, MediaKind mediaKind, String filename, String mediaType,
                        long fileSize, String sourceKind, String externalUrl, String contentUrl) {}
    public record ParseTask(UUID id, ParseStage stage, ParseTaskStatus status, int progress, int attempt,
                            ParserStrategy parserStrategy, String errorCode, String errorMessage,
                            OffsetDateTime startedAt, OffsetDateTime finishedAt, OffsetDateTime updatedAt) {}
    public record Detail(Asset asset, List<Page> pages, List<Media> media, ParseTask parseTask) {}
    public record AssetContext(UUID assetId, String assetTitle, AssetType assetType, Integer pptPage,
                               String pageTitle, List<Media> relatedImages, List<Media> relatedVideos) {}
    public record StoredFile(Path path, String filename, String mediaType) {}
    public record ReparseRequest(ParserStrategy parserStrategy, Map<String, Object> parserConfig) {}
}
