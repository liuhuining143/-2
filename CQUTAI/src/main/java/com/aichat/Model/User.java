package com.aichat.Model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String gender;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 新增字段
    private String wechatOpenid;
    private String wechatNickname;
    private String university;
    private String major;
    private Integer age;
    private Integer workExperience;
}