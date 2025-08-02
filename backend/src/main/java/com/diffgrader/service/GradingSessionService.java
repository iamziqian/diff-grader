package com.diffgrader.service;

import com.diffgrader.entity.*;
import com.diffgrader.repository.GradingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing grading sessions and orchestrating the entire grading workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GradingSessionService {

    private final GradingSessionRepository gradingSessionRepository;
    private final FileService fileService;
    private final ComparisonService comparisonService;

    /**
     * Create a new grading session
     */
    public GradingSession createGradingSession(Long studentFileId, Long referenceFileId) {
        log.info("Creating grading session for student file: {} and reference file: {}", 
            studentFileId, referenceFileId);

        // Get uploaded files
        UploadedFile studentFile = fileService.getFile(studentFileId);
        UploadedFile referenceFile = fileService.getFile(referenceFileId);

        // Validate file types
        if (studentFile.getType() != UploadedFile.FileType.STUDENT) {
            throw new IllegalArgumentException("First file must be a student file");
        }
        if (referenceFile.getType() != UploadedFile.FileType.REFERENCE) {
            throw new IllegalArgumentException("Second file must be a reference file");
        }

        // Create grading session
        GradingSession session = GradingSession.builder()
            .studentFile(studentFile)
            .referenceFile(referenceFile)
            .status(GradingSession.SessionStatus.CREATED)
            .build();

        session = gradingSessionRepository.save(session);

        log.info("Created grading session: {}", session.getId());

        // Start asynchronous analysis
        analyzeCodeAsync(session);

        return session;
    }

    /**
     * Asynchronously analyze code and generate comparison
     */
    @Async
    public CompletableFuture<Void> analyzeCodeAsync(GradingSession session) {
        log.info("Starting asynchronous code analysis for session: {}", session.getId());

        try {
            // Update status to analyzing
            session.setStatus(GradingSession.SessionStatus.ANALYZING);
            gradingSessionRepository.save(session);

            // Perform code comparison
            ComparisonResult comparison = comparisonService.compareCode(session);

            // Update session with comparison result
            session.setComparison(comparison);
            session.setStatus(GradingSession.SessionStatus.READY);
            gradingSessionRepository.save(session);

            // Update file statuses
            UploadedFile studentFile = session.getStudentFile();
            UploadedFile referenceFile = session.getReferenceFile();
            
            studentFile.setStatus(UploadedFile.FileStatus.COMPLETED);
            referenceFile.setStatus(UploadedFile.FileStatus.COMPLETED);

            log.info("Code analysis completed for session: {}", session.getId());

        } catch (Exception e) {
            log.error("Code analysis failed for session: {}", session.getId(), e);
            
            // Update status to error
            session.setStatus(GradingSession.SessionStatus.CREATED);
            gradingSessionRepository.save(session);

            // Update file statuses
            UploadedFile studentFile = session.getStudentFile();
            UploadedFile referenceFile = session.getReferenceFile();
            
            studentFile.setStatus(UploadedFile.FileStatus.ERROR);
            studentFile.setErrorMessage("Analysis failed: " + e.getMessage());
            referenceFile.setStatus(UploadedFile.FileStatus.ERROR);
            referenceFile.setErrorMessage("Analysis failed: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get grading session by ID
     */
    public GradingSession getGradingSession(Long sessionId) {
        return gradingSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Grading session not found: " + sessionId));
    }

    /**
     * Get all grading sessions with pagination
     */
    public Page<GradingSession> getAllGradingSessions(Pageable pageable) {
        return gradingSessionRepository.findAll(pageable);
    }

    /**
     * Get grading sessions by status
     */
    public List<GradingSession> getGradingSessionsByStatus(GradingSession.SessionStatus status) {
        return gradingSessionRepository.findByStatus(status);
    }

    /**
     * Complete a grading session
     */
    public GradingSession completeGradingSession(Long sessionId, Integer overallScore, String finalComments) {
        log.info("Completing grading session: {} with score: {}", sessionId, overallScore);

        GradingSession session = getGradingSession(sessionId);

        if (session.getStatus() != GradingSession.SessionStatus.READY) {
            throw new IllegalStateException("Session must be in READY status to complete");
        }

        // Validate overall score
        if (overallScore < 0 || overallScore > 100) {
            throw new IllegalArgumentException("Overall score must be between 0 and 100");
        }

        // Update session
        session.setOverallScore(overallScore);
        session.setFinalComments(finalComments);
        session.setStatus(GradingSession.SessionStatus.COMPLETED);

        session = gradingSessionRepository.save(session);

        log.info("Grading session completed: {}", sessionId);

        return session;
    }

    /**
     * Delete a grading session and all related data
     */
    public void deleteGradingSession(Long sessionId) {
        log.info("Deleting grading session: {}", sessionId);

        GradingSession session = getGradingSession(sessionId);

        // Delete associated files if they exist
        try {
            if (session.getStudentFile() != null) {
                fileService.deleteFile(session.getStudentFile().getId());
            }
            if (session.getReferenceFile() != null) {
                fileService.deleteFile(session.getReferenceFile().getId());
            }
        } catch (Exception e) {
            log.warn("Failed to delete associated files for session: {}", sessionId, e);
            // Continue with session deletion even if file deletion fails
        }

        // Delete the session (cascade will handle related entities)
        gradingSessionRepository.delete(session);

        log.info("Grading session deleted: {}", sessionId);
    }

    /**
     * Get comparison result for a session
     */
    public ComparisonResult getComparisonResult(Long sessionId) {
        GradingSession session = getGradingSession(sessionId);
        
        if (session.getComparison() == null) {
            throw new RuntimeException("Comparison not available for session: " + sessionId);
        }

        return session.getComparison();
    }

    /**
     * Check if session is ready for grading
     */
    public boolean isSessionReady(Long sessionId) {
        GradingSession session = getGradingSession(sessionId);
        return session.getStatus() == GradingSession.SessionStatus.READY;
    }

    /**
     * Get session statistics
     */
    public SessionStatistics getSessionStatistics(Long sessionId) {
        GradingSession session = getGradingSession(sessionId);
        
        if (session.getComparison() == null) {
            throw new RuntimeException("Comparison not available for session: " + sessionId);
        }

        ComparisonResult comparison = session.getComparison();
        List<Feedback> feedbacks = session.getFeedbacks();

        return SessionStatistics.builder()
            .sessionId(sessionId)
            .totalStudentElements(comparison.getTotalStudentElements())
            .totalReferenceElements(comparison.getTotalReferenceElements())
            .matchedElements(comparison.getMatchedElements())
            .overallSimilarity(comparison.getOverallSimilarity())
            .feedbackCount(feedbacks.size())
            .averageFeedbackScore(calculateAverageFeedbackScore(feedbacks))
            .overallScore(session.getOverallScore())
            .status(session.getStatus())
            .build();
    }

    /**
     * Calculate average feedback score
     */
    private Double calculateAverageFeedbackScore(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return null;
        }

        return feedbacks.stream()
            .mapToInt(Feedback::getScore)
            .average()
            .orElse(0.0);
    }

    /**
     * Restart analysis for a session
     */
    public GradingSession restartAnalysis(Long sessionId) {
        log.info("Restarting analysis for session: {}", sessionId);

        GradingSession session = getGradingSession(sessionId);

        // Clear existing comparison and feedbacks
        session.setComparison(null);
        session.getFeedbacks().clear();
        session.setOverallScore(null);
        session.setFinalComments(null);
        session.setStatus(GradingSession.SessionStatus.CREATED);

        session = gradingSessionRepository.save(session);

        // Start analysis again
        analyzeCodeAsync(session);

        return session;
    }

    /**
     * Get recent sessions
     */
    public List<GradingSession> getRecentSessions(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return gradingSessionRepository.findAll(pageable).getContent();
    }

    /**
     * Get sessions with high similarity
     */
    public List<GradingSession> getHighSimilaritySessions(double threshold) {
        return gradingSessionRepository.findSessionsWithComparison().stream()
            .filter(session -> session.getComparison().getOverallSimilarity() >= threshold)
            .toList();
    }

    /**
     * Session statistics data class
     */
    public static class SessionStatistics {
        private Long sessionId;
        private Integer totalStudentElements;
        private Integer totalReferenceElements;
        private Integer matchedElements;
        private Double overallSimilarity;
        private Integer feedbackCount;
        private Double averageFeedbackScore;
        private Integer overallScore;
        private GradingSession.SessionStatus status;

        // Builder pattern
        public static SessionStatisticsBuilder builder() {
            return new SessionStatisticsBuilder();
        }

        public static class SessionStatisticsBuilder {
            private SessionStatistics statistics = new SessionStatistics();

            public SessionStatisticsBuilder sessionId(Long sessionId) {
                statistics.sessionId = sessionId;
                return this;
            }

            public SessionStatisticsBuilder totalStudentElements(Integer totalStudentElements) {
                statistics.totalStudentElements = totalStudentElements;
                return this;
            }

            public SessionStatisticsBuilder totalReferenceElements(Integer totalReferenceElements) {
                statistics.totalReferenceElements = totalReferenceElements;
                return this;
            }

            public SessionStatisticsBuilder matchedElements(Integer matchedElements) {
                statistics.matchedElements = matchedElements;
                return this;
            }

            public SessionStatisticsBuilder overallSimilarity(Double overallSimilarity) {
                statistics.overallSimilarity = overallSimilarity;
                return this;
            }

            public SessionStatisticsBuilder feedbackCount(Integer feedbackCount) {
                statistics.feedbackCount = feedbackCount;
                return this;
            }

            public SessionStatisticsBuilder averageFeedbackScore(Double averageFeedbackScore) {
                statistics.averageFeedbackScore = averageFeedbackScore;
                return this;
            }

            public SessionStatisticsBuilder overallScore(Integer overallScore) {
                statistics.overallScore = overallScore;
                return this;
            }

            public SessionStatisticsBuilder status(GradingSession.SessionStatus status) {
                statistics.status = status;
                return this;
            }

            public SessionStatistics build() {
                return statistics;
            }
        }

        // Getters
        public Long getSessionId() { return sessionId; }
        public Integer getTotalStudentElements() { return totalStudentElements; }
        public Integer getTotalReferenceElements() { return totalReferenceElements; }
        public Integer getMatchedElements() { return matchedElements; }
        public Double getOverallSimilarity() { return overallSimilarity; }
        public Integer getFeedbackCount() { return feedbackCount; }
        public Double getAverageFeedbackScore() { return averageFeedbackScore; }
        public Integer getOverallScore() { return overallScore; }
        public GradingSession.SessionStatus getStatus() { return status; }
    }
} 