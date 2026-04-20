package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.DashboardResponse;
import com.vanilo.psych.agent.dto.ReportResponse;
import com.vanilo.psych.agent.dto.TopRiskUserResponse;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.PsychologicalReportRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import com.vanilo.psych.agent.service.PsychologicalService;
import com.vanilo.psych.agent.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;
    private final PsychologicalService psychologicalService;
    private final PsychologicalReportRepository psychologicalReportRepository;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, PsychologicalService psychologicalService, PsychologicalReportRepository psychologicalReportRepository, UserRepository userRepository) {
        this.reportService = reportService;
        this.psychologicalService = psychologicalService;
        this.psychologicalReportRepository = psychologicalReportRepository;
        this.userRepository = userRepository;
    }


    @GetMapping
    public List<ReportResponse> getPsychologicalReports(){
        return reportService.listAll();
    }
    @GetMapping("/page")
    public Page<ReportResponse> getReportByPage
            (@RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size){
        return reportService.listByPage(page,size);
    }
    @GetMapping("/my")
    public List<ReportResponse> getMyReports(Authentication authentication){
        String username =authentication.getName();
        return reportService.listMyReports(username);
    }
    @GetMapping("/{id}")
    public ReportResponse getPsychologicalReport(@PathVariable Long id){
        return reportService.getPsychologicalReportById(id);
    }
    @GetMapping("/top-risk-users")
    public List<TopRiskUserResponse> getTopRiskUsers(@RequestParam(value = "limit",required = false)Integer limit){
        return psychologicalService.getTopRiskUsers(limit==null?10:limit);
    }
    @GetMapping("/dashboard")
    public DashboardResponse getDashboardReports(Authentication authentication, @RequestParam(value="recentLimit",required = false)Integer recentLimit, @RequestParam(value="topRiskLimit",required = false)Integer topRiskLimit){
        if(authentication==null){
            throw new RuntimeException("Did not pass authentication");
        }
        String username =authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("User not found"));
        Long userId = user.getId();
        return new DashboardResponse(
                psychologicalService.getRecentReports(userId,recentLimit==null?10:recentLimit),
                psychologicalService.getTopRiskUsers(topRiskLimit==null?10:topRiskLimit)
        );
    }
}
