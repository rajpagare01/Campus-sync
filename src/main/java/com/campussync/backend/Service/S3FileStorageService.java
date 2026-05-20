package com.campussync.backend.Service;

import com.campussync.backend.config.StorageProperties;
import com.campussync.backend.Dto.FileUploadResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3", matchIfMissing = true)
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final Counter uploadCounter;

    public S3FileStorageService(StorageProperties storageProperties, MeterRegistry meterRegistry) {
        this.storageProperties = storageProperties;
        if (!StringUtils.hasText(storageProperties.s3().bucket())) {
            throw new IllegalStateException("S3 bucket must be configured when storage.provider=s3");
        }

        Region region = Region.of(storageProperties.s3().region());
        var credentialsProvider = StringUtils.hasText(storageProperties.s3().accessKey())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(
                storageProperties.s3().accessKey(),
                storageProperties.s3().secretKey()
        ))
                : DefaultCredentialsProvider.create();

        var s3Builder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .forcePathStyle(storageProperties.s3().pathStyleAccess());
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider);

        if (StringUtils.hasText(storageProperties.s3().endpoint())) {
            URI endpoint = URI.create(storageProperties.s3().endpoint());
            s3Builder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }

        this.s3Client = s3Builder.build();
        this.presigner = presignerBuilder.build();
        this.uploadCounter = meterRegistry.counter("storage.uploads", "provider", "s3");
    }

    @Override
    public FileUploadResponse upload(MultipartFile file, String folder) throws IOException {
        validateFile(file);

        String key = buildKey(folder, file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.s3().bucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        uploadCounter.increment();

        String publicBaseUrl = storageProperties.publicBaseUrl();
        if (StringUtils.hasText(publicBaseUrl)) {
            String normalized = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return new FileUploadResponse(key, normalized + "/" + key, false);
        }

        PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(resolveTtl())
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(storageProperties.s3().bucket())
                        .key(key)
                        .build())
                .build());

        log.info("Uploaded object {} to managed storage bucket {}", key, storageProperties.s3().bucket());
        return new FileUploadResponse(key, presigned.url().toString(), true);
    }

    private Duration resolveTtl() {
        return storageProperties.signedUrlTtl() == null ? Duration.ofMinutes(15) : storageProperties.signedUrlTtl();
    }

    private String buildKey(String folder, String originalFilename) {
        String safeFolder = (folder == null || folder.isBlank()) ? "general" : folder.trim().toLowerCase(Locale.ROOT);
        String fileName = originalFilename == null ? "upload.bin" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safeFolder + "/" + UUID.randomUUID() + "_" + fileName;
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new RuntimeException("Content type is required");
        }
        if (!contentType.startsWith("image/") && !contentType.startsWith("video/")) {
            throw new RuntimeException("Only image or video allowed");
        }
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new RuntimeException("File too large");
        }
    }
}
