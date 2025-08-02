package com.diffgrader.repository;

import com.diffgrader.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Feedback entity
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Find all feedbacks for a grading session
     */
    List<Feedback> findByGradingSessionId(Long gradingSessionId);

    /**
     * Find feedback by grading session and code element
     */
    Optional<Feedback> findByGradingSessionIdAndCodeElementId(Long gradingSessionId, Long codeElementId);

    /**
     * Find feedbacks by score range
     */
    @Query("SELECT f FROM Feedback f WHERE f.score >= :minScore AND f.score <= :maxScore")
    List<Feedback> findByScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    /**
     * Find feedbacks with high scores (>= 80)
     */
    @Query("SELECT f FROM Feedback f WHERE f.score >= 80")
    List<Feedback> findHighScoreFeedbacks();

    /**
     * Find feedbacks with low scores (< 60)
     */
    @Query("SELECT f FROM Feedback f WHERE f.score < 60")
    List<Feedback> findLowScoreFeedbacks();

    /**
     * Get average score for a grading session
     */
    @Query("SELECT AVG(f.score) FROM Feedback f WHERE f.gradingSession.id = :sessionId")
    Double getAverageScoreForSession(@Param("sessionId") Long sessionId);

    /**
     * Get average score across all feedbacks
     */
    @Query("SELECT AVG(f.score) FROM Feedback f")
    Double getOverallAverageScore();

    /**
     * Count feedbacks by grading session
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.gradingSession.id = :sessionId")
    long countByGradingSession(@Param("sessionId") Long sessionId);

    /**
     * Find feedbacks with non-empty comments
     */
    @Query("SELECT f FROM Feedback f WHERE f.comments IS NOT NULL AND f.comments != ''")
    List<Feedback> findFeedbacksWithComments();

    /**
     * Find feedbacks with design pattern feedback
     */
    @Query("SELECT f FROM Feedback f WHERE f.designPatternFeedback IS NOT NULL AND f.designPatternFeedback != ''")
    List<Feedback> findFeedbacksWithDesignPatterns();

    /**
     * Find feedbacks with best practices feedback
     */
    @Query("SELECT f FROM Feedback f WHERE f.bestPracticesFeedback IS NOT NULL AND f.bestPracticesFeedback != ''")
    List<Feedback> findFeedbacksWithBestPractices();

    /**
     * Delete all feedbacks for a grading session
     */
    void deleteByGradingSessionId(Long gradingSessionId);
} 