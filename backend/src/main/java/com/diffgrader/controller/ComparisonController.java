package com.diffgrader.controller;

import com.diffgrader.entity.*;
import com.diffgrader.service.ComparisonService;
import com.diffgrader.service.GradingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for code comparison operations
 */
@RestController
@RequestMapping("/grading-sessions/{sessionId}/comparison")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Code Comparison", description = "APIs for code comparison and analysis")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final GradingSessionService gradingSessionService;

    /**
     * Get comparison result for a grading session
     */
    @GetMapping
    @Operation(summary = "Get comparison result", description = "Retrieve code comparison result for a grading session")
    public ResponseEntity<Map<String, Object>> getComparisonResult(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            // Check if session is ready
            if (!gradingSessionService.isSessionReady(sessionId)) {
                GradingSession session = gradingSessionService.getGradingSession(sessionId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Comparison not ready. Current status: " + session.getStatus()));
            }

            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);

            Map<String, Object> response = createSuccessResponse("Comparison result retrieved successfully");
            response.put("data", createComparisonInfo(comparison));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving comparison result: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve comparison result: " + e.getMessage()));
        }
    }

    /**
     * Get matched elements from comparison
     */
    @GetMapping("/matches")
    @Operation(summary = "Get matched elements", description = "Retrieve all matched elements from comparison")
    public ResponseEntity<Map<String, Object>> getMatchedElements(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);

            List<Map<String, Object>> matches = comparison.getMatches().stream()
                .map(this::createMatchInfo)
                .collect(Collectors.toList());

            Map<String, Object> response = createSuccessResponse("Matched elements retrieved successfully");
            response.put("data", matches);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving matched elements: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve matched elements: " + e.getMessage()));
        }
    }

    /**
     * Get unmatched student elements (extra elements)
     */
    @GetMapping("/unmatched-student")
    @Operation(summary = "Get unmatched student elements", description = "Retrieve student elements that have no match in reference")
    public ResponseEntity<Map<String, Object>> getUnmatchedStudentElements(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);
            List<CodeElement> unmatchedElements = comparisonService.getUnmatchedStudentElements(comparison);

            List<Map<String, Object>> elements = unmatchedElements.stream()
                .map(this::createElementInfo)
                .collect(Collectors.toList());

            Map<String, Object> response = createSuccessResponse("Unmatched student elements retrieved successfully");
            response.put("data", elements);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving unmatched student elements: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve unmatched elements: " + e.getMessage()));
        }
    }

    /**
     * Get missing elements (present in reference but not in student)
     */
    @GetMapping("/missing-elements")
    @Operation(summary = "Get missing elements", description = "Retrieve reference elements missing from student code")
    public ResponseEntity<Map<String, Object>> getMissingElements(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);
            List<CodeElement> missingElements = comparisonService.getMissingElements(comparison);

            List<Map<String, Object>> elements = missingElements.stream()
                .map(this::createElementInfo)
                .collect(Collectors.toList());

            Map<String, Object> response = createSuccessResponse("Missing elements retrieved successfully");
            response.put("data", elements);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving missing elements: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve missing elements: " + e.getMessage()));
        }
    }

    /**
     * Get code structures (student and reference)
     */
    @GetMapping("/structures")
    @Operation(summary = "Get code structures", description = "Retrieve parsed code structures for both student and reference")
    public ResponseEntity<Map<String, Object>> getCodeStructures(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);

            Map<String, Object> structures = new HashMap<>();
            
            // Student code structures
            List<Map<String, Object>> studentStructures = comparison.getStudentCodeStructures().stream()
                .map(this::createStructureInfo)
                .collect(Collectors.toList());
            structures.put("studentCode", studentStructures);
            
            // Reference code structures
            List<Map<String, Object>> referenceStructures = comparison.getReferenceCodeStructures().stream()
                .map(this::createStructureInfo)
                .collect(Collectors.toList());
            structures.put("referenceCode", referenceStructures);

            Map<String, Object> response = createSuccessResponse("Code structures retrieved successfully");
            response.put("data", structures);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving code structures: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve code structures: " + e.getMessage()));
        }
    }

    /**
     * Get elements by type
     */
    @GetMapping("/elements/{elementType}")
    @Operation(summary = "Get elements by type", description = "Retrieve elements filtered by type (CLASS, METHOD, FIELD, CONSTRUCTOR)")
    public ResponseEntity<Map<String, Object>> getElementsByType(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Element type", required = true)
            @PathVariable String elementType) {
        
        try {
            // Validate element type
            CodeElement.ElementType type;
            try {
                type = CodeElement.ElementType.valueOf(elementType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid element type. Must be one of: CLASS, METHOD, FIELD, CONSTRUCTOR"));
            }

            ComparisonResult comparison = comparisonService.getComparisonResult(sessionId);

            // Get student elements of specified type
            List<CodeElement> studentElements = comparison.getStudentCodeStructures().stream()
                .flatMap(structure -> structure.getElements().stream())
                .filter(element -> element.getType() == type)
                .collect(Collectors.toList());

            // Get reference elements of specified type
            List<CodeElement> referenceElements = comparison.getReferenceCodeStructures().stream()
                .flatMap(structure -> structure.getElements().stream())
                .filter(element -> element.getType() == type)
                .collect(Collectors.toList());

            Map<String, Object> elements = new HashMap<>();
            elements.put("studentElements", studentElements.stream()
                .map(this::createElementInfo)
                .collect(Collectors.toList()));
            elements.put("referenceElements", referenceElements.stream()
                .map(this::createElementInfo)
                .collect(Collectors.toList()));

            Map<String, Object> response = createSuccessResponse("Elements retrieved successfully");
            response.put("data", elements);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Comparison result not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving elements by type: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve elements: " + e.getMessage()));
        }
    }

    /**
     * Create comparison information object
     */
    private Map<String, Object> createComparisonInfo(ComparisonResult comparison) {
        Map<String, Object> comparisonInfo = new HashMap<>();
        comparisonInfo.put("id", comparison.getId());
        comparisonInfo.put("overallSimilarity", comparison.getOverallSimilarity());
        comparisonInfo.put("totalStudentElements", comparison.getTotalStudentElements());
        comparisonInfo.put("totalReferenceElements", comparison.getTotalReferenceElements());
        comparisonInfo.put("matchedElements", comparison.getMatchedElements());
        comparisonInfo.put("createdAt", comparison.getCreatedAt());

        // Matches
        List<Map<String, Object>> matches = comparison.getMatches().stream()
            .map(this::createMatchInfo)
            .collect(Collectors.toList());
        comparisonInfo.put("matchedElements", matches);

        // Unmatched student elements
        List<CodeElement> unmatchedStudent = comparisonService.getUnmatchedStudentElements(comparison);
        comparisonInfo.put("unmatchedStudentElements", unmatchedStudent.stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));

        // Missing elements
        List<CodeElement> missingElements = comparisonService.getMissingElements(comparison);
        comparisonInfo.put("unmatchedReferenceElements", missingElements.stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));

        // Code structures
        comparisonInfo.put("studentCode", comparison.getStudentCodeStructures().stream()
            .map(this::createStructureInfo)
            .collect(Collectors.toList()));
        comparisonInfo.put("referenceCode", comparison.getReferenceCodeStructures().stream()
            .map(this::createStructureInfo)
            .collect(Collectors.toList()));

        return comparisonInfo;
    }

    /**
     * Create comparison match information object
     */
    private Map<String, Object> createMatchInfo(ComparisonMatch match) {
        Map<String, Object> matchInfo = new HashMap<>();
        matchInfo.put("studentElement", createElementInfo(match.getStudentElement()));
        matchInfo.put("referenceElement", createElementInfo(match.getReferenceElement()));
        matchInfo.put("similarity", match.getSimilarity());
        matchInfo.put("differences", match.getDifferences());
        return matchInfo;
    }

    /**
     * Create code element information object
     */
    private Map<String, Object> createElementInfo(CodeElement element) {
        Map<String, Object> elementInfo = new HashMap<>();
        elementInfo.put("id", element.getId());
        elementInfo.put("name", element.getName());
        elementInfo.put("type", element.getType());
        elementInfo.put("signature", element.getSignature());
        elementInfo.put("sourceCode", element.getSourceCode());
        elementInfo.put("lineNumber", element.getLineNumber());
        elementInfo.put("matched", element.getMatched());
        elementInfo.put("matchType", element.getMatchType());
        elementInfo.put("similarity", element.getSimilarity());
        return elementInfo;
    }

    /**
     * Create code structure information object
     */
    private Map<String, Object> createStructureInfo(CodeStructure structure) {
        Map<String, Object> structureInfo = new HashMap<>();
        structureInfo.put("id", structure.getId());
        structureInfo.put("fileName", structure.getFileName());
        structureInfo.put("packageName", structure.getPackageName());
        structureInfo.put("type", structure.getType());
        
        // Group elements by type
        Map<String, List<Map<String, Object>>> elementsByType = new HashMap<>();
        elementsByType.put("classes", structure.getClasses().stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));
        elementsByType.put("fields", structure.getFields().stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));
        elementsByType.put("methods", structure.getMethods().stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));
        elementsByType.put("constructors", structure.getConstructors().stream()
            .map(this::createElementInfo)
            .collect(Collectors.toList()));
        
        structureInfo.put("elements", elementsByType);
        
        return structureInfo;
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
} 