package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a match between a student code element and a reference code element
 */
@Entity
@Table(name = "comparison_matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_element_id", nullable = false)
    private CodeElement studentElement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_element_id", nullable = false)
    private CodeElement referenceElement;

    @Column(nullable = false)
    private Double similarity;

    @ElementCollection
    @CollectionTable(name = "match_differences", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "difference", length = 1000)
    @Builder.Default
    private List<String> differences = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_result_id")
    private ComparisonResult comparisonResult;
} 