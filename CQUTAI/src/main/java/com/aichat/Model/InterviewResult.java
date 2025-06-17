package com.aichat.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterviewResult {
    private Integer id;
    private Integer interviewId;
    private Double totalScore;
    private String evaluationReport;
    private String dimensionScores; // JSON格式的维度得分
    private String improvementSuggestions;
    private LocalDateTime generatedAt;
}