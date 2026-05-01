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
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        User u = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return toUserDto(u);
    }

    private AuthDtos.AuthResponse toResponse(User user) {
        return AuthDtos.AuthResponse.builder()
                .token(jwtService.createToken(user))
                .user(toUserDto(user))
                .build();
    }

    public static AuthDtos.UserDto toUserDto(User u) {
        return AuthDtos.UserDto.builder()
                .id(u.getId().toString())
                .name(u.getName())
                .email(u.getEmail())
                .provider(u.getProvider())
                .premium(u.isPremium())
                .build();
    }
}
