package com.aichat.Model;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
public class Interview {
    private Integer id;
    private Integer userId;
    private Integer jobId;
    private Integer companyId;
    private String status; // scheduled/completed/canceled/missed
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String aiAnalysis;
    private String videoUrl;
    private Float score;
    private LocalDateTime createdAt;


    // 新增字段
    private String aiModelVersion;
    private String audioUrl;

}