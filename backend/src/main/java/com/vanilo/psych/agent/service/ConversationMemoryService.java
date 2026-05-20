package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.UserProfileResponse;
import com.vanilo.psych.agent.entity.ConversationMemory;
import com.vanilo.psych.agent.entity.ConversationMessage;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.entity.UserProfile;
import com.vanilo.psych.agent.repository.ConversationMemoryRepository;
import com.vanilo.psych.agent.repository.ConversationMessageRepository;
import com.vanilo.psych.agent.repository.UserProfileRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    public ConversationMemoryService(ChatClient.Builder chatClientBuilder,
                                     ObjectMapper objectMapper,
                                     UserRepository userRepository,
                                     ConversationMessageRepository conversationMessageRepository,
                                     ConversationMemoryRepository conversationMemoryRepository,
                                     UserProfileRepository userProfileRepository,
                                     UserProfileService userProfileService) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
    }

    public String buildMemoryContext(String username) {
        User user = findUser(username);
        ConversationMemory memory = getOrCreateMemory(user);
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        List<ConversationMessage> recentMessages = conversationMessageRepository.findTop12ByUserOrderByCreatedAtDesc(user);
        Collections.reverse(recentMessages);

        String recentContext = recentMessages.isEmpty()
                ? "暂无最近对话"
                : recentMessages.stream()
                .map(message -> "%s：%s".formatted(toChineseRole(message.getRole()), message.getContent()))
                .collect(Collectors.joining("\n"));

        return """
                【短期记忆：最近对话】
                %s

                【中期记忆：对话摘要】
                %s

                【长期记忆：稳定信息】
                %s

                【用户画像】
                概览：%s
                主要困扰：%s
                偏好：%s
                有效应对方式：%s
                风险信号：%s
                支持目标：%s
                """.formatted(
                recentContext,
                valueOrDefault(memory.getSummary(), "暂无摘要"),
                valueOrDefault(memory.getLongTermMemory(), "暂无长期记忆"),
                valueOrDefault(profile.getProfileSummary(), "暂无稳定画像"),
                valueOrDefault(profile.getConcerns(), "暂无"),
                valueOrDefault(profile.getPreferences(), "暂无"),
                valueOrDefault(profile.getCopingStrategies(), "暂无"),
                valueOrDefault(profile.getRiskSignals(), "暂无"),
                valueOrDefault(profile.getSupportGoals(), "暂无")
        );
    }

    @Transactional
    public void rememberTurn(String username, String userMessage, String assistantReply) {
        User user = findUser(username);
        saveMessage(user, "user", userMessage);
        saveMessage(user, "assistant", assistantReply);
        updateMemoryAndProfile(user, userMessage, assistantReply);
    }

    public UserProfileResponse getProfile(String username) {
        User user = findUser(username);
        return userProfileService.toResponse(userProfileService.getOrCreateProfile(user));
    }

    private void updateMemoryAndProfile(User user, String userMessage, String assistantReply) {
        ConversationMemory memory = getOrCreateMemory(user);
        UserProfile profile = userProfileService.getOrCreateProfile(user);

        String json = chatClient.prompt()
                .system("""
                        你是一个心理支持产品的记忆整理助手。
                        请基于旧记忆、旧画像和最新一轮对话，更新多级对话记忆与用户画像。

                        要求：
                        1. 只返回 JSON，不要 Markdown
                        2. 不要写确定性医学诊断
                        3. 只保留对后续支持有帮助、相对稳定的信息
                        4. 对自伤、自杀等风险信号要谨慎记录，未明确表达时不要夸大
                        5. 每个字段尽量简洁，最多 120 字

                        返回格式：
                        {
                          "summary": "中期对话摘要",
                          "longTermMemory": "长期稳定信息",
                          "profileSummary": "用户画像概览",
                          "concerns": "主要困扰",
                          "preferences": "沟通偏好",
                          "copingStrategies": "有效应对方式",
                          "riskSignals": "风险信号",
                          "supportGoals": "支持目标"
                        }
                        """)
                .user("""
                        旧中期摘要：%s
                        旧长期记忆：%s
                        旧画像概览：%s
                        旧主要困扰：%s
                        旧偏好：%s
                        旧有效应对方式：%s
                        旧风险信号：%s
                        旧支持目标：%s

                        最新用户消息：%s
                        最新助手回复：%s
                        """.formatted(
                        valueOrDefault(memory.getSummary(), "暂无"),
                        valueOrDefault(memory.getLongTermMemory(), "暂无"),
                        valueOrDefault(profile.getProfileSummary(), "暂无"),
                        valueOrDefault(profile.getConcerns(), "暂无"),
                        valueOrDefault(profile.getPreferences(), "暂无"),
                        valueOrDefault(profile.getCopingStrategies(), "暂无"),
                        valueOrDefault(profile.getRiskSignals(), "暂无"),
                        valueOrDefault(profile.getSupportGoals(), "暂无"),
                        userMessage,
                        assistantReply
                ))
                .call()
                .content();

        try {
            Map<String, String> updates = objectMapper.readValue(extractJson(json), new TypeReference<>() {});
            applyMemoryUpdates(memory, profile, updates);
            conversationMemoryRepository.save(memory);
            userProfileRepository.save(profile);
        } catch (Exception e) {
            memory.setSummary(limit(valueOrDefault(memory.getSummary(), "") + "\n用户：" + userMessage, 1000));
            memory.setUpdatedAt(LocalDateTime.now());
            conversationMemoryRepository.save(memory);
        }
    }

    private void saveMessage(User user, String role, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setUser(user);
        message.setRole(role);
        message.setContent(limit(content, 4000));
        message.setCreatedAt(LocalDateTime.now());
        conversationMessageRepository.save(message);
    }

    private void applyMemoryUpdates(ConversationMemory memory, UserProfile profile, Map<String, String> updates) {
        memory.setSummary(limit(updates.get("summary"), 1000));
        memory.setLongTermMemory(limit(updates.get("longTermMemory"), 1000));
        memory.setUpdatedAt(LocalDateTime.now());

        profile.setProfileSummary(limit(updates.get("profileSummary"), 1000));
        profile.setConcerns(limit(updates.get("concerns"), 1000));
        profile.setPreferences(limit(updates.get("preferences"), 1000));
        profile.setCopingStrategies(limit(updates.get("copingStrategies"), 1000));
        profile.setRiskSignals(limit(updates.get("riskSignals"), 1000));
        profile.setSupportGoals(limit(updates.get("supportGoals"), 1000));
        profile.setUpdatedAt(LocalDateTime.now());
    }

    private ConversationMemory getOrCreateMemory(User user) {
        return conversationMemoryRepository.findByUser(user).orElseGet(() -> {
            ConversationMemory memory = new ConversationMemory();
            memory.setUser(user);
            memory.setSummary("暂无摘要");
            memory.setLongTermMemory("暂无长期记忆");
            memory.setUpdatedAt(LocalDateTime.now());
            return conversationMemoryRepository.save(memory);
        });
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在！"));
    }

    private String extractJson(String text) {
        if (text == null) {
            throw new RuntimeException("未返回合法json");
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start == -1 || end == -1 || end < start) {
            throw new RuntimeException("未返回合法json");
        }
        return text.substring(start, end + 1);
    }

    private String toChineseRole(String role) {
        if ("assistant".equals(role)) {
            return "助手";
        }
        return "用户";
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
