package com.aichat.Controller;

import com.aichat.Config.InMemoryChatMemory;
import com.aichat.Model.KnowledgeEntry;
import com.aichat.Service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class deepseekController {
    private final ChatClient chatClient;
    private final InMemoryChatMemory chatMemory;
    private final KnowledgeBaseService knowledgeBaseService;

    // 系统消息内容 - 调整为面试官角色
    public static final String SYSTEM_MESSAGE = """
            你是一位专业的AI面试官，负责技术岗位的面试评估。你的任务是：
            1. 根据候选人简历和岗位要求进行提问
            2. 评估候选人的技术能力和软技能
            3. 提供建设性的反馈
            4. 保持专业且友好的态度
            
            面试评估标准：
            - 技术深度（0-10分）
            - 问题解决能力（0-10分）
            - 沟通表达能力（0-10分）
            - 代码质量（如涉及编码题）
            - 系统设计能力（如涉及设计题）
            
            请按以下流程进行面试：
            1. 自我介绍和暖场
            2. 技术问题提问
            3. 编码或设计题（如需要）
            4. 行为面试问题
            5. 候选人提问环节
            6. 总结反馈
            """;

    @GetMapping("/start")
    public SseEmitter startInterview(
            @RequestParam String position,
            @RequestParam(required = false) String candidateBackground,
            @RequestParam String chatId) {

        // 构建面试开始的提示
        StringBuilder systemPromptBuilder = new StringBuilder(SYSTEM_MESSAGE);
        systemPromptBuilder.append("\n\n当前面试岗位: ").append(position);

        if (candidateBackground != null && !candidateBackground.isEmpty()) {
            systemPromptBuilder.append("\n候选人背景: ").append(candidateBackground);
        }

        systemPromptBuilder.append("\n\n请开始面试，首先进行自我介绍并询问候选人是否准备好开始面试。");

        return generateResponse(chatId, systemPromptBuilder.toString(), "面试已开始，请等待AI面试官发言...");
    }

    @GetMapping("/next")
    public SseEmitter nextQuestion(@RequestParam String chatId) {
        // 获取历史消息
        List<Message> existingHistory = chatMemory.get(chatId, Integer.MAX_VALUE);

        // 检查是否已经开始面试
        if (existingHistory.isEmpty()) {
            return generateErrorResponse("请先调用/start接口开始面试");
        }

        String prompt = "请根据之前的对话，提出下一个合适的面试问题或进行相应的评估。";
        return generateResponse(chatId, SYSTEM_MESSAGE, prompt);
    }

    @GetMapping("/evaluate")
    public SseEmitter evaluatePerformance(@RequestParam String chatId) {
        // 获取历史消息
        List<Message> existingHistory = chatMemory.get(chatId, Integer.MAX_VALUE);

        if (existingHistory.isEmpty()) {
            return generateErrorResponse("没有可评估的对话历史");
        }

        String prompt = "请根据整个面试过程，提供对候选人的综合评估，包括技术能力、问题解决能力和沟通能力等方面的评分和反馈。";
        return generateResponse(chatId, SYSTEM_MESSAGE, prompt);
    }

    @PostMapping("/submit-answers")
    public SseEmitter submitAnswer(
            @RequestParam String answer,
            @RequestParam String chatId) {
        return generateResponse(chatId, SYSTEM_MESSAGE, answer);
    }

    @GetMapping("/end")
    public SseEmitter endInterview(@RequestParam String chatId) {
        String prompt = "面试已结束，请总结本次面试并给出最终反馈和建议。";
        return generateResponse(chatId, SYSTEM_MESSAGE, prompt);
    }

    private SseEmitter generateResponse(String chatId, String systemMessage, String userPrompt) {
        // 1. 从知识库中检索相关信息（针对面试问题）
        List<KnowledgeEntry> relatedDocs = knowledgeBaseService.semanticSearch(
                userPrompt, 5, 0.6);

        // 2. 构建系统提示词 + 知识片段
        StringBuilder systemPromptBuilder = new StringBuilder(systemMessage);

        if (!relatedDocs.isEmpty()) {
            systemPromptBuilder.append("\n\n相关面试知识参考:");
            for (int i = 0; i < relatedDocs.size(); i++) {
                systemPromptBuilder.append("\n").append(i + 1).append(". ")
                        .append(relatedDocs.get(i).toVectorDocument().getContent());
            }
        }

        // 3. 获取历史消息
        List<Message> existingHistory = chatMemory.get(chatId, Integer.MAX_VALUE);

        // 4. 构建完整对话上下文
        StringBuilder fullContentBuilder = new StringBuilder(systemPromptBuilder.toString())
                .append("\n\n面试对话历史:\n");

        for (Message msg : existingHistory) {
            if (msg instanceof UserMessage) {
                fullContentBuilder.append("候选人: ").append(msg.getContent()).append("\n");
            } else if (msg instanceof AssistantMessage) {
                fullContentBuilder.append("面试官: ").append(msg.getContent()).append("\n");
            }
        }

        fullContentBuilder.append("候选人: ").append(userPrompt);

        Prompt chatPrompt = new Prompt(fullContentBuilder.toString());

        // 5. 流式回复
        SseEmitter emitter = new SseEmitter(60 * 1000L);
        StringBuilder aiResponseBuilder = new StringBuilder();

        chatClient.prompt(chatPrompt)
                .stream()
                .content()
                .subscribe(
                        content -> {
                            try {
                                String utf8Content = new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                                emitter.send(SseEmitter.event().data(utf8Content));
                                aiResponseBuilder.append(utf8Content);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        () -> {
                            List<Message> messagesToAdd = new ArrayList<>();
                            messagesToAdd.add(new UserMessage(userPrompt));
                            messagesToAdd.add(new AssistantMessage(aiResponseBuilder.toString()));
                            chatMemory.add(chatId, messagesToAdd);
                            emitter.complete();
                        }
                );

        return emitter;
    }

    private SseEmitter generateErrorResponse(String errorMessage) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event().data(errorMessage));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping("/clearSession")
    public String clearSession(@RequestParam String chatId) {
        chatMemory.clear(chatId);
        return "面试会话 " + chatId + " 已清除";
    }
}