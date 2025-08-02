package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the result of comparing student code with reference solution
 */
@Entity
@Table(name = "comparison_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_session_id", nullable = false)
    private GradingSession gradingSession;

    @OneToMany(mappedBy = "comparisonResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CodeStructure> studentCodeStructures = new ArrayList<>();

    @OneToMany(mappedBy = "comparisonResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CodeStructure> referenceCodeStructures = new ArrayList<>();

    @OneToMany(mappedBy = "comparisonResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ComparisonMatch> matches = new ArrayList<>();

    @Column(nullable = false)
    private Double overallSimilarity;

    @Column(nullable = false)
    private Integer totalStudentElements;

    @Column(nullable = false)
    private Integer totalReferenceElements;

    @Column(nullable = false)
    private Integer matchedElements;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 