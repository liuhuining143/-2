package com.aichat.Mapper;

import com.aichat.Model.Interview;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InterviewMapper {

    void insert(Interview interview);


    int update(Interview interview);


    Interview selectById(Long id);
}