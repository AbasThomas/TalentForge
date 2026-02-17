package com.TalentForge.talentforge.auth.service;

import com.TalentForge.talentforge.auth.dto.AuthRequest;
import com.TalentForge.talentforge.auth.dto.AuthResponse;
import com.TalentForge.talentforge.auth.dto.LoginRoleOptionsResponse;
import com.TalentForge.talentforge.auth.dto.RegisterRequest;
import com.TalentForge.talentforge.user.entity.UserRole;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);

    LoginRoleOptionsResponse loginRoles(AuthRequest request);

    AuthResponse switchRole(String email, UserRole role);
}
