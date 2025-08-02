package com.diffgrader.service;

import com.diffgrader.entity.UploadedFile;
import com.diffgrader.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Service for handling file upload, storage, and ZIP extraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final UploadedFileRepository uploadedFileRepository;

    @Value("${diffgrader.file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${diffgrader.file.max-size:52428800}")
    private long maxFileSize;

    @Value("#{'${diffgrader.file.allowed-types}'.split(',')}")
    private List<String> allowedContentTypes;

    /**
     * Upload and process a ZIP file
     */
    public UploadedFile uploadFile(MultipartFile file, UploadedFile.FileType fileType) {
        log.info("Starting file upload: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
        
        // Validate file
        validateFile(file);
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(uploadPath);
            Files.createDirectories(uploadDir);
            
            // Generate unique filename
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
            Path filePath = uploadDir.resolve(uniqueFileName);
            
            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create and save file entity
            UploadedFile uploadedFile = UploadedFile.builder()
                .name(uniqueFileName)
                .originalName(file.getOriginalFilename())
                .size(file.getSize())
                .type(fileType)
                .status(UploadedFile.FileStatus.UPLOADED)
                .storageLocation(filePath.toString())
                .contentType(file.getContentType())
                .build();
            
            uploadedFile = uploadedFileRepository.save(uploadedFile);
            
            log.info("File uploaded successfully: {} -> {}", file.getOriginalFilename(), uniqueFileName);
            
            return uploadedFile;
            
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Extract Java files from ZIP archive
     */
    public List<File> extractJavaFiles(UploadedFile uploadedFile) {
        log.info("Extracting Java files from ZIP: {}", uploadedFile.getName());
        
        List<File> javaFiles = new ArrayList<>();
        File zipFile = new File(uploadedFile.getStorageLocation());
        
        if (!zipFile.exists()) {
            throw new RuntimeException("ZIP file not found: " + uploadedFile.getStorageLocation());
        }
        
        // Create extraction directory
        String extractDirName = FilenameUtils.removeExtension(uploadedFile.getName()) + "_extracted";
        Path extractDir = Paths.get(uploadPath, extractDirName);
        
        try {
            Files.createDirectories(extractDir);
            
            try (FileInputStream fis = new FileInputStream(zipFile);
                 ZipArchiveInputStream zis = new ZipArchiveInputStream(fis)) {
                
                ZipArchiveEntry entry;
                while ((entry = zis.getNextZipEntry()) != null) {
                    if (!entry.isDirectory() && isJavaFile(entry.getName())) {
                        File extractedFile = extractZipEntry(zis, entry, extractDir);
                        if (extractedFile != null) {
                            javaFiles.add(extractedFile);
                        }
                    }
                }
            }
            
            log.info("Extracted {} Java files from ZIP: {}", javaFiles.size(), uploadedFile.getName());
            
            // Update file status
            uploadedFile.setStatus(UploadedFile.FileStatus.PROCESSING);
            uploadedFileRepository.save(uploadedFile);
            
        } catch (IOException e) {
            log.error("Failed to extract ZIP file: {}", uploadedFile.getName(), e);
            uploadedFile.setStatus(UploadedFile.FileStatus.ERROR);
            uploadedFile.setErrorMessage("Failed to extract ZIP file: " + e.getMessage());
            uploadedFileRepository.save(uploadedFile);
            throw new RuntimeException("Failed to extract ZIP file", e);
        }
        
        return javaFiles;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size: " + maxFileSize + " bytes");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Only ZIP files are allowed");
        }
        
        // More flexible content type validation
        String contentType = file.getContentType();
        if (contentType != null) {
            boolean isValidType = allowedContentTypes.contains(contentType) ||
                                contentType.equals("application/octet-stream") ||
                                contentType.startsWith("application/zip") ||
                                contentType.startsWith("application/x-zip");
            
            if (!isValidType) {
                log.warn("Unexpected content type: {} for file: {}", contentType, fileName);
            }
        }
    }

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFileName(String originalFileName) {
        String baseName = FilenameUtils.removeExtension(originalFileName);
        String extension = FilenameUtils.getExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s_%s_%s.%s", baseName, timestamp, uuid, extension);
    }

    /**
     * Check if file is a Java source file
     */
    private boolean isJavaFile(String fileName) {
        return fileName.toLowerCase().endsWith(".java");
    }

    /**
     * Extract a single ZIP entry to the extraction directory
     */
    private File extractZipEntry(ZipArchiveInputStream zis, ZipArchiveEntry entry, Path extractDir) {
        try {
            // Sanitize entry name to prevent path traversal attacks
            String sanitizedName = sanitizeFileName(entry.getName());
            Path extractedFilePath = extractDir.resolve(sanitizedName);
            
            // Ensure the extracted file is within the extraction directory
            if (!extractedFilePath.normalize().startsWith(extractDir.normalize())) {
                log.warn("Skipping potentially malicious ZIP entry: {}", entry.getName());
                return null;
            }
            
            // Create parent directories if needed
            Files.createDirectories(extractedFilePath.getParent());
            
            // Extract file content
            try (FileOutputStream fos = new FileOutputStream(extractedFilePath.toFile())) {
                IOUtils.copy(zis, fos);
            }
            
            log.debug("Extracted file: {} -> {}", entry.getName(), extractedFilePath);
            
            return extractedFilePath.toFile();
            
        } catch (IOException e) {
            log.error("Failed to extract ZIP entry: {}", entry.getName(), e);
            return null;
        }
    }

    /**
     * Sanitize filename to prevent path traversal and other security issues
     */
    private String sanitizeFileName(String fileName) {
        // Remove path separators and keep only the filename
        String sanitized = Paths.get(fileName).getFileName().toString();
        
        // Replace any remaining problematic characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Ensure filename is not empty and doesn't start with a dot
        if (sanitized.isEmpty() || sanitized.startsWith(".")) {
            sanitized = "file_" + sanitized;
        }
        
        return sanitized;
    }

    /**
     * Get file by ID
     */
    public UploadedFile getFile(Long fileId) {
        return uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }

    /**
     * Delete file and its extracted content
     */
    public void deleteFile(Long fileId) {
        UploadedFile file = getFile(fileId);
        
        try {
            // Delete original file
            Path filePath = Paths.get(file.getStorageLocation());
            Files.deleteIfExists(filePath);
            
            // Delete extraction directory
            String extractDirName = FilenameUtils.removeExtension(file.getName()) + "_extracted";
            Path extractDir = Paths.get(uploadPath, extractDirName);
            deleteDirectoryRecursively(extractDir);
            
            // Delete from database
            uploadedFileRepository.delete(file);
            
            log.info("Deleted file: {}", file.getName());
            
        } catch (IOException e) {
            log.error("Failed to delete file: {}", file.getName(), e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Delete directory and all its contents recursively
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    /**
     * Get file content as string
     */
    public String getFileContent(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Failed to read file content: {}", file.getName(), e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    /**
     * Clean up old files (for maintenance)
     */
    public void cleanupOldFiles(int daysOld) {
        // This would be called by a scheduled task
        log.info("Cleanup of files older than {} days would be performed here", daysOld);
    }
} 