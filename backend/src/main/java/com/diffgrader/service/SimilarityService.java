package com.diffgrader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for calculating similarity between code elements using various algorithms
 * including Levenshtein Edit Distance Algorithm
 */
@Service
@Slf4j
public class SimilarityService {

    private static final double SIGNATURE_WEIGHT = 0.4;
    private static final double NAME_WEIGHT = 0.3;
    private static final double STRUCTURE_WEIGHT = 0.3;

    /**
     * Calculate similarity between two code elements
     * Combines signature similarity, name similarity, and structural similarity
     */
    public double calculateSimilarity(String studentSignature, String referenceSignature,
                                    String studentName, String referenceName,
                                    String studentCode, String referenceCode) {
        
        double signatureSimilarity = calculateStringSimilarity(studentSignature, referenceSignature);
        double nameSimilarity = calculateStringSimilarity(studentName, referenceName);
        double structuralSimilarity = calculateStructuralSimilarity(studentCode, referenceCode);
        
        double overallSimilarity = (signatureSimilarity * SIGNATURE_WEIGHT) +
                                 (nameSimilarity * NAME_WEIGHT) +
                                 (structuralSimilarity * STRUCTURE_WEIGHT);
        
        log.debug("Similarity calculation - Signature: {}, Name: {}, Structure: {}, Overall: {}",
            signatureSimilarity, nameSimilarity, structuralSimilarity, overallSimilarity);
        
        return Math.min(1.0, overallSimilarity);
    }

