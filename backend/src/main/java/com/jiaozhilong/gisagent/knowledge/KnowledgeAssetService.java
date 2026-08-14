package com.jiaozhilong.gisagent.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.integration.ragflow.RagflowManagementClient;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KnowledgeAssetService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeAssetService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final Pattern PPT_PAGE = Pattern.compile("PPT_PAGE:(\\d+)");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RagflowManagementClient ragflow;
    private final PptxAssetExtractor pptxExtractor;
    private final FileValidator validator;
    private final DocumentParserRouter parserRouter;
    private final AssetObjectStorage objectStorage;
    private final Executor executor;
    private final Path storageRoot;
    private final long maxFileSize;

    public KnowledgeAssetService(JdbcTemplate jdbc, ObjectMapper objectMapper, RagflowManagementClient ragflow,
                                 PptxAssetExtractor pptxExtractor, FileValidator validator,
                                 DocumentParserRouter parserRouter, AssetObjectStorage objectStorage,
                                 @Qualifier("knowledgeAssetExecutor") Executor executor,
                                 @Value("${platform.assets.storage-path}") String storagePath,
                                 @Value("${platform.assets.max-file-size-mb:600}") long maxFileSizeMb) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.ragflow = ragflow;
        this.pptxExtractor = pptxExtractor;
        this.validator = validator;
        this.parserRouter = parserRouter;
        this.objectStorage = objectStorage;
        this.executor = executor;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSizeMb * 1024 * 1024;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedTasks() {
        jdbc.update("update knowledge_asset_parse_tasks set status='PENDING', stage='VALIDATE', progress=5, error_code=null, error_message=null where status='RUNNING'");
        jdbc.queryForList("select distinct on (asset_id) asset_id from knowledge_asset_parse_tasks where status='PENDING' order by asset_id, created_at desc", UUID.class)
                .forEach(this::schedule);
    }

    public KnowledgeAssetDtos.Summary summary() {
        return jdbc.queryForObject("""
                select count(*) total,
                  count(*) filter (where asset_type='DOCUMENT') documents,
                  count(*) filter (where asset_type='IMAGE') images,
                  count(*) filter (where asset_type='VIDEO') videos,
                  coalesce(sum(page_count),0) ppt_pages,
                  count(*) filter (where status='READY') ready
                from knowledge_assets
                """, (rs, row) -> new KnowledgeAssetDtos.Summary(rs.getInt("total"), rs.getInt("documents"),
                rs.getInt("images"), rs.getInt("videos"), rs.getInt("ppt_pages"), rs.getInt("ready")));
    }

    public List<KnowledgeAssetDtos.Asset> list(String keyword, KnowledgeAssetDtos.AssetType type,
                                                KnowledgeAssetDtos.AssetStatus status) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return jdbc.query("select * from knowledge_assets order by created_at desc", this::asset).stream()
                .filter(item -> type == null || item.assetType() == type)
                .filter(item -> status == null || item.status() == status)
                .filter(item -> normalized.isBlank() || (item.title() + item.originalFilename() + item.description()
                        + item.industry() + item.product() + String.join(" ", item.tags())).toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public KnowledgeAssetDtos.Detail detail(String id) {
        UUID assetId = uuid(id);
        List<KnowledgeAssetDtos.Asset> assets = jdbc.query("select * from knowledge_assets where id=?", this::asset, assetId);
        if (assets.isEmpty()) throw notFound();
        List<KnowledgeAssetDtos.Page> pages = jdbc.query("select * from knowledge_asset_pages where asset_id=? order by page_number", this::page, assetId);
        List<KnowledgeAssetDtos.Media> media = jdbc.query("select * from knowledge_asset_media where asset_id=? order by page_number nulls first, created_at", this::mediaRow, assetId);
        return new KnowledgeAssetDtos.Detail(assets.get(0), pages, media, latestTask(assetId));
    }

    public KnowledgeAssetDtos.Detail upload(String username, String datasetId, String title, String description,
                                             String knowledgeType, String documentType, String industry, String gisDomain,
                                             String product, String productVersion, String projectType, String region,
                                             Integer year, String tags, MultipartFile file) {
        if (datasetId == null || datasetId.isBlank())
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DATASET_REQUIRED", "请选择目标 RAGFlow 知识库");
        FileValidator.UploadInfo info = validator.validateUpload(file, maxFileSize);
        KnowledgeAssetDtos.KnowledgeType resolvedKnowledge = enumValue(KnowledgeAssetDtos.KnowledgeType.class, knowledgeType, inferKnowledge(datasetId));
        KnowledgeAssetDtos.DocumentType resolvedDocument = enumValue(KnowledgeAssetDtos.DocumentType.class, documentType, KnowledgeAssetDtos.DocumentType.OTHER);
        UUID id = UUID.randomUUID();
        Path directory = assetDirectory(id);
        Path original = directory.resolve("original-" + sanitize(info.filename())).normalize();
        try {
            Files.createDirectories(directory);
            file.transferTo(original);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ASSET_STORE_FAILED", "资产原文件保存失败");
        }
        String assetTitle = title == null || title.isBlank() ? baseName(info.filename()) : title.trim();
        jdbc.update("""
                insert into knowledge_assets(id, ragflow_dataset_id, asset_type, document_format, title, original_filename,
                  media_type, mime_type, description, industry, tags, status, status_message, storage_path, file_size, created_by,
                  knowledge_type, document_type, gis_domain, product, product_version, project_type, region, document_year,
                  parse_status, ragflow_status, media_status)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'PROCESSING', '原文件已保存，等待后台校验', ?, ?, ?,
                  ?, ?, ?, ?, ?, ?, ?, ?, 'UPLOADED', 'PENDING', 'PENDING')
                """, id, datasetId, info.assetType().name(), info.assetType() == KnowledgeAssetDtos.AssetType.DOCUMENT ? documentFormat(info.extension()) : null,
                assetTitle, info.filename(), info.mimeType(), info.mimeType(), clean(description), clean(industry), json(parseTags(tags)),
                original.toString(), file.getSize(), username, resolvedKnowledge.name(), resolvedDocument.name(), clean(gisDomain),
                clean(product), clean(productVersion), clean(projectType), clean(region), year);
        UUID taskId = UUID.randomUUID();
        jdbc.update("""
                insert into knowledge_asset_parse_tasks(id, asset_id, stage, status, progress, attempt)
                values (?, ?, 'UPLOAD', 'PENDING', 5, 0)
                """, taskId, id);
        schedule(id);
        return detail(id.toString());
    }

    public KnowledgeAssetDtos.Detail retrySync(String id) {
        return reparse(id, new KnowledgeAssetDtos.ReparseRequest(null, Map.of()));
    }

    public KnowledgeAssetDtos.Detail reparse(String id, KnowledgeAssetDtos.ReparseRequest request) {
        UUID assetId = uuid(id);
        KnowledgeAssetDtos.Asset current = detail(id).asset();
        if (current.status() == KnowledgeAssetDtos.AssetStatus.PROCESSING) throw new BusinessException(HttpStatus.CONFLICT, "ASSET_BUSY", "该资产正在处理中");
        Map<String, Object> config = request == null || request.parserConfig() == null ? Map.of() : request.parserConfig();
        jdbc.update("""
                update knowledge_assets set status='PROCESSING', parse_status='UPLOADED', status_message='等待重新解析',
                  parser_override=?, parser_strategy=coalesce(?, parser_strategy), parser_config=?::jsonb,
                  error_code=null, error_message=null, updated_at=current_timestamp where id=?
                """, request != null && request.parserStrategy() != null,
                request == null || request.parserStrategy() == null ? null : request.parserStrategy().name(), json(config), assetId);
        jdbc.update("""
                insert into knowledge_asset_parse_tasks(asset_id, stage, status, progress, attempt, parser_strategy)
                values (?, 'VALIDATE', 'PENDING', 5,
                  coalesce((select max(attempt) from knowledge_asset_parse_tasks where asset_id=?),0)+1, ?)
                """, assetId, assetId, request == null || request.parserStrategy() == null ? null : request.parserStrategy().name());
        schedule(assetId);
        return detail(id);
    }

    public KnowledgeAssetDtos.ParseTask parseStatus(String id) { return latestTask(uuid(id)); }

    public void delete(String id) {
        UUID assetId = uuid(id);
        KnowledgeAssetDtos.Asset asset = detail(id).asset();
        if (asset.status() == KnowledgeAssetDtos.AssetStatus.PROCESSING)
            throw new BusinessException(HttpStatus.CONFLICT, "ASSET_BUSY", "该资产正在处理中，暂不能删除");
        if (asset.ragflowDocumentId() != null && !asset.ragflowDocumentId().isBlank())
            ragflow.deleteDocument(asset.ragflowDatasetId(), asset.ragflowDocumentId());
        String originalKey = jdbc.queryForObject("select original_object_key from knowledge_assets where id=?", String.class, assetId);
        List<String> mediaKeys = jdbc.queryForList("select object_key from knowledge_asset_media where asset_id=? and object_key is not null", String.class, assetId);
        List<String> previewKeys = jdbc.queryForList("select preview_object_key from knowledge_asset_pages where asset_id=? and preview_object_key is not null", String.class, assetId);
        objectStorage.deleteOriginal(originalKey);
        mediaKeys.forEach(objectStorage::deleteMedia);
        previewKeys.forEach(objectStorage::deletePreview);
        Path directory = assetDirectory(assetId);
        jdbc.update("delete from knowledge_assets where id=?", assetId);
        deleteDirectory(directory);
    }

    private void schedule(UUID assetId) {
        try { executor.execute(() -> process(assetId)); }
        catch (Exception exception) { fail(assetId, "TASK_QUEUE_FULL", "知识资产处理队列已满，请稍后重试"); }
    }

    private void process(UUID assetId) {
        KnowledgeAssetDtos.Detail detail;
        try {
            UUID taskId = latestTask(assetId).id();
            stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.VALIDATE, 10, "正在校验文件格式和内容");
            detail = detail(assetId.toString());
            KnowledgeAssetDtos.Asset asset = detail.asset();
            Path original = assetStorage(assetId);
            String ext = extension(asset.originalFilename());
            FileValidator.UploadInfo info = new FileValidator.UploadInfo(asset.originalFilename(), ext, asset.assetType(), asset.mediaType());
            FileValidator.ValidationResult validated = validator.validateStored(original, info);
            String originalKey = objectStorage.original(assetId, asset.originalFilename(), validated.mimeType(), original);
            jdbc.update("update knowledge_assets set sha256=?, mime_type=?, original_object_key=?, parse_status='VALIDATED' where id=?",
                    validated.sha256(), validated.mimeType(), originalKey, assetId);

            KnowledgeAssetDtos.ParserStrategy override = asset.parserOverride() ? asset.parserStrategy() : null;
            DocumentParserRouter.ParserDecision decision = parserRouter.route(new DocumentParserRouter.ParserRouteContext(
                    ext, validated.mimeType(), asset.assetType(), asset.knowledgeType(), asset.documentType(),
                    asset.industry(), asset.product(), asset.fileSize()), override, asset.parserConfig());
            stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.STORE_ORIGINAL, 18, "文件校验完成，已选择 " + decision.strategy() + " 解析器");
            jdbc.update("""
                    update knowledge_assets set parser_strategy=?, parser_reason=?, parser_config=?::jsonb,
                      parse_status='PARSER_ROUTED', status_message=? where id=?
                    """, decision.strategy().name(), decision.decisionReason(), json(decision.parserConfig()),
                    "自动选择 " + decision.strategy() + "：" + decision.decisionReason(), assetId);

            if (decision.pptAssetProcessingRequired()) {
                stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.MEDIA_EXTRACT, 26, "正在提取 PPT 页面、图片和视频");
                clearMedia(assetId);
                parsePptx(assetId, original, assetDirectory(assetId));
            } else if (asset.assetType() != KnowledgeAssetDtos.AssetType.DOCUMENT) {
                addUploadedMedia(assetId, asset.assetType(), asset.originalFilename(), asset.mediaType(), original, asset.fileSize());
                jdbc.update("update knowledge_assets set media_status='COMPLETED' where id=?", assetId);
            } else jdbc.update("update knowledge_assets set media_status='NOT_REQUIRED' where id=?", assetId);

            stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.RAGFLOW_UPLOAD, 42, "正在上传 RAGFlow，尚未启动解析");
            detail = detail(assetId.toString());
            String documentId = detail.asset().ragflowDocumentId();
            if (documentId != null && !documentId.isBlank() && decision.strategy() == KnowledgeAssetDtos.ParserStrategy.PRESENTATION) {
                RagflowManagementClient.Document remote = ragflow.getDocument(detail.asset().ragflowDatasetId(), documentId);
                if (remote == null || !remote.name().toLowerCase(Locale.ROOT).endsWith(".pptx")) {
                    documentId = null;
                }
            }
            if (documentId == null || documentId.isBlank()) {
                RagflowManagementClient.UploadedDocument uploaded;
                if (detail.asset().assetType() == KnowledgeAssetDtos.AssetType.DOCUMENT) {
                    uploaded = ragflow.uploadDocument(detail.asset().ragflowDatasetId(), detail.asset().originalFilename(), original);
                } else {
                    uploaded = ragflow.uploadDocument(detail.asset().ragflowDatasetId(),
                            baseName(detail.asset().originalFilename()) + "-知识资产说明.docx", indexDocx(metadataIndex(detail)));
                }
                documentId = uploaded.id();
                jdbc.update("update knowledge_assets set ragflow_document_id=?, ragflow_status='UPLOADED' where id=?", documentId, assetId);
            } else ragflow.deleteAllChunks(detail.asset().ragflowDatasetId(), documentId);

            stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.PARSER_CONFIG, 55, "正在设置文档级解析器与业务 Metadata");
            detail = detail(assetId.toString());
            ragflow.updateDocument(detail.asset().ragflowDatasetId(), documentId, decision.ragflowChunkMethod(),
                    decision.parserConfig(), metadata(detail.asset()));
            jdbc.update("update knowledge_assets set ragflow_status='CONFIGURED' where id=?", assetId);

            stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.RAGFLOW_PARSE, 62, "正在启动 RAGFlow 解析任务");
            ragflow.parseDocument(detail.asset().ragflowDatasetId(), documentId);
            waitForRagflow(taskId, assetId, detail.asset().ragflowDatasetId(), documentId);
        } catch (Exception exception) {
            String code = exception instanceof BusinessException business ? business.getCode() : classify(exception);
            KnowledgeAssetDtos.ParseTask task = null;
            try { task = latestTask(assetId); } catch (Exception ignored) { /* best effort diagnostics */ }
            log.error("Knowledge asset ingestion failed assetId={} stage={} code={} error={}", assetId,
                    task == null ? "UNKNOWN" : task.stage(), code, exception.getMessage(), exception);
            fail(assetId, code, businessMessage(code, exception));
        }
    }

    private void waitForRagflow(UUID taskId, UUID assetId, String datasetId, String documentId) throws InterruptedException {
        int maxPolls = 900;
        for (int poll = 0; poll < maxPolls; poll++) {
            RagflowManagementClient.Document document = ragflow.getDocument(datasetId, documentId);
            if (document != null) {
                int progress = 65 + (int) Math.round(Math.max(0, Math.min(1, document.progress())) * 30);
                stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.INDEXING, progress,
                        "RAGFlow 正在切片、向量化与建立索引 " + Math.round(document.progress() * 100) + "%");
                if ("DONE".equalsIgnoreCase(document.run())) {
                    complete(taskId, assetId, document.chunkCount());
                    return;
                }
                if ("FAIL".equalsIgnoreCase(document.run())) {
                    String detail = document.progressMessage();
                    String message = detail == null || detail.isBlank()
                            ? "RAGFlow 文档解析失败"
                            : "RAGFlow 文档解析失败：" + detail.strip();
                    throw new BusinessException(HttpStatus.BAD_GATEWAY, "RAGFLOW_PARSE_FAILED", message);
                }
            }
            Thread.sleep(2000);
        }
        throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "RAGFLOW_PARSE_TIMEOUT", "RAGFlow 解析超时，可稍后在资产详情中重新解析");
    }

    private void complete(UUID taskId, UUID assetId, int chunkCount) {
        stage(taskId, assetId, KnowledgeAssetDtos.ParseStage.METADATA_SYNC, 98, "正在完成业务元数据关联");
        jdbc.update("""
                update knowledge_assets set status='READY', parse_status='READY', ragflow_status='READY', chunk_count=?,
                  status_message='知识资产已完成解析并可供 Agent 检索', error_code=null, error_message=null,
                  updated_at=current_timestamp where id=?
                """, chunkCount, assetId);
        jdbc.update("""
                update knowledge_asset_parse_tasks set stage='COMPLETED', status='SUCCEEDED', progress=100,
                  finished_at=current_timestamp, updated_at=current_timestamp where id=?
                """, taskId);
    }

    private void stage(UUID taskId, UUID assetId, KnowledgeAssetDtos.ParseStage stage, int progress, String message) {
        jdbc.update("""
                update knowledge_asset_parse_tasks set stage=?, status='RUNNING', progress=?,
                  started_at=coalesce(started_at,current_timestamp), updated_at=current_timestamp where id=?
                """, stage.name(), progress, taskId);
        jdbc.update("update knowledge_assets set parse_status=?, status_message=?, updated_at=current_timestamp where id=?",
                stage.name(), message, assetId);
    }

    private void fail(UUID assetId, String code, String message) {
        jdbc.update("""
                update knowledge_assets set status='ERROR', parse_status='FAILED', error_code=?, error_message=?,
                  status_message=?, updated_at=current_timestamp where id=?
                """, code, truncate(message, 2000), truncate(message, 1000), assetId);
        jdbc.update("""
                update knowledge_asset_parse_tasks set status='FAILED', error_code=?, error_message=?,
                  finished_at=current_timestamp, updated_at=current_timestamp
                where id=(select id from knowledge_asset_parse_tasks where asset_id=? order by created_at desc limit 1)
                """, code, truncate(message, 2000), assetId);
    }

    public KnowledgeAssetDtos.AssetContext contextFor(String ragflowDocumentId, String content, Integer fallbackPage) {
        if (ragflowDocumentId == null || ragflowDocumentId.isBlank()) return null;
        List<KnowledgeAssetDtos.Asset> assets = jdbc.query("select * from knowledge_assets where ragflow_document_id=?", this::asset, ragflowDocumentId);
        if (assets.isEmpty()) return null;
        KnowledgeAssetDtos.Asset asset = assets.get(0);
        Integer pageNumber = pptPage(content, fallbackPage, asset.pageCount());
        String pageTitle = pageNumber == null ? null : jdbc.query("select title from knowledge_asset_pages where asset_id=? and page_number=?",
                rs -> rs.next() ? rs.getString(1) : null, asset.id(), pageNumber);
        List<KnowledgeAssetDtos.Media> media = jdbc.query("""
                select * from knowledge_asset_media where asset_id=? and (page_number is null or page_number=?) order by created_at
                """, this::mediaRow, asset.id(), pageNumber == null ? -1 : pageNumber);
        return new KnowledgeAssetDtos.AssetContext(asset.id(), asset.title(), asset.assetType(), pageNumber, pageTitle,
                media.stream().filter(item -> item.mediaKind() == KnowledgeAssetDtos.MediaKind.IMAGE).toList(),
                media.stream().filter(item -> item.mediaKind() == KnowledgeAssetDtos.MediaKind.VIDEO).toList());
    }

    public KnowledgeAssetDtos.StoredFile original(String id) {
        KnowledgeAssetDtos.Asset asset = detail(id).asset();
        return checkedFile(assetStorage(asset.id()), asset.originalFilename(), asset.mediaType());
    }
    public KnowledgeAssetDtos.StoredFile media(String id) {
        UUID mediaId = uuid(id);
        return jdbc.query("select storage_path, filename, media_type from knowledge_asset_media where id=?", rs -> {
            if (!rs.next() || rs.getString("storage_path") == null) throw notFound();
            return checkedFile(Path.of(rs.getString("storage_path")), rs.getString("filename"), rs.getString("media_type"));
        }, mediaId);
    }
    public KnowledgeAssetDtos.StoredFile preview(String assetId, int pageNumber) {
        UUID id = uuid(assetId);
        return jdbc.query("select preview_path from knowledge_asset_pages where asset_id=? and page_number=?", rs -> {
            if (!rs.next() || rs.getString(1) == null) throw notFound();
            return checkedFile(Path.of(rs.getString(1)), "page-" + pageNumber + ".png", "image/png");
        }, id, pageNumber);
    }

    private void clearMedia(UUID assetId) {
        jdbc.update("delete from knowledge_asset_media where asset_id=?", assetId);
        jdbc.update("delete from knowledge_asset_pages where asset_id=?", assetId);
    }
    private void parsePptx(UUID assetId, Path original, Path directory) throws Exception {
        PptxAssetExtractor.Extraction extraction = pptxExtractor.extract(original, directory);
        for (PptxAssetExtractor.SlidePage page : extraction.pages()) {
            String previewKey = objectStorage.preview(assetId, page.pageNumber(), page.previewPath());
            jdbc.update("""
                insert into knowledge_asset_pages(asset_id, page_number, title, text_content, preview_path, preview_object_key, image_count, video_count)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, assetId, page.pageNumber(), page.title(), page.text(), page.previewPath() == null ? null : page.previewPath().toString(),
                    previewKey, page.imageCount(), page.videoCount());
        }
        for (PptxAssetExtractor.ExtractedMedia media : extraction.media()) {
            String objectKey = media.path() == null ? null : objectStorage.media(assetId, media.pageNumber(), media.filename(), media.mediaType(), media.path());
            jdbc.update("""
                insert into knowledge_asset_media(asset_id, page_number, media_kind, filename, media_type, storage_path,
                  object_key, external_url, file_size, source_kind) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, assetId, media.pageNumber(), media.kind(), media.filename(), media.mediaType(),
                    media.path() == null ? null : media.path().toString(), objectKey, media.externalUrl(), media.fileSize(), media.sourceKind());
        }
        int images = (int) extraction.media().stream().filter(item -> "IMAGE".equals(item.kind())).count();
        int videos = (int) extraction.media().stream().filter(item -> "VIDEO".equals(item.kind())).count();
        jdbc.update("""
                update knowledge_assets set page_count=?, extracted_image_count=?, extracted_video_count=?, media_status='COMPLETED' where id=?
                """, extraction.pages().size(), images, videos, assetId);
    }
    private void addUploadedMedia(UUID assetId, KnowledgeAssetDtos.AssetType type, String filename, String mediaType, Path original, long size) {
        String objectKey = objectStorage.media(assetId, 0, filename, mediaType, original);
        jdbc.update("""
                insert into knowledge_asset_media(asset_id, media_kind, filename, media_type, storage_path, object_key, file_size, source_kind)
                values (?, ?, ?, ?, ?, ?, ?, 'UPLOADED')
                """, assetId, type.name(), filename, mediaType, original.toString(), objectKey, size);
    }

    private Map<String, Object> metadata(KnowledgeAssetDtos.Asset asset) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("asset_id", asset.id().toString());
        metadata.put("knowledge_type", asset.knowledgeType().name());
        metadata.put("document_type", asset.documentType().name());
        put(metadata, "industry", asset.industry()); put(metadata, "gis_domain", asset.gisDomain());
        put(metadata, "product", asset.product()); put(metadata, "product_version", asset.productVersion());
        put(metadata, "project_type", asset.projectType()); put(metadata, "region", asset.region());
        if (asset.year() != null) metadata.put("year", asset.year());
        if (!asset.tags().isEmpty()) metadata.put("tags", asset.tags());
        return metadata;
    }
    private void put(Map<String, Object> map, String key, String value) { if (value != null && !value.isBlank()) map.put(key, value); }
    private String metadataIndex(KnowledgeAssetDtos.Detail detail) {
        KnowledgeAssetDtos.Asset asset = detail.asset();
        return "# GIS 知识资产：" + asset.title() + "\n\n知识类型：" + asset.knowledgeType() + "\n资料类型：" + asset.documentType()
                + "\n行业：" + asset.industry() + "\n产品：" + asset.product() + "\n标签：" + String.join("、", asset.tags())
                + "\n原始文件：" + asset.originalFilename() + "\n业务说明：" + asset.description() + "\n";
    }
    private byte[] indexDocx(String content) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String line : content.split("\\R", -1)) {
                XWPFParagraph paragraph = document.createParagraph();
                var run = paragraph.createRun(); run.setText(line.replaceFirst("^#{1,6}\\s*", ""));
                if (line.startsWith("#")) run.setBold(true);
            }
            document.write(output); return output.toByteArray();
        }
    }

    private KnowledgeAssetDtos.Asset asset(ResultSet rs, int row) throws SQLException {
        return new KnowledgeAssetDtos.Asset((UUID) rs.getObject("id"), rs.getString("ragflow_dataset_id"), rs.getString("ragflow_document_id"),
                KnowledgeAssetDtos.AssetType.valueOf(rs.getString("asset_type")), rs.getString("document_format"), rs.getString("title"),
                rs.getString("original_filename"), rs.getString("media_type"), rs.getString("description"), rs.getString("industry"),
                strings(rs.getString("tags")), KnowledgeAssetDtos.AssetStatus.valueOf(rs.getString("status")), rs.getString("status_message"),
                rs.getLong("file_size"), rs.getInt("page_count"), rs.getInt("extracted_image_count"), rs.getInt("extracted_video_count"),
                rs.getString("created_by"), rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class),
                KnowledgeAssetDtos.KnowledgeType.valueOf(rs.getString("knowledge_type")), KnowledgeAssetDtos.DocumentType.valueOf(rs.getString("document_type")),
                rs.getString("gis_domain"), rs.getString("product"), rs.getString("product_version"), rs.getString("project_type"), rs.getString("region"),
                rs.getObject("document_year") == null ? null : rs.getInt("document_year"), enumValue(KnowledgeAssetDtos.ParserStrategy.class, rs.getString("parser_strategy"), null),
                rs.getString("parser_reason"), objectMap(rs.getString("parser_config")), rs.getBoolean("parser_override"), rs.getString("parse_status"),
                rs.getString("ragflow_status"), rs.getString("media_status"), rs.getInt("chunk_count"), rs.getLong("token_count"),
                rs.getString("error_code"), rs.getString("error_message"));
    }
    private KnowledgeAssetDtos.Page page(ResultSet rs, int row) throws SQLException {
        UUID assetId = (UUID) rs.getObject("asset_id"); int number = rs.getInt("page_number");
        String preview = rs.getString("preview_path") == null ? null : "/knowledge-assets/" + assetId + "/pages/" + number + "/preview";
        return new KnowledgeAssetDtos.Page((UUID) rs.getObject("id"), number, rs.getString("title"), rs.getString("text_content"),
                rs.getInt("image_count"), rs.getInt("video_count"), preview);
    }
    private KnowledgeAssetDtos.Media mediaRow(ResultSet rs, int row) throws SQLException {
        UUID id = (UUID) rs.getObject("id"); Integer page = rs.getObject("page_number") == null ? null : rs.getInt("page_number");
        String storage = rs.getString("storage_path");
        return new KnowledgeAssetDtos.Media(id, page, KnowledgeAssetDtos.MediaKind.valueOf(rs.getString("media_kind")), rs.getString("filename"),
                rs.getString("media_type"), rs.getLong("file_size"), rs.getString("source_kind"), rs.getString("external_url"),
                storage == null ? null : "/knowledge-assets/media/" + id);
    }
    private KnowledgeAssetDtos.ParseTask latestTask(UUID assetId) {
        return jdbc.query("select * from knowledge_asset_parse_tasks where asset_id=? order by created_at desc limit 1", rs -> {
            if (!rs.next()) return null;
            return new KnowledgeAssetDtos.ParseTask((UUID) rs.getObject("id"), KnowledgeAssetDtos.ParseStage.valueOf(rs.getString("stage")),
                    KnowledgeAssetDtos.ParseTaskStatus.valueOf(rs.getString("status")), rs.getInt("progress"), rs.getInt("attempt"),
                    enumValue(KnowledgeAssetDtos.ParserStrategy.class, rs.getString("parser_strategy"), null), rs.getString("error_code"),
                    rs.getString("error_message"), rs.getObject("started_at", OffsetDateTime.class), rs.getObject("finished_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class));
        }, assetId);
    }

    private Integer pptPage(String content, Integer fallback, int pageCount) {
        if (content != null) { Matcher matcher = PPT_PAGE.matcher(content); if (matcher.find()) return Integer.parseInt(matcher.group(1)); }
        return fallback != null && fallback > 0 && fallback <= pageCount ? fallback : null;
    }
    private Path assetDirectory(UUID id) { Path path = storageRoot.resolve(id.toString()).normalize(); if (!path.startsWith(storageRoot)) throw new IllegalStateException("非法资产存储路径"); return path; }
    private void deleteDirectory(Path directory) {
        if (!directory.normalize().startsWith(storageRoot) || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (Exception exception) { throw new RuntimeException(exception); }
            });
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ASSET_DELETE_FAILED", "知识资产记录已删除，但本地文件清理失败");
        }
    }
    private Path assetStorage(UUID id) { return jdbc.query("select storage_path from knowledge_assets where id=?", rs -> { if (!rs.next()) throw notFound(); return Path.of(rs.getString(1)); }, id); }
    private KnowledgeAssetDtos.StoredFile checkedFile(Path path, String filename, String mediaType) { Path normalized = path.toAbsolutePath().normalize(); if (!normalized.startsWith(storageRoot) || !Files.isRegularFile(normalized)) throw notFound(); return new KnowledgeAssetDtos.StoredFile(normalized, filename, mediaType); }
    private List<String> parseTags(String value) { return value == null ? List.of() : Arrays.stream(value.split("[,，]")).map(String::trim).filter(item -> !item.isBlank()).distinct().limit(30).toList(); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalArgumentException(e); } }
    private List<String> strings(String value) { try { return objectMapper.readValue(value, STRING_LIST); } catch (Exception e) { return new ArrayList<>(); } }
    private Map<String, Object> objectMap(String value) { try { return objectMapper.readValue(value, MAP); } catch (Exception e) { return new LinkedHashMap<>(); } }
    private String documentFormat(String ext) { return switch (ext) { case "docx" -> "WORD"; case "pptx" -> "PPTX"; case "xls", "xlsx" -> "EXCEL"; case "txt" -> "TXT"; case "md" -> "MARKDOWN"; default -> "PDF"; }; }
    private KnowledgeAssetDtos.KnowledgeType inferKnowledge(String datasetId) { return KnowledgeAssetDtos.KnowledgeType.SOLUTION; }
    private String extension(String filename) { int dot = filename.lastIndexOf('.'); return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private String baseName(String filename) { int dot = filename.lastIndexOf('.'); return dot < 0 ? filename : filename.substring(0, dot); }
    private String sanitize(String filename) { return Path.of(filename).getFileName().toString().replaceAll("[\\r\\n]", "_"); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String truncate(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max); }
    private UUID uuid(String value) { try { return UUID.fromString(value); } catch (Exception e) { throw notFound(); } }
    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) { try { return value == null || value.isBlank() ? fallback : Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); } catch (Exception e) { return fallback; } }
    private String classify(Exception exception) {
        String value = (exception.getClass().getSimpleName() + " " + exception.getMessage()).toLowerCase(Locale.ROOT);
        return value.contains("ppt") || value.contains("slide") || value.contains("zip") || value.contains("ooxml")
                ? "MEDIA_EXTRACTION_FAILED" : "INGESTION_FAILED";
    }
    private String businessMessage(String code, Exception exception) {
        if (exception instanceof BusinessException business) return business.getMessage();
        return switch (code) {
            case "MEDIA_EXTRACTION_FAILED" -> "PPTX 页面或媒体提取失败，请确认文件可在 PowerPoint 中正常打开后重新解析";
            default -> "知识资产入库失败，可在详情中重新执行；失败阶段和技术原因已写入后端日志";
        };
    }
    private BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "知识资产不存在"); }
}
