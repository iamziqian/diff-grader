package com.diffgrader.controller;

import com.diffgrader.entity.GradingSession;
import com.diffgrader.service.GradingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for grading session management
 */
@RestController
@RequestMapping("/grading-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Grading Sessions", description = "APIs for managing grading sessions")
public class GradingSessionController {

    private final GradingSessionService gradingSessionService;

    /**
     * Create a new grading session
     */
    @PostMapping
    @Operation(summary = "Create grading session", description = "Create new grading session with student and reference files")
    public ResponseEntity<Map<String, Object>> createGradingSession(
            @Valid @RequestBody CreateGradingSessionRequest request) {
        
        log.info("Creating grading session with student file: {} and reference file: {}", 
            request.getStudentFileId(), request.getReferenceFileId());
        
        try {
            GradingSession session = gradingSessionService.createGradingSession(
                request.getStudentFileId(), 
                request.getReferenceFileId()
            );

            Map<String, Object> response = createSuccessResponse("Grading session created successfully");
            response.put("data", createSessionInfo(session));

            log.info("Grading session created: {}", session.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for grading session creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Failed to create grading session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to create grading session: " + e.getMessage()));
        }
    }

    /**
     * Get grading session by ID
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get grading session", description = "Retrieve grading session by ID")
    public ResponseEntity<Map<String, Object>> getGradingSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            GradingSession session = gradingSessionService.getGradingSession(sessionId);

            Map<String, Object> response = createSuccessResponse("Grading session retrieved successfully");
            response.put("data", createSessionInfo(session));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Grading session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving grading session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve grading session: " + e.getMessage()));
        }
    }

    /**
     * Get all grading sessions with pagination
     */
    @GetMapping
    @Operation(summary = "Get all grading sessions", description = "Retrieve all grading sessions with pagination")
    public ResponseEntity<Map<String, Object>> getAllGradingSessions(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (ASC/DESC)")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<GradingSession> sessionsPage = gradingSessionService.getAllGradingSessions(pageable);

            Map<String, Object> response = createSuccessResponse("Grading sessions retrieved successfully");
            response.put("data", sessionsPage.getContent().stream()
                .map(this::createSessionInfo)
                .toList());
            response.put("totalElements", sessionsPage.getTotalElements());
            response.put("totalPages", sessionsPage.getTotalPages());
            response.put("currentPage", sessionsPage.getNumber());
            response.put("size", sessionsPage.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving grading sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve grading sessions: " + e.getMessage()));
        }
    }

    /**
     * Complete a grading session
     */
    @PutMapping("/{sessionId}/complete")
    @Operation(summary = "Complete grading session", description = "Mark grading session as completed with final score and comments")
    public ResponseEntity<Map<String, Object>> completeGradingSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Valid @RequestBody CompleteGradingSessionRequest request) {
        
        log.info("Completing grading session: {} with score: {}", sessionId, request.getOverallScore());
        
        try {
            GradingSession session = gradingSessionService.completeGradingSession(
                sessionId, 
                request.getOverallScore(), 
                request.getFinalComments()
            );

            Map<String, Object> response = createSuccessResponse("Grading session completed successfully");
            response.put("data", createSessionInfo(session));

            log.info("Grading session completed: {}", sessionId);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Invalid request for completing grading session: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (RuntimeException e) {
            log.warn("Grading session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error completing grading session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to complete grading session: " + e.getMessage()));
        }
    }

