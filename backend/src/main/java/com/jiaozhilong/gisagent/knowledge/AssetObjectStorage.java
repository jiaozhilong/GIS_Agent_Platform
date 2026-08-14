package com.jiaozhilong.gisagent.knowledge;

import com.jiaozhilong.gisagent.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AssetObjectStorage {
    private final boolean enabled;
    private final MinioClient client;
    private final String originalBucket;
    private final String mediaBucket;
    private final String previewBucket;
    private final Set<String> initialized = ConcurrentHashMap.newKeySet();

    public AssetObjectStorage(@Value("${platform.assets.object-storage.enabled:true}") boolean enabled,
                              @Value("${platform.assets.object-storage.endpoint:http://localhost:19000}") String endpoint,
                              @Value("${platform.assets.object-storage.access-key:}") String accessKey,
                              @Value("${platform.assets.object-storage.secret-key:}") String secretKey,
                              @Value("${platform.assets.object-storage.original-bucket:gis-knowledge-original}") String originalBucket,
                              @Value("${platform.assets.object-storage.media-bucket:gis-knowledge-media}") String mediaBucket,
                              @Value("${platform.assets.object-storage.preview-bucket:gis-knowledge-preview}") String previewBucket) {
        this.enabled = enabled && accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
        this.client = this.enabled ? MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build() : null;
        this.originalBucket = originalBucket; this.mediaBucket = mediaBucket; this.previewBucket = previewBucket;
    }

    public String original(UUID assetId, String filename, String contentType, Path path) {
        return put(originalBucket, assetId + "/" + safe(filename), contentType, path);
    }
    public String media(UUID assetId, int slide, String filename, String contentType, Path path) {
        return put(mediaBucket, assetId + "/slide-" + slide + "/" + safe(filename), contentType, path);
    }
    public String preview(UUID assetId, int slide, Path path) {
        return put(previewBucket, assetId + "/slide-" + slide + ".png", "image/png", path);
    }
    public boolean enabled() { return enabled; }
    @EventListener(ApplicationReadyEvent.class)
    public void initializeBuckets() {
        if (!enabled) return;
        try { ensureBucket(originalBucket); ensureBucket(mediaBucket); ensureBucket(previewBucket); }
        catch (Exception exception) { throw new BusinessException(HttpStatus.BAD_GATEWAY, "MINIO_INIT_FAILED", "MinIO 业务 bucket 初始化失败：" + exception.getMessage()); }
    }
    public void deleteOriginal(String key) { remove(originalBucket, key); }
    public void deleteMedia(String key) { remove(mediaBucket, key); }
    public void deletePreview(String key) { remove(previewBucket, key); }

    private String put(String bucket, String key, String contentType, Path path) {
        if (!enabled || path == null || !Files.isRegularFile(path)) return null;
        try {
            ensureBucket(bucket);
            try (var input = Files.newInputStream(path)) {
                client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(input, Files.size(path), -1)
                        .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType).build());
            }
            return key;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "MINIO_STORE_FAILED", "知识资产写入 MinIO 失败：" + exception.getMessage());
        }
    }
    private synchronized void ensureBucket(String bucket) throws Exception {
        if (initialized.contains(bucket)) return;
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        initialized.add(bucket);
    }
    private void remove(String bucket, String key) {
        if (!enabled || key == null || key.isBlank()) return;
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception exception) { throw new BusinessException(HttpStatus.BAD_GATEWAY, "MINIO_DELETE_FAILED", "MinIO 对象清理失败：" + exception.getMessage()); }
    }
    private String safe(String value) { return value.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_"); }
}
