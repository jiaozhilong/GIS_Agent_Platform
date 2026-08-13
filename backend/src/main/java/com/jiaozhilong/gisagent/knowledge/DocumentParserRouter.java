package com.jiaozhilong.gisagent.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DocumentParserRouter {
    private final boolean docxManualEnabled;
    private final boolean pdfManualEnabled;

    public DocumentParserRouter(@Value("${platform.knowledge.parser.docx-manual-enabled:false}") boolean docxManualEnabled,
                                @Value("${platform.knowledge.parser.pdf-manual-enabled:false}") boolean pdfManualEnabled) {
        this.docxManualEnabled = docxManualEnabled;
        this.pdfManualEnabled = pdfManualEnabled;
    }

    public ParserDecision route(ParserRouteContext context, KnowledgeAssetDtos.ParserStrategy override,
                                Map<String, Object> overrideConfig) {
        if (override != null) return decision(override, "ADMIN_OVERRIDE", .99, overrideConfig, "pptx".equals(context.extension()));
        String ext = context.extension();
        if ("pptx".equals(ext)) return decision(KnowledgeAssetDtos.ParserStrategy.PRESENTATION, "FILE_EXT_PPTX", 1, Map.of(), true);
        if (context.documentType() == KnowledgeAssetDtos.DocumentType.FAQ) return decision(KnowledgeAssetDtos.ParserStrategy.QA, "DOCUMENT_TYPE_FAQ", .98, Map.of(), false);
        if ("xlsx".equals(ext) || "xls".equals(ext)) {
            boolean structured = context.documentType() == KnowledgeAssetDtos.DocumentType.TECHNICAL_MANUAL;
            return decision(structured ? KnowledgeAssetDtos.ParserStrategy.TABLE : KnowledgeAssetDtos.ParserStrategy.GENERAL,
                    structured ? "EXCEL_STRUCTURED_PARAMETER_TABLE" : "EXCEL_GENERAL", .9, Map.of(), false);
        }
        boolean manual = context.documentType() == KnowledgeAssetDtos.DocumentType.PRODUCT_MANUAL
                || context.documentType() == KnowledgeAssetDtos.DocumentType.TECHNICAL_MANUAL;
        if ("docx".equals(ext) && manual && docxManualEnabled)
            return decision(KnowledgeAssetDtos.ParserStrategy.MANUAL, "DOCX_PRODUCT_MANUAL_FEATURE_ENABLED", .95, Map.of(), false);
        if ("pdf".equals(ext) && manual && pdfManualEnabled)
            return decision(KnowledgeAssetDtos.ParserStrategy.MANUAL, "PDF_PRODUCT_MANUAL_FEATURE_ENABLED", .9, Map.of(), false);
        if (context.assetType() == KnowledgeAssetDtos.AssetType.IMAGE || context.assetType() == KnowledgeAssetDtos.AssetType.VIDEO)
            return decision(KnowledgeAssetDtos.ParserStrategy.GENERAL, "NON_DOCUMENT_ASSET_METADATA", 1, Map.of(), false);
        return decision(KnowledgeAssetDtos.ParserStrategy.GENERAL,
                manual ? "MANUAL_COMPATIBILITY_FEATURE_DISABLED_FALLBACK_GENERAL" : "GENERAL_COMPATIBLE_DOCUMENT", .92, Map.of(), false);
    }

    private ParserDecision decision(KnowledgeAssetDtos.ParserStrategy strategy, String reason, double confidence,
                                    Map<String, Object> config, boolean media) {
        return new ParserDecision(strategy, config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config),
                reason, confidence, KnowledgeAssetDtos.ParserStrategy.GENERAL, media);
    }

    public record ParserRouteContext(String extension, String mimeType, KnowledgeAssetDtos.AssetType assetType,
                                     KnowledgeAssetDtos.KnowledgeType knowledgeType, KnowledgeAssetDtos.DocumentType documentType,
                                     String industry, String product, long fileSize) {}
    public record ParserDecision(KnowledgeAssetDtos.ParserStrategy strategy, Map<String, Object> parserConfig,
                                 String decisionReason, double confidence, KnowledgeAssetDtos.ParserStrategy fallbackMethod,
                                 boolean pptAssetProcessingRequired) {
        public String ragflowChunkMethod() {
            return switch (strategy) {
                case GENERAL -> "naive";
                case MANUAL -> "manual";
                case PRESENTATION -> "presentation";
                case TABLE -> "table";
                case QA -> "qa";
                case PICTURE -> "picture";
            };
        }
    }
}
