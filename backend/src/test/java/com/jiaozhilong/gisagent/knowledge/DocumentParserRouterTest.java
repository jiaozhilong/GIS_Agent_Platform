package com.jiaozhilong.gisagent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParserRouterTest {
    @Test
    void routesPptxToPresentationRegardlessOfKnowledgeType() {
        DocumentParserRouter router = new DocumentParserRouter(false, false);
        var decision = router.route(new DocumentParserRouter.ParserRouteContext("pptx", "application/pptx",
                KnowledgeAssetDtos.AssetType.DOCUMENT, KnowledgeAssetDtos.KnowledgeType.PRODUCT,
                KnowledgeAssetDtos.DocumentType.PRODUCT_PRESENTATION, "", "SuperMap iServer", 10), null, Map.of());
        assertThat(decision.strategy()).isEqualTo(KnowledgeAssetDtos.ParserStrategy.PRESENTATION);
        assertThat(decision.pptAssetProcessingRequired()).isTrue();
        assertThat(decision.ragflowChunkMethod()).isEqualTo("presentation");
    }

    @Test
    void fallsBackProductDocxToGeneralUntilManualCompatibilityEnabled() {
        DocumentParserRouter router = new DocumentParserRouter(false, false);
        var decision = router.route(new DocumentParserRouter.ParserRouteContext("docx", "application/docx",
                KnowledgeAssetDtos.AssetType.DOCUMENT, KnowledgeAssetDtos.KnowledgeType.PRODUCT,
                KnowledgeAssetDtos.DocumentType.PRODUCT_MANUAL, "", "SuperMap iServer", 10), null, Map.of());
        assertThat(decision.strategy()).isEqualTo(KnowledgeAssetDtos.ParserStrategy.GENERAL);
        assertThat(decision.decisionReason()).contains("COMPATIBILITY_FEATURE_DISABLED");
    }

    @Test
    void routesIndustrySolutionDocxToGeneral() {
        DocumentParserRouter router = new DocumentParserRouter(true, true);
        var decision = router.route(new DocumentParserRouter.ParserRouteContext("docx", "application/docx",
                KnowledgeAssetDtos.AssetType.DOCUMENT, KnowledgeAssetDtos.KnowledgeType.SOLUTION,
                KnowledgeAssetDtos.DocumentType.INDUSTRY_SOLUTION, "自然资源", "", 10), null, Map.of());
        assertThat(decision.strategy()).isEqualTo(KnowledgeAssetDtos.ParserStrategy.GENERAL);
    }
}
