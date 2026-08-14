package com.jiaozhilong.gisagent.knowledge;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class PptxAssetExtractor {
    private static final String RELATIONSHIP_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final long MEBIBYTE = 1024L * 1024L;
    private final long streamingThresholdBytes;

    public PptxAssetExtractor(@Value("${platform.assets.pptx.streaming-threshold-mb:128}") long streamingThresholdMb) {
        this.streamingThresholdBytes = Math.max(1, streamingThresholdMb) * MEBIBYTE;
    }

    public record SlidePage(int pageNumber, String title, String text, Path previewPath, int imageCount, int videoCount) {}
    public record ExtractedMedia(int pageNumber, String kind, String filename, String mediaType, Path path,
                                 String externalUrl, long fileSize, String sourceKind) {}
    public record Extraction(List<SlidePage> pages, List<ExtractedMedia> media) {}

    /**
     * Large, media-heavy PPTX files are ZIP containers that can expand to several times their file size.
     * Opening those files as a complete POI object graph is unnecessarily expensive, so they are parsed
     * directly from OOXML entries. Small presentations retain POI rendering and automatically fall back to
     * streaming when a malformed drawing or an unsupported embedded object prevents POI from opening it.
     */
    public Extraction extract(Path pptxPath, Path assetDirectory) throws Exception {
        prepareDirectory(assetDirectory.resolve("previews"));
        prepareDirectory(assetDirectory.resolve("media"));
        if (Files.size(pptxPath) >= streamingThresholdBytes) return extractStreaming(pptxPath, assetDirectory);
        try {
            return extractWithPoi(pptxPath, assetDirectory);
        } catch (Exception poiFailure) {
            prepareDirectory(assetDirectory.resolve("previews"));
            prepareDirectory(assetDirectory.resolve("media"));
            return extractStreaming(pptxPath, assetDirectory);
        }
    }

    private Extraction extractWithPoi(Path pptxPath, Path assetDirectory) throws Exception {
        Path previewDirectory = assetDirectory.resolve("previews");
        Path mediaDirectory = assetDirectory.resolve("media");
        List<SlidePage> pages = new ArrayList<>();
        List<ExtractedMedia> media = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(OPCPackage.open(pptxPath.toFile(), PackageAccess.READ))) {
            int pageNumber = 0;
            for (XSLFSlide slide : show.getSlides()) {
                pageNumber++;
                List<String> blocks = new ArrayList<>();
                List<XSLFPictureShape> pictures = new ArrayList<>();
                for (XSLFShape shape : slide.getShapes()) collect(shape, blocks, pictures);
                String text = String.join("\n", blocks).trim();
                String title = firstText(blocks, pageNumber);
                int imageIndex = 0;
                Set<String> pictureKeys = new LinkedHashSet<>();
                for (XSLFPictureShape picture : pictures) {
                    XSLFPictureData data = picture.getPictureData();
                    String key = data.getPackagePart().getPartName() + ":" + data.getPackagePart().getSize();
                    if (!pictureKeys.add(key)) continue;
                    imageIndex++;
                    String extension = extension(data.getFileName(), data.getContentType(), "png");
                    String filename = "page-" + pageNumber + "-image-" + imageIndex + "." + extension;
                    Path path = mediaDirectory.resolve(filename);
                    try (InputStream input = data.getPackagePart().getInputStream()) {
                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                    media.add(new ExtractedMedia(pageNumber, "IMAGE", filename, data.getContentType(), path, null,
                            Files.size(path), "EXTRACTED"));
                }
                List<ExtractedMedia> videos = slideVideos(show.getPackage(), slide, pageNumber, mediaDirectory);
                media.addAll(videos);
                Path preview = render(show, slide, previewDirectory.resolve("page-" + pageNumber + ".png"));
                pages.add(new SlidePage(pageNumber, truncate(title, 500), text, preview, imageIndex, videos.size()));
            }
        }
        return new Extraction(pages, media);
    }

    private Extraction extractStreaming(Path pptxPath, Path assetDirectory) throws Exception {
        Path previewDirectory = assetDirectory.resolve("previews");
        Path mediaDirectory = assetDirectory.resolve("media");
        List<SlidePage> pages = new ArrayList<>();
        List<ExtractedMedia> media = new ArrayList<>();
        try (ZipFile zip = new ZipFile(pptxPath.toFile())) {
            List<String> slideNames = orderedSlides(zip);
            int pageNumber = 0;
            for (String slideName : slideNames) {
                pageNumber++;
                ZipEntry slideEntry = required(zip, slideName);
                List<String> blocks;
                try (InputStream input = zip.getInputStream(slideEntry)) {
                    blocks = xmlText(input, "t");
                }
                String text = String.join("\n", blocks).trim();
                String title = firstText(blocks, pageNumber);
                List<Relationship> relationships = relationships(zip, slideName);
                int imageCount = 0;
                int videoCount = 0;
                int mediaIndex = 0;
                Set<String> seen = new LinkedHashSet<>();
                for (Relationship relationship : relationships) {
                    String target = relationship.target();
                    boolean image = isImage(target, relationship.type());
                    boolean video = isVideo(target, relationship.type());
                    if (!image && !video) continue;
                    if (!seen.add(relationship.targetMode() + ":" + target)) continue;
                    mediaIndex++;
                    if (image) imageCount++; else videoCount++;
                    String filename = filenameFromTarget(target);
                    if (relationship.external()) {
                        media.add(new ExtractedMedia(pageNumber, video ? "VIDEO" : "IMAGE", filename,
                                video ? videoMime(filename) : imageMime(filename), null, target, 0, "LINKED"));
                        continue;
                    }
                    String entryName = resolvePart(slideName, target);
                    ZipEntry mediaEntry = zip.getEntry(entryName);
                    if (mediaEntry == null || mediaEntry.isDirectory()) continue;
                    String storedName = "page-" + pageNumber + "-media-" + mediaIndex + "-" + safe(filename);
                    Path path = mediaDirectory.resolve(storedName);
                    try (InputStream input = zip.getInputStream(mediaEntry)) {
                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                    media.add(new ExtractedMedia(pageNumber, video ? "VIDEO" : "IMAGE", filename,
                            video ? videoMime(filename) : imageMime(filename), path, null, Files.size(path), "EXTRACTED"));
                }
                Path preview = renderTextPreview(title, text, pageNumber, previewDirectory.resolve("page-" + pageNumber + ".png"));
                pages.add(new SlidePage(pageNumber, truncate(title, 500), text, preview, imageCount, videoCount));
            }
        }
        return new Extraction(pages, media);
    }

    private List<String> orderedSlides(ZipFile zip) throws Exception {
        ZipEntry presentation = required(zip, "ppt/presentation.xml");
        Map<String, String> relationshipTargets = new HashMap<>();
        ZipEntry rels = zip.getEntry("ppt/_rels/presentation.xml.rels");
        if (rels != null) {
            try (InputStream input = zip.getInputStream(rels)) {
                for (Relationship relationship : relationshipXml(input)) {
                    relationshipTargets.put(relationship.id(), resolvePart("ppt/presentation.xml", relationship.target()));
                }
            }
        }
        List<String> ids;
        try (InputStream input = zip.getInputStream(presentation)) {
            ids = relationshipIds(input);
        }
        List<String> ordered = ids.stream().map(relationshipTargets::get).filter(value -> value != null && zip.getEntry(value) != null).toList();
        if (!ordered.isEmpty()) return ordered;
        return zip.stream().map(ZipEntry::getName).filter(name -> name.matches("ppt/slides/slide\\d+\\.xml"))
                .sorted(Comparator.comparingInt(this::slideNumber)).toList();
    }

    private List<Relationship> relationships(ZipFile zip, String slideName) throws Exception {
        int slash = slideName.lastIndexOf('/');
        String relName = slideName.substring(0, slash + 1) + "_rels/" + slideName.substring(slash + 1) + ".rels";
        ZipEntry entry = zip.getEntry(relName);
        if (entry == null) return List.of();
        try (InputStream input = zip.getInputStream(entry)) {
            return relationshipXml(input);
        }
    }

    private List<Relationship> relationshipXml(InputStream input) throws Exception {
        XMLStreamReader reader = xmlFactory().createXMLStreamReader(input);
        List<Relationship> result = new ArrayList<>();
        try {
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT || !"Relationship".equals(reader.getLocalName())) continue;
                result.add(new Relationship(attribute(reader, "Id"), attribute(reader, "Type"), attribute(reader, "Target"),
                        attribute(reader, "TargetMode")));
            }
        } finally {
            reader.close();
        }
        return result;
    }

    private List<String> relationshipIds(InputStream input) throws Exception {
        XMLStreamReader reader = xmlFactory().createXMLStreamReader(input);
        List<String> result = new ArrayList<>();
        try {
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT || !"sldId".equals(reader.getLocalName())) continue;
                String id = reader.getAttributeValue(RELATIONSHIP_NS, "id");
                if (id != null && !id.isBlank()) result.add(id);
            }
        } finally {
            reader.close();
        }
        return result;
    }

    private List<String> xmlText(InputStream input, String localName) throws Exception {
        XMLStreamReader reader = xmlFactory().createXMLStreamReader(input);
        List<String> result = new ArrayList<>();
        try {
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT || !localName.equals(reader.getLocalName())) continue;
                String value = reader.getElementText();
                if (value != null && !value.isBlank()) result.add(value.trim());
            }
        } finally {
            reader.close();
        }
        return result;
    }

    private XMLInputFactory xmlFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        return factory;
    }

    private String attribute(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(null, name);
        return value == null ? "" : value;
    }

    private ZipEntry required(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) throw new IllegalArgumentException("PPTX 缺少必要组件：" + name);
        return entry;
    }

    private void collect(XSLFShape shape, List<String> text, List<XSLFPictureShape> pictures) {
        if (shape instanceof XSLFTextShape textShape) {
            String value = textShape.getText();
            if (value != null && !value.isBlank()) text.add(value.trim());
        }
        if (shape instanceof XSLFPictureShape pictureShape) pictures.add(pictureShape);
        if (shape instanceof XSLFGroupShape group) for (XSLFShape child : group.getShapes()) collect(child, text, pictures);
    }

    private List<ExtractedMedia> slideVideos(OPCPackage pkg, XSLFSlide slide, int pageNumber, Path mediaDirectory) {
        List<ExtractedMedia> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try {
            for (PackageRelationship relationship : slide.getPackagePart().getRelationships()) {
                String target = relationship.getTargetURI().toString();
                if (!isVideo(target, relationship.getRelationshipType())) continue;
                if (!seen.add(target)) continue;
                String filename = filenameFromTarget(target);
                if (relationship.getTargetMode() == TargetMode.EXTERNAL) {
                    result.add(new ExtractedMedia(pageNumber, "VIDEO", filename, videoMime(filename), null,
                            target, 0, "LINKED"));
                    continue;
                }
                URI resolved = PackagingURIHelper.resolvePartUri(slide.getPackagePart().getPartName().getURI(), relationship.getTargetURI());
                PackagePart part = pkg.getPart(PackagingURIHelper.createPartName(resolved));
                if (part == null) continue;
                Path path = mediaDirectory.resolve("page-" + pageNumber + "-" + safe(filename));
                try (InputStream input = part.getInputStream()) { Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING); }
                result.add(new ExtractedMedia(pageNumber, "VIDEO", filename, part.getContentType(), path,
                        null, Files.size(path), "EXTRACTED"));
            }
        } catch (Exception ignored) {
            // Broken media relationships do not prevent text and image ingestion.
        }
        return result;
    }

    private Path render(XMLSlideShow show, XSLFSlide slide, Path target) {
        try {
            Dimension size = show.getPageSize();
            double scale = Math.min(1, 960d / Math.max(1, size.width));
            BufferedImage image = new BufferedImage(Math.max(1, (int) (size.width * scale)),
                    Math.max(1, (int) (size.height * scale)), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.scale(scale, scale);
            slide.draw(graphics);
            graphics.dispose();
            ImageIO.write(image, "png", target.toFile());
            return target;
        } catch (Exception exception) {
            return null;
        }
    }

    private Path renderTextPreview(String title, String text, int pageNumber, Path target) {
        try {
            int width = 960;
            int height = 540;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(4, 18, 29));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(33, 171, 225));
            graphics.fillRect(0, 0, 8, height);
            graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
            graphics.drawString("PPTX PAGE " + pageNumber + " · STREAMING PREVIEW", 42, 48);
            graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 30));
            graphics.setColor(Color.WHITE);
            int y = drawWrapped(graphics, title, 42, 98, width - 84, 40, 2);
            graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
            graphics.setColor(new Color(171, 196, 210));
            drawWrapped(graphics, text, 42, y + 26, width - 84, 29, 10);
            graphics.dispose();
            ImageIO.write(image, "png", target.toFile());
            return target;
        } catch (Exception exception) {
            return null;
        }
    }

    private int drawWrapped(Graphics2D graphics, String value, int x, int y, int maxWidth, int lineHeight, int maxLines) {
        FontMetrics metrics = graphics.getFontMetrics();
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        int line = 0;
        int offset = 0;
        while (offset < normalized.length() && line < maxLines) {
            int end = offset;
            while (end < normalized.length() && metrics.stringWidth(normalized.substring(offset, end + 1)) <= maxWidth) end++;
            if (end == offset) end = Math.min(normalized.length(), offset + 1);
            graphics.drawString(normalized.substring(offset, end), x, y + line * lineHeight);
            offset = end;
            line++;
        }
        return y + Math.max(1, line) * lineHeight;
    }

    private void prepareDirectory(Path directory) throws Exception {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        Files.createDirectories(directory);
    }

    private String firstText(List<String> blocks, int pageNumber) {
        return blocks.stream().flatMap(value -> value.lines()).map(String::trim).filter(value -> !value.isBlank())
                .findFirst().orElse("第 " + pageNumber + " 页");
    }

    private boolean isImage(String value, String type) {
        String lower = value.toLowerCase(Locale.ROOT);
        return type.toLowerCase(Locale.ROOT).endsWith("/image") || lower.matches(".*\\.(png|jpe?g|gif|bmp|tiff?|webp|emf|wmf|svg)(\\?.*)?$");
    }

    private boolean isVideo(String value, String type) {
        String lower = value.toLowerCase(Locale.ROOT);
        return type.toLowerCase(Locale.ROOT).contains("video") || lower.matches(".*\\.(mp4|mov|avi|wmv|webm|m4v)(\\?.*)?$");
    }

    private String resolvePart(String sourcePart, String target) {
        if (target.startsWith("/")) return target.substring(1);
        URI source = URI.create("/" + sourcePart);
        String resolved = source.resolve(target).normalize().getPath();
        return resolved.startsWith("/") ? resolved.substring(1) : resolved;
    }

    private int slideNumber(String name) {
        String value = name.replaceFirst(".*slide", "").replaceFirst("\\.xml$", "");
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return Integer.MAX_VALUE; }
    }

    private String filenameFromTarget(String target) {
        String path = target;
        try {
            URI uri = URI.create(target);
            if (uri.getPath() != null && !uri.getPath().isBlank()) path = uri.getPath();
        } catch (IllegalArgumentException ignored) {
            // Fall back to the relationship target as-is for malformed legacy links.
        }
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        path = path.replace('\\', '/');
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        return filename.isBlank() ? "linked-media.bin" : safe(filename);
    }

    private String imageMime(String value) {
        String ext = extension(value, "", "png");
        return "image/" + switch (ext) { case "jpg" -> "jpeg"; case "svg" -> "svg+xml"; default -> ext; };
    }

    private String videoMime(String value) {
        String ext = extension(value, "", "mp4");
        return "video/" + ("mov".equals(ext) ? "quicktime" : ext);
    }

    private String extension(String filename, String mediaType, String fallback) {
        if (filename != null && filename.lastIndexOf('.') >= 0) return safe(filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
        if (mediaType != null && mediaType.contains("/")) return safe(mediaType.substring(mediaType.indexOf('/') + 1).replace("jpeg", "jpg"));
        return fallback;
    }

    private String safe(String value) { return value.replaceAll("[^a-zA-Z0-9._-]", "_"); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    private record Relationship(String id, String type, String target, String targetMode) {
        boolean external() { return "EXTERNAL".equalsIgnoreCase(targetMode); }
    }
}