    /**
     * Calculate string similarity using Levenshtein Distance Algorithm
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     */
    public double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2 ? 1.0 : 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        int distance = calculateLevenshteinDistance(str1.toLowerCase(), str2.toLowerCase());
        int maxLength = Math.max(str1.length(), str2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Implementation of Levenshtein Edit Distance Algorithm
     * Calculates the minimum number of single-character edits required to transform one string into another
     */
    public int calculateLevenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        
        // Create a matrix to store distances
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // Initialize base cases
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // Fill the matrix using dynamic programming
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation needed
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j],      // Deletion
                                dp[i][j - 1]),      // Insertion
                        dp[i - 1][j - 1]           // Substitution
                    );
                }
            }
        }
        
        return dp[len1][len2];
    }

    /**
     * Calculate structural similarity based on code patterns and structure
     */
    public double calculateStructuralSimilarity(String code1, String code2) {
        if (code1 == null || code2 == null) {
            return code1 == code2 ? 1.0 : 0.0;
        }
        
        // Extract structural features
        List<String> features1 = extractStructuralFeatures(code1);
        List<String> features2 = extractStructuralFeatures(code2);
        
        return calculateFeatureSimilarity(features1, features2);
    }

    /**
     * Extract structural features from code (keywords, patterns, etc.)
     */
    private List<String> extractStructuralFeatures(String code) {
        List<String> features = new ArrayList<>();
        
        // Remove whitespace and normalize
        String normalizedCode = code.replaceAll("\\s+", " ").toLowerCase();
        
        // Extract Java keywords and patterns
        String[] keywords = {"public", "private", "protected", "static", "final", "abstract",
                           "class", "interface", "extends", "implements", "throws",
                           "if", "else", "for", "while", "switch", "case", "try", "catch",
                           "return", "new", "this", "super"};
        
        for (String keyword : keywords) {
            int count = countOccurrences(normalizedCode, keyword);
            if (count > 0) {
                features.add(keyword + ":" + count);
            }
        }
        
        // Extract brackets and punctuation patterns
        features.add("openBrace:" + countOccurrences(normalizedCode, "{"));
        features.add("closeBrace:" + countOccurrences(normalizedCode, "}"));
        features.add("openParen:" + countOccurrences(normalizedCode, "("));
        features.add("closeParen:" + countOccurrences(normalizedCode, ")"));
        features.add("semicolon:" + countOccurrences(normalizedCode, ";"));
        
        return features;
    }

    /**
     * Calculate similarity between two feature sets
     */
    private double calculateFeatureSimilarity(List<String> features1, List<String> features2) {
        if (features1.isEmpty() && features2.isEmpty()) {
            return 1.0;
        }
        
        if (features1.isEmpty() || features2.isEmpty()) {
            return 0.0;
        }
        
        int commonFeatures = 0;
        int totalFeatures = Math.max(features1.size(), features2.size());
        
        for (String feature1 : features1) {
            if (features2.contains(feature1)) {
                commonFeatures++;
            }
        }
        
        return (double) commonFeatures / totalFeatures;
    }

    /**
     * Count occurrences of a substring in a string
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        
        return count;
    }

    /**
     * Calculate token-based similarity using Jaccard coefficient
     */
    public double calculateJaccardSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2 ? 1.0 : 0.0;
        }
        
        String[] tokens1 = tokenize(str1);
        String[] tokens2 = tokenize(str2);
        
        if (tokens1.length == 0 && tokens2.length == 0) {
            return 1.0;
        }
        
        if (tokens1.length == 0 || tokens2.length == 0) {
            return 0.0;
        }
        
        // Calculate intersection and union
        long intersection = Arrays.stream(tokens1)
            .distinct()
            .mapToLong(token -> Arrays.stream(tokens2).anyMatch(t -> t.equals(token)) ? 1 : 0)
            .sum();
        
        long union = Arrays.stream(tokens1).distinct().count() +
                    Arrays.stream(tokens2).distinct().count() - intersection;
        
        return union > 0 ? (double) intersection / union : 0.0;
    }

    /**
     * Tokenize string into meaningful tokens
     */
    private String[] tokenize(String str) {
        return str.toLowerCase()
                 .replaceAll("[^a-zA-Z0-9_]", " ")
                 .split("\\s+");
    }

    /**
     * Find differences between two strings
     */
    public List<String> findDifferences(String str1, String str2, String elementType) {
        List<String> differences = new ArrayList<>();
        
        if (str1 == null || str2 == null) {
            if (str1 != str2) {
                differences.add("One element is null while the other is not");
            }
            return differences;
        }
        
        // Check for exact match
        if (str1.equals(str2)) {
            return differences; // No differences
        }
        
        // Check specific differences based on element type
        switch (elementType.toLowerCase()) {
            case "method":
                findMethodDifferences(str1, str2, differences);
                break;
            case "field":
                findFieldDifferences(str1, str2, differences);
                break;
            case "class":
                findClassDifferences(str1, str2, differences);
                break;
            case "constructor":
                findConstructorDifferences(str1, str2, differences);
                break;
            default:
                findGenericDifferences(str1, str2, differences);
        }
        
        return differences;
    }

    private void findMethodDifferences(String method1, String method2, List<String> differences) {
        if (!extractAccessModifier(method1).equals(extractAccessModifier(method2))) {
            differences.add("Different access modifiers");
        }
        
        if (!extractReturnType(method1).equals(extractReturnType(method2))) {
            differences.add("Different return types");
        }
        
        if (!extractParameters(method1).equals(extractParameters(method2))) {
            differences.add("Different parameters");
        }
        
        if (method1.contains("static") != method2.contains("static")) {
            differences.add("Different static modifier");
        }
    }

    private void findFieldDifferences(String field1, String field2, List<String> differences) {
        if (!extractAccessModifier(field1).equals(extractAccessModifier(field2))) {
            differences.add("Different access modifiers");
        }
        
        if (!extractFieldType(field1).equals(extractFieldType(field2))) {
            differences.add("Different field types");
        }
        
        if (field1.contains("static") != field2.contains("static")) {
            differences.add("Different static modifier");
        }
        
        if (field1.contains("final") != field2.contains("final")) {
            differences.add("Different final modifier");
        }
    }

    private void findClassDifferences(String class1, String class2, List<String> differences) {
        if (!extractAccessModifier(class1).equals(extractAccessModifier(class2))) {
            differences.add("Different access modifiers");
        }
        
        if (class1.contains("abstract") != class2.contains("abstract")) {
            differences.add("Different abstract modifier");
        }
        
        if (class1.contains("final") != class2.contains("final")) {
            differences.add("Different final modifier");
        }
        
        if (class1.contains("interface") != class2.contains("interface")) {
            differences.add("Different class/interface type");
        }
    }

    private void findConstructorDifferences(String constructor1, String constructor2, List<String> differences) {
        if (!extractAccessModifier(constructor1).equals(extractAccessModifier(constructor2))) {
            differences.add("Different access modifiers");
        }
        
        if (!extractParameters(constructor1).equals(extractParameters(constructor2))) {
            differences.add("Different parameters");
        }
    }

    private void findGenericDifferences(String str1, String str2, List<String> differences) {
        double similarity = calculateStringSimilarity(str1, str2);
        
        if (similarity < 0.8) {
            differences.add("Significant structural differences");
        }
        
        if (similarity < 0.5) {
            differences.add("Major differences in implementation");
        }
    }

    private String extractAccessModifier(String signature) {
        if (signature.contains("public")) return "public";
        if (signature.contains("private")) return "private";
        if (signature.contains("protected")) return "protected";
        return "package-private";
    }

    private String extractReturnType(String methodSignature) {
        String[] parts = methodSignature.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i + 1].contains("(")) {
                return parts[i];
            }
        }
        return "";
    }

    private String extractFieldType(String fieldSignature) {
        String[] parts = fieldSignature.split("\\s+");
        if (parts.length >= 2) {
            return parts[parts.length - 2];
        }
        return "";
    }

    private String extractParameters(String signature) {
        int start = signature.indexOf('(');
        int end = signature.indexOf(')');
        if (start != -1 && end != -1 && end > start) {
            return signature.substring(start + 1, end);
        }
        return "";
    }
} 