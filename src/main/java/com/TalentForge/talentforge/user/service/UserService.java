package com.TalentForge.talentforge.user.service;

import com.TalentForge.talentforge.user.dto.UserCreateRequest;
import com.TalentForge.talentforge.user.dto.UserResponse;
import com.TalentForge.talentforge.user.dto.UserUpdateRequest;
import com.TalentForge.talentforge.user.entity.UserRole;

import java.util.List;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    List<UserResponse> getAll();

    UserResponse getById(Long id);

    UserResponse deactivate(Long id);

    UserResponse activate(Long id);

    UserResponse setVerified(Long id, boolean verified);

    List<UserResponse> bulkSetVerified(List<Long> userIds, boolean verified);

    UserResponse switchRole(Long id, UserRole targetRole, boolean addIfMissing);

    UserResponse getCurrentUser(String email);

    UserResponse updateCurrentUser(String email, UserUpdateRequest request);
}
