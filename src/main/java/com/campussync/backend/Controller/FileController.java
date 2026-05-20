package com.campussync.backend.Controller;

import com.campussync.backend.Dto.FileUploadResponse;
import com.campussync.backend.Service.FileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/files", "/api/v1/files"})
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // 📷 Upload event images (authenticated users)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/events/upload")
    public FileUploadResponse uploadEventFile(@RequestParam("file") MultipartFile file) throws Exception {
        return fileService.uploadEventFile(file);
    }

    // 📷 Upload post media (only SOCIETY/DEPARTMENT/ADMIN)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/upload")
    public FileUploadResponse uploadPostMedia(@RequestParam("file") MultipartFile file) throws Exception {
        return fileService.uploadPostMedia(file);
    }
}
