package com.diffgrader.service;

import com.diffgrader.entity.CodeElement;
import com.diffgrader.entity.Feedback;
import com.diffgrader.entity.GradingSession;
import com.diffgrader.repository.CodeElementRepository;
import com.diffgrader.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing feedback and scoring for code elements
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CodeElementRepository codeElementRepository;
    private final GradingSessionService gradingSessionService;

    /**
     * Submit feedback for a code element
     */
    public Feedback submitFeedback(Long sessionId, Long codeElementId, 
                                 Integer score, String comments, 
                                 String designPatternFeedback, String bestPracticesFeedback) {
        log.info("Submitting feedback for session: {}, element: {}, score: {}", 
            sessionId, codeElementId, score);

        // Validate inputs
        validateFeedbackData(score, comments, designPatternFeedback, bestPracticesFeedback);

        // Get grading session and code element
        GradingSession session = gradingSessionService.getGradingSession(sessionId);
        CodeElement codeElement = codeElementRepository.findById(codeElementId)
            .orElseThrow(() -> new RuntimeException("Code element not found: " + codeElementId));

        // Check if feedback already exists
        Optional<Feedback> existingFeedback = feedbackRepository
            .findByGradingSessionIdAndCodeElementId(sessionId, codeElementId);

        if (existingFeedback.isPresent()) {
            throw new IllegalStateException("Feedback already exists for this element. Use update instead.");
        }

        // Create new feedback
        Feedback feedback = Feedback.builder()
            .gradingSession(session)
            .codeElement(codeElement)
            .score(score)
            .comments(comments)
            .designPatternFeedback(designPatternFeedback)
            .bestPracticesFeedback(bestPracticesFeedback)
            .build();

        feedback = feedbackRepository.save(feedback);

        log.info("Feedback submitted successfully: {}", feedback.getId());

        return feedback;
    }

    /**
     * Update existing feedback
     */
    public Feedback updateFeedback(Long sessionId, Long feedbackId, 
                                 Integer score, String comments, 
                                 String designPatternFeedback, String bestPracticesFeedback) {
        log.info("Updating feedback: {} for session: {}", feedbackId, sessionId);

        // Validate inputs
        validateFeedbackData(score, comments, designPatternFeedback, bestPracticesFeedback);

        // Get existing feedback
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new RuntimeException("Feedback not found: " + feedbackId));

        // Verify feedback belongs to the session
        if (!feedback.getGradingSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Feedback does not belong to the specified session");
        }

        // Update feedback
        if (score != null) feedback.setScore(score);
        if (comments != null) feedback.setComments(comments);
        if (designPatternFeedback != null) feedback.setDesignPatternFeedback(designPatternFeedback);
        if (bestPracticesFeedback != null) feedback.setBestPracticesFeedback(bestPracticesFeedback);

        feedback = feedbackRepository.save(feedback);

        log.info("Feedback updated successfully: {}", feedback.getId());

        return feedback;
    }

    /**
     * Get all feedbacks for a grading session
     */
    public List<Feedback> getFeedbacksForSession(Long sessionId) {
        return feedbackRepository.findByGradingSessionId(sessionId);
    }

    /**
     * Get specific feedback by ID
     */
    public Feedback getFeedback(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new RuntimeException("Feedback not found: " + feedbackId));
    }

    /**
     * Get feedback for a specific code element in a session
     */
    public Optional<Feedback> getFeedbackForElement(Long sessionId, Long codeElementId) {
        return feedbackRepository.findByGradingSessionIdAndCodeElementId(sessionId, codeElementId);
    }

    /**
     * Delete feedback
     */
    public void deleteFeedback(Long feedbackId) {
        log.info("Deleting feedback: {}", feedbackId);
        
        Feedback feedback = getFeedback(feedbackId);
        feedbackRepository.delete(feedback);
        
        log.info("Feedback deleted successfully: {}", feedbackId);
    }

    /**
     * Calculate average score for a session
     */
    public Double getAverageScoreForSession(Long sessionId) {
        return feedbackRepository.getAverageScoreForSession(sessionId);
    }

    /**
     * Get feedbacks by score range
     */
    public List<Feedback> getFeedbacksByScoreRange(Integer minScore, Integer maxScore) {
        return feedbackRepository.findByScoreRange(minScore, maxScore);
    }

    /**
     * Get high score feedbacks (>= 80)
     */
    public List<Feedback> getHighScoreFeedbacks() {
        return feedbackRepository.findHighScoreFeedbacks();
    }

    /**
     * Get low score feedbacks (< 60)
     */
    public List<Feedback> getLowScoreFeedbacks() {
        return feedbackRepository.findLowScoreFeedbacks();
    }

    /**
     * Get feedbacks with comments
     */
    public List<Feedback> getFeedbacksWithComments() {
        return feedbackRepository.findFeedbacksWithComments();
    }

    /**
     * Get feedbacks with design pattern feedback
     */
    public List<Feedback> getFeedbacksWithDesignPatterns() {
        return feedbackRepository.findFeedbacksWithDesignPatterns();
    }

    /**
     * Get feedbacks with best practices feedback
     */
    public List<Feedback> getFeedbacksWithBestPractices() {
        return feedbackRepository.findFeedbacksWithBestPractices();
    }

    /**
     * Get overall average score across all feedbacks
     */
    public Double getOverallAverageScore() {
        return feedbackRepository.getOverallAverageScore();
    }

    /**
     * Count feedbacks for a session
     */
    public long countFeedbacksForSession(Long sessionId) {
        return feedbackRepository.countByGradingSession(sessionId);
    }

    /**
     * Generate automatic feedback suggestions based on code analysis
     */
    public FeedbackSuggestion generateFeedbackSuggestions(CodeElement codeElement, Double similarity) {
        log.debug("Generating feedback suggestions for element: {}", codeElement.getName());

        FeedbackSuggestion suggestion = new FeedbackSuggestion();

        // Suggest score based on similarity and match type
        suggestion.suggestedScore = calculateSuggestedScore(codeElement, similarity);

        // Generate comments based on element type and match status
        suggestion.suggestedComments = generateComments(codeElement, similarity);

        // Generate design pattern feedback
        suggestion.suggestedDesignPatternFeedback = generateDesignPatternFeedback(codeElement);

        // Generate best practices feedback
        suggestion.suggestedBestPracticesFeedback = generateBestPracticesFeedback(codeElement);

        return suggestion;
    }

    /**
     * Calculate suggested score based on similarity and match type
     */
    private Integer calculateSuggestedScore(CodeElement codeElement, Double similarity) {
        if (similarity == null) {
            similarity = 0.0;
        }

        int baseScore = (int) Math.round(similarity * 100);

        // Adjust based on match type
        switch (codeElement.getMatchType()) {
            case EXACT:
                return Math.min(100, baseScore + 10);
            case SIMILAR:
                return baseScore;
            case EXTRA:
                return Math.max(0, baseScore - 20); // Extra code might be unnecessary
            case MISSING:
                return 0; // Missing elements get zero
            default:
                return baseScore;
        }
    }

    /**
     * Generate suggested comments
     */
    private String generateComments(CodeElement codeElement, Double similarity) {
        StringBuilder comments = new StringBuilder();

        switch (codeElement.getMatchType()) {
            case EXACT:
                comments.append("Perfect match with reference implementation. ");
                break;
            case SIMILAR:
                comments.append(String.format("Good implementation with %.0f%% similarity to reference. ", 
                    similarity * 100));
                if (similarity < 0.8) {
                    comments.append("Consider reviewing the implementation for closer alignment. ");
                }
                break;
            case EXTRA:
                comments.append("Additional element not found in reference. ");
                comments.append("Evaluate if this is necessary or could be refactored. ");
                break;
            case MISSING:
                comments.append("This element is missing from your implementation. ");
                comments.append("Please ensure all required functionality is implemented. ");
                break;
        }

        // Add element-specific comments
        switch (codeElement.getType()) {
            case CLASS:
                comments.append("Class structure and organization are important for maintainability.");
                break;
            case METHOD:
                comments.append("Method implementation should follow single responsibility principle.");
                break;
            case FIELD:
                comments.append("Field declarations should follow proper encapsulation principles.");
                break;
            case CONSTRUCTOR:
                comments.append("Constructor should properly initialize all necessary fields.");
                break;
        }

        return comments.toString().trim();
    }

    /**
     * Generate design pattern feedback
     */
    private String generateDesignPatternFeedback(CodeElement codeElement) {
        switch (codeElement.getType()) {
            case CLASS:
                return "Consider applying appropriate design patterns like Singleton, Factory, or Strategy pattern where suitable.";
            case METHOD:
                return "Ensure methods follow SOLID principles, especially Single Responsibility and Open/Closed principles.";
            case FIELD:
                return "Consider using private fields with appropriate getters/setters for proper encapsulation.";
            case CONSTRUCTOR:
                return "Constructor should use dependency injection pattern where appropriate.";
            default:
                return "Apply relevant design patterns to improve code structure and maintainability.";
        }
    }

    /**
     * Generate best practices feedback
     */
    private String generateBestPracticesFeedback(CodeElement codeElement) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("Follow Java naming conventions: ");
        
        switch (codeElement.getType()) {
            case CLASS:
                feedback.append("class names should be PascalCase. ");
                break;
            case METHOD:
                feedback.append("method names should be camelCase and start with a verb. ");
                break;
            case FIELD:
                feedback.append("field names should be camelCase. ");
                break;
            case CONSTRUCTOR:
                feedback.append("constructor should validate inputs and handle edge cases. ");
                break;
        }
        
        feedback.append("Add meaningful comments for complex logic. ");
        feedback.append("Ensure proper error handling and input validation.");
        
        return feedback.toString();
    }

    /**
     * Validate feedback data
     */
    private void validateFeedbackData(Integer score, String comments, 
                                    String designPatternFeedback, String bestPracticesFeedback) {
        if (score != null && (score < 0 || score > 100)) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }

        if (comments != null && comments.length() > 2000) {
            throw new IllegalArgumentException("Comments cannot exceed 2000 characters");
        }

        if (designPatternFeedback != null && designPatternFeedback.length() > 1000) {
            throw new IllegalArgumentException("Design pattern feedback cannot exceed 1000 characters");
        }

        if (bestPracticesFeedback != null && bestPracticesFeedback.length() > 1000) {
            throw new IllegalArgumentException("Best practices feedback cannot exceed 1000 characters");
        }
    }

    /**
     * Feedback suggestion data class
     */
    public static class FeedbackSuggestion {
        public Integer suggestedScore;
        public String suggestedComments;
        public String suggestedDesignPatternFeedback;
        public String suggestedBestPracticesFeedback;

        // Getters
        public Integer getSuggestedScore() { return suggestedScore; }
        public String getSuggestedComments() { return suggestedComments; }
        public String getSuggestedDesignPatternFeedback() { return suggestedDesignPatternFeedback; }
        public String getSuggestedBestPracticesFeedback() { return suggestedBestPracticesFeedback; }
    }
} 