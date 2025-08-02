package com.diffgrader.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the structure of a Java file containing classes, fields, methods, and constructors
 */
@Entity
@Table(name = "code_structures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String packageName;

    @OneToMany(mappedBy = "codeStructure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CodeElement> elements = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_result_id")
    private ComparisonResult comparisonResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StructureType type;

    public enum StructureType {
        STUDENT, REFERENCE
    }

    // Helper methods to get elements by type
    public List<CodeElement> getClasses() {
        return elements.stream()
                .filter(element -> element.getType() == CodeElement.ElementType.CLASS)
                .toList();
    }

    public List<CodeElement> getFields() {
        return elements.stream()
                .filter(element -> element.getType() == CodeElement.ElementType.FIELD)
                .toList();
    }

    public List<CodeElement> getMethods() {
        return elements.stream()
                .filter(element -> element.getType() == CodeElement.ElementType.METHOD)
                .toList();
    }

    public List<CodeElement> getConstructors() {
        return elements.stream()
                .filter(element -> element.getType() == CodeElement.ElementType.CONSTRUCTOR)
                .toList();
    }
} 