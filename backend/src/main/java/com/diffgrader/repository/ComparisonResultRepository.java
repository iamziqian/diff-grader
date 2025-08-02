package com.diffgrader.repository;

import com.diffgrader.entity.ComparisonResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ComparisonResult entity
 */
@Repository
public interface ComparisonResultRepository extends JpaRepository<ComparisonResult, Long> {

    /**
     * Find comparison result by grading session id
     */
    Optional<ComparisonResult> findByGradingSessionId(Long gradingSessionId);

    /**
     * Find comparisons by similarity threshold
     */
    @Query("SELECT c FROM ComparisonResult c WHERE c.overallSimilarity >= :threshold")
    List<ComparisonResult> findBySimilarityThreshold(@Param("threshold") Double threshold);

    /**
     * Find comparisons with high similarity (>= 0.8)
     */
    @Query("SELECT c FROM ComparisonResult c WHERE c.overallSimilarity >= 0.8")
    List<ComparisonResult> findHighSimilarityComparisons();

    /**
     * Find comparisons with low similarity (< 0.5)
     */
    @Query("SELECT c FROM ComparisonResult c WHERE c.overallSimilarity < 0.5")
    List<ComparisonResult> findLowSimilarityComparisons();

    /**
     * Get average similarity across all comparisons
     */
    @Query("SELECT AVG(c.overallSimilarity) FROM ComparisonResult c")
    Double getAverageSimilarity();

    /**
     * Count comparisons by similarity range
     */
    @Query("SELECT COUNT(c) FROM ComparisonResult c WHERE c.overallSimilarity >= :minSimilarity AND c.overallSimilarity <= :maxSimilarity")
    long countBySimilarityRange(@Param("minSimilarity") Double minSimilarity, @Param("maxSimilarity") Double maxSimilarity);

    /**
     * Find comparisons with most matches
     */
    @Query("SELECT c FROM ComparisonResult c ORDER BY c.matchedElements DESC")
    List<ComparisonResult> findByMostMatches();

    /**
     * Find comparisons with specific number of student elements
     */
    List<ComparisonResult> findByTotalStudentElements(Integer totalStudentElements);
} 