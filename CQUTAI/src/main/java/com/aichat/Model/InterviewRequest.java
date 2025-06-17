package com.aichat.Model;

import lombok.Data;

@Data
public class InterviewRequest {
    private Long userId;
    private Long jobId;
    private Long companyId;
}