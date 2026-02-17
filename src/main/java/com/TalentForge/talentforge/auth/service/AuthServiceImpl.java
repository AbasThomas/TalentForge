package com.TalentForge.talentforge.auth.service;

import com.TalentForge.talentforge.auth.dto.AuthRequest;
import com.TalentForge.talentforge.auth.dto.AuthResponse;
import com.TalentForge.talentforge.auth.dto.LoginRoleOptionsResponse;
import com.TalentForge.talentforge.auth.dto.RegisterRequest;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.security.JwtService;
import com.TalentForge.talentforge.subscription.entity.PlanType;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import com.TalentForge.talentforge.subscription.repository.SubscriptionRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.mapper.UserMapper;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int FREE_RECRUITER_JOB_POST_LIMIT = 3;
    private static final int FREE_RECRUITER_APPLICANT_LIMIT = 50;
    private static final int FREE_CANDIDATE_APPLICATION_LIMIT = 10;
    private static final int FREE_CANDIDATE_RESUME_SCORE_LIMIT = 20;

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    public AuthResponse register(RegisterRequest request) {
        UserRole requestedRole = request.role() == null ? UserRole.CANDIDATE : request.role();
        User existingUser = userRepository.findByEmail(request.email()).orElse(null);

        if (existingUser != null) {
            if (!passwordEncoder.matches(request.password(), existingUser.getPassword())) {
                throw new BadRequestException("Email is already registered");
            }
            if (existingUser.hasRole(requestedRole)) {
                throw new BadRequestException("This email is already registered for the selected role");
            }
            try {
                existingUser.addRole(requestedRole);
                existingUser.switchToRole(requestedRole);
            } catch (IllegalStateException ex) {
                throw new BadRequestException("Account already has the maximum number of roles");
            }

            existingUser.setFullName(request.fullName().trim());
            if (request.company() != null && !request.company().isBlank()) {
                existingUser.setCompany(request.company().trim());
            }
            if (request.phone() != null && !request.phone().isBlank()) {
                existingUser.setPhone(request.phone().trim());
            }
            existingUser.setActive(true);

            User updated = userRepository.save(existingUser);
            ensureFreeSubscription(updated);
            return buildAuthResponse(updated);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(requestedRole)
                .company(request.company())
                .phone(request.phone())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        ensureFreeSubscription(saved);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticateCredentials(request.email(), request.password());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid login credentials"));

        UserRole loginRole = resolveLoginRole(user, request.role());
        if (user.getRole() != loginRole) {
            user.switchToRole(loginRole);
            user = userRepository.save(user);
        }

        ensureFreeSubscription(user);
        return buildAuthResponse(user);
    }

    @Override
    public LoginRoleOptionsResponse loginRoles(AuthRequest request) {
        authenticateCredentials(request.email(), request.password());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid login credentials"));

        return new LoginRoleOptionsResponse(user.getAvailableRoles());
    }

    @Override
    public AuthResponse switchRole(String email, UserRole role) {
        if (role == null) {
            throw new BadRequestException("Role is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Authenticated user not found"));

        if (!user.hasRole(role)) {
            throw new BadRequestException("Selected role is not linked to this account");
        }

        if (user.getRole() != role) {
            user.switchToRole(role);
            user = userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, userMapper.toResponse(user));
    }

    private void authenticateCredentials(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid login credentials");
        }
    }

    private UserRole resolveLoginRole(User user, UserRole requestedRole) {
        if (requestedRole != null) {
            if (!user.hasRole(requestedRole)) {
                throw new BadRequestException("Selected role is not linked to this account");
            }
            return requestedRole;
        }

        List<UserRole> availableRoles = user.getAvailableRoles();
        if (availableRoles.isEmpty()) {
            throw new BadRequestException("No roles are linked to this account");
        }
        if (availableRoles.size() > 1) {
            throw new BadRequestException("Select a role to continue");
        }
        return availableRoles.get(0);
    }

    private void ensureFreeSubscription(User user) {
        Subscription existing = subscriptionRepository.findByUserId(user.getId()).orElse(null);
        boolean hasRecruiterAccess = user.hasRole(UserRole.RECRUITER) || user.hasRole(UserRole.ADMIN);
        boolean hasCandidateAccess = user.hasRole(UserRole.CANDIDATE);

        if (existing == null) {
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .planType(PlanType.FREE)
                    .startDate(LocalDateTime.now())
                    .endDate(null)
                    .active(true)
                    .jobPostLimit(hasRecruiterAccess ? FREE_RECRUITER_JOB_POST_LIMIT : null)
                    .applicantLimit(hasRecruiterAccess ? FREE_RECRUITER_APPLICANT_LIMIT : null)
                    .applicationLimit(hasCandidateAccess ? FREE_CANDIDATE_APPLICATION_LIMIT : null)
                    .resumeScoreLimit(hasCandidateAccess ? FREE_CANDIDATE_RESUME_SCORE_LIMIT : null)
                    .applicationUsed(0)
                    .resumeScoreUsed(0)
                    .paymentReference("FREE_PLAN")
                    .build();

            subscriptionRepository.save(subscription);
            return;
        }

        boolean changed = false;
        if (hasRecruiterAccess) {
            if (existing.getJobPostLimit() == null) {
                existing.setJobPostLimit(FREE_RECRUITER_JOB_POST_LIMIT);
                changed = true;
            }
            if (existing.getApplicantLimit() == null) {
                existing.setApplicantLimit(FREE_RECRUITER_APPLICANT_LIMIT);
                changed = true;
            }
        }
        if (hasCandidateAccess) {
            if (existing.getApplicationLimit() == null) {
                existing.setApplicationLimit(FREE_CANDIDATE_APPLICATION_LIMIT);
                changed = true;
            }
            if (existing.getResumeScoreLimit() == null) {
                existing.setResumeScoreLimit(FREE_CANDIDATE_RESUME_SCORE_LIMIT);
                changed = true;
            }
            if (existing.getApplicationUsed() == null) {
                existing.setApplicationUsed(0);
                changed = true;
            }
            if (existing.getResumeScoreUsed() == null) {
                existing.setResumeScoreUsed(0);
                changed = true;
            }
        }
        if (changed) {
            subscriptionRepository.save(existing);
        }
    }
}
