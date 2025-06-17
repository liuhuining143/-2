package com.aichat.Controller;

import com.aichat.Model.*;
import com.aichat.Service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewSession> startInterview(
            @RequestBody InterviewRequest request) {
        InterviewSession session = interviewService.startInterview(
                request.getUserId(),
                request.getJobId(),
                request.getCompanyId()
        );
        return ResponseEntity.ok(session);
    }

    @PostMapping("/submit-answer")
    public ResponseEntity<InterviewResponse> submitAnswer(
            @RequestBody InterviewAnswer answer) {
        InterviewResponse response = interviewService.processAnswer(
                answer.getInterviewId(),
                answer.getQuestionId(),
                answer.getAnswer()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end")
    public ResponseEntity<InterviewResult> endInterview(
            @RequestParam Long interviewId) {
        InterviewResult result = interviewService.completeInterview(interviewId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/report")
    public ResponseEntity<InterviewResult> getInterviewReport(
            @RequestParam Long interviewId) {
        InterviewResult report = interviewService.getInterviewReport(interviewId);
        return ResponseEntity.ok(report);
    }
}