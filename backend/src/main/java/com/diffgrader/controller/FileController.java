package com.diffgrader.controller;

import com.diffgrader.entity.UploadedFile;
import com.diffgrader.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for file upload and management operations
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "APIs for file upload and management")
public class FileController {

    private final FileService fileService;

    /**
     * Upload a ZIP file (student assignment or reference solution)
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload ZIP file", description = "Upload student assignment or reference solution ZIP file")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "ZIP file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "File type: STUDENT or REFERENCE", required = true)
            @RequestParam("type") String type) {
        
        log.info("Received file upload request: {} (type: {})", file.getOriginalFilename(), type);
        
        try {
            // Validate file type parameter
            UploadedFile.FileType fileType;
            try {
                fileType = UploadedFile.FileType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid file type. Must be STUDENT or REFERENCE"));
            }

            // Upload file
            UploadedFile uploadedFile = fileService.uploadFile(file, fileType);

            // Create response
            Map<String, Object> response = createSuccessResponse("File uploaded successfully");
            response.put("fileId", uploadedFile.getId().toString());
            response.put("fileName", uploadedFile.getName());
            response.put("originalName", uploadedFile.getOriginalName());
            response.put("size", uploadedFile.getSize());
            response.put("type", uploadedFile.getType());
            response.put("status", uploadedFile.getStatus());

            log.info("File upload successful: {} -> ID: {}", file.getOriginalFilename(), uploadedFile.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("File upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("File upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get file information by ID
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "Get file information", description = "Retrieve file information by ID")
    public ResponseEntity<Map<String, Object>> getFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            UploadedFile file = fileService.getFile(fileId);

            Map<String, Object> response = createSuccessResponse("File retrieved successfully");
            response.put("file", createFileInfo(file));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("File not found: {}", fileId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve file: " + e.getMessage()));
        }
    }

    /**
     * Delete file by ID
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete file", description = "Delete uploaded file and extracted content")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            fileService.deleteFile(fileId);

            Map<String, Object> response = createSuccessResponse("File deleted successfully");
            log.info("File deleted: {}", fileId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("File not found for deletion: {}", fileId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        return response;
    }

    /**
     * Create file information object
     */
    private Map<String, Object> createFileInfo(UploadedFile file) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", file.getId());
        fileInfo.put("name", file.getName());
        fileInfo.put("originalName", file.getOriginalName());
        fileInfo.put("size", file.getSize());
        fileInfo.put("type", file.getType());
        fileInfo.put("status", file.getStatus());
        fileInfo.put("contentType", file.getContentType());
        fileInfo.put("uploadDate", file.getUploadDate());
        fileInfo.put("updatedAt", file.getUpdatedAt());
        
        if (file.getErrorMessage() != null) {
            fileInfo.put("errorMessage", file.getErrorMessage());
        }
        
        return fileInfo;
    }

    /**
     * Handle file upload errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled exception in FileController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("An unexpected error occurred"));
    }
} 