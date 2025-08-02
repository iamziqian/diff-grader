package com.diffgrader.repository;

import com.diffgrader.entity.CodeElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CodeElement entity
 */
@Repository
public interface CodeElementRepository extends JpaRepository<CodeElement, Long> {

    /**
     * Find elements by type
     */
    List<CodeElement> findByType(CodeElement.ElementType type);

    /**
     * Find elements by code structure
     */
    List<CodeElement> findByCodeStructureId(Long codeStructureId);

    /**
     * Find elements by type and code structure
     */
    List<CodeElement> findByTypeAndCodeStructureId(CodeElement.ElementType type, Long codeStructureId);

    /**
     * Find matched elements
     */
    List<CodeElement> findByMatched(Boolean matched);

    /**
     * Find elements by match type
     */
    List<CodeElement> findByMatchType(CodeElement.MatchType matchType);

    /**
     * Find elements by name containing keyword
     */
    List<CodeElement> findByNameContainingIgnoreCase(String keyword);

    /**
     * Find elements by similarity threshold
     */
    @Query("SELECT e FROM CodeElement e WHERE e.similarity >= :threshold")
    List<CodeElement> findBySimilarityThreshold(@Param("threshold") Double threshold);

    /**
     * Count elements by type
     */
    @Query("SELECT COUNT(e) FROM CodeElement e WHERE e.type = :type")
    long countByType(@Param("type") CodeElement.ElementType type);

    /**
     * Count matched elements by code structure
     */
    @Query("SELECT COUNT(e) FROM CodeElement e WHERE e.codeStructure.id = :structureId AND e.matched = true")
    long countMatchedByCodeStructure(@Param("structureId") Long structureId);

    /**
     * Find elements with highest similarity
     */
    @Query("SELECT e FROM CodeElement e WHERE e.similarity IS NOT NULL ORDER BY e.similarity DESC")
    List<CodeElement> findByHighestSimilarity();

    /**
     * Find unmatched elements
     */
    @Query("SELECT e FROM CodeElement e WHERE e.matched = false OR e.matchType = 'MISSING' OR e.matchType = 'EXTRA'")
    List<CodeElement> findUnmatchedElements();
} 