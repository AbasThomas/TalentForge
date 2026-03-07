package com.TalentForge.talentforge.auth.controller;

import com.TalentForge.talentforge.auth.dto.AuthRequest;
import com.TalentForge.talentforge.auth.dto.AuthResponse;
import com.TalentForge.talentforge.auth.dto.LoginRoleOptionsResponse;
import com.TalentForge.talentforge.auth.dto.OtpRequest;
import com.TalentForge.talentforge.auth.dto.RegisterRequest;
import com.TalentForge.talentforge.auth.dto.RoleSelectionRequest;
import com.TalentForge.talentforge.auth.service.AuthService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request.email());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Verification code sent")
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Registration successful")
                .data(response)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
    }

    @PostMapping("/login/roles")
    public ResponseEntity<ApiResponse<LoginRoleOptionsResponse>> loginRoles(@Valid @RequestBody AuthRequest request) {
        LoginRoleOptionsResponse response = authService.loginRoles(request);
        return ResponseEntity.ok(ApiResponse.<LoginRoleOptionsResponse>builder()
                .success(true)
                .message("Available roles fetched")
                .data(response)
                .build());
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<ApiResponse<LoginRoleOptionsResponse>> loginInitiate(@Valid @RequestBody AuthRequest request) {
        return loginRoles(request);
    }

    @PostMapping("/switch-role")
    public ResponseEntity<ApiResponse<AuthResponse>> switchRole(
            Authentication authentication,
            @Valid @RequestBody RoleSelectionRequest request
    ) {
        AuthResponse response = authService.switchRole(authentication.getName(), request.role());
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Role switched successfully")
                .data(response)
                .build());
    }
}
