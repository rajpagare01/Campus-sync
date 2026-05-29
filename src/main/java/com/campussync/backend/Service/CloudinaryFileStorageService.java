package com.campussync.backend.Service;

import com.campussync.backend.Dto.FileUploadResponse;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorageService.class);
    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public FileUploadResponse upload(MultipartFile file, String folder) throws IOException {
        validate(file);

        log.info("Uploading file to Cloudinary in folder: {}", folder);
        
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            
            return new FileUploadResponse(publicId, secureUrl, true);
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    private void validate(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new RuntimeException("Only image or video allowed");
        }
        if (file.getSize() > 50L * 1024L * 1024L) {
            throw new RuntimeException("File too large");
        }
    }
}
