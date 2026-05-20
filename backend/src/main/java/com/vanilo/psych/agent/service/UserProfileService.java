package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.UserProfileResponse;
import com.vanilo.psych.agent.dto.UserProfileUpdateRequest;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.entity.UserProfile;
import com.vanilo.psych.agent.repository.UserProfileRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfileResponse getProfile(String username) {
        User user = findUser(username);
        UserProfile profile = getOrCreateProfile(user);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UserProfileUpdateRequest request) {
        User user = findUser(username);
        UserProfile profile = getOrCreateProfile(user);
        if (request.getProfileSummary() != null) {
            profile.setProfileSummary(request.getProfileSummary());
        }
        if (request.getConcerns() != null) {
            profile.setConcerns(request.getConcerns());
        }
        if (request.getPreferences() != null) {
            profile.setPreferences(request.getPreferences());
        }
        if (request.getCopingStrategies() != null) {
            profile.setCopingStrategies(request.getCopingStrategies());
        }
        if (request.getRiskSignals() != null) {
            profile.setRiskSignals(request.getRiskSignals());
        }
        if (request.getSupportGoals() != null) {
            profile.setSupportGoals(request.getSupportGoals());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        return toResponse(userProfileRepository.save(profile));
    }

    UserProfile getOrCreateProfile(User user) {
        return userProfileRepository.findByUser(user).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setProfileSummary("暂无稳定画像");
            profile.setConcerns("暂无");
            profile.setPreferences("暂无");
            profile.setCopingStrategies("暂无");
            profile.setRiskSignals("暂无");
            profile.setSupportGoals("暂无");
            profile.setUpdatedAt(LocalDateTime.now());
            return userProfileRepository.save(profile);
        });
    }

    UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getProfileSummary(),
                profile.getConcerns(),
                profile.getPreferences(),
                profile.getCopingStrategies(),
                profile.getRiskSignals(),
                profile.getSupportGoals(),
                profile.getUpdatedAt()
        );
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在！"));
    }
}
