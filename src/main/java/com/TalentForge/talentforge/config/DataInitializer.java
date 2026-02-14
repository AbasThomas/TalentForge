package com.TalentForge.talentforge.config;

import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdminUser() {
        return args -> userRepository.findByEmail("admin@talentforge.local").orElseGet(() -> {
            User admin = User.builder()
                    .email("admin@talentforge.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("TalentForge Admin")
                    .role(UserRole.ADMIN)
                    .active(true)
                    .build();
            return userRepository.save(admin);
        });
    }
}
