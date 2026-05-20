package com.campussync.backend.Service;

import com.campussync.backend.Dto.FileUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileService {

    private final FileStorageService fileStorageService;

    public FileService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public FileUploadResponse uploadEventFile(MultipartFile file) throws IOException {
        return fileStorageService.upload(file, "events");
    }

    public FileUploadResponse uploadPostMedia(MultipartFile file) throws IOException {
        return fileStorageService.upload(file, "posts");
    }
}
