package com.aichat.Mapper;


import com.aichat.Model.InterviewResult;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InterviewResultMapper {
    @Insert("INSERT INTO interview_results(interview_id, total_score, evaluation_report, dimension_scores, improvement_suggestions) " +
            "VALUES(#{interviewId}, #{totalScore}, #{evaluationReport}, #{dimensionScores}, #{improvementSuggestions})")
    void insert(InterviewResult result);

    @Select("SELECT * FROM interview_results WHERE interview_id = #{interviewId}")
    InterviewResult selectByInterviewId(Long interviewId);
}