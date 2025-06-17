package com.aichat.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Job {
    private Integer id;
    private Integer companyId;
    private String title;
    private String description;
    private String requirements;
    private String salaryRange;
    private String location;
    private String status; // 0/1
    private LocalDateTime createdAt;
}