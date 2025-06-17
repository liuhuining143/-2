package com.aichat.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
public class InterviewResponse {
    private Integer nextQuestionId;
    private String nextQuestion;
    private String analysis;
    private Map<String, Double> dimensionScores;

    public InterviewResponse(Integer nextQuestionId, String nextQuestion, String analysis, Map<String, Double> dimensionScores) {
        this.nextQuestionId = nextQuestionId;
        this.nextQuestion = nextQuestion;
        this.analysis = analysis;
        this.dimensionScores = dimensionScores;
    }

}