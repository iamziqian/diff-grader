package com.diffgrader.controller;

import com.diffgrader.entity.Feedback;
import com.diffgrader.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for feedback management
 */
@RestController
@RequestMapping("/grading-sessions/{sessionId}/feedback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback Management", description = "APIs for managing feedback and scoring")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit feedback for a code element
     */
    @PostMapping
    @Operation(summary = "Submit feedback", description = "Submit feedback for a specific code element")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Valid @RequestBody SubmitFeedbackRequest request) {
        
        log.info("Submitting feedback for session: {}, element: {}", sessionId, request.getElementId());
        
        try {
            Feedback feedback = feedbackService.submitFeedback(
                sessionId,
                request.getElementId(),
                request.getScore(),
                request.getComments(),
                request.getDesignPatternFeedback(),
                request.getBestPracticesFeedback()
            );

            Map<String, Object> response = createSuccessResponse("Feedback submitted successfully");
            response.put("data", createFeedbackInfo(feedback));

            log.info("Feedback submitted: {}", feedback.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.warn("Feedback already exists: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (IllegalArgumentException e) {
            log.warn("Invalid feedback data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (RuntimeException e) {
            log.warn("Session or element not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error submitting feedback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to submit feedback: " + e.getMessage()));
        }
    }

    /**
     * Update existing feedback
     */
    @PutMapping("/{feedbackId}")
    @Operation(summary = "Update feedback", description = "Update existing feedback")
    public ResponseEntity<Map<String, Object>> updateFeedback(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Feedback ID", required = true)
            @PathVariable Long feedbackId,
            
            @Valid @RequestBody UpdateFeedbackRequest request) {
        
        log.info("Updating feedback: {} for session: {}", feedbackId, sessionId);
        
        try {
            Feedback feedback = feedbackService.updateFeedback(
                sessionId,
                feedbackId,
                request.getScore(),
                request.getComments(),
                request.getDesignPatternFeedback(),
                request.getBestPracticesFeedback()
            );

            Map<String, Object> response = createSuccessResponse("Feedback updated successfully");
            response.put("data", createFeedbackInfo(feedback));

            log.info("Feedback updated: {}", feedbackId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid feedback data or session mismatch: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
                
        } catch (RuntimeException e) {
            log.warn("Feedback not found: {}", feedbackId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error updating feedback: {}", feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to update feedback: " + e.getMessage()));
        }
    }

    /**
     * Get all feedbacks for a grading session
     */
    @GetMapping
    @Operation(summary = "Get all feedbacks", description = "Retrieve all feedbacks for a grading session")
    public ResponseEntity<Map<String, Object>> getAllFeedbacks(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            List<Feedback> feedbacks = feedbackService.getFeedbacksForSession(sessionId);

            Map<String, Object> response = createSuccessResponse("Feedbacks retrieved successfully");
            response.put("data", feedbacks.stream()
                .map(this::createFeedbackInfo)
                .toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving feedbacks for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve feedbacks: " + e.getMessage()));
        }
    }

    /**
     * Get specific feedback by ID
     */
    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get feedback", description = "Retrieve specific feedback by ID")
    public ResponseEntity<Map<String, Object>> getFeedback(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Feedback ID", required = true)
            @PathVariable Long feedbackId) {
        
        try {
            Feedback feedback = feedbackService.getFeedback(feedbackId);

            // Verify feedback belongs to the session
            if (!feedback.getGradingSession().getId().equals(sessionId)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Feedback does not belong to the specified session"));
            }

            Map<String, Object> response = createSuccessResponse("Feedback retrieved successfully");
            response.put("data", createFeedbackInfo(feedback));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Feedback not found: {}", feedbackId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error retrieving feedback: {}", feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve feedback: " + e.getMessage()));
        }
    }

    /**
     * Get feedback for a specific code element
     */
    @GetMapping("/element/{elementId}")
    @Operation(summary = "Get feedback for element", description = "Retrieve feedback for a specific code element")
    public ResponseEntity<Map<String, Object>> getFeedbackForElement(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Element ID", required = true)
            @PathVariable Long elementId) {
        
        try {
            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackForElement(sessionId, elementId);

            if (feedbackOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = createSuccessResponse("Feedback retrieved successfully");
            response.put("data", createFeedbackInfo(feedbackOpt.get()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving feedback for element: {} in session: {}", elementId, sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve feedback: " + e.getMessage()));
        }
    }

    /**
     * Delete feedback
     */
    @DeleteMapping("/{feedbackId}")
    @Operation(summary = "Delete feedback", description = "Delete feedback by ID")
    public ResponseEntity<Map<String, Object>> deleteFeedback(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Feedback ID", required = true)
            @PathVariable Long feedbackId) {
        
        try {
            // Verify feedback belongs to session before deletion
            Feedback feedback = feedbackService.getFeedback(feedbackId);
            if (!feedback.getGradingSession().getId().equals(sessionId)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Feedback does not belong to the specified session"));
            }

            feedbackService.deleteFeedback(feedbackId);

            Map<String, Object> response = createSuccessResponse("Feedback deleted successfully");
            log.info("Feedback deleted: {}", feedbackId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Feedback not found: {}", feedbackId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting feedback: {}", feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to delete feedback: " + e.getMessage()));
        }
    }

    /**
     * Get session average score
     */
    @GetMapping("/average-score")
    @Operation(summary = "Get average score", description = "Get average score for the session")
    public ResponseEntity<Map<String, Object>> getAverageScore(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId) {
        
        try {
            Double averageScore = feedbackService.getAverageScoreForSession(sessionId);

            Map<String, Object> response = createSuccessResponse("Average score retrieved successfully");
            response.put("data", Map.of("averageScore", averageScore != null ? averageScore : 0.0));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving average score for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve average score: " + e.getMessage()));
        }
    }

    /**
     * Generate feedback suggestions for a code element
     */
    @PostMapping("/element/{elementId}/suggestions")
    @Operation(summary = "Generate feedback suggestions", description = "Generate automatic feedback suggestions for a code element")
    public ResponseEntity<Map<String, Object>> generateFeedbackSuggestions(
            @Parameter(description = "Session ID", required = true)
            @PathVariable Long sessionId,
            
            @Parameter(description = "Element ID", required = true)
            @PathVariable Long elementId,
            
            @RequestBody(required = false) Map<String, Object> requestBody) {
        
        try {
            // Extract similarity from request body if provided
            Double similarity = null;
            if (requestBody != null && requestBody.containsKey("similarity")) {
                similarity = ((Number) requestBody.get("similarity")).doubleValue();
            }

            // This would require getting the code element first
            // For now, return a placeholder response
            Map<String, Object> suggestions = new HashMap<>();
            suggestions.put("suggestedScore", 75);
            suggestions.put("suggestedComments", "Generated feedback based on code analysis");
            suggestions.put("suggestedDesignPatternFeedback", "Consider applying appropriate design patterns");
            suggestions.put("suggestedBestPracticesFeedback", "Follow Java coding standards and best practices");

            Map<String, Object> response = createSuccessResponse("Feedback suggestions generated successfully");
            response.put("data", suggestions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating feedback suggestions for element: {} in session: {}", elementId, sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to generate suggestions: " + e.getMessage()));
        }
    }

    /**
     * Create feedback information object
     */
    private Map<String, Object> createFeedbackInfo(Feedback feedback) {
        Map<String, Object> feedbackInfo = new HashMap<>();
        feedbackInfo.put("id", feedback.getId());
        feedbackInfo.put("score", feedback.getScore());
        feedbackInfo.put("comments", feedback.getComments());
        feedbackInfo.put("designPatternFeedback", feedback.getDesignPatternFeedback());
        feedbackInfo.put("bestPracticesFeedback", feedback.getBestPracticesFeedback());
        feedbackInfo.put("createdAt", feedback.getCreatedAt());
        feedbackInfo.put("updatedAt", feedback.getUpdatedAt());
        
        // Code element info
        if (feedback.getCodeElement() != null) {
            Map<String, Object> elementInfo = new HashMap<>();
            elementInfo.put("id", feedback.getCodeElement().getId());
            elementInfo.put("name", feedback.getCodeElement().getName());
            elementInfo.put("type", feedback.getCodeElement().getType());
            elementInfo.put("signature", feedback.getCodeElement().getSignature());
            feedbackInfo.put("codeElement", elementInfo);
        }
        
        return feedbackInfo;
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
    public static class SubmitFeedbackRequest {
        @NotNull(message = "Element ID is required")
        private Long elementId;
        
        @NotNull(message = "Score is required")
        @Min(value = 0, message = "Score must be between 0 and 100")
        @Max(value = 100, message = "Score must be between 0 and 100")
        private Integer score;
        
        private String comments;
        private String designPatternFeedback;
        private String bestPracticesFeedback;

        // Getters and setters
        public Long getElementId() { return elementId; }
        public void setElementId(Long elementId) { this.elementId = elementId; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getDesignPatternFeedback() { return designPatternFeedback; }
        public void setDesignPatternFeedback(String designPatternFeedback) { this.designPatternFeedback = designPatternFeedback; }
        public String getBestPracticesFeedback() { return bestPracticesFeedback; }
        public void setBestPracticesFeedback(String bestPracticesFeedback) { this.bestPracticesFeedback = bestPracticesFeedback; }
    }

    public static class UpdateFeedbackRequest {
        @Min(value = 0, message = "Score must be between 0 and 100")
        @Max(value = 100, message = "Score must be between 0 and 100")
        private Integer score;
        
        private String comments;
        private String designPatternFeedback;
        private String bestPracticesFeedback;

        // Getters and setters
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getDesignPatternFeedback() { return designPatternFeedback; }
        public void setDesignPatternFeedback(String designPatternFeedback) { this.designPatternFeedback = designPatternFeedback; }
        public String getBestPracticesFeedback() { return bestPracticesFeedback; }
        public void setBestPracticesFeedback(String bestPracticesFeedback) { this.bestPracticesFeedback = bestPracticesFeedback; }
    }
} 