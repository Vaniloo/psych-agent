package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.ReportResponse;
import com.vanilo.psych.agent.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
}
