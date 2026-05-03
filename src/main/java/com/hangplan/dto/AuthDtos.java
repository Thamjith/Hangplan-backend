package com.hangplan.dto;

import com.hangplan.entity.AuthProvider;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    public enum LocationUpdateMode {
        /** Store latitude and longitude from this request */
        SET,
        /** Remove stored coordinates */
        CLEAR
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank
        private String name;
        /** E.164; omit to leave unchanged, blank string clears */
        @Size(max = 32)
        private String phoneE164;
        private LocationUpdateMode locationUpdate;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private AuthProvider provider;
        /** Plan name, e.g. FREE, PAID_1Y */
        private String subscriptionPlan;
        private LocalDateTime subscriptionEnd;
        /** E.164 when set */
        private String phoneE164;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class AuthResponse {
        private String token;
        private UserDto user;
    }
}
