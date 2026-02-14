package com.TalentForge.talentforge.user.service;

import com.TalentForge.talentforge.user.dto.UserCreateRequest;
import com.TalentForge.talentforge.user.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    List<UserResponse> getAll();

    UserResponse getById(Long id);

    UserResponse deactivate(Long id);
}
