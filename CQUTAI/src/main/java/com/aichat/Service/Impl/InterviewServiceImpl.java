package com.aichat.Service.Impl;

import com.aichat.Mapper.*;
import com.aichat.Model.*;
import com.aichat.Service.InterviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl extends InterviewService {

    private final UserMapper userMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final EvaluationDimensionMapper dimensionMapper;
    private final InterviewMapper interviewMapper;
    private final InterviewDetailMapper detailMapper;
    private final InterviewResultMapper resultMapper;
    private final ChatClient chatClient;

@Transactional
@Override
public InterviewSession startInterview(Long userId, Long jobId, Long companyId) {
        // 1. 获取用户、职位、公司信息
        User user = userMapper.selectById(Math.toIntExact(userId));
        Job job = jobMapper.selectById(Math.toIntExact(jobId));
        Company company = companyMapper.selectById(Math.toIntExact(companyId));

    System.out.println("User: " + user);
    System.out.println("Job: " + job);
    System.out.println("Company: " + company);


    // 2. 获取考核维度及权重
        List<EvaluationDimension> dimensions = dimensionMapper.selectByCompanyAndJob(Math.toIntExact(companyId), Math.toIntExact(jobId));
        Map<String, Double> dimensionWeights = dimensions.stream()
                .collect(Collectors.toMap(
                        EvaluationDimension::getName,
                        EvaluationDimension::getWeight
                ));
    System.out.println("Dimensions size: " + dimensions.size());

        // 3. 创建面试记录
        Interview interview = new Interview();
        interview.setUserId(Math.toIntExact(userId));
        interview.setJobId(Math.toIntExact(jobId));
        interview.setCompanyId(Math.toIntExact(companyId));
        interview.setStartTime(LocalDateTime.now());
        interview.setStatus("in_progress");
        interview.setAiModelVersion("deepseek-r1");
        interviewMapper.insert(interview);

        // 4. 构建提示词生成问题（整合用户、职位、公司信息和维度权重）
        String prompt = buildInitialPrompt(user, job, company, dimensionWeights);
        String question = generateQuestion(prompt);
    System.out.println("Prompt: " + prompt);
    System.out.println("Generated Question: " + question);

        // 5. 保存第一个问题
        InterviewDetail firstQuestion = new InterviewDetail();
        firstQuestion.setInterviewId(interview.getId());
        firstQuestion.setQuestion(question);
    firstQuestion.setDimensionScores(dimensionWeights);
        firstQuestion.setQuestionType("technical");
        detailMapper.insert(firstQuestion);
    System.out.println("Interview ID: " + interview.getId());
    System.out.println("First Question: " + firstQuestion.getQuestion());

        // 6. 返回会话信息
    return new InterviewSession(
            interview.getId(),
            firstQuestion.getId(),
            question,
            dimensionWeights
    );

}

    private String buildInitialPrompt(User user, Job job, Company company, Map<String, Double> dimensions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的AI面试官，正在面试").append(user.getFullName()).append("应聘")
                .append(company.getName()).append("的").append(job.getTitle()).append("职位。\n\n");

        prompt.append("求职者信息:\n");
        prompt.append("- 姓名: ").append(user.getFullName()).append("\n");
        prompt.append("- 毕业院校: ").append(user.getUniversity()).append("\n");
        prompt.append("- 专业: ").append(user.getMajor()).append("\n");
        prompt.append("- 工作经验: ").append(user.getWorkExperience()).append("年\n\n");

        prompt.append("职位要求:\n");
        prompt.append(job.getRequirements()).append("\n\n");

        prompt.append("公司简介:\n");
        prompt.append(company.getDescription()).append("\n\n");

        prompt.append("考核维度及权重:\n");
        dimensions.forEach((dimension, weight) ->
                prompt.append("- ").append(dimension).append(": ").append(weight * 100).append("%\n"));

        prompt.append("\n请根据以上信息，提出第一个技术面试问题，问题应重点考察高权重的维度。");
        return prompt.toString();
    }

    @Transactional
    @Override
    public InterviewResponse processAnswer(Long interviewId, Long questionId, String answer) {
        // 1. 保存回答
        InterviewDetail detail = detailMapper.selectById(questionId);
        if (detail == null) {
            throw new IllegalArgumentException("问题 ID 不存在，请确认是否已开始面试并获取有效问题 ID。");
        }
        detail.setAnswer(answer);
        detailMapper.updateAnswer(detail);

        // 2. AI分析当前回答
        String analysis = analyzeAnswer(detail.getQuestion(), answer);
        detail.setAnalysis(analysis);
        detailMapper.updateAnalysis(detail);

        // 3. 生成维度评分
        Map<String, Double> dimensionScores = evaluateAnswerDimensions(
                detail.getQuestion(),
                answer,
                detail.getDimensionScores() == null ? new HashMap<>() : detail.getDimensionScores()
        );

        detail.setDimensionScores(dimensionScores);
        detailMapper.updateDimensionScores(detail);

        // 4. 生成下一个问题
        String nextQuestion = generateNextQuestion(interviewId, questionId, answer);
        // 5. 获取维度权重（新增逻辑）
        Interview interview = interviewMapper.selectById(interviewId);
        List<EvaluationDimension> dimensions = dimensionMapper.selectByCompanyAndJob(
                interview.getCompanyId(),
                interview.getJobId()
        );
        Map<String, Double> dimensionWeights = dimensions.stream()
                .collect(Collectors.toMap(
                        EvaluationDimension::getName,
                        EvaluationDimension::getWeight  // 👈 改为 getWeight 方法
                ));
        // 5. 保存新问题
        InterviewDetail nextDetail = new InterviewDetail();
        nextDetail.setInterviewId(Math.toIntExact(interviewId));
        nextDetail.setQuestion(nextQuestion);
        nextDetail.setQuestionType("technical");
        detailMapper.insert(nextDetail);
        System.out.println("Next Question: " + nextQuestion);
        System.out.println("Next Question ID: " + nextDetail.getId());
        System.out.println("Next Question Type: " + nextDetail.getQuestionType());
        System.out.println("Next Question Dimension Scores: " + nextDetail.getDimensionScores());
        System.out.println("Next Question Question Type: " + nextDetail.getQuestionType());
        System.out.println("analysis: " + analysis);
        return new InterviewResponse(
                nextDetail.getId(),
                nextQuestion,
                analysis,
                null
        );
    }

    @Transactional
    @Override
    public InterviewResult completeInterview(Long interviewId) {
        // 1. 结束面试
        Interview interview = interviewMapper.selectById(interviewId);
        interview.setEndTime(LocalDateTime.now());
        interview.setStatus("completed");
        interviewMapper.update(interview);


        // 3. 生成并保存结果
        InterviewResult result = generateFinalReport(interviewId);
        result.setInterviewId(Math.toIntExact(interviewId));


        resultMapper.insert(result);

        return result;
    }


    @Override
    public com. aichat. Model. InterviewResult getInterviewReport(Long interviewId) {
        return resultMapper.selectByInterviewId(interviewId);
    }

    // 私有辅助方法


    private String generateQuestion(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String analyzeAnswer(String question, String answer) {
        String prompt = "分析以下面试回答的质量:\n\n问题: " + question + "\n回答: " + answer +
                "\n\n请评估回答的技术准确性、完整性和相关性。";
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private Map<String, Double> evaluateAnswerDimensions(String question, String answer, Map<String, Double> weights) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下维度和权重评估面试回答:\n\n");

        prompt.append("考核维度及权重:\n");
        weights.forEach((dimension, weight) ->
                prompt.append("- ").append(dimension).append(": ").append(weight * 100).append("%\n"));

        prompt.append("\n问题: ").append(question)
                .append("\n回答: ").append(answer)
                .append("\n\n请对上述回答进行评分，每个维度给出一个 0 到 10 分之间的评分。\n")
                .append("输出格式必须严格符合 JSON：{\"维度1\": 分数, \"维度2\": 分数}，不要包含其他内容。\n")
                .append("例如：{\"Java基础\": 8.5, \"Spring Boot\": 7.5}");

        String jsonResponse = chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();

        return parseDimensionScores(jsonResponse);
    }


    private String generateNextQuestion(Long interviewId, Long questionId, String answer) {
        // 获取面试上下文
        List<InterviewDetail> history = detailMapper.selectByInterviewId(interviewId);

        StringBuilder context = new StringBuilder("面试历史:\n");
        for (InterviewDetail detail : history) {
            context.append("面试官: ").append(detail.getQuestion()).append("\n");
            context.append("求职者: ").append(detail.getAnswer()).append("\n\n");
        }

        String prompt = context.toString() +
                "基于以上对话历史，请提出下一个合适的技术面试问题。";

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private InterviewResult generateFinalReport(Long interviewId) {
        // 1. 获取所有面试细节
        List<InterviewDetail> details = detailMapper.selectByInterviewId(interviewId);

        // 2. 生成报告提示
        StringBuilder prompt = new StringBuilder("生成面试最终报告:\n\n");
        prompt.append("面试问题与回答:\n");
        details.forEach(detail -> {
            prompt.append("问题: ").append(detail.getQuestion()).append("\n");
            prompt.append("回答: ").append(detail.getAnswer()).append("\n");
            prompt.append("分析: ").append(detail.getAnalysis()).append("\n\n");
        });
        // 所有维度评分汇总
        Map<String, List<Double>> scoresByDimension = details.stream()
                .flatMap(d -> d.getDimensionScores().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

// 平均分计算
        Map<String, Double> averageScores = scoresByDimension.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
                ));


        prompt.append("请根据以上内容生成包含以下部分的综合报告:\n");
        prompt.append("1. 总体评价\n2. 各维度评分总结\n3. 优势与不足\n4. 改进建议");

        String reportContent = chatClient.prompt()
                .user("请根据以下维度评分生成综合报告：" + averageScores)
                .call()
                .content();
        // 3. 计算总分
        double totalScore = calculateTotalScore(details);

        // 4. 创建结果对象
        InterviewResult result = new InterviewResult();
        result.setInterviewId(Math.toIntExact(interviewId));
        result.setTotalScore(totalScore);
        result.setEvaluationReport(reportContent);
        result.setDimensionScores(aggregateDimensionScores(details));
        result.setImprovementSuggestions(extractSuggestions(reportContent));

        // 5. 保存结果
        resultMapper.insert(result);

        return result;
    }

    private double calculateTotalScore(List<InterviewDetail> details) {
        // 实现评分计算逻辑
        return details.stream()
                .flatMap(d -> d.getDimensionScores().values().stream())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private String aggregateDimensionScores(List<InterviewDetail> details) {
        // 实现维度分数聚合逻辑
        return "维度评分聚合结果";
    }

    private String extractSuggestions(String report) {
        // 从报告中提取改进建议
        return "改进建议内容";
    }

    private Map<String, Double> parseDimensionScores(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Double>> typeRef = new TypeReference<>() {};
            return mapper.readValue(json, typeRef);
        } catch (Exception e) {
            // 解析失败时返回默认空 map 或抛出异常
            return new HashMap<>();
        }
    }

}