package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a grading session that contains student and reference files,
 * comparison results, and feedback
 */
@Entity
@Table(name = "grading_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_file_id", nullable = false)
    private UploadedFile studentFile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_file_id", nullable = false)
    private UploadedFile referenceFile;

    @OneToOne(mappedBy = "gradingSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ComparisonResult comparison;

    @OneToMany(mappedBy = "gradingSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Feedback> feedbacks = new ArrayList<>();

    @Column
    private Integer overallScore;

    @Column(length = 2000)
    private String finalComments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.CREATED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        CREATED, ANALYZING, READY, COMPLETED
    }
} 