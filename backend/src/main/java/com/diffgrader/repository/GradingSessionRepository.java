package com.diffgrader.repository;

import com.diffgrader.entity.GradingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GradingSession entity
 */
@Repository
public interface GradingSessionRepository extends JpaRepository<GradingSession, Long> {

    /**
     * Find sessions by status
     */
    List<GradingSession> findByStatus(GradingSession.SessionStatus status);

    /**
     * Find sessions by status with pagination
     */
    Page<GradingSession> findByStatus(GradingSession.SessionStatus status, Pageable pageable);

    /**
     * Find sessions created after a specific date
     */
    List<GradingSession> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find sessions by student file id
     */
    Optional<GradingSession> findByStudentFileId(Long studentFileId);

    /**
     * Find sessions by reference file id
     */
    Optional<GradingSession> findByReferenceFileId(Long referenceFileId);

    /**
     * Count sessions by status
     */
    @Query("SELECT COUNT(s) FROM GradingSession s WHERE s.status = :status")
    long countByStatus(@Param("status") GradingSession.SessionStatus status);

    /**
     * Find sessions with comparison results
     */
    @Query("SELECT s FROM GradingSession s LEFT JOIN FETCH s.comparison WHERE s.comparison IS NOT NULL")
    List<GradingSession> findSessionsWithComparison();

    /**
     * Find sessions with feedbacks
     */
    @Query("SELECT DISTINCT s FROM GradingSession s LEFT JOIN FETCH s.feedbacks WHERE SIZE(s.feedbacks) > 0")
    List<GradingSession> findSessionsWithFeedbacks();

    /**
     * Find sessions by overall score range
     */
    @Query("SELECT s FROM GradingSession s WHERE s.overallScore >= :minScore AND s.overallScore <= :maxScore")
    List<GradingSession> findByScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    /**
     * Find incomplete sessions (not completed)
     */
    @Query("SELECT s FROM GradingSession s WHERE s.status != 'COMPLETED'")
    List<GradingSession> findIncompleteSessions();

    /**
     * Delete sessions older than specified date
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
} 