package com.aichat.Model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class InterviewDetail {
    private Integer id;
    private Integer interviewId;
    private String question;
    private String answer;
    private String analysis;
    private Map<String, Double> dimensionScores = new HashMap<>(); // 维度评分

    private Integer responseDuration; // 回答时长(秒)
    private LocalDateTime createdAt;

    // 新增字段
    private String questionType; // 问题类型: technical/behavioral/coding
    private String audioUrl;     // 回答音频URL
}
