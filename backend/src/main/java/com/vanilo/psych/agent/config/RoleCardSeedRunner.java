package com.vanilo.psych.agent.config;

import com.vanilo.psych.agent.entity.RoleCard;
import com.vanilo.psych.agent.repository.RoleCardRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RoleCardSeedRunner implements ApplicationRunner {
    private final RoleCardRepository roleCardRepository;

    public RoleCardSeedRunner(RoleCardRepository roleCardRepository) {
        this.roleCardRepository = roleCardRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        presets().forEach(card -> roleCardRepository.findByNameAndPresetTrue(card.getName())
                .orElseGet(() -> roleCardRepository.save(card)));
    }

    private List<RoleCard> presets() {
        return List.of(
                preset("温柔倾听者", "耐心共情与回应", "温柔、接纳、不评判", "先回应感受，再提出一个小建议"),
                preset("理性分析师", "帮助梳理问题和可控因素", "沉稳、清晰、克制", "结构化分析并给出步骤"),
                preset("学业陪伴者", "缓解学习压力与拖延焦虑", "鼓励、务实", "拆分任务并给出短周期行动"),
                preset("睡眠调节助手", "提供睡眠卫生和放松引导", "缓慢、安静、治愈", "简短引导，不制造睡眠压力"),
                preset("情绪急救助手", "情绪过载时帮助稳定当下", "坚定、简洁、安全", "先地面化与安全确认，再讨论原因")
        );
    }

    private RoleCard preset(String name, String description, String tone, String responseStyle) {
        RoleCard card = new RoleCard();
        card.setName(name);
        card.setDescription(description);
        card.setTone(tone);
        card.setResponseStyle(responseStyle);
        card.setCustomInstructions("");
        card.setForbiddenExpressions("禁止羞辱、指责、确定性诊断和夸大风险");
        card.setPreset(true);
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }
}
