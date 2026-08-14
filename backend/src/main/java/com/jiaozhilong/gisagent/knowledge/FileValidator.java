package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class FileValidator {
    private static final Set<String> DOCUMENTS = Set.of("pdf", "docx", "pptx", "xls", "xlsx", "txt", "md");
    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> VIDEOS = Set.of("mp4", "mov", "avi", "wmv", "webm", "m4v");
    private static final Set<String> EXCLUDED = Set.of("dwg", "dxf", "shp", "gdb", "geojson", "kml", "kmz", "tif", "tiff",
            "las", "laz", "obj", "fbx", "3ds", "glb", "gltf", "slpk", "udbx");

    public UploadInfo validateUpload(MultipartFile file, long maximumBytes) {
        if (file == null || file.isEmpty()) throw error("EMPTY_FILE", "上传文件不能为空");
        if (file.getSize() > maximumBytes) throw error("FILE_TOO_LARGE", "文件超过平台允许的最大体积 " + maximumBytes / 1024 / 1024 + "MB");
        String original = file.getOriginalFilename() == null ? "" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String extension = extension(original);
        if ("ppt".equals(extension)) throw error("UNSUPPORTED_FILE", "暂不支持旧版 PPT 文件，请使用 PowerPoint 另存为 PPTX 后重新上传");
        if (EXCLUDED.contains(extension)) throw error("UNSUPPORTED_FILE", "该格式属于 GIS 数据、CAD、遥感影像或三维模型，不进入知识资产中心");
        KnowledgeAssetDtos.AssetType type = DOCUMENTS.contains(extension) ? KnowledgeAssetDtos.AssetType.DOCUMENT
                : IMAGES.contains(extension) ? KnowledgeAssetDtos.AssetType.IMAGE
                : VIDEOS.contains(extension) ? KnowledgeAssetDtos.AssetType.VIDEO : null;
        if (type == null) throw error("UNSUPPORTED_FILE", "仅支持 PDF、DOCX、PPTX、Excel、TXT、Markdown、方案图片和项目/产品视频");
        return new UploadInfo(original, extension, type, normalizeMime(file.getContentType(), extension));
    }

    public ValidationResult validateStored(Path path, UploadInfo info) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) == 0) throw error("EMPTY_FILE", "上传文件为空或保存失败");
            byte[] header = readHeader(path, 12);
            validateHeader(path, info.extension(), header);
            return new ValidationResult(canonicalMime(info.extension(), info.mimeType()), sha256(path));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw error("FILE_VALIDATION_FAILED", "文件合法性检查失败，请确认文件未损坏后重新上传");
        }
    }

    private void validateHeader(Path path, String extension, byte[] header) throws IOException {
        if ("pdf".equals(extension) && !starts(header, "%PDF".getBytes())) throw error("INVALID_FILE_HEADER", "文件扩展名与实际 PDF 格式不一致");
        if (List.of("docx", "pptx", "xlsx").contains(extension)) {
            if (header.length < 4 || header[0] != 'P' || header[1] != 'K') invalidOffice(extension);
            try (ZipFile zip = new ZipFile(path.toFile())) {
                require(zip, "[Content_Types].xml", extension);
                if ("pptx".equals(extension)) {
                    require(zip, "ppt/presentation.xml", extension);
                    boolean hasSlide = zip.stream().anyMatch(entry -> entry.getName().matches("ppt/slides/slide\\d+\\.xml"));
                    if (!hasSlide) invalidOffice(extension);
                }
                if ("docx".equals(extension)) require(zip, "word/document.xml", extension);
                if ("xlsx".equals(extension)) require(zip, "xl/workbook.xml", extension);
            } catch (BusinessException exception) {
                throw exception;
            } catch (Exception exception) {
                invalidOffice(extension);
            }
        }
        if ("doc".equals(extension) || "xls".equals(extension)) {
            byte[] ole = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};
            if (!starts(header, ole)) throw error("INVALID_FILE_HEADER", "文件扩展名与实际 Office 格式不一致");
        }
    }

    private void invalidOffice(String extension) {
        if ("pptx".equals(extension)) throw error("INVALID_PPTX", "文件不是有效的 PPTX 格式。请使用 PowerPoint 打开文件并另存为 PPTX 后重新上传");
        throw error("INVALID_OFFICE_FILE", "文件不是有效的 " + extension.toUpperCase(Locale.ROOT) + " 格式，请重新另存后上传");
    }

    private void require(ZipFile zip, String name, String extension) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) invalidOffice(extension);
    }

    private byte[] readHeader(Path path, int size) throws IOException {
        try (InputStream input = Files.newInputStream(path)) { return input.readNBytes(size); }
    }
    private boolean starts(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }
    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private String extension(String filename) { int dot = filename.lastIndexOf('.'); return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private String normalizeMime(String supplied, String extension) {
        if (supplied != null && !supplied.isBlank() && !"application/octet-stream".equals(supplied)) return supplied;
        return canonicalMime(extension, supplied);
    }
    private String canonicalMime(String extension, String fallback) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> VIDEOS.contains(extension) ? "video/" + extension
                    : fallback == null || fallback.isBlank() ? "text/plain" : fallback;
        };
    }
    private BusinessException error(String code, String message) { return new BusinessException(HttpStatus.BAD_REQUEST, code, message); }

    public record UploadInfo(String filename, String extension, KnowledgeAssetDtos.AssetType assetType, String mimeType) {}
    public record ValidationResult(String mimeType, String sha256) {}
}
