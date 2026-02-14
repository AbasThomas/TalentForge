package com.TalentForge.talentforge.auth.service;

import com.TalentForge.talentforge.auth.dto.AuthRequest;
import com.TalentForge.talentforge.auth.dto.AuthResponse;
import com.TalentForge.talentforge.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}
