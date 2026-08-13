package com.jiaozhilong.gisagent.knowledge;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PptxAssetExtractorTest {
    @Test
    void extractsSmallPresentationWithPoi(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("sample.pptx");
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = show.createSlide();
            var shape = slide.createTextBox();
            shape.setAnchor(new Rectangle(20, 20, 500, 100));
            shape.setText("SuperMap GIS 测试演示文稿");
            try (var output = Files.newOutputStream(source)) { show.write(output); }
        }
        var extraction = new PptxAssetExtractor(128).extract(source, directory.resolve("asset"));
        assertThat(extraction.pages()).hasSize(1);
        assertThat(extraction.pages().get(0).text()).contains("SuperMap GIS");
        assertThat(extraction.pages().get(0).previewPath()).isRegularFile();
    }

    @Test
    void extractsConfiguredRealSampleWithStreamingParser(@TempDir Path directory) throws Exception {
        String configured = System.getProperty("pptx.sample", "");
        org.junit.jupiter.api.Assumptions.assumeTrue(!configured.isBlank());
        Path source = Path.of(configured);
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(source));
        var extraction = new PptxAssetExtractor(1).extract(source, directory.resolve("asset"));
        assertThat(extraction.pages()).isNotEmpty();
        assertThat(extraction.pages()).allSatisfy(page -> {
            assertThat(page.pageNumber()).isPositive();
            assertThat(page.previewPath()).isRegularFile();
        });
        System.out.printf("PPTX_SAMPLE_RESULT file=%s pages=%d media=%d images=%d videos=%d%n",
                source.getFileName(), extraction.pages().size(), extraction.media().size(),
                extraction.media().stream().filter(media -> "IMAGE".equals(media.kind())).count(),
                extraction.media().stream().filter(media -> "VIDEO".equals(media.kind())).count());
    }
}
