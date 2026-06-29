package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AdminMemoryResponse;
import com.vanilo.psych.agent.dto.ConversationMessageResponse;
import com.vanilo.psych.agent.dto.ConversationSessionResponse;
import com.vanilo.psych.agent.dto.UserProfileResponse;
import com.vanilo.psych.agent.entity.ConversationMemory;
import com.vanilo.psych.agent.entity.ConversationMessage;
import com.vanilo.psych.agent.entity.ConversationSession;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.entity.UserProfile;
import com.vanilo.psych.agent.repository.ConversationMemoryRepository;
import com.vanilo.psych.agent.repository.ConversationMessageRepository;
import com.vanilo.psych.agent.repository.ConversationSessionRepository;
import com.vanilo.psych.agent.repository.UserProfileRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final StringRedisTemplate stringRedisTemplate;

    public ConversationMemoryService(LlmService llmService,
                                     ObjectMapper objectMapper,
                                     UserRepository userRepository,
                                     ConversationMessageRepository conversationMessageRepository,
                                     ConversationSessionRepository conversationSessionRepository,
                                     ConversationMemoryRepository conversationMemoryRepository,
                                     UserProfileRepository userProfileRepository,
                                     UserProfileService userProfileService,
                                     StringRedisTemplate stringRedisTemplate) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationSessionRepository = conversationSessionRepository;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Long resolveSessionId(String username, Long sessionId, String firstMessage) {
        User user = findUser(username);
        return getOrCreateSession(user, sessionId, firstMessage).getId();
    }

    public String buildMemoryContext(String username, Long sessionId) {
        User user = findUser(username);
        ConversationSession session = findSession(user, sessionId);
        ConversationMemory memory = getOrCreateMemory(user);
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        String recentContext = loadRecentContext(user, session);

        return """
                【短期记忆：最近对话】
                %s

                【当前会话摘要】
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
                valueOrDefault(session.getSummary(), "暂无会话摘要"),
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
    public void rememberTurn(String username, Long sessionId, String userMessage, String assistantReply) {
        User user = findUser(username);
        ConversationSession session = findSession(user, sessionId);
        saveMessage(user, session, "user", userMessage);
        saveMessage(user, session, "assistant", assistantReply);
        updateMemoryAndProfile(user, session, userMessage, assistantReply);
    }

    public List<ConversationSessionResponse> listSessions(String username) {
        User user = findUser(username);
        return conversationSessionRepository.findByUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public List<ConversationMessageResponse> listMessages(String username, Long sessionId) {
        User user = findUser(username);
        ConversationSession session = findSession(user, sessionId);
        return conversationMessageRepository.findByUserAndSessionOrderByCreatedAtAsc(user, session)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public List<AdminMemoryResponse> listAdminMemories() {
        return userRepository.findAll()
                .stream()
                .map(this::toAdminMemoryResponse)
                .toList();
    }

    public AdminMemoryResponse getAdminMemory(String username) {
        return toAdminMemoryResponse(findUser(username));
    }

    public List<ConversationMessageResponse> listAdminMessages(String username, Long sessionId) {
        return listMessages(username, sessionId);
    }

    private void updateMemoryAndProfile(User user, ConversationSession session, String userMessage, String assistantReply) {
        ConversationMemory memory = getOrCreateMemory(user);
        UserProfile profile = userProfileService.getOrCreateProfile(user);

        String json = llmService.complete("""
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
                          "sessionSummary": "当前会话摘要",
                          "summary": "中期对话摘要",
                          "longTermMemory": "长期稳定信息",
                          "profileSummary": "用户画像概览",
                          "concerns": "主要困扰",
                          "preferences": "沟通偏好",
                          "copingStrategies": "有效应对方式",
                          "riskSignals": "风险信号",
                          "supportGoals": "支持目标"
                        }
                        """, """
                        当前会话标题：%s
                        旧当前会话摘要：%s
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
                        session.getTitle(),
                        valueOrDefault(session.getSummary(), "暂无"),
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
                ));

        try {
            Map<String, String> updates = objectMapper.readValue(extractJson(json), new TypeReference<>() {});
            applyMemoryUpdates(session, memory, profile, updates);
            conversationSessionRepository.save(session);
            conversationMemoryRepository.save(memory);
            userProfileRepository.save(profile);
        } catch (Exception e) {
            session.setSummary(limit(valueOrDefault(session.getSummary(), "") + "\n用户：" + userMessage, 1000));
            session.setUpdatedAt(LocalDateTime.now());
            memory.setSummary(limit(valueOrDefault(memory.getSummary(), "") + "\n用户：" + userMessage, 1000));
            memory.setUpdatedAt(LocalDateTime.now());
            conversationSessionRepository.save(session);
            conversationMemoryRepository.save(memory);
        }
    }

    private void saveMessage(User user, ConversationSession session, String role, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setUser(user);
        message.setSession(session);
        message.setRole(role);
        message.setContent(limit(content, 4000));
        message.setCreatedAt(LocalDateTime.now());
        conversationMessageRepository.save(message);
        cacheRecentMessage(user, session, role, message.getContent());
        session.setUpdatedAt(message.getCreatedAt());
        conversationSessionRepository.save(session);
    }

    private void applyMemoryUpdates(ConversationSession session, ConversationMemory memory, UserProfile profile, Map<String, String> updates) {
        session.setSummary(limit(updates.get("sessionSummary"), 1000));
        session.setUpdatedAt(LocalDateTime.now());

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

    private String loadRecentContext(User user, ConversationSession session) {
        String key = contextKey(user, session);
        try {
            List<String> cached = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream().map(this::formatCachedMessage).collect(Collectors.joining("\n"));
            }
        } catch (Exception ignored) {
        }
        List<ConversationMessage> recentMessages = conversationMessageRepository
                .findTop12ByUserAndSessionOrderByCreatedAtDesc(user, session);
        Collections.reverse(recentMessages);
        if (recentMessages.isEmpty()) {
            return "暂无最近对话";
        }
        return recentMessages.stream()
                .map(message -> "%s：%s".formatted(toChineseRole(message.getRole()), message.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private void cacheRecentMessage(User user, ConversationSession session, String role, String content) {
        String key = contextKey(user, session);
        try {
            String value = objectMapper.writeValueAsString(Map.of("role", role, "content", content));
            stringRedisTemplate.opsForList().rightPush(key, value);
            stringRedisTemplate.opsForList().trim(key, -12, -1);
            stringRedisTemplate.expire(key, Duration.ofHours(24));
        } catch (Exception ignored) {
        }
    }

    private String formatCachedMessage(String value) {
        try {
            Map<String, String> message = objectMapper.readValue(value, new TypeReference<>() {});
            return "%s：%s".formatted(toChineseRole(message.get("role")), message.getOrDefault("content", ""));
        } catch (Exception e) {
            return value;
        }
    }

    private String contextKey(User user, ConversationSession session) {
        return "context:%d:%d".formatted(user.getId(), session.getId());
    }

    private ConversationSession getOrCreateSession(User user, Long sessionId, String firstMessage) {
        if (sessionId != null) {
            return findSession(user, sessionId);
        }
        ConversationSession session = new ConversationSession();
        LocalDateTime now = LocalDateTime.now();
        session.setUser(user);
        session.setTitle(buildSessionTitle(firstMessage));
        session.setSummary("暂无会话摘要");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return conversationSessionRepository.save(session);
    }

    private ConversationSession findSession(User user, Long sessionId) {
        if (sessionId == null) {
            throw new RuntimeException("sessionId不能为空");
        }
        return conversationSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
    }

    private ConversationSessionResponse toSessionResponse(ConversationSession session) {
        return new ConversationSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getSummary(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ConversationMessageResponse toMessageResponse(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getSession() == null ? null : message.getSession().getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private AdminMemoryResponse toAdminMemoryResponse(User user) {
        ConversationMemory memory = getOrCreateMemory(user);
        UserProfile profile = userProfileService.getOrCreateProfile(user);
        return new AdminMemoryResponse(
                user.getUsername(),
                memory.getSummary(),
                memory.getLongTermMemory(),
                memory.getUpdatedAt(),
                userProfileService.toResponse(profile),
                conversationSessionRepository.findByUserOrderByUpdatedAtDesc(user)
                        .stream()
                        .map(this::toSessionResponse)
                        .toList()
        );
    }

    private String buildSessionTitle(String message) {
        String title = valueOrDefault(message, "新对话").strip().replaceAll("\\s+", " ");
        if (title.length() > 24) {
            return title.substring(0, 24);
        }
        return title;
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
