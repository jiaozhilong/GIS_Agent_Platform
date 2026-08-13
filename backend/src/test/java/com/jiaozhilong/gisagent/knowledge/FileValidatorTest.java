package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {
    private final FileValidator validator = new FileValidator();

    @Test
    void rejectsLegacyPptBeforeRagflow() {
        var file = new MockMultipartFile("file", "legacy.ppt", "application/vnd.ms-powerpoint", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> validator.validateUpload(file, 1024))
                .isInstanceOf(BusinessException.class).hasMessageContaining("另存为 PPTX");
    }

    @Test
    void rejectsRenamedOrDamagedPptx(@TempDir Path directory) throws Exception {
        Path fake = directory.resolve("damaged.pptx");
        Files.writeString(fake, "this is not a zip container");
        var info = new FileValidator.UploadInfo("damaged.pptx", "pptx", KnowledgeAssetDtos.AssetType.DOCUMENT,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        assertThatThrownBy(() -> validator.validateStored(fake, info))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不是有效的 PPTX");
    }

    @Test
    void rejectsZipWithoutSlidesEvenWhenItHasPresentationXml(@TempDir Path directory) throws Exception {
        Path fake = directory.resolve("empty.pptx");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fake))) {
            add(zip, "[Content_Types].xml", "<Types/>");
            add(zip, "ppt/presentation.xml", "<p:presentation xmlns:p=\"p\"/>");
        }
        var info = new FileValidator.UploadInfo("empty.pptx", "pptx", KnowledgeAssetDtos.AssetType.DOCUMENT,
                "application/zip");
        assertThatThrownBy(() -> validator.validateStored(fake, info))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不是有效的 PPTX");
    }

    @Test
    void canonicalizesPptxMimeAfterContentValidation(@TempDir Path directory) throws Exception {
        Path pptx = directory.resolve("valid.pptx");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pptx))) {
            add(zip, "[Content_Types].xml", "<Types/>");
            add(zip, "ppt/presentation.xml", "<p:presentation xmlns:p=\"p\"/>");
            add(zip, "ppt/slides/slide1.xml", "<p:sld xmlns:p=\"p\"/>");
        }
        var info = new FileValidator.UploadInfo("valid.pptx", "pptx", KnowledgeAssetDtos.AssetType.DOCUMENT,
                "application/zip");
        assertThat(validator.validateStored(pptx, info).mimeType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    private void add(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