    /**
     * Delete grading session
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete grading session", description = "Delete grading session and all related data")
    public ResponseEntity<Map<String, Object>> deleteGradingSession(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            gradingSessionService.deleteGradingSession(sessionId);

            Map<String, Object> response = createSuccessResponse("Grading session deleted successfully");
            log.info("Grading session deleted: {}", sessionId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Grading session not found for deletion: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting grading session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to delete grading session: " + e.getMessage()));
        }
    }

    /**
     * Get session statistics
     */
    @GetMapping("/{sessionId}/statistics")
    @Operation(summary = "Get session statistics", description = "Retrieve detailed statistics for a grading session")
    public ResponseEntity<Map<String, Object>> getSessionStatistics(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            GradingSessionService.SessionStatistics statistics = 
                gradingSessionService.getSessionStatistics(sessionId);

            Map<String, Object> response = createSuccessResponse("Session statistics retrieved successfully");
            response.put("data", statistics);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Session or comparison not found: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving session statistics: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve session statistics: " + e.getMessage()));
        }
    }

    /**
     * Restart analysis for a session
     */
    @PostMapping("/{sessionId}/restart")
    @Operation(summary = "Restart analysis", description = "Restart code analysis for a grading session")
    public ResponseEntity<Map<String, Object>> restartAnalysis(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            GradingSession session = gradingSessionService.restartAnalysis(sessionId);

            Map<String, Object> response = createSuccessResponse("Analysis restarted successfully");
            response.put("data", createSessionInfo(session));

            log.info("Analysis restarted for session: {}", sessionId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Grading session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error restarting analysis: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to restart analysis: " + e.getMessage()));
        }
    }

    /**
     * Get grading sessions by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get sessions by status", description = "Retrieve grading sessions by status")
    public ResponseEntity<Map<String, Object>> getGradingSessionsByStatus(
            @Parameter(description = "Session status", required = true)
            @PathVariable String status) {
        
        try {
            GradingSession.SessionStatus sessionStatus = GradingSession.SessionStatus.valueOf(status.toUpperCase());
            List<GradingSession> sessions = gradingSessionService.getGradingSessionsByStatus(sessionStatus);

            Map<String, Object> response = createSuccessResponse("Sessions retrieved successfully");
            response.put("data", sessions.stream()
                .map(this::createSessionInfo)
                .toList());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid status. Must be one of: CREATED, ANALYZING, READY, COMPLETED"));
                
        } catch (Exception e) {
            log.error("Error retrieving sessions by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve sessions: " + e.getMessage()));
        }
    }

    /**
     * Create session information object
     */
    private Map<String, Object> createSessionInfo(GradingSession session) {
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("id", session.getId());
        sessionInfo.put("status", session.getStatus());
        sessionInfo.put("createdAt", session.getCreatedAt());
        sessionInfo.put("updatedAt", session.getUpdatedAt());
        
        // Student file info
        if (session.getStudentFile() != null) {
            Map<String, Object> studentFile = new HashMap<>();
            studentFile.put("id", session.getStudentFile().getId());
            studentFile.put("name", session.getStudentFile().getOriginalName());
            studentFile.put("status", session.getStudentFile().getStatus());
            sessionInfo.put("studentFile", studentFile);
        }
        
        // Reference file info
        if (session.getReferenceFile() != null) {
            Map<String, Object> referenceFile = new HashMap<>();
            referenceFile.put("id", session.getReferenceFile().getId());
            referenceFile.put("name", session.getReferenceFile().getOriginalName());
            referenceFile.put("status", session.getReferenceFile().getStatus());
            sessionInfo.put("referenceFile", referenceFile);
        }
        
        // Comparison info
        if (session.getComparison() != null) {
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("id", session.getComparison().getId());
            comparison.put("overallSimilarity", session.getComparison().getOverallSimilarity());
            comparison.put("totalStudentElements", session.getComparison().getTotalStudentElements());
            comparison.put("totalReferenceElements", session.getComparison().getTotalReferenceElements());
            comparison.put("matchedElements", session.getComparison().getMatchedElements());
            sessionInfo.put("comparison", comparison);
        }
        
        // Score and comments
        if (session.getOverallScore() != null) {
            sessionInfo.put("overallScore", session.getOverallScore());
        }
        if (session.getFinalComments() != null) {
            sessionInfo.put("finalComments", session.getFinalComments());
        }
        
        return sessionInfo;
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
     * Request DTOs
     */
    public static class CreateGradingSessionRequest {
        @NotNull(message = "Student file ID is required")
        private Long studentFileId;
        
        @NotNull(message = "Reference file ID is required")
        private Long referenceFileId;

        // Getters and setters
        public Long getStudentFileId() { return studentFileId; }
        public void setStudentFileId(Long studentFileId) { this.studentFileId = studentFileId; }
        public Long getReferenceFileId() { return referenceFileId; }
        public void setReferenceFileId(Long referenceFileId) { this.referenceFileId = referenceFileId; }
    }

    public static class CompleteGradingSessionRequest {
        @NotNull(message = "Overall score is required")
        private Integer overallScore;
        
        private String finalComments;

        // Getters and setters
        public Integer getOverallScore() { return overallScore; }
        public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
        public String getFinalComments() { return finalComments; }
        public void setFinalComments(String finalComments) { this.finalComments = finalComments; }
    }
} 