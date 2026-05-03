package com.hangplan.service;

import com.hangplan.dto.AuthDtos;
import com.hangplan.entity.AuthProvider;
import com.hangplan.entity.User;
import com.hangplan.repository.UserRepository;
import com.hangplan.security.HangplanUserPrincipal;
import com.hangplan.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;

    public AuthDtos.AuthResponse signup(AuthDtos.SignupRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .provider(AuthProvider.LOCAL)
                .build();
        subscriptionService.assignFreePlan(user);
        user = userRepository.save(user);
        return toResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getProvider() == AuthProvider.GOOGLE
                && (user.getPassword() == null || user.getPassword().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Use Google to sign in");
        }
        if (user.getPassword() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toResponse(user);
    }

    public AuthDtos.UserDto me(HangplanUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User u = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return toUserDto(u);
    }

    @Transactional
    public AuthDtos.UserDto updateProfile(HangplanUserPrincipal principal, AuthDtos.UpdateProfileRequest req) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User u = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        u.setName(req.getName().trim());

        if (req.getPhoneE164() != null) {
            String phone = req.getPhoneE164();
            if (phone.isBlank()) {
                u.setPhoneE164(null);
            } else {
                String t = phone.trim();
                if (t.length() > 32) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number too long");
                }
                u.setPhoneE164(t);
            }
        }

        if (req.getLocationUpdate() == AuthDtos.LocationUpdateMode.CLEAR) {
            u.setLatitude(null);
            u.setLongitude(null);
        } else if (req.getLocationUpdate() == AuthDtos.LocationUpdateMode.SET) {
            Double lat = req.getLatitude();
            Double lng = req.getLongitude();
            if (lat == null || lng == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude and longitude are required when updating location");
            }
            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
            }
            u.setLatitude(lat);
            u.setLongitude(lng);
        }

        userRepository.save(u);
        return toUserDto(u);
    }

    private AuthDtos.AuthResponse toResponse(User user) {
        return AuthDtos.AuthResponse.builder()
                .token(jwtService.createToken(user))
                .user(toUserDto(user))
                .build();
    }

    public static AuthDtos.UserDto toUserDto(User u) {
        String planName = u.getSubscriptionPlan() != null ? u.getSubscriptionPlan().getName() : "FREE";
        return AuthDtos.UserDto.builder()
                .id(u.getId().toString())
                .name(u.getName())
                .email(u.getEmail())
                .provider(u.getProvider())
                .subscriptionPlan(planName)
                .subscriptionEnd(u.getSubscriptionEnd())
                .phoneE164(u.getPhoneE164())
                .latitude(u.getLatitude())
                .longitude(u.getLongitude())
                .build();
    }
}
