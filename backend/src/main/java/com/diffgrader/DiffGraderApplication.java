package com.diffgrader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * DiffGrader - Intelligent Code Comparison and Feedback Grading System
 * 
 * Main Spring Boot Application class that bootstraps the entire backend system.
 * This application provides RESTful APIs for code analysis, comparison, and feedback management.
 */
@SpringBootApplication
@EnableAsync
public class DiffGraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiffGraderApplication.class, args);
    }
} 