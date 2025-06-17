package com.aichat.Model;

import lombok.Data;

@Data
public class InterviewAnswer {
    private Long interviewId;
    private Long questionId;
    private String answer;
}
