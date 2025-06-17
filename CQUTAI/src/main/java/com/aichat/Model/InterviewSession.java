package com.aichat.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class InterviewSession {
    private Integer interviewId;
    private Integer questionId;
    private String question;
    private Map<String, Double> dimensionScores;


}