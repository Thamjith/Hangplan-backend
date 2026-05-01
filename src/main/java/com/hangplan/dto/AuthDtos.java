package com.hangplan.dto;

import com.hangplan.entity.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class SignupRequest {
        @NotBlank
        private String name;
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private AuthProvider provider;
        private boolean isPremium;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private UserDto user;
    }
}
