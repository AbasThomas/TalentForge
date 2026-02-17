package com.TalentForge.talentforge.user.mapper;

import com.TalentForge.talentforge.user.dto.UserResponse;
import com.TalentForge.talentforge.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getSecondaryRole(),
                user.getAvailableRoles(),
                user.getCompany(),
                user.getPhone(),
                user.isActive(),
                user.isVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
