package com.hangplan.controller;

import com.hangplan.dto.AuthDtos;
import com.hangplan.service.AuthService;
import com.hangplan.security.HangplanUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public AuthDtos.AuthResponse signup(@Valid @RequestBody AuthDtos.SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.UserDto me(@AuthenticationPrincipal HangplanUserPrincipal principal) {
        return authService.me(principal);
    }

    @PatchMapping("/me")
    public AuthDtos.UserDto updateMe(
            @AuthenticationPrincipal HangplanUserPrincipal principal,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request
    ) {
        return authService.updateProfile(principal, request);
    }
}
