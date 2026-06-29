package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.HelpResourceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrisisSupportService {
    private static final String CRISIS_REPLY = """
            我很担心你现在的安全。请先不要独处，也不要靠近可能伤害自己的物品或地点。

            现在先做三件事：
            1. 联系一位你信任的人，请对方立刻陪着你；
            2. 如果你有马上行动的冲动，请拨打 120 或 110，或直接前往最近的急诊；
            3. 你也可以拨打全国统一心理援助热线 12356，获得专业支持。

            你不需要一个人扛过去。请告诉我：你现在是否已经准备了伤害自己的具体方式或物品？
            """;

    public String crisisReply() {
        return CRISIS_REPLY;
    }

    public String helpCenterUrl() {
        return "/help/crisis";
    }

    public List<HelpResourceResponse> resources() {
        return List.of(
                new HelpResourceResponse(
                        "全国统一心理援助热线",
                        "phone",
                        "12356",
                        "心理健康教育、心理疏导与心理危机干预。各地服务时段可能不同。",
                        false
                ),
                new HelpResourceResponse(
                        "医疗急救",
                        "phone",
                        "120",
                        "存在立即人身危险、已经自伤或服用危险物品时，请立即拨打。",
                        true
                ),
                new HelpResourceResponse(
                        "公安报警",
                        "phone",
                        "110",
                        "无法确保自身或他人安全时，请立即联系。",
                        true
                )
        );
    }
}
