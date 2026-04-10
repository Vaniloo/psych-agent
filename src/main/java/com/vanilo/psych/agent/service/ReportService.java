package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.ReportResponse;
import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.PsychologicalReportRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {
    private final PsychologicalReportRepository psychologicalReportRepository;
    private final UserRepository userRepository;
    private ReportResponse toReportResponse(PsychologicalReport report) {
        return new ReportResponse(
                report.getId(),
                report.getMessage(),
                report.getRisk(),
                report.getEmotion(),
                report.getConfidence(),
                report.getCreatedAt(),
                report.getUser() != null ? report.getUser().getUsername() : null);
    }
    public ReportService(PsychologicalReportRepository psychologicalReportRepository, UserRepository userRepository) {
        this.psychologicalReportRepository = psychologicalReportRepository;
        this.userRepository = userRepository;
    }
    public List<ReportResponse> listAll(){
        return psychologicalReportRepository.findAll().stream().map(this::toReportResponse).toList();
    }
    public ReportResponse getPsychologicalReportById(long id){
        PsychologicalReport report=psychologicalReportRepository.findById(id).orElseThrow(()->new RuntimeException("报告不存在 id="+id));
    return toReportResponse(report);
    }


    public Page<ReportResponse> listByPage(int page,int size){
        if (page < 0 ){
            page = 0;
        }
        if (size <= 0 ){
            size = 10;
        }
        Pageable pageable=PageRequest.of(page,size, Sort.by("createdAt").descending());
        return psychologicalReportRepository.findAll(pageable).map(this::toReportResponse);
    }
    public List<ReportResponse> listMyReports(String username){
        User user=userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("用户不存在！"));
        return psychologicalReportRepository.findByUser(user).stream().map(this::toReportResponse).toList();
    }

}
