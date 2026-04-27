package com.vanilo.psych.agent.tool;


import com.vanilo.psych.agent.dto.DashboardResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.UserRepository;
import com.vanilo.psych.agent.service.PsychologicalService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetDashboardTool implements ToolExecutor{
    private final UserRepository userRepository;
    private final PsychologicalService psychologicalService;

    public GetDashboardTool(UserRepository userRepository, PsychologicalService psychologicalService) {
        this.userRepository = userRepository;
        this.psychologicalService = psychologicalService;
    }

    @Override
    public String getName(){
        return "get_dashboard";
    }
    @Override
    public ToolInfoResponse getToolInfo(){
        return new ToolInfoResponse(
                getName(),
                "获取用户心理报告和风险统计",
                List.of(
                        new ToolParameterInfo("username","string",true,"用户名，由登录态注入"),
                        new ToolParameterInfo("recentLimit","integer",false,"最近报告数量"),
                        new ToolParameterInfo("topRiskLimit","integer",false,"高风险用户数量")
                )
        );
    }
    @Override
    public DashboardResponse execute(Map<String,Object> arguments){
        String username = arguments.get("username") == null ? null : arguments.get("username").toString();
        if(username == null||username.isBlank()){
            throw new RuntimeException("用户未登录！");
        }
        Object recentLimitObject = arguments.get("recentLimit");
        Object topRiskLimitObject = arguments.get("topRiskLimit");
        int recentLimit = recentLimitObject == null ? 20 : ((Number) recentLimitObject).intValue();
        int topRiskLimit = topRiskLimitObject == null ? 10 : ((Number) topRiskLimitObject).intValue();
        User user =userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("用户不存在"));
        return new DashboardResponse(
                psychologicalService.getRecentReports(user.getId(),recentLimit),
                psychologicalService.getTopRiskUsers(topRiskLimit)
        );

    }
}
