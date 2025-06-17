package com.aichat.Model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Company {
    private Integer id;
    private String name;
    private String licenseNo;
    private String adminEmail;
    private String address;
    private String logoUrl;
    private String status; // active/pending/suspended
    private LocalDateTime createdAt;

    // 新增字段
    private String description;
    private String scale; // 公司规模
    private String mainBusiness; // 主营业务
}
