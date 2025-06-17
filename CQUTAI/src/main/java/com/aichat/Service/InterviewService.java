package com.aichat.Service;

import com.aichat.Model.InterviewResponse;
import com.aichat.Model.InterviewResult;
import com.aichat.Model.InterviewSession;
import org.springframework.transaction.annotation.Transactional;

public abstract class InterviewService {
    @Transactional
        public abstract InterviewSession startInterview(Long userId, Long jobId, Long companyId);

    @Transactional
    public abstract InterviewResponse processAnswer(Long interviewId, Long questionId, String answer);

    @Transactional
    public abstract InterviewResult completeInterview(Long interviewId);

    public abstract InterviewResult getInterviewReport(Long interviewId);
}
