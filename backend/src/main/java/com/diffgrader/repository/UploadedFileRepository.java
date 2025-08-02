package com.diffgrader.repository;

import com.diffgrader.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UploadedFile entity
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    /**
     * Find files by type
     */
    List<UploadedFile> findByType(UploadedFile.FileType type);

    /**
     * Find files by status
     */
    List<UploadedFile> findByStatus(UploadedFile.FileStatus status);

    /**
     * Find files by type and status
     */
    List<UploadedFile> findByTypeAndStatus(UploadedFile.FileType type, UploadedFile.FileStatus status);

    /**
     * Find files uploaded after a specific date
     */
    List<UploadedFile> findByUploadDateAfter(LocalDateTime date);

    /**
     * Find files by original name containing keyword
     */
    List<UploadedFile> findByOriginalNameContainingIgnoreCase(String keyword);

    /**
     * Count files by status
     */
    @Query("SELECT COUNT(f) FROM UploadedFile f WHERE f.status = :status")
    long countByStatus(@Param("status") UploadedFile.FileStatus status);

    /**
     * Find files larger than specified size
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.size > :size")
    List<UploadedFile> findFilesLargerThan(@Param("size") Long size);

    /**
     * Delete files older than specified date
     */
    void deleteByUploadDateBefore(LocalDateTime date);
} 