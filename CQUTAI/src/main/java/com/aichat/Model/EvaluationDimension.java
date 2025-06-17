package com.aichat.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EvaluationDimension {
    private Integer id;
    private Integer companyId;
    private String name;
    private String description;
    private Double weight; // 权重值 0-1
    private LocalDateTime createdAt;
}