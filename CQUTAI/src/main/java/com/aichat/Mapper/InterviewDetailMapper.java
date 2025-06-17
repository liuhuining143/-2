package com.aichat.Mapper;

import com.aichat.Model.InterviewDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterviewDetailMapper {

    void insert(InterviewDetail detail);


    void updateAnswer(InterviewDetail detail);


    void updateAnalysis(InterviewDetail detail);


    void updateDimensionScores(InterviewDetail detail);


    InterviewDetail selectById(Long id);


    List<InterviewDetail> selectByInterviewId(Long interviewId);
}
