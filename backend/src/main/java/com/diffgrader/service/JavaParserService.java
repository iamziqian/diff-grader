package com.diffgrader.service;

import com.diffgrader.entity.CodeElement;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for parsing Java code using JavaParser AST and Visitor Pattern
 * Extracts code elements (classes, fields, methods, constructors) from Java source files
 */
@Service
@Slf4j
public class JavaParserService {

    private final JavaParser javaParser;

    public JavaParserService() {
        this.javaParser = new JavaParser();
    }

    /**
     * Parse a Java file and extract code elements
     */
    public List<CodeElement> parseJavaFile(File javaFile) {
        List<CodeElement> elements = new ArrayList<>();
        
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                // Use visitor pattern to extract code elements
                CodeElementVisitor visitor = new CodeElementVisitor(javaFile.getName());
                cu.accept(visitor, null);
                elements = visitor.getCodeElements();
                
                log.debug("Parsed {} elements from file: {}", elements.size(), javaFile.getName());
            } else {
                log.error("Failed to parse Java file: {} - Problems: {}", 
                    javaFile.getName(), parseResult.getProblems());
            }
        } catch (FileNotFoundException e) {
            log.error("Java file not found: {}", javaFile.getAbsolutePath(), e);
        } catch (Exception e) {
            log.error("Error parsing Java file: {}", javaFile.getName(), e);
        }
        
        return elements;
    }

    /**
     * Parse Java content from string
     */
    public List<CodeElement> parseJavaContent(String content, String fileName) {
        List<CodeElement> elements = new ArrayList<>();
        
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                CodeElementVisitor visitor = new CodeElementVisitor(fileName);
                cu.accept(visitor, null);
                elements = visitor.getCodeElements();
                
                log.debug("Parsed {} elements from content of file: {}", elements.size(), fileName);
            } else {
                log.error("Failed to parse Java content for file: {} - Problems: {}", 
                    fileName, parseResult.getProblems());
            }
        } catch (Exception e) {
            log.error("Error parsing Java content for file: {}", fileName, e);
        }
        
        return elements;
    }

    /**
     * Extract package name from Java file
     */
    public String extractPackageName(File javaFile) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                Optional<String> packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString());
                return packageName.orElse("");
            }
        } catch (Exception e) {
            log.error("Error extracting package name from file: {}", javaFile.getName(), e);
        }
        
        return "";
    }

    /**
     * Visitor implementation to extract code elements using Visitor Pattern
     */
    private static class CodeElementVisitor extends VoidVisitorAdapter<Void> {
        
        private final String fileName;
        private final List<CodeElement> codeElements;

        public CodeElementVisitor(String fileName) {
            this.fileName = fileName;
            this.codeElements = new ArrayList<>();
        }

        public List<CodeElement> getCodeElements() {
            return codeElements;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            CodeElement element = CodeElement.builder()
                .name(n.getNameAsString())
                .type(CodeElement.ElementType.CLASS)
                .signature(generateClassSignature(n))
                .sourceCode(n.toString())
                .lineNumber(n.getBegin().map(pos -> pos.line).orElse(0))
                .build();
            
            codeElements.add(element);
            
            log.debug("Found class: {} at line {}", element.getName(), element.getLineNumber());
            
            // Continue visiting child nodes
            super.visit(n, arg);
        }

        @Override
        public void visit(FieldDeclaration n, Void arg) {
            n.getVariables().forEach(variable -> {
                CodeElement element = CodeElement.builder()
                    .name(variable.getNameAsString())
                    .type(CodeElement.ElementType.FIELD)
                    .signature(generateFieldSignature(n, variable))
                    .sourceCode(n.toString())
                    .lineNumber(n.getBegin().map(pos -> pos.line).orElse(0))
                    .build();
                
                codeElements.add(element);
                
                log.debug("Found field: {} at line {}", element.getName(), element.getLineNumber());
            });
            
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            CodeElement element = CodeElement.builder()
                .name(n.getNameAsString())
                .type(CodeElement.ElementType.METHOD)
                .signature(generateMethodSignature(n))
                .sourceCode(n.toString())
                .lineNumber(n.getBegin().map(pos -> pos.line).orElse(0))
                .build();
            
            codeElements.add(element);
            
            log.debug("Found method: {} at line {}", element.getName(), element.getLineNumber());
            
            super.visit(n, arg);
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            CodeElement element = CodeElement.builder()
                .name(n.getNameAsString())
                .type(CodeElement.ElementType.CONSTRUCTOR)
                .signature(generateConstructorSignature(n))
                .sourceCode(n.toString())
                .lineNumber(n.getBegin().map(pos -> pos.line).orElse(0))
                .build();
            
            codeElements.add(element);
            
            log.debug("Found constructor: {} at line {}", element.getName(), element.getLineNumber());
            
            super.visit(n, arg);
        }

        private String generateClassSignature(ClassOrInterfaceDeclaration n) {
            StringBuilder signature = new StringBuilder();
            
            // Access modifiers
            signature.append(n.getAccessSpecifier().asString()).append(" ");
            
            if (n.isInterface()) {
                signature.append("interface ");
            } else {
                if (n.isAbstract()) signature.append("abstract ");
                if (n.isFinal()) signature.append("final ");
                signature.append("class ");
            }
            
            signature.append(n.getNameAsString());
            
            // Type parameters
            if (!n.getTypeParameters().isEmpty()) {
                signature.append("<");
                signature.append(n.getTypeParameters().stream()
                    .map(tp -> tp.getNameAsString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
                signature.append(">");
            }
            
            // Extends
            if (!n.getExtendedTypes().isEmpty()) {
                signature.append(" extends ");
                signature.append(n.getExtendedTypes().stream()
                    .map(et -> et.getNameAsString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            }
            
            // Implements
            if (!n.getImplementedTypes().isEmpty()) {
                signature.append(" implements ");
                signature.append(n.getImplementedTypes().stream()
                    .map(it -> it.getNameAsString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            }
            
            return signature.toString().trim();
        }

        private String generateFieldSignature(FieldDeclaration n, VariableDeclarator variable) {
            StringBuilder signature = new StringBuilder();
            
            // Access modifiers
            signature.append(n.getAccessSpecifier().asString()).append(" ");
            
            if (n.isStatic()) signature.append("static ");
            if (n.isFinal()) signature.append("final ");
            
            signature.append(variable.getType().asString()).append(" ");
            signature.append(variable.getNameAsString());
            
            return signature.toString().trim();
        }

        private String generateMethodSignature(MethodDeclaration n) {
            StringBuilder signature = new StringBuilder();
            
            // Access modifiers
            signature.append(n.getAccessSpecifier().asString()).append(" ");
            
            if (n.isStatic()) signature.append("static ");
            if (n.isAbstract()) signature.append("abstract ");
            if (n.isFinal()) signature.append("final ");
            
            signature.append(n.getType().asString()).append(" ");
            signature.append(n.getNameAsString()).append("(");
            
            // Parameters
            if (!n.getParameters().isEmpty()) {
                signature.append(n.getParameters().stream()
                    .map(p -> p.getType().asString() + " " + p.getNameAsString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            }
            
            signature.append(")");
            
            return signature.toString().trim();
        }

        private String generateConstructorSignature(ConstructorDeclaration n) {
            StringBuilder signature = new StringBuilder();
            
            // Access modifiers
            signature.append(n.getAccessSpecifier().asString()).append(" ");
            signature.append(n.getNameAsString()).append("(");
            
            // Parameters
            if (!n.getParameters().isEmpty()) {
                signature.append(n.getParameters().stream()
                    .map(p -> p.getType().asString() + " " + p.getNameAsString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            }
            
            signature.append(")");
            
            return signature.toString().trim();
        }
    }
} 