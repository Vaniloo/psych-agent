package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.FeedbackMessageResponse;
import com.vanilo.psych.agent.entity.FeedbackMessage;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.FeedbackMessageRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackMessageRepository feedbackMessageRepository,
                           UserRepository userRepository) {
        this.feedbackMessageRepository = feedbackMessageRepository;
        this.userRepository = userRepository;
    }

    public FeedbackMessageResponse submitFeedback(String username, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("反馈内容不能为空");
        }
        if (content.length() > 2000) {
            throw new RuntimeException("反馈内容不能超过2000字");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        FeedbackMessage feedback = new FeedbackMessage();
        feedback.setUser(user);
        feedback.setContent(content);
        feedback.setStatus("PENDING");
        feedback = feedbackMessageRepository.save(feedback);

        return toResponse(feedback);
    }

    public List<FeedbackMessageResponse> getMyFeedback(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return feedbackMessageRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<FeedbackMessageResponse> getAllFeedback(Pageable pageable) {
        return feedbackMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public Page<FeedbackMessageResponse> getFeedbackByStatus(String status, Pageable pageable) {
        return feedbackMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toResponse);
    }

    public FeedbackMessageResponse getFeedbackById(Long id) {
        FeedbackMessage feedback = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));
        return toResponse(feedback);
    }

    public FeedbackMessageResponse replyFeedback(Long id, String adminUsername, String reply) {
        if (reply == null || reply.isBlank()) {
            throw new RuntimeException("回复内容不能为空");
        }

        FeedbackMessage feedback = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        feedback.setReply(reply);
        feedback.setRepliedBy(admin.getId());
        feedback.setRepliedAt(LocalDateTime.now());
        feedback.setStatus("REPLIED");
        feedback = feedbackMessageRepository.save(feedback);

        return toResponse(feedback);
    }

    public FeedbackMessageResponse closeFeedback(Long id) {
        FeedbackMessage feedback = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("反馈不存在"));

        feedback.setStatus("CLOSED");
        feedback = feedbackMessageRepository.save(feedback);

        return toResponse(feedback);
    }

    public long countByStatus(String status) {
        return feedbackMessageRepository.countByStatus(status);
    }

    private FeedbackMessageResponse toResponse(FeedbackMessage feedback) {
        return new FeedbackMessageResponse(
                feedback.getId(),
                feedback.getUser().getUsername(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getReply(),
                feedback.getRepliedAt(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}