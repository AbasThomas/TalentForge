package com.TalentForge.talentforge.user.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.user.dto.UserCreateRequest;
import com.TalentForge.talentforge.user.dto.UserResponse;
import com.TalentForge.talentforge.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User created")
                .data(userService.create(request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("Users fetched")
                .data(userService.getAll())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User fetched")
                .data(userService.getById(id))
                .build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User deactivated")
                .data(userService.deactivate(id))
                .build());
    }
}
