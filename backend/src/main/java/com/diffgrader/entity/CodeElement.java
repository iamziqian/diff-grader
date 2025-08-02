package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Entity representing a code element (class, field, method, constructor)
 * extracted from Java source code using AST parsing
 */
@Entity
@Table(name = "code_elements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElementType type;

    @Column(nullable = false, length = 5000)
    private String signature;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean matched = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchType matchType = MatchType.MISSING;

    @Column
    private Double similarity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_structure_id")
    private CodeStructure codeStructure;

    public enum ElementType {
        CLASS, FIELD, METHOD, CONSTRUCTOR
    }

    public enum MatchType {
        EXACT, SIMILAR, MISSING, EXTRA
    }
} 