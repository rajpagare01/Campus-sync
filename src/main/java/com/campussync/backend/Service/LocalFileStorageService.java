package com.campussync.backend.Service;

import com.campussync.backend.Dto.FileUploadResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    @Override
    public FileUploadResponse upload(MultipartFile file, String folder) throws IOException {
        validate(file);

        String safeFolder = (folder == null || folder.isBlank()) ? "general" : folder.trim().toLowerCase(Locale.ROOT);
        File directory = new File(uploadDir + safeFolder + "/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        File destination = new File(directory, fileName);
        file.transferTo(destination);

        String relativeKey = safeFolder + "/" + fileName;
        return new FileUploadResponse(relativeKey, "/uploads/" + relativeKey, false);
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

    private String sanitize(String originalFilename) {
        return originalFilename == null ? "upload.bin" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
