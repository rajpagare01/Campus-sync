package com.campussync.backend.Service;

import com.campussync.backend.Dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    FileUploadResponse upload(MultipartFile file, String folder) throws IOException;
}
