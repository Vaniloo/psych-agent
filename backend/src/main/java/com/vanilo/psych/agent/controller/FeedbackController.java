package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.FeedbackMessageRequest;
import com.vanilo.psych.agent.dto.FeedbackMessageResponse;
import com.vanilo.psych.agent.dto.FeedbackReplyRequest;
import com.vanilo.psych.agent.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public FeedbackMessageResponse submitFeedback(@RequestBody FeedbackMessageRequest request,
                                                  Authentication authentication) {
        String username = authentication.getName();
        return feedbackService.submitFeedback(username, request.content());
    }

    @GetMapping("/my")
    public List<FeedbackMessageResponse> getMyFeedback(Authentication authentication) {
        String username = authentication.getName();
        return feedbackService.getMyFeedback(username);
    }

    @GetMapping
    public Page<FeedbackMessageResponse> getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return feedbackService.getAllFeedback(pageable);
    }

    @GetMapping("/status/{status}")
    public Page<FeedbackMessageResponse> getFeedbackByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return feedbackService.getFeedbackByStatus(status, pageable);
    }

    @GetMapping("/{id}")
    public FeedbackMessageResponse getFeedbackById(@PathVariable Long id) {
        return feedbackService.getFeedbackById(id);
    }

    @PostMapping("/{id}/reply")
    public FeedbackMessageResponse replyFeedback(@PathVariable Long id,
                                                 @RequestBody FeedbackReplyRequest request,
                                                 Authentication authentication) {
        String adminUsername = authentication.getName();
        return feedbackService.replyFeedback(id, adminUsername, request.reply());
    }

    @PostMapping("/{id}/close")
    public FeedbackMessageResponse closeFeedback(@PathVariable Long id) {
        return feedbackService.closeFeedback(id);
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return Map.of(
                "pending", feedbackService.countByStatus("PENDING"),
                "replied", feedbackService.countByStatus("REPLIED"),
                "closed", feedbackService.countByStatus("CLOSED")
        );
    }
}