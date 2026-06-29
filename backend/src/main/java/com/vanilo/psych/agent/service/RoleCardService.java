package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.RoleCardRequest;
import com.vanilo.psych.agent.dto.RoleCardResponse;
import com.vanilo.psych.agent.entity.RoleCard;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.RoleCardRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoleCardService {
    private final RoleCardRepository roleCardRepository;
    private final UserRepository userRepository;

    public RoleCardService(RoleCardRepository roleCardRepository, UserRepository userRepository) {
        this.roleCardRepository = roleCardRepository;
        this.userRepository = userRepository;
    }

    public List<RoleCardResponse> listAvailable(String username) {
        User user = findUser(username);
        List<RoleCard> cards = new ArrayList<>(roleCardRepository.findByPresetTrueOrderByIdAsc());
        cards.addAll(roleCardRepository.findByOwnerOrderByCreatedAtDesc(user));
        return cards.stream().map(card -> toResponse(card, user)).toList();
    }

    @Transactional
    public RoleCardResponse create(String username, RoleCardRequest request) {
        validate(request);
        User user = findUser(username);
        RoleCard card = new RoleCard();
        card.setOwner(user);
        card.setName(request.getName().strip());
        card.setDescription(valueOrDefault(request.getDescription(), "自定义陪伴角色"));
        card.setTone(valueOrDefault(request.getTone(), "温和"));
        card.setResponseStyle(valueOrDefault(request.getResponseStyle(), "简洁直接"));
        card.setCustomInstructions(valueOrDefault(request.getCustomInstructions(), ""));
        card.setForbiddenExpressions(valueOrDefault(request.getForbiddenExpressions(), ""));
        card.setPreset(false);
        card.setCreatedAt(LocalDateTime.now());
        return toResponse(roleCardRepository.save(card), user);
    }

    @Transactional
    public RoleCardResponse activate(String username, Long roleCardId) {
        User user = findUser(username);
        RoleCard card = roleCardRepository.findById(roleCardId)
                .orElseThrow(() -> new RuntimeException("角色卡不存在"));
        if (!card.isPreset() && (card.getOwner() == null || !card.getOwner().getId().equals(user.getId()))) {
            throw new RuntimeException("无权使用该角色卡");
        }
        user.setActiveRoleCard(card);
        userRepository.save(user);
        return toResponse(card, user);
    }

    public String buildRolePrompt(String username) {
        User user = findUser(username);
        RoleCard card = user.getActiveRoleCard();
        if (card == null) {
            return "使用默认的温和、自然、谨慎的心理支持风格。";
        }
        return """
                当前角色卡：%s
                角色说明：%s
                语气：%s
                建议方式：%s
                用户自定义要求：%s
                禁忌表达：%s

                角色卡只能改变表达风格，不能绕过风险识别、安全兜底、禁止诊断等底层规则。
                """.formatted(
                card.getName(),
                card.getDescription(),
                card.getTone(),
                card.getResponseStyle(),
                valueOrDefault(card.getCustomInstructions(), "无"),
                valueOrDefault(card.getForbiddenExpressions(), "无")
        );
    }

    private RoleCardResponse toResponse(RoleCard card, User user) {
        return new RoleCardResponse(
                card.getId(),
                card.getName(),
                card.getDescription(),
                card.getTone(),
                card.getResponseStyle(),
                card.getCustomInstructions(),
                card.getForbiddenExpressions(),
                card.isPreset(),
                user.getActiveRoleCard() != null && user.getActiveRoleCard().getId().equals(card.getId())
        );
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private void validate(RoleCardRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("角色卡名称不能为空");
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }
}
