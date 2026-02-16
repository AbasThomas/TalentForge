package com.TalentForge.talentforge.auth.service;

import com.TalentForge.talentforge.auth.dto.AuthRequest;
import com.TalentForge.talentforge.auth.dto.AuthResponse;
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

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role() == null ? UserRole.CANDIDATE : request.role())
                .company(request.company())
                .phone(request.phone())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        Integer freeJobPostLimit = (saved.getRole() == UserRole.RECRUITER || saved.getRole() == UserRole.ADMIN) ? 3 : null;
        Integer freeApplicantLimit = (saved.getRole() == UserRole.RECRUITER || saved.getRole() == UserRole.ADMIN) ? 50 : null;
        Integer freeApplicationLimit = saved.getRole() == UserRole.CANDIDATE ? 10 : null;
        Integer freeResumeScoreLimit = saved.getRole() == UserRole.CANDIDATE ? 20 : null;

        Subscription subscription = Subscription.builder()
                .user(saved)
                .planType(PlanType.FREE)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .active(true)
                .jobPostLimit(freeJobPostLimit)
                .applicantLimit(freeApplicantLimit)
                .applicationLimit(freeApplicationLimit)
                .resumeScoreLimit(freeResumeScoreLimit)
                .applicationUsed(0)
                .resumeScoreUsed(0)
                .paymentReference("FREE_PLAN")
                .build();

        subscriptionRepository.save(subscription);

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, userMapper.toResponse(saved));
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid login credentials"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, userMapper.toResponse(user));
    }
}
