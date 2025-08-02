package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing feedback for a specific code element
 */
@Entity
@Table(name = "feedbacks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_session_id", nullable = false)
    private GradingSession gradingSession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_element_id", nullable = false)
    private CodeElement codeElement;

    @Column(nullable = false)
    private Integer score; // 0-100

    @Column(length = 2000)
    private String comments;

    @Column(length = 1000)
    private String designPatternFeedback;

    @Column(length = 1000)
    private String bestPracticesFeedback;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
} 