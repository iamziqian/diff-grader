package com.diffgrader.service;

import com.diffgrader.entity.*;
import com.diffgrader.repository.ComparisonResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for comparing student code with reference solution
 * Combines AST parsing, similarity calculation, and intelligent matching
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ComparisonService {

    private final JavaParserService javaParserService;
    private final SimilarityService similarityService;
    private final FileService fileService;
    private final ComparisonResultRepository comparisonResultRepository;

    @Value("${diffgrader.analysis.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * Perform complete code comparison between student and reference files
     */
    public ComparisonResult compareCode(GradingSession gradingSession) {
        log.info("Starting code comparison for session: {}", gradingSession.getId());

        try {
            // Extract Java files from both ZIP archives
            List<File> studentFiles = fileService.extractJavaFiles(gradingSession.getStudentFile());
            List<File> referenceFiles = fileService.extractJavaFiles(gradingSession.getReferenceFile());

            // Parse and extract code structures
            List<CodeStructure> studentStructures = parseCodeStructures(studentFiles, CodeStructure.StructureType.STUDENT);
            List<CodeStructure> referenceStructures = parseCodeStructures(referenceFiles, CodeStructure.StructureType.REFERENCE);

            // Perform intelligent matching
            List<ComparisonMatch> matches = performIntelligentMatching(studentStructures, referenceStructures);

            // Calculate overall statistics
            int totalStudentElements = studentStructures.stream()
                .mapToInt(structure -> structure.getElements().size())
                .sum();
            int totalReferenceElements = referenceStructures.stream()
                .mapToInt(structure -> structure.getElements().size())
                .sum();
            int matchedElements = matches.size();

            double overallSimilarity = calculateOverallSimilarity(matches, totalStudentElements, totalReferenceElements);

            // Create and save comparison result
            ComparisonResult result = ComparisonResult.builder()
                .gradingSession(gradingSession)
                .studentCodeStructures(studentStructures)
                .referenceCodeStructures(referenceStructures)
                .matches(matches)
                .overallSimilarity(overallSimilarity)
                .totalStudentElements(totalStudentElements)
                .totalReferenceElements(totalReferenceElements)
                .matchedElements(matchedElements)
                .build();

            // Set bidirectional relationships
            final ComparisonResult finalResult = result;
            studentStructures.forEach(structure -> structure.setComparisonResult(finalResult));
            referenceStructures.forEach(structure -> structure.setComparisonResult(finalResult));
            matches.forEach(match -> match.setComparisonResult(finalResult));

            result = comparisonResultRepository.save(result);

            log.info("Code comparison completed. Overall similarity: {:.2%}, Matched elements: {}/{}", 
                overallSimilarity, matchedElements, totalStudentElements);

            return result;

        } catch (Exception e) {
            log.error("Error during code comparison for session: {}", gradingSession.getId(), e);
            throw new RuntimeException("Code comparison failed", e);
        }
    }

    /**
     * Parse code structures from Java files
     */
    private List<CodeStructure> parseCodeStructures(List<File> javaFiles, CodeStructure.StructureType type) {
        List<CodeStructure> structures = new ArrayList<>();

        for (File javaFile : javaFiles) {
            try {
                // Parse code elements from the file
                List<CodeElement> elements = javaParserService.parseJavaFile(javaFile);
                
                // Extract package name
                String packageName = javaParserService.extractPackageName(javaFile);

                // Create code structure
                CodeStructure structure = CodeStructure.builder()
                    .fileName(javaFile.getName())
                    .packageName(packageName)
                    .elements(elements)
                    .type(type)
                    .build();

                // Set bidirectional relationship
                elements.forEach(element -> element.setCodeStructure(structure));

                structures.add(structure);

                log.debug("Parsed {} elements from file: {} (package: {})", 
                    elements.size(), javaFile.getName(), packageName);

            } catch (Exception e) {
                log.error("Failed to parse Java file: {}", javaFile.getName(), e);
                // Continue with other files
            }
        }

        return structures;
    }

    /**
     * Perform intelligent matching between student and reference code elements
     */
    private List<ComparisonMatch> performIntelligentMatching(
            List<CodeStructure> studentStructures, 
            List<CodeStructure> referenceStructures) {
        
        List<ComparisonMatch> matches = new ArrayList<>();

        // Get all student and reference elements
        List<CodeElement> studentElements = studentStructures.stream()
            .flatMap(structure -> structure.getElements().stream())
            .collect(Collectors.toList());

        List<CodeElement> referenceElements = referenceStructures.stream()
            .flatMap(structure -> structure.getElements().stream())
            .collect(Collectors.toList());

        // Group elements by type for more efficient matching
        Map<CodeElement.ElementType, List<CodeElement>> studentByType = studentElements.stream()
            .collect(Collectors.groupingBy(CodeElement::getType));

        Map<CodeElement.ElementType, List<CodeElement>> referenceByType = referenceElements.stream()
            .collect(Collectors.groupingBy(CodeElement::getType));

        // Match elements of the same type
        for (CodeElement.ElementType type : CodeElement.ElementType.values()) {
            List<CodeElement> studentOfType = studentByType.getOrDefault(type, new ArrayList<>());
            List<CodeElement> referenceOfType = referenceByType.getOrDefault(type, new ArrayList<>());

            matches.addAll(matchElementsOfType(studentOfType, referenceOfType));
        }

        // Update match status for all elements
        updateElementMatchStatus(studentElements, referenceElements, matches);

        return matches;
    }

    /**
     * Match elements of the same type using similarity algorithms
     */
    private List<ComparisonMatch> matchElementsOfType(
            List<CodeElement> studentElements, 
            List<CodeElement> referenceElements) {
        
        List<ComparisonMatch> matches = new ArrayList<>();
        List<CodeElement> unmatchedStudent = new ArrayList<>(studentElements);
        List<CodeElement> unmatchedReference = new ArrayList<>(referenceElements);

        // First pass: Find exact matches
        for (CodeElement studentElement : new ArrayList<>(unmatchedStudent)) {
            for (CodeElement referenceElement : new ArrayList<>(unmatchedReference)) {
                if (isExactMatch(studentElement, referenceElement)) {
                    ComparisonMatch match = createMatch(studentElement, referenceElement, 1.0);
                    matches.add(match);
                    unmatchedStudent.remove(studentElement);
                    unmatchedReference.remove(referenceElement);
                    break;
                }
            }
        }

        // Second pass: Find similar matches using similarity algorithms
        for (CodeElement studentElement : new ArrayList<>(unmatchedStudent)) {
            CodeElement bestMatch = null;
            double bestSimilarity = 0.0;

            for (CodeElement referenceElement : unmatchedReference) {
                double similarity = calculateElementSimilarity(studentElement, referenceElement);
                
                if (similarity >= similarityThreshold && similarity > bestSimilarity) {
                    bestMatch = referenceElement;
                    bestSimilarity = similarity;
                }
            }

            if (bestMatch != null) {
                ComparisonMatch match = createMatch(studentElement, bestMatch, bestSimilarity);
                matches.add(match);
                unmatchedStudent.remove(studentElement);
                unmatchedReference.remove(bestMatch);
            }
        }

        return matches;
    }

    /**
     * Check if two elements are exact matches
     */
    private boolean isExactMatch(CodeElement student, CodeElement reference) {
        return student.getSignature().equals(reference.getSignature()) &&
               student.getName().equals(reference.getName());
    }

    /**
     * Calculate similarity between two code elements
     */
    private double calculateElementSimilarity(CodeElement student, CodeElement reference) {
        return similarityService.calculateSimilarity(
            student.getSignature(), reference.getSignature(),
            student.getName(), reference.getName(),
            student.getSourceCode(), reference.getSourceCode()
        );
    }

    /**
     * Create a comparison match with differences analysis
     */
    private ComparisonMatch createMatch(CodeElement studentElement, CodeElement referenceElement, double similarity) {
        // Find specific differences
        List<String> differences = similarityService.findDifferences(
            studentElement.getSignature(), 
            referenceElement.getSignature(),
            studentElement.getType().name()
        );

        return ComparisonMatch.builder()
            .studentElement(studentElement)
            .referenceElement(referenceElement)
            .similarity(similarity)
            .differences(differences)
            .build();
    }

    /**
     * Update match status for all elements based on comparison results
     */
    private void updateElementMatchStatus(
            List<CodeElement> studentElements, 
            List<CodeElement> referenceElements, 
            List<ComparisonMatch> matches) {
        
        // Mark matched elements
        for (ComparisonMatch match : matches) {
            CodeElement studentElement = match.getStudentElement();
            CodeElement referenceElement = match.getReferenceElement();
            
            studentElement.setMatched(true);
            referenceElement.setMatched(true);
            
            if (match.getSimilarity() >= 0.95) {
                studentElement.setMatchType(CodeElement.MatchType.EXACT);
                referenceElement.setMatchType(CodeElement.MatchType.EXACT);
            } else {
                studentElement.setMatchType(CodeElement.MatchType.SIMILAR);
                referenceElement.setMatchType(CodeElement.MatchType.SIMILAR);
            }
            
            studentElement.setSimilarity(match.getSimilarity());
            referenceElement.setSimilarity(match.getSimilarity());
        }

        // Mark unmatched student elements as EXTRA
        studentElements.stream()
            .filter(element -> !element.getMatched())
            .forEach(element -> {
                element.setMatchType(CodeElement.MatchType.EXTRA);
                element.setSimilarity(0.0);
            });

        // Mark unmatched reference elements as MISSING
        referenceElements.stream()
            .filter(element -> !element.getMatched())
            .forEach(element -> {
                element.setMatchType(CodeElement.MatchType.MISSING);
                element.setSimilarity(0.0);
            });
    }

    /**
     * Calculate overall similarity score
     */
    private double calculateOverallSimilarity(List<ComparisonMatch> matches, int totalStudentElements, int totalReferenceElements) {
        if (matches.isEmpty() || totalStudentElements == 0) {
            return 0.0;
        }

        // Calculate weighted average similarity
        double totalSimilarity = matches.stream()
            .mapToDouble(ComparisonMatch::getSimilarity)
            .sum();

        double averageSimilarity = totalSimilarity / matches.size();
        
        // Adjust for coverage (how many elements were matched)
        double coverage = (double) matches.size() / Math.max(totalStudentElements, totalReferenceElements);
        
        // Combine similarity and coverage
        return averageSimilarity * coverage;
    }

    /**
     * Get comparison result by grading session ID
     */
    public ComparisonResult getComparisonResult(Long gradingSessionId) {
        return comparisonResultRepository.findByGradingSessionId(gradingSessionId)
            .orElseThrow(() -> new RuntimeException("Comparison result not found for session: " + gradingSessionId));
    }

    /**
     * Get unmatched elements from student code
     */
    public List<CodeElement> getUnmatchedStudentElements(ComparisonResult comparison) {
        return comparison.getStudentCodeStructures().stream()
            .flatMap(structure -> structure.getElements().stream())
            .filter(element -> element.getMatchType() == CodeElement.MatchType.EXTRA)
            .collect(Collectors.toList());
    }

    /**
     * Get missing elements (present in reference but not in student code)
     */
    public List<CodeElement> getMissingElements(ComparisonResult comparison) {
        return comparison.getReferenceCodeStructures().stream()
            .flatMap(structure -> structure.getElements().stream())
            .filter(element -> element.getMatchType() == CodeElement.MatchType.MISSING)
            .collect(Collectors.toList());
    }
} 