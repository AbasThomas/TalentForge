package com.TalentForge.talentforge.user.service;

import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.notification.entity.NotificationType;
import com.TalentForge.talentforge.notification.service.NotificationService;
import com.TalentForge.talentforge.user.dto.UserCreateRequest;
import com.TalentForge.talentforge.user.dto.UserResponse;
import com.TalentForge.talentforge.user.dto.UserUpdateRequest;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.mapper.UserMapper;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Override
    public UserResponse create(UserCreateRequest request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role() == null ? UserRole.CANDIDATE : request.role())
                .company(request.company())
                .phone(request.phone())
                .active(true)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Override
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setActive(false);
        User saved = userRepository.save(user);
        notificationService.createForUser(
                saved.getId(),
                NotificationType.SYSTEM,
                "Account suspended",
                "Your TalentForge account has been suspended by an administrator.",
                "/login"
        );
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setActive(true);
        User saved = userRepository.save(user);
        notificationService.createForUser(
                saved.getId(),
                NotificationType.SYSTEM,
                "Account activated",
                "Your TalentForge account has been reactivated by an administrator.",
                "/dashboard"
        );
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse setVerified(Long id, boolean verified) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setVerified(verified);
        User saved = userRepository.save(user);
        notificationService.createForUser(
                saved.getId(),
                NotificationType.SYSTEM,
                verified ? "Account verified" : "Verification removed",
                verified
                        ? "Your TalentForge account is now verified for promotional repost eligibility."
                        : "Your promotional repost verification has been removed.",
                "/dashboard"
        );
        return userMapper.toResponse(saved);
    }

    @Override
    public List<UserResponse> bulkSetVerified(List<Long> userIds, boolean verified) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .distinct()
                .map(userId -> setVerified(userId, verified))
                .toList();
    }

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateCurrentUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));

        user.setFullName(request.fullName().trim());
        user.setCompany(request.company() == null || request.company().isBlank() ? null : request.company().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());

        return userMapper.toResponse(userRepository.save(user));
    }
}
