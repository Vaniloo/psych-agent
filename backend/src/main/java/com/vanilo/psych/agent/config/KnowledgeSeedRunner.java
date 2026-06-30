package com.vanilo.psych.agent.config;

import com.vanilo.psych.agent.dto.KnowledgeAddRequest;
import com.vanilo.psych.agent.service.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KnowledgeSeedRunner implements ApplicationRunner {
    private static final String SOURCE = "seed:psych-faq:v1";
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSeedRunner.class);

    private final KnowledgeService knowledgeService;
    private final boolean enabled;

    public KnowledgeSeedRunner(KnowledgeService knowledgeService,
                               @Value("${psych.seed-knowledge.enabled:true}") boolean enabled) {
        this.knowledgeService = knowledgeService;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        List<KnowledgeAddRequest> seeds = seedItems();
        try {
            Set<String> existingContents = knowledgeService.listAllDocuments().stream()
                    .filter(document -> SOURCE.equals(document.getSource()))
                    .map(document -> document.getContent().strip())
                    .collect(Collectors.toSet());
            if (existingContents.size() >= seeds.size()) {
                return;
            }
            knowledgeService.reindexBySource(SOURCE);
            seeds.stream()
                    .filter(seed -> !existingContents.contains(seed.getContent()))
                    .forEach(knowledgeService::addKnowledge);
        } catch (RuntimeException exception) {
            log.warn("Psychology knowledge seed is temporarily unavailable; startup will continue: {}",
                    exception.getMessage());
        }
    }

    private List<KnowledgeAddRequest> seedItems() {
        return List.of(
                faq("anxiety", """
                        问：突然焦虑、心慌的时候可以怎么做？
                        答：先把注意力放回当下，尝试 4-6 呼吸：吸气 4 秒，呼气 6 秒，重复 3 到 5 分钟。然后说出你看到的 5 样东西、摸到的 4 样东西、听到的 3 种声音，帮助身体从警觉状态回到现实环境。如果胸痛、晕厥或症状持续加重，需要及时就医。
                        """),
                faq("anxiety", """
                        问：总是担心还没发生的事，怎么缓解？
                        答：可以把担心写成两列：我能控制的、我暂时不能控制的。对能控制的部分写下一个最小行动，例如发一封邮件、整理材料、休息十分钟；对不能控制的部分，提醒自己“我现在先不解决它”。这不是逃避，而是把大脑从反复预演中拉回可执行步骤。
                        """),
                faq("stress", """
                        问：学习或工作压力很大，感觉快撑不住了怎么办？
                        答：先做压力分层：必须今天完成、可以延后、可以求助。只保留 1 到 3 个今天真正要做的任务，并把每个任务拆成 25 分钟内可完成的小块。压力很高时，不要用“彻底改变生活”作为目标，先恢复睡眠、饮食和基本节奏。
                        """),
                faq("sleep", """
                        问：失眠时越想睡越睡不着怎么办？
                        答：不要在床上和睡眠较劲。如果躺了二十分钟仍很清醒，可以离开床，到光线较暗的地方做单调、安静的事，困意回来再上床。睡前减少刷手机和激烈讨论，固定起床时间通常比强迫自己早睡更有效。
                        """),
                faq("mood", """
                        问：最近情绪低落，对什么都提不起兴趣怎么办？
                        答：低落时先不要要求自己立刻变积极。可以从低成本行为激活开始：洗澡、出门走十分钟、吃一顿简单的饭、联系一个安全的人。若低落持续两周以上，明显影响学习工作，或伴随自伤念头，建议尽快寻求专业心理咨询或精神科评估。
                        """),
                faq("relationship", """
                        问：和家人或朋友沟通总是吵起来怎么办？
                        答：可以使用“我感受-我需要-我请求”的表达方式，例如“我听到这句话会很紧张，我需要一点时间整理，我希望我们十分钟后再聊”。尽量少用“你总是”“你从来不”，这类句式容易让对方进入防御状态。
                        """),
                faq("self_esteem", """
                        问：总觉得自己不够好怎么办？
                        答：把“我不够好”改写成更具体的问题：我在哪件事上遇到了困难？我缺少什么资源？我下一步能练什么？自我否定常常很笼统，具体化之后才可能行动。也可以每天记录一个完成的小事，训练大脑看见证据而不是只看缺口。
                        """),
                faq("panic", """
                        问：惊恐发作会不会有危险？
                        答：惊恐发作通常会让人感觉心跳很快、喘不过气、快要失控，但它本身多半会在一段时间后下降。可以提醒自己“这是身体警报，不等于真实危险”，同时做缓慢呼气和地面化练习。如果是第一次出现、伴随强烈胸痛或身体疾病风险，应优先做医学检查。
                        """),
                faq("crisis", """
                        问：如果我出现自伤或轻生念头，该怎么办？
                        答：请立刻把自己从危险物品或危险地点旁边移开，联系一个可信任的人陪你，不要独自硬扛。如果有马上行动的冲动，请立即拨打当地紧急电话或前往急诊。你不需要先证明自己“足够严重”才可以求助。
                        """),
                faq("help_seeking", """
                        问：什么时候需要找专业帮助？
                        答：当情绪困扰持续两周以上、睡眠食欲明显变化、学习工作受影响、人际关系严重受损，或出现自伤自杀想法时，建议寻求专业心理咨询、学校心理中心或精神科帮助。寻求帮助不是失败，而是为自己增加支持资源。
                        """),
                faq("grounding", """
                        问：什么是 5-4-3-2-1 地面化练习？
                        答：它是一种把注意力带回当下的方法：说出 5 个看到的东西、4 个摸到的东西、3 个听到的声音、2 个闻到的气味、1 个尝到的味道。适合焦虑、解离感、情绪过载时使用。
                        """),
                faq("boundary", """
                        问：如何建立边界又不伤害关系？
                        答：边界不是惩罚别人，而是说明自己能承受什么。可以用清晰温和的句子：“我愿意听你说，但我现在不能继续被大声指责；如果声音变大，我会先离开十分钟。”边界要具体、可执行，并且前后一致。
                        """)
        );
    }

    private KnowledgeAddRequest faq(String category, String content) {
        return new KnowledgeAddRequest(content.strip(), category, SOURCE);
    }
}
